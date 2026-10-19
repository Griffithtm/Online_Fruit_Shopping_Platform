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
    static long buyer,seller,other,courier,keeper,pwUser,store,store2,listing,variant,category;
    static Connection open() throws Exception {return new DBConnect().getConnection();}
    static long number(Object o){return ((Number)o).longValue();}
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS "+name);}
    static void reject(Throwing action,String name)throws Exception {try{action.run();throw new AssertionError(name);}catch(IllegalArgumentException expected){System.out.println("PASS "+name);}}
    static void rejectWith(Throwing action,String message,String name)throws Exception {try{action.run();throw new AssertionError(name);}catch(IllegalArgumentException expected){if(!message.equals(expected.getMessage()))throw new AssertionError(name+": got \""+expected.getMessage()+"\"");System.out.println("PASS "+name);}}
    interface Throwing {void run()throws Exception;}
    static long auditCount(long uid)throws Exception {try(Connection c=open()){return number(TestRows.one(c,"SELECT COUNT(*) n FROM audit_logs WHERE actor_id=?",uid).get("n"));}}
    /** Runs the action and checks it wrote exactly one audit row with the expected action. */
    static void audited(long uid,Throwing action,String expected,String name)throws Exception {long before=auditCount(uid);action.run();
      try(Connection c=open()){check(auditCount(uid)==before+1 && expected.equals(TestRows.one(c,"SELECT TOP 1 action FROM audit_logs WHERE actor_id=? ORDER BY id DESC",uid).get("action")),name);}}
    /** Id of the user's default address, or 0 when there is none. */
    static long defaultOf(long uid)throws Exception {try(Connection c=open()){Map<String,Object> a=TestRows.one(c,"SELECT id FROM addresses WHERE user_id=? AND is_default=1",uid);return a==null?0:number(a.get("id"));}}
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
        String buyerEmail=(tag+"b@test.invalid").toLowerCase(Locale.ROOT);
        Map<String,Object> before=null;
        try(Connection c=open()){before=TestRows.one(c,"SELECT role,points_balance FROM users WHERE id=?",buyer);}
        as.profile(buyer,tag,buyerEmail.toUpperCase(Locale.ROOT),"0912 345.678");
        User saved=as.profile(buyer);
        check(buyerEmail.equals(saved.getEmail()),"profile: own email in upper case is saved lower case, not MSG13");
        check("0912345678".equals(saved.getPhone()),"profile: phone spaces/dots removed before saving");
        rejectWith(()->as.profile(buyer,tag,(tag+"o@test.invalid").toUpperCase(Locale.ROOT),null),util.Messages.get(util.Messages.MSG13),"profile: another user's email in different case gives MSG13");
        rejectWith(()->as.profile(buyer,tag,buyerEmail,"abc"),"Vui lòng nhập số điện thoại hợp lệ.","profile: phone letters give MSG04");
        rejectWith(()->as.profile(buyer,tag,buyerEmail,"123"),"Vui lòng nhập số điện thoại hợp lệ.","profile: phone shorter than 7 digits gives MSG04");
        rejectWith(()->as.profile(buyer," ",buyerEmail,null),"Họ tên là bắt buộc.","profile: blank name gives MSG02");
        rejectWith(()->as.profile(buyer,"x".repeat(101),buyerEmail,null),"Họ tên không được vượt quá 100 ký tự.","profile: 101-character name gives MSG03");
        rejectWith(()->as.profile(buyer,tag,"not-an-email",null),"Vui lòng nhập địa chỉ thư điện tử hợp lệ.","profile: malformed email gives MSG04");
        as.profile(buyer,tag,buyerEmail,"  ");
        check(as.profile(buyer).getPhone()==null,"profile: blank phone is stored as NULL");
        // The service API has no role/points input; the servlet only reads name, email and phone,
        // so role=ADMIN / points_balance=9999 in the form cannot reach the UPDATE.
        try(Connection c=open()){Map<String,Object> after=TestRows.one(c,"SELECT role,points_balance FROM users WHERE id=?",buyer);
          check(before.equals(after) && "CUSTOMER".equals(after.get("role")),"profile: role and points unchanged by profile saves");}
        as.address(buyer,"saveAddress",null,"Buyer","0900000000","Line","Ward","District","City",false);
        check(Boolean.TRUE.equals(as.addresses(buyer).get(0).getIsDefault()),"first address automatically default");
        String msg19=util.Messages.get(util.Messages.MSG19),msg12=util.Messages.get(util.Messages.MSG12);
        rejectWith(()->as.address(buyer,"saveAddress",null,"Buyer","abc","Line","Ward","District","City",false),"Vui lòng nhập số điện thoại hợp lệ.","address: phone letters give MSG04");
        rejectWith(()->as.address(buyer,"saveAddress",null,"Buyer"," ","Line","Ward","District","City",false),"Số điện thoại là bắt buộc.","address: blank phone gives MSG02");
        rejectWith(()->as.address(buyer,"saveAddress",null,"Buyer","0900000000","Line","Ward","District","",false),"Tỉnh/thành phố là bắt buộc.","address: blank province (Other left empty) gives MSG02");
        as.address(buyer,"saveAddress",null,"Second","0900 000 001","Line 2","Ward","District","City",false);
        long usedAddress=number(as.addresses(buyer).get(0).getId()),freeAddress=number(as.addresses(buyer).get(1).getId());
        check("0900000001".equals(as.addresses(buyer).get(1).getPhone()),"address: phone normalised before saving");
        long addressOrder;
        try(Connection c=open()){addressOrder=Database.insert(c,"INSERT orders(customer_id,order_code,status,recipient_name,recipient_phone,delivery_address,delivery_address_id,payment_method,placed_at) VALUES(?,?,'CONFIRMED','Test','0900000000','Test address',?,'COD',SYSUTCDATETIME())",buyer,tag+"a",usedAddress);}
        rejectWith(()->as.address(buyer,"saveAddress",""+usedAddress,"Buyer","0900000000","Changed","Ward","District","City",true),msg19,"address used by an open order cannot be edited (MSG19)");
        rejectWith(()->as.address(buyer,"deleteAddress",""+usedAddress,null,null,null,null,null,null,false),msg19,"address used by an open order cannot be deleted (MSG19)");
        as.address(buyer,"saveAddress",""+freeAddress,"Second","0900000001","Changed line","Ward","District","City",false);
        check(as.addresses(buyer).stream().anyMatch(a->"Changed line".equals(a.getAddressLine())),"address not used by the open order can still be edited");
        as.address(seller,"saveAddress",null,"Seller","0900000002","Pickup","Ward","District","City",true);
        long pickup=number(as.addresses(seller).get(0).getId());
        try(Connection c=open()){Database.update(c,"UPDATE stores SET pickup_address_id=? WHERE id=?",pickup,store);}
        rejectWith(()->as.address(seller,"deleteAddress",""+pickup,null,null,null,null,null,null,false),msg19,"store pickup address cannot be deleted (MSG19)");
        as.address(seller,"saveAddress",""+pickup,"Seller","0900000002","Pickup 2","Ward","District","City",true);
        check("Pickup 2".equals(as.addresses(seller).get(0).getAddressLine()),"pickup address editable while no delivery is pending");
        try(Connection c=open()){Database.insert(c,"INSERT seller_orders(order_id,store_id,status) VALUES(?,?,'READY')",addressOrder,store);}
        rejectWith(()->as.address(seller,"saveAddress",""+pickup,"Seller","0900000002","Pickup 3","Ward","District","City",true),msg19,"pickup address of a READY seller order cannot be edited (MSG19)");
        try(Connection c=open()){courier=Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,'unused',?,'DELIVERY_STAFF')",tag+"d@test.invalid",tag);}
        rejectWith(()->as.address(courier,"saveAddress",null,"Courier","0900000003","Line","Ward","District","City",false),msg12,"delivery staff cannot add an address (MSG12)");
        // BA-04: a separate customer with no open orders, so every address can be edited and deleted.
        try(Connection c=open()){keeper=Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,'unused',?,'CUSTOMER')",tag+"k@test.invalid",tag);}
        as.address(keeper,"saveAddress",null,"Alice","0900000010","A street","Ward","District","City",false);
        long addrA=number(as.addresses(keeper).get(0).getId());
        check(defaultOf(keeper)==addrA,"default: first address becomes default even unticked");
        as.address(keeper,"saveAddress",null,"Bob","0900000011","B street","Ward","District","City",false);
        long addrB=number(as.addresses(keeper).get(1).getId());
        check(defaultOf(keeper)==addrA,"default: adding an unticked address keeps the default");
        as.address(keeper,"saveAddress",""+addrA,"Alice","0900000010","A street","Ward","District","City",false);
        check(defaultOf(keeper)==addrA,"default: editing the default address keeps it default");
        as.address(keeper,"saveAddress",""+addrB,"Bob","0900000011","B street","Ward","District","City",true);
        check(defaultOf(keeper)==addrA,"default: the checkbox is ignored when editing");
        check(util.Messages.get(util.Messages.MSG18).equals(as.address(keeper,"defaultAddress",""+addrB,null,null,null,null,null,null,false)) && defaultOf(keeper)==addrB,"default: Set as default switches to the chosen address (MSG18)");
        try(Connection c=open()){check(number(TestRows.one(c,"SELECT COUNT(*) n FROM addresses WHERE user_id=? AND is_default=1",keeper).get("n"))==1,"default: exactly one default address");}
        rejectWith(()->as.address(keeper,"deleteAddress",null,null,null,null,null,null,null,false),msg12,"default: delete without id gives MSG12");
        rejectWith(()->as.address(keeper,"defaultAddress","abc",null,null,null,null,null,null,false),msg12,"default: malformed id gives MSG12");
        rejectWith(()->as.address(keeper,"defaultAddress",""+freeAddress,null,null,null,null,null,null,false),msg12,"default: another user's address gives MSG12");
        as.address(keeper,"saveAddress",null,"Carol","0900000012","C street","Ward","District","City",false);
        String moved=as.address(keeper,"deleteAddress",""+addrB,null,null,null,null,null,null,false);
        check(defaultOf(keeper)==addrA && "Đã xóa địa chỉ. Alice - A street, Ward, District, City hiện là địa chỉ giao hàng mặc định của bạn.".equals(moved),"default: deleting the default makes the oldest remaining one default and says which (MSG82)");
        long addrC=as.addresses(keeper).stream().mapToLong(a->number(a.getId())).filter(x->x!=addrA).findFirst().getAsLong();
        check("Đã xóa địa chỉ thành công.".equals(as.address(keeper,"deleteAddress",""+addrC,null,null,null,null,null,null,false)) && defaultOf(keeper)==addrA,"default: deleting a non-default address (MSG81)");
        check("Đã xóa địa chỉ thành công.".equals(as.address(keeper,"deleteAddress",""+addrA,null,null,null,null,null,null,false)) && as.addresses(keeper).isEmpty(),"default: deleting the last address leaves no default");
        // BA-06: password change. A user with a real BCrypt hash so login can be checked.
        String pwEmail=(tag+"p@test.invalid").toLowerCase(Locale.ROOT),oldPw="OldPass123!",newPw="NewPass456!";
        try(Connection c=open()){pwUser=Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,?,?,'CUSTOMER')",pwEmail,org.mindrot.jbcrypt.BCrypt.hashpw(oldPw,org.mindrot.jbcrypt.BCrypt.gensalt(4)),tag);}
        rejectWith(()->as.password(pwUser,"WrongPass1!",newPw,newPw),"Vui lòng nhập mật khẩu hiện tại hợp lệ.","password: wrong current password gives MSG04");
        rejectWith(()->as.password(pwUser,oldPw,"Short1!","Short1!"),"Vui lòng nhập mật khẩu mới (8–64 ký tự) hợp lệ.","password: 7-character new password gives MSG04");
        rejectWith(()->as.password(pwUser,oldPw,"x".repeat(65),"x".repeat(65)),"Mật khẩu mới không được vượt quá 64 ký tự.","password: 65-character new password gives MSG03");
        rejectWith(()->as.password(pwUser,oldPw,oldPw,oldPw),"Vui lòng nhập mật khẩu mới hợp lệ. Mật khẩu mới phải khác mật khẩu hiện tại.","password: new password equal to current is rejected");
        try{as.password(pwUser,oldPw,newPw,newPw+"x");throw new AssertionError("password: confirmation mismatch");}
        catch(util.FieldErrors fe){check(util.Messages.get(util.Messages.MSG16).equals(fe.getErrors().get("confirm")),"password: confirmation mismatch gives MSG16 under the confirm field");}
        check(auditCount(pwUser)==0,"audit: failed password attempts write nothing");
        audited(pwUser,()->as.password(pwUser,oldPw,newPw,newPw),"PASSWORD_CHANGED","audit: password change writes one row");
        try(Connection c=open()){Map<String,Object> row=TestRows.one(c,"SELECT TOP 1 old_value,new_value,note FROM audit_logs WHERE actor_id=? ORDER BY id DESC",pwUser);
          check(row.get("old_value")==null && row.get("new_value")==null && row.get("note")==null,"audit: password row holds no password or hash");}
        check(as.login(pwEmail,newPw).getId()==pwUser,"password: new password signs in");
        reject(()->as.login(pwEmail,oldPw),"password: old password no longer signs in");
        audited(pwUser,()->as.profile(pwUser,"Renamed",pwEmail,"0911111111"),"PROFILE_UPDATED","audit: profile change writes one row");
        try(Connection c=open()){Map<String,Object> row=TestRows.one(c,"SELECT TOP 1 old_value,new_value FROM audit_logs WHERE actor_id=? ORDER BY id DESC",pwUser);
          check(((String)row.get("old_value")).contains("\"full_name\":\""+tag+"\"") && ((String)row.get("new_value")).contains("\"phone\":\"0911111111\""),"audit: profile row has old and new values");}
        audited(pwUser,()->as.address(pwUser,"saveAddress",null,"P1","0900000020","P street","Ward","District","City",false),"ADDRESS_CREATED","audit: address create writes one row");
        long pw1=number(as.addresses(pwUser).get(0).getId());
        audited(pwUser,()->as.address(pwUser,"saveAddress",""+pw1,"P1","0900000020","P street 2","Ward","District","City",false),"ADDRESS_UPDATED","audit: address update writes one row");
        as.address(pwUser,"saveAddress",null,"P2","0900000021","Q street","Ward","District","City",false);
        long pw2=as.addresses(pwUser).stream().mapToLong(a->number(a.getId())).filter(x->x!=pw1).findFirst().getAsLong();
        audited(pwUser,()->as.address(pwUser,"defaultAddress",""+pw2,null,null,null,null,null,null,false),"ADDRESS_DEFAULT_SET","audit: set default writes one row");
        audited(pwUser,()->as.address(pwUser,"deleteAddress",""+pw2,null,null,null,null,null,null,false),"ADDRESS_DELETED","audit: deleting the default writes one row (default move in note)");
        try(Connection c=open()){check(("Default moved to address #"+pw1).equals(TestRows.one(c,"SELECT TOP 1 note FROM audit_logs WHERE actor_id=? ORDER BY id DESC",pwUser).get("note")),"audit: delete note names the new default");}
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
        rejectWith(()->as.address(buyer,"saveAddress",""+freeAddress,"Second","0900000001","Line","Ward","District","City",false),msg19,"open order without delivery_address_id still locks every address (legacy)");
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
        Database.update(c,"UPDATE users SET status='SUSPENDED' WHERE id IN (?,?,?,?,?,?) AND EXISTS(SELECT 1 FROM audit_logs a WHERE a.actor_id=users.id)",buyer,seller,other,courier,keeper,pwUser);
        Database.update(c,"DELETE FROM users WHERE id IN (?,?,?,?,?,?) AND NOT EXISTS(SELECT 1 FROM audit_logs a WHERE a.actor_id=users.id)",buyer,seller,other,courier,keeper,pwUser);
        c.commit();
      }
    }
}
