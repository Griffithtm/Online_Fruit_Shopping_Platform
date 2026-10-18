import model.*;
import service.*;import dao.Database;import java.sql.*;import java.util.*;import java.math.*;
public class PaymentChecks extends IterationTwoChecks {
 static Map<String,String> callback(long tid,BigDecimal amount,String no)throws Exception{
  Map<String,String> p=new TreeMap<>();p.put("vnp_TmnCode",System.getenv("FRESHORA_VNPAY_TMN_CODE"));p.put("vnp_TxnRef",Long.toString(tid));p.put("vnp_Amount",amount.movePointRight(2).toBigIntegerExact().toString());p.put("vnp_TransactionNo",no);p.put("vnp_ResponseCode","00");p.put("vnp_TransactionStatus","00");p.put("vnp_SecureHash",VnpayGateway.hash(System.getenv("FRESHORA_VNPAY_HASH_SECRET"),VnpayGateway.canonical(p)));return p;
 }
 public static void main(String[]args)throws Exception{
  fixture();try{
   CartService cart=new CartService();PaymentService pay=new PaymentService();
   cart.change(buyer,"add",variant,1);long oid=place("",0,"CARD");String url=pay.start(buyer,oid,"127.0.0.1");
   check(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"),"online payment uses sandbox endpoint");
   long tid=n(one("SELECT id FROM transactions WHERE order_id=? AND type='PAYMENT'",oid).get("id"));BigDecimal amount=(BigDecimal)one("SELECT payable_amount FROM orders WHERE id=?",oid).get("payable_amount");
   Map<String,String> p=callback(tid,amount,"9"+tid);p.put("vnp_Amount","100");check(pay.callback(p).equals("97"),"tampered signature rejected");
   check(pay.callback(callback(tid,amount.add(BigDecimal.ONE),"9"+tid)).equals("04"),"signed wrong amount rejected");
   check("AWAITING_PAYMENT".equals(one("SELECT status FROM orders WHERE id=?",oid).get("status")),"invalid callbacks cannot confirm order");
   check(pay.callback(callback(tid,amount,"9"+tid)).equals("00"),"signed matching IPN confirms payment");
   check(pay.callback(callback(tid,amount,"9"+tid)).equals("02"),"repeated IPN is idempotent");
   check("CONFIRMED".equals(one("SELECT status FROM orders WHERE id=?",oid).get("status")),"online order confirmed only after IPN");
   new FulfillmentService().change(buyer,"CUSTOMER",sid(oid,store),"cancel",null,null,false);
   check(n(one("SELECT COUNT(*) n FROM transactions WHERE order_id=? AND type='REFUND' AND status='REQUESTED'",oid).get("n"))==1,"paid cancellation creates refund review");
   long refund=n(one("SELECT id FROM transactions WHERE order_id=? AND type='REFUND'",oid).get("id"));new FinanceService().decide(admin,refund,"approve","Approved test refund");update("UPDATE transactions SET status='PENDING' WHERE id=?",refund);
   Map<String,String> result=new LinkedHashMap<>();result.put("vnp_ResponseId","test"+refund);result.put("vnp_Command","refund");result.put("vnp_ResponseCode","00");result.put("vnp_Message","Success");result.put("vnp_TmnCode",System.getenv("FRESHORA_VNPAY_TMN_CODE"));result.put("vnp_TxnRef",Long.toString(tid));result.put("vnp_Amount",amount.movePointRight(2).toBigIntegerExact().toString());result.put("vnp_BankCode","TEST");result.put("vnp_PayDate","20261007010000");result.put("vnp_TransactionNo","7"+refund);result.put("vnp_TransactionType","02");result.put("vnp_TransactionStatus","00");result.put("vnp_OrderInfo","Freshora hoàn tiền "+refund);result.put("vnp_SecureHash",VnpayGateway.hash(System.getenv("FRESHORA_VNPAY_HASH_SECRET"),String.join("|",result.values())));
   StringJoiner response=new StringJoiner(",","{","}");result.forEach((k,v)->response.add("\""+k+"\":\""+v+"\""));String signed=response.toString();
   reject(()->new OnlineRefundService().confirm(admin,refund,signed.replace("Success","Tampered")),"tampered refund confirmation rejected");
   new OnlineRefundService().confirm(admin,refund,signed);new OnlineRefundService().confirm(admin,refund,signed);
   check("REFUNDED".equals(one("SELECT status FROM transactions WHERE id=?",refund).get("status")),"signed refund response confirms once");
   cart.change(buyer,"add",variant,1);long late=place("",0,"E_WALLET");pay.start(buyer,late,"127.0.0.1");long lateTid=n(one("SELECT id FROM transactions WHERE order_id=? AND type='PAYMENT'",late).get("id"));BigDecimal lateAmount=(BigDecimal)one("SELECT payable_amount FROM orders WHERE id=?",late).get("payable_amount");
   update("UPDATE orders SET payment_expires_at=DATEADD(minute,-1,SYSUTCDATETIME()) WHERE id=?",late);pay.expireDue();
   check("EXPIRED".equals(one("SELECT status FROM orders WHERE id=?",late).get("status")),"timeout expires awaiting payment order");
   check(n(one("SELECT reserved_qty FROM batches WHERE variant_id=?",variant).get("reserved_qty"))==0,"expiry releases stock");
   check(pay.callback(callback(lateTid,lateAmount,"8"+lateTid)).equals("00"),"late successful payment is recorded");
   check("EXPIRED".equals(one("SELECT status FROM orders WHERE id=?",late).get("status")),"late success never revives expired order");
   check(n(one("SELECT COUNT(*) n FROM transactions WHERE order_id=? AND type='REFUND' AND status='REQUESTED'",late).get("n"))==1,"late success creates refund follow-up");
   System.out.println(count+" payment checks passed");
  }finally{update("UPDATE listings SET status='HIDDEN' WHERE store_id IN (?,?)",store,store2);update("UPDATE users SET status='SUSPENDED' WHERE id IN (?,?,?,?,?)",buyer,seller,other,admin,courier);}
 }
}
