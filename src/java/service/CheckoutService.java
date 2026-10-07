package service;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

/** COD checkout. All totals and stock are verified on the server. */
public class CheckoutService {
    public Map<String,Object> quote(long uid,long addressId) throws Exception { return quote(uid,addressId,"",0,"COD"); }
    public Map<String,Object> quote(long uid,long addressId,String voucher,int points,String method) throws Exception {
        try(Connection c=new DBConnect().getConnection()){c.setAutoCommit(false); try { LoyaltyService.expire(c,uid); Map<String,Object> q=quote(c,uid,addressId,voucher,points,method);c.commit();return q; }catch(Exception e){c.rollback();throw e;}}
    }
    private Map<String,Object> quote(Connection c,long uid,long addressId,String voucher,int points,String method) throws Exception {
        if(!Set.of("COD","CARD","E_WALLET").contains(method))throw new IllegalArgumentException("Invalid payment method.");
        if(!method.equals("COD"))new VnpayGateway().requireConfigured();
        if(points<0)throw new IllegalArgumentException("Invalid reward points.");
        Map<String,Object> address=Database.one(c,"SELECT * FROM addresses WHERE id=? AND user_id=?",addressId,uid);
        if(address==null)throw new IllegalArgumentException("Choose one of your delivery addresses.");
        Map<String,Object> area=Database.one(c,"SELECT TRY_CONVERT(decimal(15,2),JSON_VALUE(CASE WHEN ISJSON(value)=1 THEN value ELSE '{}' END,'$.delivery_fee')) AS fee FROM settings WHERE type='SERVICE_AREA' AND status='ACTIVE' AND name=?",address.get("city"));
        if(area==null || area.get("fee")==null || ((BigDecimal)area.get("fee")).signum()<0)throw new IllegalArgumentException("Delivery is not available for this address.");
        Map<String,Object> cart=Database.one(c,"SELECT id FROM orders WHERE customer_id=? AND status='CART'",uid);
        if(cart==null)throw new IllegalArgumentException("Your cart is empty.");
        List<Map<String,Object>> lines=Database.rows(c,"SELECT oi.id,oi.variant_id,oi.quantity,oi.unit_price,so.id AS seller_order_id,l.campaign_id,p.price,p.available_qty,l.title,s.store_name FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN stores s ON s.id=so.store_id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id LEFT JOIN v_public_products p ON p.variant_id=oi.variant_id WHERE so.order_id=? ORDER BY so.store_id,oi.id",cart.get("id"));
        if(lines.isEmpty())throw new IllegalArgumentException("Your cart is empty.");
        BigDecimal subtotal=BigDecimal.ZERO;
        StringBuilder fingerprint=new StringBuilder(cart.get("id").toString());
        for(Map<String,Object> line:lines){
            if(line.get("price")==null || ((Number)line.get("available_qty")).longValue()<((Number)line.get("quantity")).longValue())throw new IllegalArgumentException("A product is unavailable or has insufficient stock. Review your cart.");
            if(((BigDecimal)line.get("price")).compareTo((BigDecimal)line.get("unit_price"))!=0)throw new IllegalArgumentException("A price has changed. Update the affected item in your cart first.");
            subtotal=subtotal.add(((BigDecimal)line.get("price")).multiply(new BigDecimal(line.get("quantity").toString())));
            fingerprint.append('|').append(line.get("id")).append(':').append(line.get("quantity")).append(':').append(line.get("price"));
        }
        Map<Long,BigDecimal> discounts=new TreeMap<>();
        Map<Long,Map<String,Object>> groups=new LinkedHashMap<>();
        Map<Long,BigDecimal> campaignBases=new TreeMap<>();Map<Long,Long> campaignSeller=new HashMap<>();
        for(Map<String,Object> line:lines){long sid=FulfillmentService.id(line.get("seller_order_id"));Map<String,Object> g=groups.computeIfAbsent(sid,k->{Map<String,Object> m=new LinkedHashMap<>();m.put("subtotal",BigDecimal.ZERO);m.put("discount",BigDecimal.ZERO);m.put("storeName",line.get("store_name"));return m;});BigDecimal amount=((BigDecimal)line.get("price")).multiply(new BigDecimal(line.get("quantity").toString()));g.put("subtotal",((BigDecimal)g.get("subtotal")).add(amount));if(line.get("campaign_id")!=null){long pid=FulfillmentService.id(line.get("campaign_id"));campaignBases.merge(pid,amount,BigDecimal::add);campaignSeller.put(pid,sid);}}
        for(var entry:campaignBases.entrySet()){
            Map<String,Object> promo=PromotionService.active(c,entry.getKey());
            if(promo!=null){BigDecimal d=PromotionService.discount(c,uid,promo,entry.getValue());discounts.put(entry.getKey(),d);Map<String,Object> g=groups.get(campaignSeller.get(entry.getKey()));g.put("discount",((BigDecimal)g.get("discount")).add(d));}
        }
        BigDecimal campaignTotal=discounts.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add),voucherDiscount=BigDecimal.ZERO;
        Long voucherId=null;
        if(voucher!=null && !voucher.isBlank()){
            Map<String,Object> ref=Database.one(c,"SELECT id FROM promotions WHERE type='VOUCHER' AND code=?",voucher.trim().toUpperCase(Locale.ROOT));
            Map<String,Object> promo=ref==null?null:PromotionService.active(c,FulfillmentService.id(ref.get("id")));
            if(promo==null)throw new IllegalArgumentException("Voucher is invalid or outside its validity period.");
            voucherId=FulfillmentService.id(promo.get("id"));voucherDiscount=PromotionService.discount(c,uid,promo,subtotal.subtract(campaignTotal));discounts.put(voucherId,voucherDiscount);
        }
        BigDecimal eligible=subtotal.subtract(campaignTotal).subtract(voucherDiscount);
        int rate=FulfillmentService.policy(c,"points_vnd_per_point",1000);
        BigDecimal pointDiscount=new BigDecimal(points).multiply(new BigDecimal(rate));
        long available=FulfillmentService.id(Database.one(c,"SELECT points_balance FROM users WHERE id=?",uid).get("points_balance"));
        if(points>available || pointDiscount.compareTo(eligible)>0)throw new IllegalArgumentException("Reward points exceed your balance or eligible purchase amount.");
        BigDecimal fee=(BigDecimal)area.get("fee"),voucherLeft=voucherDiscount,feeLeft=fee;int pointLeft=points,index=0;
        for(var entry:groups.entrySet()){
            Map<String,Object> g=entry.getValue();boolean last=++index==groups.size();BigDecimal after=((BigDecimal)g.get("subtotal")).subtract((BigDecimal)g.get("discount"));
            BigDecimal vd=last?voucherLeft:(subtotal.subtract(campaignTotal).signum()==0?BigDecimal.ZERO:voucherDiscount.multiply(after).divide(subtotal.subtract(campaignTotal),0,java.math.RoundingMode.DOWN));voucherLeft=voucherLeft.subtract(vd);g.put("discount",((BigDecimal)g.get("discount")).add(vd));
            BigDecimal afterVoucher=after.subtract(vd);int gp=last?pointLeft:(eligible.signum()==0?0:new BigDecimal(points).multiply(afterVoucher).divide(eligible,0,java.math.RoundingMode.DOWN).intValue());
            // Allocate whole point units only where their monetary value fits.
            gp=Math.min(gp,afterVoucher.divide(new BigDecimal(rate),0,java.math.RoundingMode.DOWN).intValue());pointLeft-=gp;g.put("points",gp);g.put("pointsDiscount",new BigDecimal(gp).multiply(new BigDecimal(rate)));
            BigDecimal gf=last?feeLeft:fee.divide(new BigDecimal(groups.size()),0,java.math.RoundingMode.DOWN);feeLeft=feeLeft.subtract(gf);g.put("fee",gf);
        }
        if(pointLeft!=0)throw new IllegalArgumentException("Reduce reward points to fit the seller order allocation.");
        Map<String,Object> q=new LinkedHashMap<>();q.put("cartId",cart.get("id"));q.put("address",address);q.put("lines",lines);q.put("subtotal",subtotal);q.put("fee",fee);q.put("discount",campaignTotal.add(voucherDiscount));q.put("pointsDiscount",pointDiscount);q.put("points",points);q.put("voucherId",voucherId);q.put("discounts",discounts);q.put("groups",groups);q.put("method",method);q.put("total",eligible.subtract(pointDiscount).add(fee));
        q.put("etaHours",FulfillmentService.policy(c,"delivery_eta_hours",24));
        q.put("fingerprint",fingerprint+"|"+address+"|"+fee+"|"+discounts+"|"+points+"|"+method+"|"+q.get("etaHours"));return q;
    }

    public long place(long uid,long addressId,String key,String expected) throws Exception {return place(uid,addressId,key,expected,"",0,"COD");}
    public long place(long uid,long addressId,String key,String expected,String voucher,int points,String method) throws Exception {
        UUID.fromString(key);
        try(Connection c=new DBConnect().getConnection()){
            c.setAutoCommit(false);c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try{
                Database.one(c,"SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status='ACTIVE'",uid);
                Map<String,Object> previous=Database.one(c,"SELECT id FROM orders WHERE customer_id=? AND checkout_key=CONVERT(uniqueidentifier,?)",uid,key);
                if(previous!=null){c.commit();return ((Number)previous.get("id")).longValue();}
                LoyaltyService.expire(c,uid);
                Map<String,Object> q=quote(c,uid,addressId,voucher,points,method);
                if(!Objects.equals(expected,q.get("fingerprint")))throw new IllegalArgumentException("Your cart or delivery details changed. Review checkout again.");
                long oid=((Number)q.get("cartId")).longValue();
                @SuppressWarnings("unchecked") Map<String,Object> a=(Map<String,Object>)q.get("address");
                String delivery=a.get("address_line")+", "+a.get("ward")+", "+a.get("district")+", "+a.get("city");
                if(delivery.length()>500)throw new IllegalArgumentException("Delivery address is too long.");
                Database.update(c,"DELETE FROM seller_orders WHERE order_id=? AND NOT EXISTS(SELECT 1 FROM order_items WHERE seller_order_id=seller_orders.id)",oid);
                Database.update(c,"UPDATE orders SET status=?,order_code=?,recipient_name=?,recipient_phone=?,delivery_address=?,payment_method=?,placed_at=SYSUTCDATETIME(),checkout_key=CONVERT(uniqueidentifier,?),item_total=?,delivery_fee=?,discount_amount=?,points_discount=?,points_used=?,voucher_id=?,estimated_delivery_at=DATEADD(hour,?,SYSUTCDATETIME()),payment_expires_at=CASE WHEN ?='COD' THEN NULL ELSE DATEADD(minute,?,SYSUTCDATETIME()) END,payable_amount=?,updated_at=SYSUTCDATETIME() WHERE id=? AND status='CART'",method.equals("COD")?"CONFIRMED":"AWAITING_PAYMENT","FR"+oid,a.get("recipient_name"),a.get("phone"),delivery,method,key,q.get("subtotal"),q.get("fee"),q.get("discount"),q.get("pointsDiscount"),points,q.get("voucherId"),q.get("etaHours"),method,FulfillmentService.policy(c,"reservation_timeout_minutes",15),q.get("total"),oid);
                Database.update(c,"UPDATE so SET status='NEW',subtotal=(SELECT SUM(quantity*unit_price) FROM order_items WHERE seller_order_id=so.id),updated_at=SYSUTCDATETIME() FROM seller_orders so WHERE order_id=?",oid);
                @SuppressWarnings("unchecked") Map<Long,Map<String,Object>> groups=(Map<Long,Map<String,Object>>)q.get("groups");
                for(var entry:groups.entrySet()){Map<String,Object> g=entry.getValue();Database.update(c,"UPDATE so SET delivery_fee=?,discount_amount=?,points_discount=?,points_used=?,cod_amount=?,commission_rate=COALESCE((SELECT TRY_CONVERT(decimal(9,6),JSON_VALUE(CASE WHEN ISJSON(t.value)=1 THEN t.value ELSE '{}' END,'$.commission_rate')) FROM stores s LEFT JOIN settings t ON t.id=s.seller_tier_id WHERE s.id=so.store_id),0) FROM seller_orders so WHERE id=?",g.get("fee"),g.get("discount"),g.get("pointsDiscount"),g.get("points"),method.equals("COD")?((BigDecimal)g.get("subtotal")).subtract((BigDecimal)g.get("discount")).subtract((BigDecimal)g.get("pointsDiscount")).add((BigDecimal)g.get("fee")):BigDecimal.ZERO,entry.getKey());}
                Database.update(c,"UPDATE oi SET product_title=l.title,variant_name=v.option_name FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id WHERE so.order_id=?",oid);
                Database.update(c,"UPDATE so SET store_name_snapshot=s.store_name FROM seller_orders so JOIN stores s ON s.id=so.store_id WHERE so.order_id=?",oid);
                new StockReservationService().reserveOrder(c,oid);
                @SuppressWarnings("unchecked") Map<Long,BigDecimal> discounts=(Map<Long,BigDecimal>)q.get("discounts");PromotionService.redeem(c,uid,oid,discounts);LoyaltyService.redeem(c,uid,oid,points);
                if(method.equals("COD")) Database.update(c,"INSERT transactions(type,user_id,order_id,method,amount,status,idempotency_key) VALUES('COD',?,?,'COD',?,'PENDING',?)",uid,oid,q.get("total"),"cod:"+oid);
                c.commit();return oid;
            }catch(Exception e){c.rollback();throw e;}
        }
    }
    public List<Map<String,Object>> payments(long uid)throws Exception{
        try(Connection c=new DBConnect().getConnection()){return Database.rows(c,"SELECT id,order_id,method,amount,status,created_at FROM transactions WHERE user_id=? AND type IN ('PAYMENT','COD') ORDER BY id",uid);}
    }
    public List<Map<String,Object>> orders(long uid)throws Exception{
        try(Connection c=new DBConnect().getConnection()){return Database.rows(c,"SELECT o.*,(SELECT COALESCE(SUM(subtotal-discount_amount-points_discount+delivery_fee),0) FROM seller_orders WHERE order_id=o.id AND status NOT IN ('CANCELLED','REJECTED')) AS current_payable,(SELECT TOP 1 status FROM transactions WHERE order_id=o.id AND type IN ('PAYMENT','COD') ORDER BY id DESC) AS payment_status,CASE WHEN o.status='CONFIRMED' AND o.payment_method='COD' AND NOT EXISTS(SELECT 1 FROM seller_orders WHERE order_id=o.id AND status<>'NEW') THEN 1 ELSE 0 END AS cancellable FROM orders o WHERE customer_id=? AND status<>'CART' ORDER BY id DESC",uid);}
    }
    public List<Map<String,Object>> items(long uid)throws Exception{
        try(Connection c=new DBConnect().getConnection()){return Database.rows(c,"SELECT so.order_id,so.id AS seller_order_id,so.rejection_reason,so.status AS seller_status,COALESCE(so.store_name_snapshot,s.store_name) AS store_name,COALESCE(oi.product_title,l.title) AS title,oi.quantity,oi.unit_price FROM orders o JOIN seller_orders so ON so.order_id=o.id JOIN stores s ON s.id=so.store_id JOIN order_items oi ON oi.seller_order_id=so.id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id WHERE o.customer_id=? AND o.status<>'CART' ORDER BY so.id,oi.id",uid);}
    }
    public void cancel(long uid,long oid)throws Exception{
        try(Connection c=new DBConnect().getConnection()){
            c.setAutoCommit(false);
            try{
                Database.one(c,"SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?",uid);
                Map<String,Object> o=Database.one(c,"SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND customer_id=?",oid,uid);
                if(o==null)throw new IllegalArgumentException("Order not found.");
                if("CANCELLED".equals(o.get("status"))){c.commit();return;}
                List<Map<String,Object>> sellers=Database.rows(c,"SELECT id,status FROM seller_orders WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? ORDER BY store_id,id",oid);
                if(!"CONFIRMED".equals(o.get("status")) || !"COD".equals(o.get("payment_method")) || sellers.stream().anyMatch(x->!"NEW".equals(x.get("status"))))throw new IllegalArgumentException("This order can no longer be cancelled.");
                if(Database.one(c,"SELECT id FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? AND status IN ('PAID','COMPLETED')",oid)!=null)throw new IllegalArgumentException("Paid orders require a refund review.");
                Database.update(c,"UPDATE orders SET status='CANCELLED',updated_at=SYSUTCDATETIME() WHERE id=?",oid);
                Database.update(c,"UPDATE seller_orders SET status='CANCELLED',updated_at=SYSUTCDATETIME() WHERE order_id=?",oid);
                for(Map<String,Object> so:sellers)new StockReservationService().release(c,((Number)so.get("id")).longValue());
                Database.update(c,"UPDATE transactions SET status='EXPIRED',updated_at=SYSUTCDATETIME() WHERE order_id=? AND type='COD' AND status='PENDING'",oid);
                c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }
}
