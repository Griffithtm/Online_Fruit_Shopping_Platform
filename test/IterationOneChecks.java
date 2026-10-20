import model.*;
import config.DBConnect;
import dao.Database;
import service.*;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

public class IterationOneChecks {
    static String tag="I1"+System.currentTimeMillis();
    static long buyer,seller,other,keeper,pwUser,store,store2,listing,variant,category;
    static Connection open() throws Exception {return new DBConnect().getConnection();}
    static long number(Object o){return ((Number)o).longValue();}
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS "+name);}
    static void reject(Throwing action,String name)throws Exception {try{action.run();throw new AssertionError(name);}catch(IllegalArgumentException expected){System.out.println("PASS "+name);}}
    static void rejectWith(Throwing action,String message,String name)throws Exception {try{action.run();throw new AssertionError(name);}catch(IllegalArgumentException expected){if(!message.equals(expected.getMessage()))throw new AssertionError(name+": got \""+expected.getMessage()+"\"");System.out.println("PASS "+name);}}
    interface Throwing {void run()throws Exception;}
    /** Id of the user's default address, or 0 when there is none. */
    static long defaultOf(long uid)throws Exception {try(Connection c=open()){Map<String,Object> a=TestRows.one(c,"SELECT id FROM addresses WHERE user_id=? AND is_default=1",uid);return a==null?0:number(a.get("id"));}}
    static final String BAD="Vui lòng nhập đủ thông tin, đúng độ dài.",LOCK="Chưa thể sửa/xóa địa chỉ khi còn đơn đang xử lý.",PW="Mật khẩu cần 8–64 ký tự, tối đa 72 byte, và nhập lại trùng khớp.";
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
        category=number(TestRows.one(c,"SELECT TOP 1 id FROM settings WHERE type='CATEGORY' AND status='ACTIVE'").get("id"));
      }
      try {
        SellerService ss=new SellerService(); AccountService as=new AccountService(); CatalogService cs=new CatalogService();
        ss.listing(seller,null,tag,"Integration test",String.valueOf(category),"Test origin","Test harvest");
        listing=number(ss.listings(seller).get(0).getId());
        ss.variant(seller,null,listing,"500 g box","box","25000",false);
        variant=number(ss.variants(seller).get(0).getId());
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
        // Profile: the service edits name and phone only (email, role and points are not editable).
        Map<String,Object> before=null;
        try(Connection c=open()){before=TestRows.one(c,"SELECT role,points_balance FROM users WHERE id=?",buyer);}
        as.profile(buyer,"Renamed "+tag,"0912345678");
        User saved=as.profile(buyer);
        check(("Renamed "+tag).equals(saved.getFullName()) && "0912345678".equals(saved.getPhone()),"profile: name and phone are saved");
        rejectWith(()->as.profile(buyer," ","0912345678"),BAD,"profile: blank name rejected");
        rejectWith(()->as.profile(buyer,"x".repeat(101),"0912345678"),BAD,"profile: 101-character name rejected");
        rejectWith(()->as.profile(buyer,tag,"  "),BAD,"profile: blank phone rejected");
        rejectWith(()->as.profile(buyer,tag,"1".repeat(21)),BAD,"profile: 21-character phone rejected");
        try(Connection c=open()){Map<String,Object> after=TestRows.one(c,"SELECT role,points_balance FROM users WHERE id=?",buyer);
          check(before.equals(after) && "CUSTOMER".equals(after.get("role")),"profile: role and points unchanged by profile saves");}
        as.address(buyer,"saveAddress",null,"Buyer","0900000000","Line","Ward","District","City",false);
        check(Boolean.TRUE.equals(as.addresses(buyer).get(0).getIsDefault()),"first address automatically default");
        rejectWith(()->as.address(buyer,"saveAddress",null,"Buyer","0900000000","Line","Ward","District"," ",false),BAD,"address: blank city rejected");
        rejectWith(()->as.address(buyer,"saveAddress",null,"Buyer"," ","Line","Ward","District","City",false),BAD,"address: blank phone rejected");
        as.address(buyer,"saveAddress",null,"Second","0900000001","Line 2","Ward","District","City",false);
        long usedAddress=number(as.addresses(buyer).get(0).getId()),freeAddress=number(as.addresses(buyer).get(1).getId());
        try(Connection c=open()){Database.insert(c,"INSERT orders(customer_id,order_code,status,recipient_name,recipient_phone,delivery_address,payment_method,placed_at) VALUES(?,?,'CONFIRMED','Test','0900000000','Test address','COD',SYSUTCDATETIME())",buyer,tag+"a");}
        rejectWith(()->as.address(buyer,"saveAddress",""+usedAddress,"Buyer","0900000000","Changed","Ward","District","City",true),LOCK,"address: edit blocked while an open order exists");
        rejectWith(()->as.address(buyer,"saveAddress",""+freeAddress,"Second","0900000001","Changed","Ward","District","City",false),LOCK,"address: every address is locked while an open order exists");
        rejectWith(()->as.address(buyer,"deleteAddress",""+usedAddress,null,null,null,null,null,null,false),LOCK,"address: delete blocked while an open order exists");
        as.address(buyer,"saveAddress",null,"Third","0900000002","Line 3","Ward","District","City",false);
        check(as.addresses(buyer).size()==3,"address: a new address can still be added with an open order");
        // Default address rules, on a customer with no open orders so every address can be edited and deleted.
        try(Connection c=open()){keeper=Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,'unused',?,'CUSTOMER')",tag+"k@test.invalid",tag);}
        as.address(keeper,"saveAddress",null,"Alice","0900000010","A street","Ward","District","City",false);
        long addrA=number(as.addresses(keeper).get(0).getId());
        check(defaultOf(keeper)==addrA,"default: first address becomes default even unticked");
        as.address(keeper,"saveAddress",null,"Bob","0900000011","B street","Ward","District","City",false);
        long addrB=number(as.addresses(keeper).get(1).getId());
        check(defaultOf(keeper)==addrA,"default: adding an unticked address keeps the default");
        as.address(keeper,"saveAddress",""+addrB,"Bob","0900000011","B street","Ward","District","City",true);
        check(defaultOf(keeper)==addrB,"default: ticking default while editing makes that address default");
        as.address(keeper,"defaultAddress",""+addrA,null,null,null,null,null,null,false);
        check(defaultOf(keeper)==addrA,"default: Set as default switches to the chosen address");
        try(Connection c=open()){check(number(TestRows.one(c,"SELECT COUNT(*) n FROM addresses WHERE user_id=? AND is_default=1",keeper).get("n"))==1,"default: exactly one default address");}
        rejectWith(()->as.address(keeper,"defaultAddress","abc",null,null,null,null,null,null,false),"Mã dữ liệu không hợp lệ.","default: malformed id rejected");
        rejectWith(()->as.address(keeper,"defaultAddress",""+freeAddress,null,null,null,null,null,null,false),"Không tìm thấy địa chỉ.","default: another user's address rejected");
        as.address(keeper,"saveAddress",null,"Carol","0900000012","C street","Ward","District","City",false);
        long addrC=as.addresses(keeper).stream().mapToLong(a->number(a.getId())).filter(x->x!=addrA && x!=addrB).findFirst().getAsLong();
        as.address(keeper,"defaultAddress",""+addrB,null,null,null,null,null,null,false);
        as.address(keeper,"deleteAddress",""+addrB,null,null,null,null,null,null,false);
        check(defaultOf(keeper)==addrA,"default: deleting the default makes the oldest remaining one default");
        as.address(keeper,"deleteAddress",""+addrC,null,null,null,null,null,null,false);
        check(defaultOf(keeper)==addrA && as.addresses(keeper).size()==1,"default: deleting a non-default address keeps the default");
        as.address(keeper,"deleteAddress",""+addrA,null,null,null,null,null,null,false);
        check(as.addresses(keeper).isEmpty(),"default: deleting the last address leaves none");
        // Password change, with a real BCrypt hash so sign-in can be checked.
        String pwEmail=(tag+"p@test.invalid").toLowerCase(Locale.ROOT),oldPw="OldPass123!",newPw="NewPass456!";
        try(Connection c=open()){pwUser=Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,?,?,'CUSTOMER')",pwEmail,org.mindrot.jbcrypt.BCrypt.hashpw(oldPw,org.mindrot.jbcrypt.BCrypt.gensalt(4)),tag);}
        rejectWith(()->as.password(pwUser,"WrongPass1!",newPw,newPw),"Mật khẩu hiện tại không đúng.","password: wrong current password rejected");
        rejectWith(()->as.password(pwUser,oldPw,"Short1!","Short1!"),PW,"password: 7-character new password rejected");
        rejectWith(()->as.password(pwUser,oldPw,"x".repeat(65),"x".repeat(65)),PW,"password: 65-character new password rejected");
        rejectWith(()->as.password(pwUser,oldPw,newPw,newPw+"x"),PW,"password: confirmation mismatch rejected");
        as.password(pwUser,oldPw,newPw,newPw);
        check(as.login(pwEmail,newPw).getId()==pwUser,"password: new password signs in");
        rejectWith(()->as.login(pwEmail,oldPw),"Thư điện tử hoặc mật khẩu không đúng.","password: old password no longer signs in");
        CartService cart=new CartService();cart.change(buyer,"add",variant,2);
        check(cart.lines(buyer).size()==1 && number(cart.lines(buyer).get(0).getQuantity())==2,"cart add and totals");
        reject(()->cart.change(buyer,"add",variant,2),"cart rejects quantity above stock");
        long line=number(cart.lines(buyer).get(0).getId());cart.change(buyer,"update",line,1);cart.change(buyer,"remove",line,0);
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
          check(number(TestRows.one(c,"SELECT SUM(reserved_qty) n FROM batches WHERE variant_id=?",variant).get("n"))==2,"retry is idempotent; failed allocation rolls back");
          check(number(TestRows.one(c,"SELECT reserved_qty FROM batches WHERE batch_no=?",tag).get("reserved_qty"))==1,"FEFO consumes the earlier expiry batch first");
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
        Database.update(c,"DELETE FROM addresses WHERE user_id IN (?,?,?,?)",buyer,seller,keeper,pwUser);
        // audit_logs is append-only and references its actor: users with audit rows are kept, suspended.
        Database.update(c,"UPDATE users SET status='SUSPENDED' WHERE id IN (?,?,?,?,?) AND EXISTS(SELECT 1 FROM audit_logs a WHERE a.actor_id=users.id)",buyer,seller,other,keeper,pwUser);
        Database.update(c,"DELETE FROM users WHERE id IN (?,?,?,?,?) AND NOT EXISTS(SELECT 1 FROM audit_logs a WHERE a.actor_id=users.id)",buyer,seller,other,keeper,pwUser);
        c.commit();
      }
    }
}