import model.*;
import service.*;import dao.Database;import java.sql.*;import java.util.*;
public class MembershipChecks extends IterationTwoChecks {
 public static void main(String[]args)throws Exception{fixture();try{
 new CartService().change(buyer,"add",variant,1);long oid=place("",0,"COD");deliver(oid,sid(oid,store),seller);
 Wallet w=new LoyaltyService().wallet(buyer);MembershipProgress m=w.getMembership();check(((Number)m.getPeriodDays()).intValue()==365,"membership policy period");check(((java.math.BigDecimal)m.getQualifyingSpend()).signum()>0,"delivered spending qualifies");
 long before=n(one("SELECT COUNT(*) AS n FROM notifications WHERE user_id=? AND event_type='MEMBERSHIP_UPDATE'",buyer).get("n"));new LoyaltyService().wallet(buyer);check(before==n(one("SELECT COUNT(*) AS n FROM notifications WHERE user_id=? AND event_type='MEMBERSHIP_UPDATE'",buyer).get("n")),"unchanged tier does not notify twice");
 reject(()->new FulfillmentService().allocations(buyer,"CUSTOMER"),"customer cannot access fulfillment allocations");
 try(Connection c=db()){c.setAutoCommit(false);try{Database.update(c,"UPDATE settings SET status='INACTIVE' WHERE type='MEMBERSHIP_TIER'");check(LoyaltyService.membership(c,buyer).getWarning()!=null,"missing rules produce warning");}finally{c.rollback();}}
 System.out.println(count+" membership checks passed");
 }finally{update("UPDATE listings SET status='HIDDEN' WHERE store_id IN (?,?)",store,store2);update("UPDATE users SET status='SUSPENDED' WHERE id IN (?,?,?,?,?)",buyer,seller,other,admin,courier);}}
}
