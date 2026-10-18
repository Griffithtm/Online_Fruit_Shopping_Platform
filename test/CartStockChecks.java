import model.*;
import config.DBConnect;
import dao.Database;
import service.StockReservationService;
import java.sql.*;
import java.util.*;

public class CartStockChecks {
 static long n(Object o){return ((Number)o).longValue();}
 static void ok(boolean v,String text){if(!v)throw new AssertionError(text);System.out.println("PASS "+text);}
 public static void main(String[] args)throws Exception {
  try(Connection c=new DBConnect().getConnection()) {
   c.setAutoCommit(false);
   try {
    Map<String,Object> p=TestRows.one(c,"SELECT TOP 1 * FROM v_public_products WHERE available_qty>=2 ORDER BY variant_id");
    if(p==null)throw new IllegalStateException("Test requires one demo product with stock >=2");
    long uid=Database.insert(c,"INSERT users(email,password_hash,full_name,role) VALUES(?,'test-only','Stock test','CUSTOMER')","stock-check-"+UUID.randomUUID()+"@example.invalid");
    long oid=Database.insert(c,"INSERT orders(customer_id,status,order_code,recipient_name,recipient_phone,delivery_address,payment_method,placed_at) VALUES(?,'PLACED',LEFT(CONVERT(varchar(36),NEWID()),18),'Test','0900000000','Test address','COD',SYSUTCDATETIME())",uid);
    long sid=Database.insert(c,"INSERT seller_orders(order_id,store_id,status) VALUES(?,?,'NEW')",oid,p.get("store_id"));
    long item=Database.insert(c,"INSERT order_items(seller_order_id,variant_id,quantity,unit_price) VALUES(?,?,2,?)",sid,p.get("variant_id"),p.get("price"));
    StockReservationService service=new StockReservationService();
    Savepoint baseline=c.setSavepoint();
    Database.update(c,"UPDATE order_items SET unit_price=unit_price+1 WHERE id=?",item);
    try{service.reserveOrder(c,oid);throw new AssertionError("price accepted");}catch(IllegalArgumentException expected){ok(true,"changed price rejected");}
    c.rollback(baseline); baseline=c.setSavepoint();
    Database.update(c,"UPDATE order_items SET quantity=? WHERE id=?",n(p.get("available_qty"))+1,item);
    try{service.reserveOrder(c,oid);throw new AssertionError("oversell");}catch(IllegalArgumentException expected){ok(true,"insufficient stock rejected");}
    ok(n(TestRows.one(c,"SELECT COUNT(*) n FROM stock_movements WHERE order_item_id=?",item).get("n"))==0,"failed reservation rolls back movement records");
    ok(n(TestRows.one(c,"SELECT available_qty FROM v_available_stock WHERE variant_id=?",p.get("variant_id")).get("available_qty"))==n(p.get("available_qty")),"failed reservation restores all stock");
    c.rollback(baseline); baseline=c.setSavepoint();
    service.reserveOrder(c,oid);service.reserveOrder(c,oid);
    ok(n(TestRows.one(c,"SELECT available_qty FROM v_available_stock WHERE variant_id=?",p.get("variant_id")).get("available_qty"))==n(p.get("available_qty"))-2,"reserve repeated without double hold");
    try{service.release(c,sid);throw new AssertionError("active release accepted");}catch(IllegalArgumentException expected){ok(true,"active order cannot release stock");}
    Database.update(c,"UPDATE orders SET status='EXPIRED' WHERE id=?",oid);
    service.release(c,sid);service.release(c,sid);
    ok(n(TestRows.one(c,"SELECT available_qty FROM v_available_stock WHERE variant_id=?",p.get("variant_id")).get("available_qty"))==n(p.get("available_qty")),"expired order releases exactly once");
   } finally {c.rollback();}
  }
 }
}
