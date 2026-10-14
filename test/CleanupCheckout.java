import model.*;
import config.DBConnect;import dao.Database;import java.sql.*;import java.nio.file.*;
public class CleanupCheckout {
 public static void main(String[]args)throws Exception{
 String email=Files.readString(Path.of("tmp/checkout/email.txt")).trim();
 if(!email.matches("checkout-http-[0-9]+@test\\.invalid"))throw new IllegalArgumentException("Invalid test account");
 try(Connection c=new DBConnect().getConnection()) {c.setAutoCommit(false);try{
 long uid=((Number)TestRows.one(c,"SELECT id FROM users WHERE email=?",email).get("id")).longValue();
 long count=((Number)TestRows.one(c,"SELECT COUNT(*) n FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN orders o ON o.id=so.order_id WHERE o.customer_id=? AND oi.reservation_status='RESERVED'",uid).get("n")).longValue();
 if(count!=0)throw new AssertionError("Test left stock reserved");
 System.out.println("PASS no reserved stock remains after cancellation");
 Database.update(c,"UPDATE users SET status='SUSPENDED' WHERE id=?",uid);c.commit();System.out.println("Test account suspended; cancelled order audit retained");
 }catch(Exception e){c.rollback();throw e;}}}
}
