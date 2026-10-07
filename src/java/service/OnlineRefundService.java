package service;
import config.DBConnect;import dao.Database;import java.sql.*;import java.util.*;import java.math.*;import java.net.*;import java.net.http.*;import java.time.*;import java.time.format.DateTimeFormatter;
/** A refund is submitted once. Unknown outcomes remain pending for reconciliation. */
public class OnlineRefundService {
 public void execute(long admin,long tid,String ip)throws Exception{
  new VnpayGateway().requireConfigured();Map<String,Object> refund,payment;
  try(Connection c=new DBConnect().getConnection()){c.setAutoCommit(false);try{
   FulfillmentService.role(c,admin,"ADMIN");Map<String,Object> ref=Database.one(c,"SELECT order_id FROM transactions WHERE id=? AND type='REFUND'",tid);if(ref==null)throw new IllegalArgumentException("Refund not found.");Database.one(c,"SELECT id FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?",ref.get("order_id"));
   refund=Database.one(c,"SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND type='REFUND' AND status='APPROVED' AND method IN ('CARD','E_WALLET')",tid);
   if(refund==null)throw new IllegalArgumentException("Refund is not approved, or has already been submitted. Pending refunds require reconciliation.");
   payment=Database.one(c,"SELECT * FROM transactions WHERE id=? AND status='PAID' AND provider='VNPAY'",refund.get("parent_transaction_id"));if(payment==null || payment.get("provider_create_date")==null)throw new IllegalArgumentException("Original gateway payment metadata is missing.");
   Database.update(c,"UPDATE transactions SET status='PENDING',updated_at=SYSUTCDATETIME() WHERE id=?",tid);Database.update(c,"INSERT audit_logs(actor_id,entity_type,entity_id,action) VALUES(?,'Refund',?,'SUBMITTED')",admin,tid);c.commit();
  }catch(Exception e){c.rollback();throw e;}}
  String tmn=System.getenv("FRESHORA_VNPAY_TMN_CODE"),secret=System.getenv("FRESHORA_VNPAY_HASH_SECRET");
  Map<String,String> p=new LinkedHashMap<>();p.put("vnp_RequestId","FRREF"+tid);p.put("vnp_Version","2.1.0");p.put("vnp_Command","refund");p.put("vnp_TmnCode",tmn);p.put("vnp_TransactionType",((BigDecimal)refund.get("amount")).compareTo((BigDecimal)payment.get("amount"))==0?"02":"03");p.put("vnp_TxnRef",payment.get("id").toString());p.put("vnp_Amount",((BigDecimal)refund.get("amount")).movePointRight(2).toBigIntegerExact().toString());p.put("vnp_TransactionNo",payment.get("gateway_ref").toString());p.put("vnp_TransactionDate",payment.get("provider_create_date").toString());p.put("vnp_CreateBy",Long.toString(admin));p.put("vnp_CreateDate",DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(Instant.now()));p.put("vnp_IpAddr",ip);p.put("vnp_OrderInfo","Freshora refund "+tid);
  p.put("vnp_SecureHash",VnpayGateway.hash(secret,String.join("|",p.values())));
  StringJoiner json=new StringJoiner(",","{","}");p.forEach((k,v)->json.add("\""+k+"\":\""+v.replace("\\","\\\\").replace("\"","\\\"")+"\""));
  HttpResponse<String> response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build().send(HttpRequest.newBuilder(URI.create("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction")).timeout(Duration.ofSeconds(20)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.toString())).build(),HttpResponse.BodyHandlers.ofString());
  if(response.statusCode()!=200 || response.body().length()>65536)throw new IllegalArgumentException("Gateway outcome is unknown. Refund remains pending for reconciliation.");
  confirm(admin,tid,response.body());
 }
 public void confirm(long admin,long tid,String signedResponse)throws Exception{
  new VnpayGateway().requireConfigured();
  if(signedResponse==null || signedResponse.length()>65536)throw new IllegalArgumentException("Invalid signed response.");
  String tmn=System.getenv("FRESHORA_VNPAY_TMN_CODE"),secret=System.getenv("FRESHORA_VNPAY_HASH_SECRET");
  try(Connection c=new DBConnect().getConnection()){
   FulfillmentService.role(c,admin,"ADMIN");
   Map<String,Object> refund=Database.one(c,"SELECT * FROM transactions WHERE id=? AND type='REFUND' AND method IN ('CARD','E_WALLET') AND status IN ('PENDING','REFUNDED')",tid);
   if(refund==null)throw new IllegalArgumentException("Online refund is not pending.");
   Map<String,Object> payment=Database.one(c,"SELECT * FROM transactions WHERE id=?",refund.get("parent_transaction_id"));
   Map<String,String> result=new HashMap<>();for(Map<String,Object> row:Database.rows(c,"SELECT [key],value FROM OPENJSON(?)",signedResponse)){String key=(String)row.get("key");if(result.put(key,(String)row.get("value"))!=null)throw new IllegalArgumentException("Invalid gateway result.");}
   String[] fields={"vnp_ResponseId","vnp_Command","vnp_ResponseCode","vnp_Message","vnp_TmnCode","vnp_TxnRef","vnp_Amount","vnp_BankCode","vnp_PayDate","vnp_TransactionNo","vnp_TransactionType","vnp_TransactionStatus","vnp_OrderInfo"};StringJoiner signed=new StringJoiner("|");for(String f:fields)signed.add(result.getOrDefault(f,""));
   String signature=result.get("vnp_SecureHash");if(signature==null || !java.security.MessageDigest.isEqual(VnpayGateway.hash(secret,signed.toString()).getBytes(java.nio.charset.StandardCharsets.US_ASCII),signature.toLowerCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII)) || !tmn.equals(result.get("vnp_TmnCode")) || !"refund".equals(result.get("vnp_Command")) || !payment.get("id").toString().equals(result.get("vnp_TxnRef")) || !((BigDecimal)refund.get("amount")).movePointRight(2).toBigIntegerExact().toString().equals(result.get("vnp_Amount")) || !Set.of("02","03").contains(result.getOrDefault("vnp_TransactionType","")) || !("Freshora refund "+tid).equals(result.get("vnp_OrderInfo")))throw new IllegalArgumentException("Gateway result failed verification. Refund remains pending.");
   if(!"00".equals(result.get("vnp_ResponseCode")) || !"00".equals(result.get("vnp_TransactionStatus")))throw new IllegalArgumentException("VNPay has not confirmed completion. Refund remains pending.");
   String gateway=result.get("vnp_TransactionNo");if(gateway==null || !gateway.matches("[0-9]{1,15}"))throw new IllegalArgumentException("Gateway refund reference is missing.");
   c.setAutoCommit(false);try{
    Database.one(c,"SELECT id FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?",refund.get("order_id"));Map<String,Object> locked=Database.one(c,"SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE id=?",tid);
    if("PENDING".equals(locked.get("status")))FinanceService.complete(c,locked,null,gateway);c.commit();
   }catch(Exception e){c.rollback();throw e;}
  }
 }
}
