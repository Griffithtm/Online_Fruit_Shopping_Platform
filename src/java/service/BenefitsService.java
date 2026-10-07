package service;
import dao.Database;import java.sql.*;import java.util.*;import java.math.*;
public class BenefitsService {
 public static void returnSeller(Connection c,Map<String,Object> order,Map<String,Object> so)throws Exception{
  int points=((Number)so.get("points_used")).intValue();LoyaltyService.restore(c,FulfillmentService.id(order.get("customer_id")),FulfillmentService.id(so.get("id")),points);
 }
 public static void returnAll(Connection c,Map<String,Object> o)throws Exception{
  for(Map<String,Object> so:Database.rows(c,"SELECT * FROM seller_orders WHERE order_id=?",o.get("id")))returnSeller(c,o,so);
  for(Map<String,Object> r:Database.rows(c,"SELECT * FROM promotion_redemptions WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? AND status='APPLIED'",o.get("id"))){
   Database.update(c,"UPDATE promotions SET used_count=used_count-1,budget_used=budget_used-? WHERE id=? AND used_count>0 AND budget_used>=?",r.get("discount_amount"),r.get("promotion_id"),r.get("discount_amount"));
   Database.update(c,"UPDATE promotion_redemptions SET status='RELEASED' WHERE id=?",r.get("id"));
  }
 }
}
