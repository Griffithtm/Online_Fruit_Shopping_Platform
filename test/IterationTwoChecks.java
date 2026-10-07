import config.DBConnect;import dao.Database;import service.*;import java.sql.*;import java.util.*;import java.math.*;
public class IterationTwoChecks {
 static String tag="I2-"+System.currentTimeMillis();static long buyer,seller,other,admin,courier,store,store2,variant,variant2,address;static int count;
 interface Task{void run()throws Exception;}
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);System.out.println("PASS "+(++count)+" "+msg);}
 static void reject(Task task,String msg)throws Exception{try{task.run();throw new AssertionError(msg);}catch(IllegalArgumentException expected){check(true,msg);}}
 static long n(Object x){return ((Number)x).longValue();}
 static Connection db()throws Exception{return new DBConnect().getConnection();}
 static long user(Connection c,String suffix,String role)throws Exception{return Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,'test-only',?,?)",tag+suffix+"@test.invalid",tag+suffix,role);}
 static Map<String,Object> one(String sql,Object...args)throws Exception{try(Connection c=db()){return Database.one(c,sql,args);}}
 static void update(String sql,Object...args)throws Exception{try(Connection c=db()){Database.update(c,sql,args);}}
 static long sid(long oid,long st)throws Exception{return n(one("SELECT id FROM seller_orders WHERE order_id=? AND store_id=?",oid,st).get("id"));}
 static long place(String voucher,int points,String method)throws Exception{
  CheckoutService service=new CheckoutService();Map<String,Object> q=service.quote(buyer,address,voucher,points,method);String key=UUID.randomUUID().toString();long oid=service.place(buyer,address,key,(String)q.get("fingerprint"),voucher,points,method);
  check(oid==service.place(buyer,address,key,(String)q.get("fingerprint"),voucher,points,method),"placement idempotency");return oid;
 }
 static void fixture()throws Exception{try(Connection c=db()){
  buyer=user(c,"buyer","CUSTOMER");seller=user(c,"seller","SELLER");other=user(c,"other","SELLER");admin=user(c,"admin","ADMIN");courier=user(c,"courier","DELIVERY_STAFF");
  long cat=n(Database.one(c,"SELECT TOP 1 id FROM settings WHERE type='CATEGORY'",new Object[0]).get("id"));long tier=n(Database.one(c,"SELECT TOP 1 id FROM settings WHERE type='SELLER_TIER'").get("id"));
  store=Database.insert(c,"INSERT stores(owner_id,store_name,seller_tier_id) VALUES(?,?,?)",seller,tag+"store",tier);store2=Database.insert(c,"INSERT stores(owner_id,store_name,seller_tier_id) VALUES(?,?,?)",other,tag+"store2",tier);
  String city=(String)Database.one(c,"SELECT TOP 1 name FROM settings WHERE type='SERVICE_AREA' AND status='ACTIVE'").get("name");
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
   check(n(one("SELECT SUM(reserved_qty) n FROM batches WHERE variant_id IN (?,?)",variant,variant2).get("n"))==3,"all sellers reserved");
   reject(()->flow.change(seller,"SELLER",first,"reject","",null,false),"rejection requires reason");
   flow.change(other,"SELLER",second,"reject","Unable to fulfill",null,false);
   check(n(one("SELECT reserved_qty FROM batches WHERE variant_id=?",variant2).get("reserved_qty"))==0,"rejection releases only rejected seller stock");
   check("CONFIRMED".equals(one("SELECT status FROM orders WHERE id=?",oid).get("status")),"other seller order remains active");
   deliver(oid,first,seller);
   check("COMPLETED".equals(one("SELECT status FROM orders WHERE id=?",oid).get("status")),"parent completes after remaining seller delivers");
   check(n(one("SELECT remaining_qty FROM batches WHERE variant_id=?",variant).get("remaining_qty"))==98,"delivery consumes allocated quantity once");
   check(n(one("SELECT points_balance FROM users WHERE id=?",buyer).get("points_balance"))==20,"delivery earns points once");
   check(new LoyaltyService().wallet(buyer).get("tier")!=null,"membership tier recalculated");
   FinanceService finance=new FinanceService();finance.request(buyer,first,"Damaged fruit");long refund=n(one("SELECT id FROM transactions WHERE type='REFUND' AND seller_order_id=?",first).get("id"));
   finance.request(buyer,first,"Repeated request");check(n(one("SELECT COUNT(*) n FROM transactions WHERE type='REFUND' AND seller_order_id=?",first).get("n"))==1,"duplicate refund request cannot over-refund");
   reject(()->finance.decide(seller,refund,"approve","Approved"),"refund review is admin only");finance.decide(admin,refund,"approve","Evidence accepted");reject(()->finance.decide(admin,refund,"complete","no evidence URL"),"manual refund requires valid evidence");finance.decide(admin,refund,"complete","https://example.invalid/manual-refund");
   check("REFUNDED".equals(one("SELECT status FROM transactions WHERE id=?",refund).get("status")),"manual COD refund confirmed");check(n(one("SELECT points_balance FROM users WHERE id=?",buyer).get("points_balance"))==0,"refund reverses unspent earned points");
   reject(()->finance.settle(admin,first),"settlement waits for refund window");
   cart.change(buyer,"add",variant,1);long cancelOrder=place("",0,"COD"),cancelSid=sid(cancelOrder,store);flow.change(seller,"SELLER",cancelSid,"accept",null,null,false);flow.change(buyer,"CUSTOMER",cancelSid,"cancel",null,null,false);check("CANCELLED".equals(one("SELECT status FROM orders WHERE id=?",cancelOrder).get("status")),"customer can cancel accepted order within window");
   Map<String,String> p=new HashMap<>();p.put("action","save");p.put("name",tag+" voucher");p.put("code",tag.replace("-","")+"OFF");p.put("discountType","PERCENT");p.put("value","10");p.put("budget","1000000");p.put("minimum","0");p.put("limit","10");p.put("perCustomer","1");p.put("start","2026-01-01T00:00");p.put("end","2030-01-01T00:00");new PromotionService().save(admin,"ADMIN",p);long promo=n(one("SELECT id FROM promotions WHERE name=?",tag+" voucher").get("id"));new PromotionService().save(admin,"ADMIN",Map.of("action","state","id",Long.toString(promo),"state","SCHEDULED"));
   cart.change(buyer,"add",variant,1);Map<String,Object> quote=checkout.quote(buyer,address,p.get("code"),0,"COD");check(((BigDecimal)quote.get("discount")).compareTo(new BigDecimal("10000"))==0,"voucher computes server-side discount");long discountOrder=place(p.get("code"),0,"COD");
   cart.change(buyer,"add",variant,1);reject(()->checkout.quote(buyer,address,p.get("code"),0,"COD"),"voucher per-customer limit enforced");
   flow.change(buyer,"CUSTOMER",sid(discountOrder,store),"cancel",null,null,false);check(((BigDecimal)one("SELECT budget_used FROM promotions WHERE id=?",promo).get("budget_used")).signum()==0,"full cancellation restores voucher budget");
   System.out.println("TEST TAG "+tag+" / "+count+" checks passed");
  }finally{
   // Keep immutable financial history. Disable only dedicated fixture users/listings.
   update("UPDATE listings SET status='HIDDEN' WHERE store_id IN (?,?)",store,store2);update("UPDATE users SET status='SUSPENDED' WHERE id IN (?,?,?,?,?)",buyer,seller,other,admin,courier);
  }
 }
}
