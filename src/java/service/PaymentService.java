package service;
import config.DBConnect;import dao.Database;import java.sql.*;import java.util.*;import java.math.*;
public class PaymentService {
 public String start(long uid,long oid,String ip)throws Exception{
  VnpayGateway gateway=new VnpayGateway();gateway.requireConfigured();
  try(Connection c=new DBConnect().getConnection()){c.setAutoCommit(false);try{
   Map<String,Object> o=Database.one(c,"SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND customer_id=? AND status='AWAITING_PAYMENT' AND payment_method IN ('CARD','E_WALLET') AND payment_expires_at>SYSUTCDATETIME()",oid,uid);
   if(o==null)throw new IllegalArgumentException("Order is not available for online payment.");
   Map<String,Object> t=Database.one(c,"SELECT TOP 1 id FROM transactions WHERE order_id=? AND type='PAYMENT' AND status='INITIATED' ORDER BY id DESC",oid);
   long ref=t==null?Database.insert(c,"INSERT transactions(type,user_id,order_id,method,amount,status,provider) VALUES('PAYMENT',?,?,?,?,'INITIATED','VNPAY')",uid,oid,o.get("payment_method"),o.get("payable_amount")):FulfillmentService.id(t.get("id"));
   String created=t==null?null:(String)Database.one(c,"SELECT provider_create_date FROM transactions WHERE id=?",ref).get("provider_create_date");
   if(created==null){created=java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(java.time.Instant.now());Database.update(c,"UPDATE transactions SET provider_create_date=? WHERE id=?",created,ref);}
   String url=gateway.payment(ref,(BigDecimal)o.get("payable_amount"),(String)o.get("payment_method"),ip,(Timestamp)o.get("payment_expires_at"),created);c.commit();return url;
  }catch(Exception e){c.rollback();throw e;}}
 }
 public String callback(Map<String,String> p)throws Exception{
  if(!new VnpayGateway().verify(p)){try(Connection log=new DBConnect().getConnection()){Database.update(log,"INSERT integration_events(provider,event_key,event_type,status,error_message) VALUES('VNPAY',?,'PAYMENT_IPN','REJECTED','Signature or merchant validation failed')",UUID.randomUUID().toString());}return "97";}
  long ref;BigDecimal amount;try{ref=Long.parseLong(p.get("vnp_TxnRef"));amount=new BigDecimal(p.get("vnp_Amount")).movePointLeft(2);}catch(Exception e){return "01";}
  String gatewayRef=p.get("vnp_TransactionNo");if(gatewayRef==null || !gatewayRef.matches("[0-9]{1,15}"))return "99";
  try(Connection c=new DBConnect().getConnection()){c.setAutoCommit(false);try{
   Map<String,Object> pointer=Database.one(c,"SELECT order_id FROM transactions WHERE id=? AND type='PAYMENT' AND provider='VNPAY'",ref);if(pointer==null){c.rollback();return "01";}
   Map<String,Object> o=Database.one(c,"SELECT *,CASE WHEN payment_expires_at<=SYSUTCDATETIME() THEN 1 ELSE 0 END AS expired FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?",pointer.get("order_id"));
   Map<String,Object> t=Database.one(c,"SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE id=?",ref);
   if(((BigDecimal)t.get("amount")).compareTo(amount)!=0){c.rollback();return "04";}
   if("PAID".equals(t.get("status"))){c.commit();return Objects.equals(gatewayRef,t.get("gateway_ref"))?"02":"99";}
   if(Database.one(c,"SELECT id FROM integration_events WHERE provider='VNPAY' AND event_key=?",ref+":"+gatewayRef+":"+p.get("vnp_ResponseCode")+":"+p.get("vnp_TransactionStatus"))!=null){c.commit();return "02";}
   boolean success="00".equals(p.get("vnp_ResponseCode")) && "00".equals(p.get("vnp_TransactionStatus"));
   if(success && Database.one(c,"SELECT id FROM transactions WHERE provider='VNPAY' AND gateway_ref=? AND id<>?",gatewayRef,ref)!=null){c.rollback();return "99";}
   Database.update(c,"UPDATE transactions SET status=?,gateway_ref=?,reconciled=1,updated_at=SYSUTCDATETIME() WHERE id=?",success?"PAID":Set.of("01","04","05","06","07").contains(p.getOrDefault("vnp_TransactionStatus",""))?"INITIATED":"FAILED",success?gatewayRef:null,ref);
   if(success){
    if("AWAITING_PAYMENT".equals(o.get("status")) && ((Number)o.get("expired")).intValue()==0)Database.update(c,"UPDATE orders SET status='CONFIRMED',updated_at=SYSUTCDATETIME() WHERE id=?",o.get("id"));
    else {
     if("AWAITING_PAYMENT".equals(o.get("status")))expire(c,o);
     // A late payment is recorded, never used to revive an expired order.
     for(Map<String,Object> so:Database.rows(c,"SELECT * FROM seller_orders WHERE order_id=?",o.get("id")))FinanceService.requestAutomatic(c,o,so,ref,"AWAITING_PAYMENT".equals(o.get("status")) || "EXPIRED".equals(o.get("status")) || "CANCELLED".equals(o.get("status"))?"Late payment after expiry or cancellation":"Duplicate payment for confirmed order",Set.of("CONFIRMED","COMPLETED").contains(o.get("status")));
    }
   }
   Database.update(c,"INSERT integration_events(provider,event_key,event_type,status) VALUES('VNPAY',?,'PAYMENT_IPN','PROCESSED')",ref+":"+gatewayRef+":"+p.get("vnp_ResponseCode")+":"+p.get("vnp_TransactionStatus"));
   c.commit();return "00";
  }catch(Exception e){c.rollback();throw e;}}
 }
 public static void expire(Connection c,Map<String,Object> o)throws Exception{
  Database.update(c,"UPDATE orders SET status='EXPIRED',updated_at=SYSUTCDATETIME() WHERE id=?",o.get("id"));
  for(Map<String,Object> so:Database.rows(c,"SELECT id FROM seller_orders WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? ORDER BY store_id,id",o.get("id"))){new StockReservationService().release(c,FulfillmentService.id(so.get("id")));}
  Database.update(c,"UPDATE seller_orders SET status='CANCELLED' WHERE order_id=?",o.get("id"));
  Database.update(c,"UPDATE transactions SET status='EXPIRED' WHERE order_id=? AND type='PAYMENT' AND status='INITIATED'",o.get("id"));
  BenefitsService.returnAll(c,o);
 }
 public void expireDue()throws Exception{
  try(Connection c=new DBConnect().getConnection()){
   List<Map<String,Object>> due=Database.rows(c,"SELECT TOP 100 id FROM orders WHERE status='AWAITING_PAYMENT' AND payment_expires_at<=SYSUTCDATETIME() ORDER BY id");
   for(var id:due){c.setAutoCommit(false);try{Map<String,Object> o=Database.one(c,"SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status='AWAITING_PAYMENT' AND payment_expires_at<=SYSUTCDATETIME()",id.get("id"));if(o!=null)expire(c,o);c.commit();}catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
  }
 }
}
