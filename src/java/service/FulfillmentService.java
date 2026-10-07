package service;
import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.math.*;
import java.util.*;

/** Every order mutation locks the parent order before seller orders and batches. */
public class FulfillmentService {
 public static int policy(Connection c,String key,int fallback)throws Exception{
  Map<String,Object> p=Database.one(c,"SELECT TRY_CONVERT(int,value) AS n FROM settings WHERE type='POLICY' AND name=? AND status='ACTIVE'",key);
  return p!=null && p.get("n")!=null && ((Number)p.get("n")).intValue()>0?((Number)p.get("n")).intValue():fallback;
 }
 public static long id(Object n){return ((Number)n).longValue();}
 public static BigDecimal money(Object n){return n==null?BigDecimal.ZERO:(BigDecimal)n;}
 public static void role(Connection c,long uid,String required)throws Exception{
  if(Database.one(c,"SELECT id FROM users WHERE id=? AND role=? AND status='ACTIVE'",uid,required)==null)throw new IllegalArgumentException("Access denied.");
 }
 public List<Map<String,Object>> list(long uid,String role)throws Exception {
  try(Connection c=new DBConnect().getConnection()){
   role(c,uid,role);
   String condition=switch(role){case "SELLER"->"s.owner_id=?";case "DELIVERY_STAFF"->"so.delivery_staff_id=?";case "ADMIN"->"? > 0";default->throw new IllegalArgumentException("Access denied.");};
   return Database.rows(c,"SELECT so.*,o.order_code,o.payment_method,o.recipient_name,o.recipient_phone,o.delivery_address,o.status AS order_status,s.store_name,a.address_line AS pickup_address,a.city AS pickup_city FROM seller_orders so JOIN orders o ON o.id=so.order_id JOIN stores s ON s.id=so.store_id LEFT JOIN addresses a ON a.id=s.pickup_address_id WHERE o.status<>'CART' AND "+condition+" ORDER BY so.id DESC",uid);
  }
 }
 public List<Map<String,Object>> allocations(long uid,String role)throws Exception{
  try(Connection c=new DBConnect().getConnection()){
   if(!Set.of("SELLER","ADMIN","DELIVERY_STAFF").contains(role))throw new IllegalArgumentException("Access denied.");
   role(c,uid,role);String condition=role.equals("SELLER")?"s.owner_id=?":role.equals("DELIVERY_STAFF")?"so.delivery_staff_id=?":"?>0";
   return Database.rows(c,"SELECT oi.*,l.title,v.option_name,b.batch_no,b.expiry_date FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN orders o ON o.id=so.order_id JOIN stores s ON s.id=so.store_id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id LEFT JOIN batches b ON b.id=oi.batch_id WHERE o.status<>'CART' AND "+condition+" ORDER BY so.id,oi.id",uid);
  }
 }
 public void change(long actor,String role,long sid,String action,String detail,Long courier,boolean collected)throws Exception{
  try(Connection c=new DBConnect().getConnection()){c.setAutoCommit(false);try{
   Map<String,Object> ref=Database.one(c,"SELECT order_id FROM seller_orders WHERE id=?",sid);
   if(ref==null)throw new IllegalArgumentException("Order not found.");
   Map<String,Object> o=Database.one(c,"SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?",ref.get("order_id"));
   Map<String,Object> so=Database.one(c,"SELECT so.*,s.owner_id FROM seller_orders so WITH(UPDLOCK,HOLDLOCK) JOIN stores s ON s.id=so.store_id WHERE so.id=?",sid);
   role(c,actor,role);
   boolean customer="CUSTOMER".equals(role) || "SELLER".equals(role) && action.equals("cancel");
   if(customer){if(id(o.get("customer_id"))!=actor || !action.equals("cancel"))throw new IllegalArgumentException("Order not found.");}
   else if(role.equals("SELLER") && id(so.get("owner_id"))!=actor || role.equals("DELIVERY_STAFF") && (so.get("delivery_staff_id")==null || id(so.get("delivery_staff_id"))!=actor))throw new IllegalArgumentException("Order not found.");
   String old=(String)so.get("status"),next;
   if(!"CONFIRMED".equals(o.get("status")))throw new IllegalArgumentException("Order is not confirmed for fulfillment.");
   switch(action){
    case "accept":require(role.equals("SELLER") && old.equals("NEW"));next="ACCEPTED";break;
    case "reject":require(role.equals("SELLER") && Set.of("NEW","ACCEPTED").contains(old));detail=Validation.text(detail,500);next="REJECTED";break;
    case "cancel":
     require(customer && Set.of("NEW","ACCEPTED").contains(old));
     if(Database.one(c,"SELECT id FROM orders WHERE id=? AND placed_at>=DATEADD(minute,-?,SYSUTCDATETIME())",o.get("id"),policy(c,"cancellation_window_minutes",30))==null)throw new IllegalArgumentException("The cancellation window has expired.");
     detail="Customer cancellation";next="CANCELLED";break;
    case "ready":
     require(role.equals("SELLER") && old.equals("ACCEPTED"));
     if(Database.one(c,"SELECT TOP 1 oi.id FROM order_items oi LEFT JOIN batches b ON b.id=oi.batch_id WHERE oi.seller_order_id=? AND (oi.reservation_status<>'RESERVED' OR b.id IS NULL OR b.status<>'AVAILABLE' OR b.expiry_date<=CONVERT(date,SYSUTCDATETIME()))",sid)!=null)throw new IllegalArgumentException("Allocated stock is unavailable or expired. Contact Admin.");
     if(Database.one(c,"SELECT id FROM stores WHERE id=? AND pickup_address_id IS NOT NULL",so.get("store_id"))==null)throw new IllegalArgumentException("Set your store pickup address before handover.");
     next="READY";break;
    case "assign":
     require(role.equals("ADMIN") && old.equals("READY") && courier!=null);role(c,courier,"DELIVERY_STAFF");
     Database.update(c,"UPDATE seller_orders SET delivery_staff_id=?,assigned_at=SYSUTCDATETIME(),cold_chain_note=?,evidence_required=1 WHERE id=?",courier,Validation.text(detail,255),sid);next="ASSIGNED";break;
    case "pickup":require(role.equals("DELIVERY_STAFF") && old.equals("ASSIGNED"));next="PICKED_UP";Database.update(c,"UPDATE seller_orders SET picked_up_at=SYSUTCDATETIME() WHERE id=?",sid);break;
    case "transit":require(role.equals("DELIVERY_STAFF") && old.equals("PICKED_UP"));next="IN_TRANSIT";break;
    case "arrive":require(role.equals("DELIVERY_STAFF") && old.equals("IN_TRANSIT"));next="ARRIVING";break;
    case "deliver":
     require(role.equals("DELIVERY_STAFF") && old.equals("ARRIVING"));
     detail=evidence(detail);
     if("COD".equals(o.get("payment_method")) && !collected)throw new IllegalArgumentException("Confirm COD collection before marking Delivered.");
     consume(c,sid);
     Database.update(c,"UPDATE seller_orders SET evidence_url=?,delivered_at=SYSUTCDATETIME(),commission=ROUND(subtotal*COALESCE(commission_rate,0),2) WHERE id=?",detail,sid);
     next="DELIVERED";break;
    case "fail":require(role.equals("DELIVERY_STAFF") && Set.of("ASSIGNED","PICKED_UP","IN_TRANSIT","ARRIVING").contains(old));detail=Validation.text(detail,500);Database.update(c,"UPDATE seller_orders SET failure_reason=?,failed_at=SYSUTCDATETIME() WHERE id=?",detail,sid);next="FAILED";break;
    default:throw new IllegalArgumentException("Invalid action.");
   }
   Database.update(c,"UPDATE seller_orders SET status=?,rejection_reason=CASE WHEN ? IN ('REJECTED','CANCELLED') THEN ? ELSE rejection_reason END,updated_at=SYSUTCDATETIME() WHERE id=?",next,next,detail,sid);
   if(Set.of("REJECTED","CANCELLED").contains(next)){
    new StockReservationService().release(c,sid);BenefitsService.returnSeller(c,o,so);
    Map<String,Object> paid=Database.one(c,"SELECT TOP 1 id FROM transactions WHERE order_id=? AND type='PAYMENT' AND status='PAID' ORDER BY id",o.get("id"));if(paid!=null)FinanceService.requestAutomatic(c,o,so,FulfillmentService.id(paid.get("id")),detail);
    // Amounts remain immutable snapshots. Uncollected COD is replaced by a revised payable attempt.
    if("COD".equals(o.get("payment_method"))){
     Database.update(c,"UPDATE transactions SET status='EXPIRED',updated_at=SYSUTCDATETIME() WHERE order_id=? AND type='COD' AND status='PENDING'",o.get("id"));
     BigDecimal remaining=money(Database.one(c,"SELECT COALESCE(SUM(subtotal-discount_amount-points_discount+delivery_fee),0) AS total FROM seller_orders WHERE order_id=? AND status NOT IN ('CANCELLED','REJECTED','DELIVERED')",o.get("id")).get("total"));
     if(remaining.signum()>0)Database.update(c,"INSERT transactions(type,user_id,order_id,amount,status,idempotency_key,method) VALUES('COD',?,?,?,'PENDING',?,'COD')",o.get("customer_id"),o.get("id"),remaining,"cod-revision:"+sid);
    }
    if(Database.one(c,"SELECT id FROM seller_orders WHERE order_id=? AND status NOT IN ('CANCELLED','REJECTED')",o.get("id"))==null){Database.update(c,"UPDATE orders SET status='CANCELLED',updated_at=SYSUTCDATETIME() WHERE id=?",o.get("id"));BenefitsService.returnAll(c,o);}
   }
   Database.update(c,"INSERT audit_logs(actor_id,entity_type,entity_id,action,old_value,new_value,note) VALUES(?,'SellerOrder',?,?,?,?,?)",actor,sid,action,old,next,detail);
   Database.update(c,"INSERT notifications(user_id,event_type,title,content,link_url,dedup_key) VALUES(?,'ORDER_UPDATE','Order updated',?,'/orders',?)",o.get("customer_id"),"Seller order "+sid+": "+next,"seller-order:"+sid+":"+next);
   if(next.equals("DELIVERED")){
    LoyaltyService.earn(c,FulfillmentService.id(o.get("customer_id")),sid,FulfillmentService.money(so.get("subtotal")).subtract(FulfillmentService.money(so.get("discount_amount"))).subtract(FulfillmentService.money(so.get("points_discount"))));
    if("COD".equals(o.get("payment_method")))Database.update(c,"INSERT transactions(type,user_id,order_id,seller_order_id,method,amount,status,reconciled,idempotency_key) VALUES('COD',?,?,?,'COD',?,'PAID',1,?)",o.get("customer_id"),o.get("id"),sid,money(so.get("subtotal")).subtract(money(so.get("discount_amount"))).subtract(money(so.get("points_discount"))).add(money(so.get("delivery_fee"))),"cod-collected:"+sid);
    if(Database.one(c,"SELECT id FROM seller_orders WHERE order_id=? AND status NOT IN ('DELIVERED','CANCELLED','REJECTED')",o.get("id"))==null){Database.update(c,"UPDATE orders SET status='COMPLETED',updated_at=SYSUTCDATETIME() WHERE id=?",o.get("id"));Database.update(c,"UPDATE transactions SET status='COMPLETED' WHERE order_id=? AND type='COD' AND seller_order_id IS NULL AND status='PENDING'",o.get("id"));}
   }
   if(Database.one(c,"SELECT id FROM seller_orders WHERE order_id=? AND status NOT IN ('DELIVERED','CANCELLED','REJECTED')",o.get("id"))==null && Database.one(c,"SELECT id FROM seller_orders WHERE order_id=? AND status='DELIVERED'",o.get("id"))!=null){Database.update(c,"UPDATE orders SET status='COMPLETED',updated_at=SYSUTCDATETIME() WHERE id=?",o.get("id"));Database.update(c,"UPDATE transactions SET status='COMPLETED' WHERE order_id=? AND type='COD' AND seller_order_id IS NULL AND status='PENDING'",o.get("id"));}
   c.commit();
  }catch(Exception e){c.rollback();throw e;}}
 }
 static void require(boolean condition){if(!condition)throw new IllegalArgumentException("This action is not allowed in the current order state.");}
 public static String evidence(String url){url=Validation.text(url,500);if(!url.startsWith("https://") && !url.startsWith("/media/"))throw new IllegalArgumentException("Provide an HTTPS evidence link or uploaded media path.");return url;}
 private void consume(Connection c,long sid)throws Exception{
  for(Map<String,Object> line:Database.rows(c,"SELECT * FROM order_items WITH(UPDLOCK,HOLDLOCK) WHERE seller_order_id=? ORDER BY batch_id,id",sid)){
   if(!"RESERVED".equals(line.get("reservation_status")))throw new IllegalArgumentException("Order stock is not reserved.");
   if(Database.update(c,"UPDATE batches SET remaining_qty=remaining_qty-?,reserved_qty=reserved_qty-? WHERE id=? AND reserved_qty>=? AND remaining_qty>=? AND status='AVAILABLE' AND expiry_date>CONVERT(date,SYSUTCDATETIME())",line.get("quantity"),line.get("quantity"),line.get("batch_id"),line.get("quantity"),line.get("quantity"))!=1)throw new IllegalArgumentException("Allocated stock cannot be delivered. Contact Admin.");
   Database.update(c,"UPDATE order_items SET reservation_status='CONSUMED' WHERE id=?",line.get("id"));
   Database.update(c,"INSERT stock_movements(batch_id,order_item_id,movement_type,quantity,idempotency_key) VALUES(?,?,'CONSUME',?,?)",line.get("batch_id"),line.get("id"),line.get("quantity"),"consume:"+line.get("id"));
  }
 }
}
