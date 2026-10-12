package service;
import config.DBConnect;import dao.Database;import java.sql.*;import java.util.*;import java.math.*;
public class FinanceService {
 public static BigDecimal paidShare(Map<String,Object> so){return FulfillmentService.money(so.get("subtotal")).subtract(FulfillmentService.money(so.get("discount_amount"))).subtract(FulfillmentService.money(so.get("points_discount"))).add(FulfillmentService.money(so.get("delivery_fee")));}
 public static void requestAutomatic(Connection c,Map<String,Object> o,Map<String,Object> so,long parent,String reason)throws Exception{requestAutomatic(c,o,so,parent,reason,false);}
 public static boolean requestAutomatic(Connection c,Map<String,Object> o,Map<String,Object> so,long parent,String reason,boolean excess)throws Exception{
  if(Database.one(c,"SELECT id FROM transactions WHERE type='REFUND' AND seller_order_id=? AND parent_transaction_id=? AND status<>'REJECTED'",so.get("id"),parent)!=null)return false;
  Map<String,Object> payment=Database.one(c,"SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status='PAID'",parent);if(payment==null)throw new IllegalArgumentException("No confirmed payment to refund.");
  BigDecimal reserved=FulfillmentService.money(Database.one(c,"SELECT COALESCE(SUM(amount),0) AS total FROM transactions WHERE parent_transaction_id=? AND type='REFUND' AND status<>'REJECTED'",parent).get("total"));
  BigDecimal amount=paidShare(so);if(reserved.add(amount).compareTo((BigDecimal)payment.get("amount"))>0)throw new IllegalArgumentException("Refund exceeds the remaining paid amount.");
  Database.update(c,"INSERT transactions(type,user_id,order_id,seller_order_id,parent_transaction_id,amount,method,status,reason,idempotency_key,refund_scope) VALUES('REFUND',?,?,?,?,?,?,'REQUESTED',?,?,?)",o.get("customer_id"),o.get("id"),so.get("id"),parent,amount,o.get("payment_method"),reason,"refund:"+so.get("id")+":"+parent+":"+UUID.randomUUID(),excess?"EXCESS_PAYMENT":"PURCHASE");
  return true;
 }
 public void request(long uid,long sid,String reason)throws Exception{
  reason=Validation.text(reason,500);
  try(Connection c=new DBConnect().getConnection()){c.setAutoCommit(false);try{
   Map<String,Object> ref=Database.one(c,"SELECT order_id FROM seller_orders WHERE id=?",sid);if(ref==null)throw new IllegalArgumentException("Order not found.");
   Map<String,Object> o=Database.one(c,"SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND customer_id=?",ref.get("order_id"),uid);if(o==null)throw new IllegalArgumentException("Order not found.");
   Map<String,Object> so=Database.one(c,"SELECT * FROM seller_orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND (status IN ('CANCELLED','REJECTED') OR (status='DELIVERED' AND delivered_at>=DATEADD(day,-?,SYSUTCDATETIME())))",sid,FulfillmentService.policy(c,"refund_window_days",7));
   if(so==null || "SETTLED".equals(so.get("settlement_status")))throw new IllegalArgumentException("Order is outside the refund eligibility window.");
   Map<String,Object> payment=Database.one(c,"SELECT TOP 1 id FROM transactions WHERE order_id=? AND status='PAID' AND (type='PAYMENT' OR (type='COD' AND seller_order_id=?)) ORDER BY id",o.get("id"),sid);
   if(payment==null)throw new IllegalArgumentException("No collected payment to refund.");
   boolean created=requestAutomatic(c,o,so,FulfillmentService.id(payment.get("id")),reason,false);
   if(created && "DELIVERED".equals(so.get("status")))Database.update(c,"UPDATE seller_orders SET dispute_status='OPEN',dispute_note=? WHERE id=?",reason,sid);
   c.commit();
  }catch(Exception e){c.rollback();throw e;}}
 }
 public List<Map<String,Object>> list(long uid,boolean admin)throws Exception{
  try(Connection c=new DBConnect().getConnection()){if(admin)FulfillmentService.role(c,uid,"ADMIN");return Database.rows(c,"SELECT t.*,o.order_code FROM transactions t JOIN orders o ON o.id=t.order_id WHERE t.type='REFUND' AND "+(admin?"?>0":"t.user_id=?")+" ORDER BY t.id DESC",uid);}
 }
 public void decide(long admin,long tid,String action,String reason)throws Exception{
  reason=Validation.text(reason,500);
  try(Connection c=new DBConnect().getConnection()){c.setAutoCommit(false);try{
   FulfillmentService.role(c,admin,"ADMIN");Map<String,Object> ref=Database.one(c,"SELECT order_id FROM transactions WHERE id=? AND type='REFUND'",tid);if(ref==null)throw new IllegalArgumentException("Refund not found.");
   Database.one(c,"SELECT id FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?",ref.get("order_id"));
   Map<String,Object> t=Database.one(c,"SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE id=?",tid);
   if(action.equals("approve") || action.equals("reject")){
    if(!"REQUESTED".equals(t.get("status")))throw new IllegalArgumentException("Refund has already been reviewed.");
    Database.update(c,"UPDATE transactions SET status=?,reason=?,updated_at=SYSUTCDATETIME() WHERE id=?",action.equals("approve")?"APPROVED":"REJECTED",reason,tid);
    if(!"EXCESS_PAYMENT".equals(t.get("refund_scope")))Database.update(c,"UPDATE seller_orders SET dispute_status='RESOLVED',dispute_decision=? WHERE id=?",reason,t.get("seller_order_id"));
   }else if(action.equals("complete")){
    if(!"COD".equals(t.get("method")) || !"APPROVED".equals(t.get("status")))throw new IllegalArgumentException("Only approved COD refunds can be completed manually.");
    complete(c,t,FulfillmentService.evidence(reason),null);
   }else throw new IllegalArgumentException("Invalid refund action.");
   Database.update(c,"INSERT audit_logs(actor_id,entity_type,entity_id,action,note) VALUES(?,'Refund',?,?,?)",admin,tid,action,reason);c.commit();
  }catch(Exception e){c.rollback();throw e;}}
 }
 static void complete(Connection c,Map<String,Object> t,String evidence,String gatewayRef)throws Exception{
  Database.update(c,"UPDATE transactions SET status='REFUNDED',evidence_url=?,gateway_ref=?,provider=?,reconciled=1,updated_at=SYSUTCDATETIME() WHERE id=?",evidence,gatewayRef,gatewayRef==null?null:"VNPAY",t.get("id"));
  Map<String,Object> so=Database.one(c,"SELECT * FROM seller_orders WHERE id=?",t.get("seller_order_id"));
  if(!"EXCESS_PAYMENT".equals(t.get("refund_scope"))){
  LoyaltyService.restore(c,FulfillmentService.id(t.get("user_id")),FulfillmentService.id(so.get("id")),((Number)so.get("points_used")).intValue());
  Map<String,Object> earned=Database.one(c,"SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE idempotency_key=?","earn:"+so.get("id"));
  if(earned!=null && earned.get("remaining_points")!=null && ((Number)earned.get("remaining_points")).intValue()>0){int n=((Number)earned.get("remaining_points")).intValue();Database.update(c,"UPDATE transactions SET remaining_points=0,status='EXPIRED' WHERE id=?",earned.get("id"));Database.update(c,"UPDATE users SET points_balance=points_balance-? WHERE id=?",n,t.get("user_id"));Database.update(c,"INSERT transactions(type,user_id,seller_order_id,points,status,reason,idempotency_key) VALUES('POINTS_EXPIRE',?,?,?,'COMPLETED','Refund reversal',?)",t.get("user_id"),so.get("id"),-n,"refund-points:"+t.get("id"));}
  LoyaltyService.tier(c,FulfillmentService.id(t.get("user_id")));
  }
  Database.update(c,"INSERT notifications(user_id,event_type,title,content,link_url,dedup_key) VALUES(?,'REFUND','Refund completed','Your refund has been confirmed.','/refunds',?)",t.get("user_id"),"refund-complete:"+t.get("id"));
 }
 public void settle(long admin,long sid)throws Exception{
  try(Connection c=new DBConnect().getConnection()){c.setAutoCommit(false);try{
   FulfillmentService.role(c,admin,"ADMIN");Map<String,Object> ref=Database.one(c,"SELECT order_id FROM seller_orders WHERE id=?",sid);if(ref==null)throw new IllegalArgumentException("Order not found.");Database.one(c,"SELECT id FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?",ref.get("order_id"));
   Map<String,Object> so=Database.one(c,"SELECT * FROM seller_orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status='DELIVERED' AND settlement_status='UNSETTLED' AND delivered_at<DATEADD(day,-?,SYSUTCDATETIME())",sid,FulfillmentService.policy(c,"refund_window_days",7));
   if(so==null)throw new IllegalArgumentException("Settlement waits until the refund window closes.");
   if(Database.one(c,"SELECT id FROM transactions WHERE seller_order_id=? AND type='REFUND' AND status NOT IN ('REFUNDED','REJECTED')",sid)!=null || "OPEN".equals(so.get("dispute_status")))throw new IllegalArgumentException("Resolve pending refunds or disputes first.");
   if(Database.one(c,"SELECT id FROM transactions WHERE order_id=? AND status='PAID' AND reconciled=1 AND (type='PAYMENT' OR (type='COD' AND seller_order_id=?))",so.get("order_id"),sid)==null)throw new IllegalArgumentException("Payment has not been reconciled.");
   BigDecimal refunds=FulfillmentService.money(Database.one(c,"SELECT COALESCE(SUM(amount),0) total FROM transactions WHERE seller_order_id=? AND type='REFUND' AND status='REFUNDED' AND refund_scope='PURCHASE'",sid).get("total"));
   BigDecimal adjustments=FulfillmentService.money(Database.one(c,"SELECT COALESCE(SUM(amount),0) total FROM transactions WHERE seller_order_id=? AND type='ADJUSTMENT' AND status='COMPLETED' AND reconciled=1",sid).get("total"));
   BigDecimal net=FulfillmentService.money(so.get("subtotal")).subtract(refunds).subtract(FulfillmentService.money(so.get("commission"))).add(adjustments);
   Database.update(c,"UPDATE seller_orders SET settlement_status='SETTLED',settled_at=SYSUTCDATETIME(),net_payable=? WHERE id=?",net,sid);
   Database.update(c,"INSERT audit_logs(actor_id,entity_type,entity_id,action,note) VALUES(?,'SellerOrder',?,'SETTLED','Settlement summary recorded; not a bank payout')",admin,sid);c.commit();
  }catch(Exception e){c.rollback();throw e;}}
 }
}
