import model.*;
import config.DBConnect;import dao.Database;import service.*;import java.sql.*;import java.util.*;import java.math.*;
public class IterationTwoChecks {
 static String tag="I2-"+System.currentTimeMillis();static long buyer,seller,other,admin,courier,store,store2,variant,variant2,address;static int count;
 interface Task{void run()throws Exception;}
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);System.out.println("PASS "+(++count)+" "+msg);}
 static void reject(Task task,String msg)throws Exception{try{task.run();throw new AssertionError(msg);}catch(IllegalArgumentException expected){check(true,msg);}}
 static void rejectWith(Task task,String expected,String msg)throws Exception{try{task.run();throw new AssertionError(msg);}catch(IllegalArgumentException e){if(!expected.equals(e.getMessage()))throw new AssertionError(msg+": got \""+e.getMessage()+"\"");check(true,msg);}}
 static long n(Object x){return ((Number)x).longValue();}
 static Connection db()throws Exception{return new DBConnect().getConnection();}
 static long user(Connection c,String suffix,String role)throws Exception{return Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,'test-only',?,?)",tag+suffix+"@test.invalid",tag+suffix,role);}
 static Map<String,Object> one(String sql,Object...args)throws Exception{try(Connection c=db()){return TestRows.one(c,sql,args);}}
 static void update(String sql,Object...args)throws Exception{try(Connection c=db()){Database.update(c,sql,args);}}
 /** Seller order code as shown to users (util.OrderCodes). */
 static String code(long oid,long storeId)throws Exception{return util.OrderCodes.sellerOrderCode((String)one("SELECT order_code FROM orders WHERE id=?",oid).get("order_code"),storeId);}
 /** Content of the user's notification whose dedup key matches the LIKE pattern, or null. */
 static String notice(long userId,String dedupLike)throws Exception{Map<String,Object> r=one("SELECT TOP 1 content FROM notifications WHERE user_id=? AND dedup_key LIKE ? ORDER BY id DESC",userId,dedupLike);return r==null?null:(String)r.get("content");}
 static long sid(long oid,long st)throws Exception{return n(one("SELECT id FROM seller_orders WHERE order_id=? AND store_id=?",oid,st).get("id"));}
 static long place(String voucher,int points,String method)throws Exception{
  CheckoutService service=new CheckoutService();CheckoutQuote q=service.quote(buyer,address,voucher,points,method);String key=UUID.randomUUID().toString();long oid=service.place(buyer,address,key,(String)q.getFingerprint(),voucher,points,method);
  check(oid==service.place(buyer,address,key,(String)q.getFingerprint(),voucher,points,method),"placement idempotency");return oid;
 }
 static void fixture()throws Exception{try(Connection c=db()){
  buyer=user(c,"buyer","CUSTOMER");seller=user(c,"seller","SELLER");other=user(c,"other","SELLER");admin=user(c,"admin","ADMIN");courier=user(c,"courier","DELIVERY_STAFF");
  long cat=n(TestRows.one(c,"SELECT TOP 1 id FROM settings WHERE type='CATEGORY'",new Object[0]).get("id"));long tier=n(TestRows.one(c,"SELECT TOP 1 id FROM settings WHERE type='SELLER_TIER'").get("id"));
  store=Database.insert(c,"INSERT stores(owner_id,store_name,seller_tier_id) VALUES(?,?,?)",seller,tag+"store",tier);store2=Database.insert(c,"INSERT stores(owner_id,store_name,seller_tier_id) VALUES(?,?,?)",other,tag+"store2",tier);
  String city=(String)TestRows.one(c,"SELECT TOP 1 name FROM settings WHERE type='SERVICE_AREA' AND status='ACTIVE'").get("name");
  address=Database.insert(c,"INSERT addresses(user_id,recipient_name,phone,address_line,ward,district,city,is_default) VALUES(?,'Test','0900000000','Test','Test','Test',?,1)",buyer,city);
  for(long su:new long[]{seller,other}){long aid=Database.insert(c,"INSERT addresses(user_id,recipient_name,phone,address_line,ward,district,city) VALUES(?,'Pickup test','0900000000','Pickup test','Test','Test',?)",su,city);Database.update(c,"UPDATE stores SET pickup_address_id=? WHERE owner_id=?",aid,su);}
  for(long st:new long[]{store,store2}){long l=Database.insert(c,"INSERT listings(store_id,category_id,title,status) VALUES(?, ?,?,'APPROVED')",st,cat,tag+" fruit");long v=Database.insert(c,"INSERT variants(listing_id,option_name,unit,price) VALUES(?,'1 kg','kg',100000)",l);Database.update(c,"INSERT batches(variant_id,batch_no,origin,harvest_date,receipt_date,expiry_date,supplier_name,received_qty,remaining_qty,unit_cost) VALUES(?,?,'Test',CONVERT(date,SYSUTCDATETIME()),CONVERT(date,SYSUTCDATETIME()),DATEADD(day,30,CONVERT(date,SYSUTCDATETIME())),'Test',100,100,50000)",v,tag+st);if(st==store)variant=v;else variant2=v;}
 }}
 static void deliver(long oid,long sid,long sellerId)throws Exception{
  FulfillmentService f=new FulfillmentService();f.change(sellerId,"SELLER",sid,"accept",null,null,false);
  reject(()->f.change(other==sellerId?seller:other,"SELLER",sid,"ready",null,null,false),"seller cannot change another store order");
  f.change(sellerId,"SELLER",sid,"ready",null,null,false);f.change(admin,"ADMIN",sid,"assign","Keep chilled",courier,false);
  reject(()->f.change(courier,"DELIVERY_STAFF",sid,"deliver","https://example.invalid/evidence",null,true),"delivery cannot skip pickup and transit");
  f.change(courier,"DELIVERY_STAFF",sid,"pickup",null,null,false);f.change(courier,"DELIVERY_STAFF",sid,"transit",null,null,false);f.change(courier,"DELIVERY_STAFF",sid,"arrive",null,null,false);
  reject(()->f.change(courier,"DELIVERY_STAFF",sid,"deliver","https://example.invalid/evidence",null,false),"COD requires collection confirmation");
  f.change(courier,"DELIVERY_STAFF",sid,"deliver","https://example.invalid/evidence",null,true);
  reject(()->f.change(courier,"DELIVERY_STAFF",sid,"deliver","https://example.invalid/evidence",null,true),"duplicate delivery does not consume twice");
 }
 public static void main(String[]args)throws Exception{
  fixture();try{
   CartService cart=new CartService();CheckoutService checkout=new CheckoutService();FulfillmentService flow=new FulfillmentService();
   cart.change(buyer,"add",variant,2);cart.change(buyer,"add",variant2,1);
   long oid=place("",0,"COD"),first=sid(oid,store),second=sid(oid,store2);
   // ---- Iteration 2.2 (BA-14, BA-15, BA-44) at placement ----
   long window=n(Optional.ofNullable(one("SELECT TRY_CONVERT(int,value) n FROM settings WHERE type='POLICY' AND name='cancellation_window_minutes' AND status='ACTIVE'")).map(r->r.get("n")).orElse(30));
   check(n(one("SELECT DATEDIFF(second,placed_at,cancel_deadline_at) n FROM orders WHERE id=?",oid).get("n"))==window*60,"BA-15 cancel deadline = placed_at + cancellation window");
   check(one("SELECT id FROM audit_logs WHERE entity_type='Order' AND entity_id=? AND action='ORDER_PLACED' AND old_value='CART' AND new_value='CONFIRMED'",oid)!=null,"BA-44 placement audited (CART → CONFIRMED)");
   for(long[] so:new long[][]{{first,seller,store},{second,other,store2}})
     check(util.Messages.get(util.Messages.MSG83,code(oid,so[2])).equals(one("SELECT content FROM notifications WHERE user_id=? AND dedup_key=?",so[1],"seller-order:new:"+so[0]).get("content")),"BA-14 store owner notified of new COD order (MSG83) for store "+so[2]);
   check(n(one("SELECT delivery_address_id FROM orders WHERE id=?",oid).get("delivery_address_id"))==address,"placed order records the chosen delivery_address_id");
   BigDecimal areaFee=(BigDecimal)one("SELECT TRY_CONVERT(decimal(15,2),JSON_VALUE(s.value,'$.delivery_fee')) fee FROM settings s JOIN addresses a ON a.city=s.name WHERE s.type='SERVICE_AREA' AND a.id=?",address).get("fee");
   check(((BigDecimal)one("SELECT delivery_fee FROM seller_orders WHERE id=?",first).get("delivery_fee")).compareTo(areaFee)==0 && ((BigDecimal)one("SELECT delivery_fee FROM seller_orders WHERE id=?",second).get("delivery_fee")).compareTo(areaFee)==0,"BA-11 each seller order pays the area delivery fee");
   check(((BigDecimal)one("SELECT delivery_fee FROM orders WHERE id=?",oid).get("delivery_fee")).compareTo(areaFee.multiply(new BigDecimal(2)))==0,"BA-11 order delivery fee is the sum of its seller orders");
   check(n(one("SELECT SUM(reserved_qty) n FROM batches WHERE variant_id IN (?,?)",variant,variant2).get("n"))==3,"all sellers reserved");
   reject(()->flow.change(seller,"SELLER",first,"reject","",null,false),"rejection requires reason");
   flow.change(other,"SELLER",second,"reject","Unable to fulfill",null,false);
   check(util.Messages.get(util.Messages.MSG47,code(oid,store2),"Unable to fulfill").equals(notice(buyer,"seller-order:"+second+":REJECTED:%")),"BA-45 customer gets MSG47 with the rejection reason and seller order code");
   check(n(one("SELECT reserved_qty FROM batches WHERE variant_id=?",variant2).get("reserved_qty"))==0,"rejection releases only rejected seller stock");
   check("CONFIRMED".equals(one("SELECT status FROM orders WHERE id=?",oid).get("status")),"other seller order remains active");
   deliver(oid,first,seller);
   check("COMPLETED".equals(one("SELECT status FROM orders WHERE id=?",oid).get("status")),"parent completes after remaining seller delivers");
   check(n(one("SELECT COUNT(*) n FROM audit_logs WHERE entity_type='Order' AND entity_id=? AND action='ORDER_COMPLETED'",oid).get("n"))==1,"BA-44 1 Delivered + 1 Rejected completes the order exactly once (audited)");
   check(util.Messages.get(util.Messages.MSG61,code(oid,store)).equals(notice(buyer,"seller-order:"+first+":DELIVERED:%")),"BA-45 customer gets MSG61 on delivery");
   check(n(one("SELECT remaining_qty FROM batches WHERE variant_id=?",variant).get("remaining_qty"))==98,"delivery consumes allocated quantity once");
   check(n(one("SELECT points_balance FROM users WHERE id=?",buyer).get("points_balance"))==20,"delivery earns points once");
   check(new LoyaltyService().wallet(buyer).getTier()!=null,"membership tier recalculated");
   FinanceService finance=new FinanceService();finance.request(buyer,first,"Damaged fruit");long refund=n(one("SELECT id FROM transactions WHERE type='REFUND' AND seller_order_id=?",first).get("id"));
   finance.request(buyer,first,"Repeated request");check(n(one("SELECT COUNT(*) n FROM transactions WHERE type='REFUND' AND seller_order_id=?",first).get("n"))==1,"duplicate refund request cannot over-refund");
   reject(()->finance.decide(seller,refund,"approve","Approved"),"refund review is admin only");finance.decide(admin,refund,"approve","Evidence accepted");reject(()->finance.decide(admin,refund,"complete","no evidence URL"),"manual refund requires valid evidence");finance.decide(admin,refund,"complete","https://example.invalid/manual-refund");
   check("REFUNDED".equals(one("SELECT status FROM transactions WHERE id=?",refund).get("status")),"manual COD refund confirmed");check(n(one("SELECT points_balance FROM users WHERE id=?",buyer).get("points_balance"))==0,"refund reverses unspent earned points");
   reject(()->finance.settle(admin,first),"settlement waits for refund window");
   cart.change(buyer,"add",variant,1);long cancelOrder=place("",0,"COD"),cancelSid=sid(cancelOrder,store);flow.change(seller,"SELLER",cancelSid,"accept",null,null,false);flow.change(buyer,"CUSTOMER",cancelSid,"cancel",null,null,false);check("CANCELLED".equals(one("SELECT status FROM orders WHERE id=?",cancelOrder).get("status")),"customer can cancel accepted order within window");
   check(one("SELECT id FROM audit_logs WHERE entity_type='Order' AND entity_id=? AND action='ORDER_CANCELLED' AND old_value='CONFIRMED' AND new_value='CANCELLED'",cancelOrder)!=null,"BA-44 whole-order cancellation audited");
   check(util.Messages.get(util.Messages.MSG48,code(cancelOrder,store)).equals(notice(seller,"seller-order:"+cancelSid+":CANCELLED:%:seller")),"BA-45 seller gets MSG48 when the customer cancels");
   check(n(one("SELECT COUNT(*) n FROM notifications WHERE dedup_key LIKE ?","seller-order:"+cancelSid+":CANCELLED:%").get("n"))==2,"BA-45 customer and seller each notified once of the cancellation");
   Map<String,String> p=new HashMap<>();p.put("action","save");p.put("name",tag+" voucher");p.put("code",tag.replace("-","")+"OFF");p.put("discountType","PERCENT");p.put("value","10");p.put("budget","1000000");p.put("minimum","0");p.put("limit","10");p.put("perCustomer","1");p.put("start","2026-01-01T00:00");p.put("end","2030-01-01T00:00");new PromotionService().save(admin,"ADMIN",TestForms.promotion(p));long promo=n(one("SELECT id FROM promotions WHERE name=?",tag+" voucher").get("id"));new PromotionService().save(admin,"ADMIN",TestForms.promotion(Map.of("action","state","id",Long.toString(promo),"state","SCHEDULED")));
   cart.change(buyer,"add",variant,1);CheckoutQuote quote=checkout.quote(buyer,address,p.get("code"),0,"COD");check(((BigDecimal)quote.getDiscount()).compareTo(new BigDecimal("10000"))==0,"voucher computes server-side discount");long discountOrder=place(p.get("code"),0,"COD");
   cart.change(buyer,"add",variant,1);reject(()->checkout.quote(buyer,address,p.get("code"),0,"COD"),"voucher per-customer limit enforced");
   flow.change(buyer,"CUSTOMER",sid(discountOrder,store),"cancel",null,null,false);check(((BigDecimal)one("SELECT budget_used FROM promotions WHERE id=?",promo).get("budget_used")).signum()==0,"full cancellation restores voucher budget");
   // ---- Iteration 2.1: checkout quote (BA-09 … BA-18). The buyer's cart holds 1 x variant here. ----
   long sellerAddress=n(one("SELECT id FROM addresses WHERE user_id=?",seller).get("id")),otherAddress=n(one("SELECT id FROM addresses WHERE user_id=?",other).get("id"));
   rejectWith(()->checkout.quote(other,otherAddress),util.Messages.get(util.Messages.MSG35),"BA-17 empty cart gives MSG35");
   cart.change(seller,"add",variant,1);
   rejectWith(()->checkout.quote(seller,sellerAddress),util.Messages.get(util.Messages.MSG12),"BA-09 seller cannot check out own store products (MSG12)");
   String city=(String)one("SELECT city FROM addresses WHERE id=?",address).get("city");long messy,unknown;
   try(Connection c=db()){messy=Database.insert(c,"INSERT addresses(user_id,recipient_name,phone,address_line,ward,district,city) VALUES(?,'Test','0900000000','Test','Test','Test',?)",buyer,"  "+city.toUpperCase(Locale.ROOT).replace(" ","   ")+" ");
     unknown=Database.insert(c,"INSERT addresses(user_id,recipient_name,phone,address_line,ward,district,city) VALUES(?,'Test','0900000000','Test','Test','Test',?)",buyer,"Nowhere "+tag);}
   check(checkout.quote(buyer,messy)!=null,"BA-16 service area matches ignoring case and extra spaces");
   rejectWith(()->checkout.quote(buyer,unknown),util.Messages.get(util.Messages.MSG36),"BA-16 unknown area gives MSG36");
   update("INSERT transactions(type,user_id,points,remaining_points,status,points_expire_at,idempotency_key) VALUES('POINTS_EARN',?,30,30,'AVAILABLE',DATEADD(day,-1,CONVERT(date,SYSUTCDATETIME())),?)",buyer,tag+"-expired");
   update("INSERT transactions(type,user_id,points,remaining_points,status,points_expire_at,idempotency_key) VALUES('POINTS_EARN',?,500,500,'AVAILABLE',DATEADD(day,365,CONVERT(date,SYSUTCDATETIME())),?)",buyer,tag+"-fresh");
   update("UPDATE users SET points_balance=points_balance+530 WHERE id=?",buyer);
   long expiresBefore=n(one("SELECT COUNT(*) n FROM transactions WHERE user_id=? AND type='POINTS_EXPIRE'",buyer).get("n"));
   CheckoutQuote pq=checkout.quote(buyer,address,"",0,"COD");
   check(n(one("SELECT points_balance FROM users WHERE id=?",buyer).get("points_balance"))==530 && n(one("SELECT COUNT(*) n FROM transactions WHERE user_id=? AND type='POINTS_EXPIRE'",buyer).get("n"))==expiresBefore && pq.getUsablePoints()==500,"BA-10 quote reads usable points without writing");
   Map<String,Object> pct=one("SELECT TRY_CONVERT(int,value) n FROM settings WHERE type='POLICY' AND name='points_max_percent' AND status='ACTIVE'");
   long percent=pct==null || pct.get("n")==null?50:Math.min(100,n(pct.get("n"))),max1=Math.min(500,Math.min(100000*percent/100/1000,100));
   check(pq.getMaxPoints()==max1,"BA-12 maximum points follows points_max_percent");
   rejectWith(()->checkout.quote(buyer,address,"",(int)max1+1,"COD"),util.Messages.get(util.Messages.MSG38,max1),"BA-12 too many points gives MSG38 with the maximum");
   cart.change(buyer,"add",variant2,1);long max2=Math.min(500,Math.min(200000*percent/100/1000,200));
   long pointsOrder=place("",(int)max2,"COD");
   check(n(one("SELECT SUM(points_used) n FROM seller_orders WHERE order_id=?",pointsOrder).get("n"))==max2,"BA-12 maximum points split across two seller orders");
   check(n(one("SELECT COUNT(*) n FROM transactions WHERE user_id=? AND type='POINTS_EXPIRE'",buyer).get("n"))==expiresBefore+1 && n(one("SELECT points_balance FROM users WHERE id=?",buyer).get("points_balance"))==500-max2,"BA-10 placement (not the quote) expires old points");
   String areaName=tag+" area";long etaAddress,variant3,shortBatch,longBatch;
   try(Connection c=db()){Database.update(c,"INSERT settings(type,name,value) VALUES('SERVICE_AREA',?,?)",areaName,"{\"delivery_fee\":15000,\"eta_hours\":48}");
     etaAddress=Database.insert(c,"INSERT addresses(user_id,recipient_name,phone,address_line,ward,district,city) VALUES(?,'Test','0900000000','Test','Test','Test',?)",buyer,areaName);
     variant3=Database.insert(c,"INSERT variants(listing_id,option_name,unit,price) VALUES(?,'2 kg box','box',100000)",TestRows.one(c,"SELECT listing_id FROM variants WHERE id=?",variant).get("listing_id"));
     shortBatch=Database.insert(c,"INSERT batches(variant_id,batch_no,origin,harvest_date,receipt_date,expiry_date,supplier_name,received_qty,remaining_qty,unit_cost) VALUES(?,?,'Test',CONVERT(date,SYSUTCDATETIME()),CONVERT(date,SYSUTCDATETIME()),DATEADD(day,1,CONVERT(date,SYSUTCDATETIME())),'Test',10,10,50000)",variant3,tag+"short");
     longBatch=Database.insert(c,"INSERT batches(variant_id,batch_no,origin,harvest_date,receipt_date,expiry_date,supplier_name,received_qty,remaining_qty,unit_cost) VALUES(?,?,'Test',CONVERT(date,SYSUTCDATETIME()),CONVERT(date,SYSUTCDATETIME()),DATEADD(day,30,CONVERT(date,SYSUTCDATETIME())),'Test',5,5,50000)",variant3,tag+"long");}
   cart.change(buyer,"add",variant3,6);
   rejectWith(()->checkout.quote(buyer,etaAddress),util.Messages.get(util.Messages.MSG33,5,"box","2 kg box"),"BA-13 quote counts only batches fresh on the delivery date (MSG33)");
   long line3=n(one("SELECT oi.id FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN orders o ON o.id=so.order_id WHERE o.customer_id=? AND o.status='CART' AND oi.variant_id=?",buyer,variant3).get("id"));
   cart.change(buyer,"update",line3,2);
   CheckoutQuote eq=checkout.quote(buyer,etaAddress);
   check(eq.getEtaHours()==48 && eq.getFee().compareTo(new BigDecimal("15000"))==0,"BA-11 service area eta_hours and fee are used");
   rejectWith(()->checkout.place(buyer,etaAddress,UUID.randomUUID().toString(),"stale","",0,"COD"),util.Messages.get(util.Messages.MSG34),"BA-17 changed quote at placement gives MSG34");
   long etaOrder=checkout.place(buyer,etaAddress,UUID.randomUUID().toString(),eq.getFingerprint(),"",0,"COD");
   check(n(one("SELECT COUNT(*) n FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id WHERE so.order_id=? AND oi.batch_id<>?",etaOrder,longBatch).get("n"))==0 && n(one("SELECT reserved_qty FROM batches WHERE id=?",shortBatch).get("reserved_qty"))==0,"BA-13 reservation skips batches expiring before delivery");
   Map<String,Object> placed=one("SELECT order_code,payable_amount FROM orders WHERE id=?",etaOrder);
   check(util.Messages.get(util.Messages.MSG39,placed.get("order_code"),new java.text.DecimalFormat("#,##0").format(placed.get("payable_amount"))+" đ").equals(checkout.placedNotice(buyer,etaOrder)),"BA-18 COD placement notice is MSG39");
   long awaiting;try(Connection c=db()){awaiting=Database.insert(c,"INSERT orders(customer_id,order_code,status,recipient_name,recipient_phone,delivery_address,payment_method,placed_at,payment_expires_at) VALUES(?,?,'AWAITING_PAYMENT','Test','0900000000','Test address','CARD',SYSUTCDATETIME(),'2026-01-01T03:00:00')",buyer,tag+"P");}
   check(util.Messages.get(util.Messages.MSG40,tag+"P","10:00 01/01/2026").equals(checkout.placedNotice(buyer,awaiting)),"BA-18 online notice is MSG40 with the deadline in Vietnam time");
   check(checkout.placedNotice(seller,awaiting)==null,"BA-18 notice is not shown for another customer's order");
   cart.change(buyer,"add",variant,1);
   rejectWith(()->checkout.quote(buyer,address,"NOPE"+tag.replace("-",""),0,"COD"),util.Messages.get(util.Messages.MSG37,"mã không hợp lệ hoặc ngoài thời gian áp dụng"),"BA-17 unknown voucher gives MSG37");
   // ---- Iteration 2.2: expired cancel deadline (BA-15) and failed delivery (BA-45). The buyer's cart holds 1 x variant here. ----
   long lateOrder=place("",0,"COD"),lateSid=sid(lateOrder,store);
   update("UPDATE orders SET cancel_deadline_at=DATEADD(minute,-1,SYSUTCDATETIME()) WHERE id=?",lateOrder);
   reject(()->flow.change(buyer,"CUSTOMER",lateSid,"cancel",null,null,false),"BA-15 cancellation after the snapshotted deadline is refused");
   check("NEW".equals(one("SELECT status FROM seller_orders WHERE id=?",lateSid).get("status")),"BA-15 refused cancellation leaves the seller order unchanged");
   flow.change(seller,"SELLER",lateSid,"accept",null,null,false);flow.change(seller,"SELLER",lateSid,"ready",null,null,false);flow.change(admin,"ADMIN",lateSid,"assign","Keep chilled",courier,false);
   flow.change(courier,"DELIVERY_STAFF",lateSid,"fail","Customer not at home",null,false);
   String failed=util.Messages.get(util.Messages.MSG63,code(lateOrder,store),"Customer not at home");
   check(failed.equals(notice(buyer,"seller-order:"+lateSid+":FAILED:%")),"BA-45 customer gets MSG63 with the failure reason");
   check(failed.equals(notice(admin,"seller-order:"+lateSid+":FAILED:%:admin:"+admin)),"BA-45 every active admin gets MSG63 for a failed delivery");
   check(util.Messages.get(util.Messages.MSG49,code(lateOrder,store),util.Vietnamese.label("ACCEPTED")).equals(notice(buyer,"seller-order:"+lateSid+":ACCEPTED:%")),"BA-45 other status changes use MSG49");
   check(one("SELECT id FROM notifications WHERE dedup_key LIKE ? AND dedup_key NOT LIKE ?","seller-order:"+lateSid+":ASSIGNED:[0-9]%","%:%:%:%:%")!=null,"BA-45 dedup key carries the audit id");
   try(Connection c=db()){check(!new dao.NotificationDAO().send(c,buyer,"ORDER_UPDATE","x","x","/orders","seller-order:new:"+first),"BA-45 duplicate dedup key is skipped without an error");}
   System.out.println("TEST TAG "+tag+" / "+count+" checks passed");
  }finally{
   // Keep immutable financial history. Disable only dedicated fixture users/listings.
   update("UPDATE listings SET status='HIDDEN' WHERE store_id IN (?,?)",store,store2);update("UPDATE settings SET status='INACTIVE' WHERE type='SERVICE_AREA' AND name=?",tag+" area");update("UPDATE users SET status='SUSPENDED' WHERE id IN (?,?,?,?,?)",buyer,seller,other,admin,courier);
  }
 }
}
