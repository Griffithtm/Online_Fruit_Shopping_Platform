import config.DBConnect;
import dao.Database;
import service.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

public class IterationOneChecks {
    static String tag="I1"+System.currentTimeMillis();
    static long buyer,seller,other,store,store2,listing,variant,category;
    static Connection open() throws Exception {return new DBConnect().getConnection();}
    static long number(Object o){return ((Number)o).longValue();}
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS "+name);}
    static void reject(Throwing action,String name)throws Exception {try{action.run();throw new AssertionError(name);}catch(IllegalArgumentException expected){System.out.println("PASS "+name);}}
    static void rejectWith(Throwing action,String message,String name)throws Exception {try{action.run();throw new AssertionError(name);}catch(IllegalArgumentException expected){if(!message.equals(expected.getMessage()))throw new AssertionError(name+": got \""+expected.getMessage()+"\"");System.out.println("PASS "+name);}}
    interface Throwing {void run()throws Exception;}
    public static void main(String[] args)throws Exception {
      check(UnitPrice.perKg("500 g box",new BigDecimal("25000")).compareTo(new BigDecimal("50000"))==0,"normalize grams to VND/kg");
      check(UnitPrice.perKg("1.5 kg bag",new BigDecimal("60000")).compareTo(new BigDecimal("40000"))==0,"normalize decimal kg");
      check(UnitPrice.perKg("box",BigDecimal.TEN)==null && UnitPrice.perKg("2 x 500 g",BigDecimal.TEN)==null,"do not guess ambiguous pack weights");
      try(Connection c=open()) {
        buyer=Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,'unused',?,'CUSTOMER')",tag+"b@test.invalid",tag);
        seller=Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,'unused',?,'SELLER')",tag+"s@test.invalid",tag);
        other=Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,'unused',?,'SELLER')",tag+"o@test.invalid",tag);
        store=Database.insert(c,"INSERT stores(owner_id,store_name) VALUES(?,?)",seller,tag+"store");
        store2=Database.insert(c,"INSERT stores(owner_id,store_name) VALUES(?,?)",other,tag+"other");
        category=number(Database.one(c,"SELECT TOP 1 id FROM settings WHERE type='CATEGORY' AND status='ACTIVE'").get("id"));
      }
      try {
        SellerService ss=new SellerService(); AccountService as=new AccountService(); CatalogService cs=new CatalogService();
        ss.listing(seller,null,tag,"Integration test",String.valueOf(category),"Test origin","Test harvest");
        listing=number(ss.listings(seller).get(0).get("id"));
        ss.variant(seller,null,listing,"500 g box","box","25000",false);
        variant=number(ss.variants(seller).get(0).get("id"));
        reject(()->ss.listing(other,""+listing,"Intrusion","",String.valueOf(category),"Test",""),"seller ownership enforcement");
        ss.submit(seller,listing);
        check(cs.product(listing)==null,"pending listing is not public");
        try(Connection c=open()){Database.update(c,"UPDATE listings SET status='APPROVED' WHERE id=?",listing);}
        check(cs.products(tag,null,null,null,null,null,"new",false,"out").size()==1,"out-of-stock listing filter");
        check(cs.product(listing)!=null,"out-of-stock detail remains readable");
        ss.stock(seller,variant,tag,"Test","2026-01-01","2026-01-02","2098-01-01","Supplier","1","10000");
        ss.stock(seller,variant,tag+"b","Test","2026-01-01","2026-01-02","2099-01-01","Supplier","2","12000");
        check(cs.products(tag,null,null,null,null,null,"unitPrice",false,"in").size()==1,"in-stock catalog and unit price sorting");
        check(ss.suggestions(seller,tag).isEmpty(),"suggestions exclude own store");
        check(ss.suggestions(other,tag).size()==1,"other seller receives matching suggestion");
        reject(()->cs.products(tag,null,null,"100","10",null,"new",false,"all"),"invalid price interval rejected");
        String buyerEmail=(tag+"b@test.invalid").toLowerCase(Locale.ROOT);
        Map<String,Object> before=null;
        try(Connection c=open()){before=Database.one(c,"SELECT role,points_balance FROM users WHERE id=?",buyer);}
        as.profile(buyer,tag,buyerEmail.toUpperCase(Locale.ROOT),"0912 345.678");
        Map<String,Object> saved=as.profile(buyer);
        check(buyerEmail.equals(saved.get("email")),"profile: own email in upper case is saved lower case, not MSG13");
        check("0912345678".equals(saved.get("phone")),"profile: phone spaces/dots removed before saving");
        rejectWith(()->as.profile(buyer,tag,(tag+"o@test.invalid").toUpperCase(Locale.ROOT),null),util.Messages.get(util.Messages.MSG13),"profile: another user's email in different case gives MSG13");
        rejectWith(()->as.profile(buyer,tag,buyerEmail,"abc"),"Enter a valid phone number.","profile: phone letters give MSG04");
        rejectWith(()->as.profile(buyer,tag,buyerEmail,"123"),"Enter a valid phone number.","profile: phone shorter than 7 digits gives MSG04");
        rejectWith(()->as.profile(buyer," ",buyerEmail,null),"Full name is required.","profile: blank name gives MSG02");
        rejectWith(()->as.profile(buyer,"x".repeat(101),buyerEmail,null),"Full name must not exceed 100 characters.","profile: 101-character name gives MSG03");
        rejectWith(()->as.profile(buyer,tag,"not-an-email",null),"Enter a valid email address.","profile: malformed email gives MSG04");
        as.profile(buyer,tag,buyerEmail,"  ");
        check(as.profile(buyer).get("phone")==null,"profile: blank phone is stored as NULL");
        // The service API has no role/points input; the servlet only reads name, email and phone,
        // so role=ADMIN / points_balance=9999 in the form cannot reach the UPDATE.
        try(Connection c=open()){Map<String,Object> after=Database.one(c,"SELECT role,points_balance FROM users WHERE id=?",buyer);
          check(before.equals(after) && "CUSTOMER".equals(after.get("role")),"profile: role and points unchanged by profile saves");}
        as.address(buyer,"saveAddress",null,"Buyer","0900000000","Line","Ward","District","City",false);
        check(Boolean.TRUE.equals(as.addresses(buyer).get(0).get("is_default")),"first address automatically default");
        CartService cart=new CartService();cart.change(buyer,"add",variant,2);
        check(cart.lines(buyer).size()==1 && number(cart.lines(buyer).get(0).get("quantity"))==2,"cart add and totals");
        reject(()->cart.change(buyer,"add",variant,2),"cart rejects quantity above stock");
        long line=number(cart.lines(buyer).get(0).get("id"));cart.change(buyer,"update",line,1);cart.change(buyer,"remove",line,0);
        check(cart.lines(buyer).isEmpty(),"cart update and removal");
        long[] orders=new long[2];
        try(Connection c=open()) {
          for(int i=0;i<2;i++) {
            long o=Database.insert(c,"INSERT orders(customer_id,order_code,status,recipient_name,recipient_phone,delivery_address,payment_method,placed_at) VALUES(?,?,'CONFIRMED','Test','0900000000','Test address','COD',SYSUTCDATETIME())",buyer,tag+i);
            orders[i]=Database.insert(c,"INSERT seller_orders(order_id,store_id,status) VALUES(?,?,'NEW')",o,store);
            Database.insert(c,"INSERT order_items(seller_order_id,variant_id,quantity,unit_price) VALUES(?,?,2,25000)",orders[i],variant);
          }
        }
        ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch gate=new CountDownLatch(1);
        List<Future<Boolean>> results=new ArrayList<>();
        for(long order:orders) results.add(pool.submit(()->{try(Connection c=open()){c.setAutoCommit(false);gate.await();try{new StockReservationService().reserve(c,order);new StockReservationService().reserve(c,order);c.commit();return true;}catch(IllegalArgumentException e){c.rollback();return false;}}}));
        gate.countDown();int successes=0;
        try{for(Future<Boolean> result:results)if(result.get(30,TimeUnit.SECONDS))successes++;}finally{pool.shutdownNow();}
        check(successes==1,"concurrent orders: exactly one reserves the last stock");
        try(Connection c=open()) {
          check(number(Database.one(c,"SELECT SUM(reserved_qty) n FROM batches WHERE variant_id=?",variant).get("n"))==2,"retry is idempotent; failed allocation rolls back");
          check(number(Database.one(c,"SELECT reserved_qty FROM batches WHERE batch_no=?",tag).get("reserved_qty"))==1,"FEFO consumes the earlier expiry batch first");
        }
      } finally {cleanup();}
    }
    static void cleanup()throws Exception {
      try(Connection c=open()) {
        c.setAutoCommit(false);
        Database.update(c,"DELETE FROM stock_movements WHERE batch_id IN (SELECT b.id FROM batches b JOIN variants v ON v.id=b.variant_id JOIN listings l ON l.id=v.listing_id WHERE l.store_id IN (?,?))",store,store2);
        Database.update(c,"DELETE FROM order_items WHERE seller_order_id IN (SELECT so.id FROM seller_orders so JOIN orders o ON o.id=so.order_id WHERE o.customer_id=?)",buyer);
        Database.update(c,"DELETE FROM seller_orders WHERE order_id IN (SELECT id FROM orders WHERE customer_id=?)",buyer);
        Database.update(c,"DELETE FROM orders WHERE customer_id=?",buyer);
        Database.update(c,"DELETE FROM batches WHERE variant_id IN (SELECT v.id FROM variants v JOIN listings l ON l.id=v.listing_id WHERE l.store_id IN (?,?))",store,store2);
        Database.update(c,"DELETE FROM variants WHERE listing_id IN (SELECT id FROM listings WHERE store_id IN (?,?))",store,store2);
        Database.update(c,"DELETE FROM listings WHERE store_id IN (?,?)",store,store2);
        Database.update(c,"DELETE FROM stores WHERE id IN (?,?)",store,store2);
        Database.update(c,"DELETE FROM addresses WHERE user_id=?",buyer);
        Database.update(c,"DELETE FROM users WHERE id IN (?,?,?)",buyer,seller,other);
        c.commit();
      }
    }
}
