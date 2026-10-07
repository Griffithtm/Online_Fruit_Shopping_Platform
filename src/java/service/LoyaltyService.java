package service;
import config.DBConnect;import dao.Database;import java.sql.*;import java.util.*;import java.math.*;
public class LoyaltyService {
 public static void expire(Connection c,long uid)throws Exception{
  Database.one(c,"SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?",uid);
  for(Map<String,Object> t:Database.rows(c,"SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE user_id=? AND type='POINTS_EARN' AND status='AVAILABLE' AND remaining_points>0 AND points_expire_at<CONVERT(date,SYSUTCDATETIME()) ORDER BY id",uid)){
   int n=((Number)t.get("remaining_points")).intValue();
   if(Database.update(c,"UPDATE users SET points_balance=points_balance-? WHERE id=? AND points_balance>=?",n,uid,n)!=1)throw new IllegalStateException("Points balance needs reconciliation.");
   Database.update(c,"UPDATE transactions SET remaining_points=0,status='EXPIRED' WHERE id=?",t.get("id"));
   Database.update(c,"INSERT transactions(type,user_id,points,status,idempotency_key) VALUES('POINTS_EXPIRE',?,?,'COMPLETED',?)",uid,-n,"points-expire:"+t.get("id"));
  }
 }
 public static void redeem(Connection c,long uid,long oid,int points)throws Exception{
  if(points==0)return;expire(c,uid);int left=points;
  for(Map<String,Object> lot:Database.rows(c,"SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE user_id=? AND type='POINTS_EARN' AND status='AVAILABLE' AND remaining_points>0 ORDER BY points_expire_at,id",uid)){
   int take=Math.min(left,((Number)lot.get("remaining_points")).intValue());
   Database.update(c,"UPDATE transactions SET remaining_points=remaining_points-? WHERE id=?",take,lot.get("id"));left-=take;if(left==0)break;
  }
  if(left>0)throw new IllegalArgumentException("Not enough available reward points.");
  if(Database.update(c,"UPDATE users SET points_balance=points_balance-? WHERE id=? AND points_balance>=?",points,uid,points)!=1)throw new IllegalArgumentException("Points balance changed. Review checkout.");
  Database.update(c,"INSERT transactions(type,user_id,order_id,points,status,idempotency_key) VALUES('POINTS_REDEEM',?, ?,?,'REDEEMED',?)",uid,oid,-points,"redeem:"+oid);
 }
 public static void earn(Connection c,long uid,long sid,BigDecimal eligible)throws Exception{
  Database.one(c,"SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?",uid);
  if(Database.one(c,"SELECT id FROM transactions WHERE idempotency_key=?","earn:"+sid)!=null)return;
  int points=eligible.divide(new BigDecimal(FulfillmentService.policy(c,"points_earn_vnd",10000)),0,RoundingMode.DOWN).intValueExact();
  if(points>0){
   Database.update(c,"INSERT transactions(type,user_id,seller_order_id,points,remaining_points,status,points_expire_at,idempotency_key) VALUES('POINTS_EARN',?,?,?,?,'AVAILABLE',DATEADD(day,?,CONVERT(date,SYSUTCDATETIME())),?)",uid,sid,points,points,FulfillmentService.policy(c,"points_expiry_days",365),"earn:"+sid);
   Database.update(c,"UPDATE users SET points_balance=points_balance+? WHERE id=?",points,uid);
  }
  tier(c,uid);
 }
 public static Map<String,Object> membership(Connection c,long uid)throws Exception{
  int days=FulfillmentService.policy(c,"membership_period_days",365);
  Map<String,Object> progress=new HashMap<>();progress.put("periodDays",days);
  Map<String,Object> spend=Database.one(c,"SELECT COALESCE(SUM(so.subtotal-so.discount_amount-so.points_discount),0) AS amount FROM seller_orders so JOIN orders o ON o.id=so.order_id WHERE o.customer_id=? AND so.status='DELIVERED' AND so.delivered_at>=DATEADD(day,-?,SYSUTCDATETIME()) AND NOT EXISTS(SELECT 1 FROM transactions t WHERE t.seller_order_id=so.id AND t.type='REFUND' AND t.status='REFUNDED')",uid,days);
  BigDecimal amount=(BigDecimal)spend.get("amount");progress.put("qualifyingSpend",amount);
  List<Map<String,Object>> rules=Database.rows(c,"SELECT id,name,TRY_CONVERT(decimal(15,2),JSON_VALUE(CASE WHEN ISJSON(value)=1 THEN value ELSE '{}' END,'$.minimum_spend')) AS threshold FROM settings WHERE type='MEMBERSHIP_TIER' AND status='ACTIVE' ORDER BY threshold,priority,id");
  Map<String,Object> current=null,next=null;
  for(Map<String,Object> rule:rules){BigDecimal threshold=(BigDecimal)rule.get("threshold");if(threshold==null || threshold.signum()<0)continue;if(threshold.compareTo(amount)<=0)current=rule;else if(next==null)next=rule;}
  if(current==null)progress.put("warning","Membership rules are missing or incomplete. Your current tier has been retained.");
  else progress.put("tierId",current.get("id"));
  if(next!=null){progress.put("nextTier",next.get("name"));progress.put("remainingSpend",((BigDecimal)next.get("threshold")).subtract(amount));}
  return progress;
 }
 public static void tier(Connection c,long uid)throws Exception{
  Map<String,Object> user=Database.one(c,"SELECT membership_tier_id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?",uid);
  Map<String,Object> progress=membership(c,uid);Object target=progress.get("tierId");
  if(target!=null && !Objects.equals(user.get("membership_tier_id"),target)){
   Database.update(c,"UPDATE users SET membership_tier_id=? WHERE id=?",target,uid);
   Database.update(c,"INSERT notifications(user_id,event_type,title,content,link_url,dedup_key) VALUES(?,'MEMBERSHIP_UPDATE','Membership updated','Your membership tier has changed. View your benefits and progress.','/wallet',?)",uid,"membership:"+uid+":"+UUID.randomUUID());
  }
 }
 public Map<String,Object> wallet(long uid)throws Exception{
  try(Connection c=new DBConnect().getConnection()){c.setAutoCommit(false);try{expire(c,uid);tier(c,uid);Map<String,Object> result=Database.one(c,"SELECT u.points_balance,s.name AS tier,s.description AS benefits FROM users u LEFT JOIN settings s ON s.id=u.membership_tier_id WHERE u.id=?",uid);result.put("membership",membership(c,uid));result.put("totals",Database.one(c,"SELECT COALESCE(SUM(CASE WHEN type='POINTS_EARN' AND status='PENDING' THEN points ELSE 0 END),0) AS pending,COALESCE(SUM(CASE WHEN type='POINTS_EXPIRE' THEN -points ELSE 0 END),0) AS expired FROM transactions WHERE user_id=?",uid));result.put("entries",Database.rows(c,"SELECT type,points,remaining_points,status,points_expire_at,created_at FROM transactions WHERE user_id=? AND type LIKE 'POINTS_%' ORDER BY id DESC",uid));c.commit();return result;}catch(Exception e){c.rollback();throw e;}}
 }
 public static void restore(Connection c,long uid,long sid,int points)throws Exception{
  if(points<=0 || Database.one(c,"SELECT id FROM transactions WHERE idempotency_key=?","points-return:"+sid)!=null)return;
  Database.one(c,"SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?",uid);
  Database.update(c,"INSERT transactions(type,user_id,seller_order_id,points,remaining_points,status,points_expire_at,idempotency_key,reason) VALUES('POINTS_EARN',?,?,?,?,'AVAILABLE',DATEADD(day,?,CONVERT(date,SYSUTCDATETIME())),?,'Returned after cancellation')",uid,sid,points,points,FulfillmentService.policy(c,"points_expiry_days",365),"points-return:"+sid);
  Database.update(c,"UPDATE users SET points_balance=points_balance+? WHERE id=?",points,uid);
 }
}
