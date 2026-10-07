package service;
import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.math.*;
import java.util.*;

public class PromotionService {
 public List<Map<String,Object>> list(long uid,String role)throws Exception{
  try(Connection c=new DBConnect().getConnection()){
   FulfillmentService.role(c,uid,role);
   List<Map<String,Object>> result=Database.rows(c,"SELECT p.*,(SELECT COUNT(*) FROM promotion_redemptions r WHERE r.promotion_id=p.id AND r.status='APPLIED') AS redemptions FROM promotions p LEFT JOIN stores s ON s.id=p.store_id WHERE "+(role.equals("ADMIN")?"p.type='VOUCHER' AND ?>0":"p.type='SELLER_CAMPAIGN' AND s.owner_id=?")+" ORDER BY p.id DESC",uid);
   for(Map<String,Object> p:result)p.put("history",Database.rows(c,"SELECT r.order_id,o.order_code,r.discount_amount,r.status,r.created_at FROM promotion_redemptions r JOIN orders o ON o.id=r.order_id WHERE r.promotion_id=? ORDER BY r.id DESC",p.get("id")));return result;
  }
 }
 public void save(long uid,String role,Map<String,String> f)throws Exception{
  if(!Set.of("ADMIN","SELLER").contains(role))throw new IllegalArgumentException("Access denied.");
  try(Connection c=new DBConnect().getConnection()){c.setAutoCommit(false);try{
   FulfillmentService.role(c,uid,role);boolean admin=role.equals("ADMIN");
   Map<String,Object> store=admin?null:Database.one(c,"SELECT id FROM stores WHERE owner_id=? AND status='ACTIVE'",uid);
   if(!admin && store==null)throw new IllegalArgumentException("Active store required.");
   Long id=f.get("id")==null || f.get("id").isBlank()?null:Validation.id(f.get("id"));
   Map<String,Object> old=id==null?null:Database.one(c,"SELECT * FROM promotions WITH(UPDLOCK,HOLDLOCK) WHERE id=?",id);
   if(id!=null && (old==null || admin && !"VOUCHER".equals(old.get("type")) || !admin && !Objects.equals(old.get("store_id"),store.get("id"))))throw new IllegalArgumentException("Promotion not found.");
   String action=f.get("action");
   if("state".equals(action)){
    String state=f.get("state");if(!Set.of("SCHEDULED","RUNNING","PAUSED","ENDED").contains(state) || id==null)throw new IllegalArgumentException("Invalid promotion state.");
    if(Set.of("ENDED","DISABLED").contains(old.get("status")))throw new IllegalArgumentException("This promotion has ended.");
    Database.update(c,"UPDATE promotions SET status=?,updated_at=SYSUTCDATETIME() WHERE id=?",state,id);
   }else{
    if(old!=null && ((Number)old.get("used_count")).intValue()>0)throw new IllegalArgumentException("Used promotions cannot be edited. Pause it and create a new promotion.");
    String name=Validation.text(f.get("name"),150),kind=f.get("discountType");if(!Set.of("PERCENT","AMOUNT").contains(kind))throw new IllegalArgumentException("Invalid discount type.");
    BigDecimal amount=Validation.money(f.get("value"),false),budget=Validation.money(f.get("budget"),false),minimum=Validation.money(f.get("minimum"),true);
    if(kind.equals("PERCENT") && amount.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Percentage cannot exceed 100.");
    int limit=Validation.quantity(f.get("limit")),per=Validation.quantity(f.get("perCustomer"));
    Timestamp start=Timestamp.valueOf(java.time.LocalDateTime.parse(f.get("start")).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime());
    Timestamp end=Timestamp.valueOf(java.time.LocalDateTime.parse(f.get("end")).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime());
    if(!end.after(start))throw new IllegalArgumentException("End time must follow start time.");
    String code=admin?Validation.text(f.get("code"),30).toUpperCase(Locale.ROOT):null;
    if(admin && !code.matches("[A-Z0-9_-]+"))throw new IllegalArgumentException("Use letters, numbers, underscore or hyphen for voucher codes.");
    if(id==null)id=Database.insert(c,"INSERT promotions(type,store_id,name,code,discount_type,discount_value,min_spend,budget_limit,usage_limit,per_customer_limit,start_at,end_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",admin?"VOUCHER":"SELLER_CAMPAIGN",admin?null:store.get("id"),name,code,kind,amount,minimum,budget,limit,per,start,end);
    else Database.update(c,"UPDATE promotions SET name=?,code=?,discount_type=?,discount_value=?,min_spend=?,budget_limit=?,usage_limit=?,per_customer_limit=?,start_at=?,end_at=?,updated_at=SYSUTCDATETIME() WHERE id=?",name,code,kind,amount,minimum,budget,limit,per,start,end,id);
    if(!admin){
     String raw=Validation.text(f.get("listings"),1000);List<Long> ids=new ArrayList<>();for(String token:raw.split(",")){long lid=Validation.id(token.trim());if(Database.one(c,"SELECT id FROM listings WHERE id=? AND store_id=?",lid,store.get("id"))==null)throw new IllegalArgumentException("All listings must belong to your store.");ids.add(lid);}
     Database.update(c,"UPDATE listings SET campaign_id=NULL WHERE campaign_id=?",id);
     for(long lid:ids)Database.update(c,"UPDATE listings SET campaign_id=? WHERE id=?",id,lid);
    }
   }
   c.commit();
  }catch(Exception e){c.rollback();throw e;}}
 }
 public static BigDecimal discount(Connection c,long uid,Map<String,Object> p,BigDecimal base)throws Exception{
  if(base.compareTo((BigDecimal)p.get("min_spend"))<0)throw new IllegalArgumentException("Promotion minimum spend is not met.");
  if(p.get("usage_limit")!=null && FulfillmentService.id(p.get("used_count"))>=FulfillmentService.id(p.get("usage_limit")))throw new IllegalArgumentException("Promotion usage limit reached.");
  long uses=FulfillmentService.id(Database.one(c,"SELECT COUNT(*) n FROM promotion_redemptions WHERE promotion_id=? AND customer_id=? AND status='APPLIED'",p.get("id"),uid).get("n"));
  if(p.get("per_customer_limit")!=null && uses>=FulfillmentService.id(p.get("per_customer_limit")))throw new IllegalArgumentException("Your promotion usage limit has been reached.");
  BigDecimal d=p.get("discount_type").equals("PERCENT")?base.multiply((BigDecimal)p.get("discount_value")).divide(new BigDecimal("100"),0,RoundingMode.DOWN):(BigDecimal)p.get("discount_value");
  if(p.get("max_discount")!=null)d=d.min((BigDecimal)p.get("max_discount"));d=d.min(base).setScale(0,RoundingMode.DOWN);
  if(p.get("budget_limit")!=null && ((BigDecimal)p.get("budget_used")).add(d).compareTo((BigDecimal)p.get("budget_limit"))>0)throw new IllegalArgumentException("Promotion budget is exhausted.");return d;
 }
 public static Map<String,Object> active(Connection c,long id)throws Exception{return Database.one(c,"SELECT * FROM promotions WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status IN ('RUNNING','SCHEDULED') AND start_at<=SYSUTCDATETIME() AND end_at>SYSUTCDATETIME()",id);}
 public static void redeem(Connection c,long uid,long oid,Map<Long,BigDecimal> discounts)throws Exception{
  for(var entry:discounts.entrySet()){
   Map<String,Object> p=active(c,entry.getKey());if(p==null)throw new IllegalArgumentException("A promotion is no longer active.");
   Database.update(c,"UPDATE promotions SET used_count=used_count+1,budget_used=budget_used+?,updated_at=SYSUTCDATETIME() WHERE id=?",entry.getValue(),entry.getKey());
   Database.update(c,"INSERT promotion_redemptions(promotion_id,order_id,customer_id,discount_amount) VALUES(?,?,?,?)",entry.getKey(),oid,uid,entry.getValue());
  }
 }
}
