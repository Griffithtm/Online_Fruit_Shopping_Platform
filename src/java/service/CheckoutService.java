package service;

import config.DBConnect;
import dao.AddressDAO;
import dao.CheckoutDAO;
import dao.Database;
import dao.SettingDAO;
import util.Messages;
import java.sql.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Checkout. All totals and stock are verified on the server. */
public class CheckoutService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final CheckoutDAO dao = new CheckoutDAO();
    private final AddressDAO addresses = new AddressDAO();
    private final SettingDAO settings = new SettingDAO();

    public Map<String,Object> quote(long uid,long addressId) throws Exception { return quote(uid,addressId,"",0,"COD"); }
    /** Read-only quote for GET /checkout (BA-10): no points expiry, no commit. */
    public Map<String,Object> quote(long uid,long addressId,String voucher,int points,String method) throws Exception {
        try(Connection c=new DBConnect().getConnection()){ return quote(c,uid,addressId,voucher,points,method); }
    }

    /** Lower case, trimmed, single spaces (BA-16). Accents are kept, so "Ha Noi" is not "Hà Nội". */
    static String normalizeArea(Object name){ return name==null?"":name.toString().trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT); }

    private Map<String,Object> serviceArea(Connection c,Object city) throws Exception {
        String wanted=normalizeArea(city);
        for(Map<String,Object> area:settings.activeServiceAreaRows(c)) if(!wanted.isEmpty() && normalizeArea(area.get("name")).equals(wanted)) return area;
        return null;
    }

    /** MSG37 reason from a promotion error: "Promotion budget is exhausted." becomes "promotion budget is exhausted". */
    private static String reason(Exception e){
        String m=e.getMessage()==null?"":e.getMessage().trim();
        if(m.endsWith("."))m=m.substring(0,m.length()-1);
        return m.isEmpty()?m:Character.toLowerCase(m.charAt(0))+m.substring(1);
    }

    private Map<String,Object> quote(Connection c,long uid,long addressId,String voucher,int points,String method) throws Exception {
        if(method==null || !Set.of("COD","CARD","E_WALLET").contains(method))throw Messages.error(Messages.MSG04,"payment method");
        if(!method.equals("COD"))new VnpayGateway().requireConfigured();
        if(points<0)throw Messages.error(Messages.MSG04,"reward points");
        Map<String,Object> address=addresses.findById(c,addressId,uid);
        if(address==null)throw Messages.error(Messages.MSG12);
        Map<String,Object> area=serviceArea(c,address.get("city"));
        if(area==null || area.get("fee")==null || ((BigDecimal)area.get("fee")).signum()<0)throw Messages.error(Messages.MSG36);
        // BA-11: the area may define its own eta_hours; otherwise the delivery_eta_hours policy applies.
        int etaHours=area.get("eta_hours")!=null && ((Number)area.get("eta_hours")).intValue()>0?((Number)area.get("eta_hours")).intValue():FulfillmentService.policy(c,"delivery_eta_hours",24);
        Map<String,Object> cart=dao.findCart(c,uid);
        if(cart==null)throw Messages.error(Messages.MSG35);
        List<Map<String,Object>> lines=dao.cartLines(c,FulfillmentService.id(cart.get("id")));
        if(lines.isEmpty())throw Messages.error(Messages.MSG35);
        BigDecimal subtotal=BigDecimal.ZERO;
        StringBuilder fingerprint=new StringBuilder(cart.get("id").toString());
        for(Map<String,Object> line:lines){
            // BA-09: a Seller may buy, but not from their own store.
            if(FulfillmentService.id(line.get("owner_id"))==uid)throw Messages.error(Messages.MSG12);
            if(line.get("price")==null)throw Messages.error(Messages.MSG32);
            if(((BigDecimal)line.get("price")).compareTo((BigDecimal)line.get("unit_price"))!=0)throw Messages.error(Messages.MSG34);
            // BA-13: only stock from batches that are still fresh on the estimated delivery date counts.
            long available=dao.availableUntilDelivery(c,line.get("variant_id"),etaHours);
            if(available<((Number)line.get("quantity")).longValue())throw Messages.error(Messages.MSG33,available,line.get("unit"),line.get("option_name"));
            subtotal=subtotal.add(((BigDecimal)line.get("price")).multiply(new BigDecimal(line.get("quantity").toString())));
            fingerprint.append('|').append(line.get("id")).append(':').append(line.get("quantity")).append(':').append(line.get("price"));
        }
        Map<Long,BigDecimal> discounts=new TreeMap<>();
        Map<Long,Map<String,Object>> groups=new LinkedHashMap<>();
        Map<Long,BigDecimal> campaignBases=new TreeMap<>();Map<Long,Long> campaignSeller=new HashMap<>();
        for(Map<String,Object> line:lines){long sid=FulfillmentService.id(line.get("seller_order_id"));Map<String,Object> g=groups.computeIfAbsent(sid,k->{Map<String,Object> m=new LinkedHashMap<>();m.put("subtotal",BigDecimal.ZERO);m.put("discount",BigDecimal.ZERO);m.put("storeName",line.get("store_name"));return m;});BigDecimal amount=((BigDecimal)line.get("price")).multiply(new BigDecimal(line.get("quantity").toString()));g.put("subtotal",((BigDecimal)g.get("subtotal")).add(amount));if(line.get("campaign_id")!=null){long pid=FulfillmentService.id(line.get("campaign_id"));campaignBases.merge(pid,amount,BigDecimal::add);campaignSeller.put(pid,sid);}}
        for(var entry:campaignBases.entrySet()){
            Map<String,Object> promo=PromotionService.active(c,entry.getKey());
            if(promo!=null){BigDecimal d;try{d=PromotionService.discount(c,uid,promo,entry.getValue());}catch(IllegalArgumentException ineligible){continue;}if(d.signum()==0)continue;discounts.put(entry.getKey(),d);Map<String,Object> g=groups.get(campaignSeller.get(entry.getKey()));g.put("discount",((BigDecimal)g.get("discount")).add(d));}
        }
        BigDecimal campaignTotal=discounts.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add),voucherDiscount=BigDecimal.ZERO;
        Long voucherId=null;
        if(voucher!=null && !voucher.isBlank()){
            Map<String,Object> ref=Database.one(c,"SELECT id FROM promotions WHERE type='VOUCHER' AND code=?",voucher.trim().toUpperCase(Locale.ROOT));
            Map<String,Object> promo=ref==null?null:PromotionService.active(c,FulfillmentService.id(ref.get("id")));
            if(promo==null)throw Messages.error(Messages.MSG37,"it is invalid or outside its validity period");
            voucherId=FulfillmentService.id(promo.get("id"));
            try{voucherDiscount=PromotionService.discount(c,uid,promo,subtotal.subtract(campaignTotal));}catch(IllegalArgumentException ineligible){throw Messages.error(Messages.MSG37,reason(ineligible));}
            discounts.put(voucherId,voucherDiscount);
        }
        BigDecimal eligible=subtotal.subtract(campaignTotal).subtract(voucherDiscount);
        int rate=FulfillmentService.policy(c,"points_vnd_per_point",1000);
        BigDecimal rateValue=new BigDecimal(rate);
        // Voucher share per seller order first; what is left is where points and their capacity come from.
        BigDecimal voucherLeft=voucherDiscount;int index=0;long capacity=0;
        Map<Long,BigDecimal> afterVoucher=new LinkedHashMap<>();
        for(var entry:groups.entrySet()){
            Map<String,Object> g=entry.getValue();boolean last=++index==groups.size();BigDecimal after=((BigDecimal)g.get("subtotal")).subtract((BigDecimal)g.get("discount"));
            BigDecimal vd=last?voucherLeft:(subtotal.subtract(campaignTotal).signum()==0?BigDecimal.ZERO:voucherDiscount.multiply(after).divide(subtotal.subtract(campaignTotal),0,RoundingMode.DOWN));voucherLeft=voucherLeft.subtract(vd);g.put("discount",((BigDecimal)g.get("discount")).add(vd));
            BigDecimal av=after.subtract(vd);afterVoucher.put(entry.getKey(),av);capacity+=av.divide(rateValue,0,RoundingMode.DOWN).longValueExact();
        }
        // BA-12: maximum = balance, policy share of the discounted item total, and what fits into the seller orders.
        long usable=dao.usablePoints(c,uid);
        int percent=Math.min(100,FulfillmentService.policy(c,"points_max_percent",50));
        long policyMax=eligible.multiply(new BigDecimal(percent)).divide(new BigDecimal(100L*rate),0,RoundingMode.DOWN).longValueExact();
        long maxPoints=Math.max(0,Math.min(usable,Math.min(policyMax,capacity)));
        if(points>maxPoints)throw Messages.error(Messages.MSG38,maxPoints);
        // Points are split in proportion to each group's amount, then any remainder goes to groups that still have room.
        Map<Long,Integer> pointShare=new LinkedHashMap<>();int given=0;
        for(var entry:afterVoucher.entrySet()){
            int room=entry.getValue().divide(rateValue,0,RoundingMode.DOWN).intValueExact();
            int share=eligible.signum()==0?0:new BigDecimal(points).multiply(entry.getValue()).divide(eligible,0,RoundingMode.DOWN).intValue();
            share=Math.min(share,room);pointShare.put(entry.getKey(),share);given+=share;
        }
        for(var entry:afterVoucher.entrySet()){
            if(given==points)break;
            int room=entry.getValue().divide(rateValue,0,RoundingMode.DOWN).intValueExact()-pointShare.get(entry.getKey());
            int extra=Math.min(room,points-given);pointShare.merge(entry.getKey(),extra,Integer::sum);given+=extra;
        }
        // BA-11: every seller order is a separate delivery and pays the area fee.
        BigDecimal areaFee=(BigDecimal)area.get("fee");
        for(var entry:groups.entrySet()){
            Map<String,Object> g=entry.getValue();int gp=pointShare.get(entry.getKey());
            g.put("points",gp);g.put("pointsDiscount",new BigDecimal(gp).multiply(rateValue));g.put("fee",areaFee);
        }
        BigDecimal fee=areaFee.multiply(new BigDecimal(groups.size()));
        BigDecimal pointDiscount=new BigDecimal(points).multiply(rateValue);
        Map<String,Object> q=new LinkedHashMap<>();q.put("cartId",cart.get("id"));q.put("address",address);q.put("lines",lines);q.put("subtotal",subtotal);q.put("fee",fee);q.put("areaFee",areaFee);q.put("discount",campaignTotal.add(voucherDiscount));q.put("pointsDiscount",pointDiscount);q.put("points",points);q.put("maxPoints",maxPoints);q.put("usablePoints",usable);q.put("voucherId",voucherId);q.put("discounts",discounts);q.put("groups",groups);q.put("method",method);q.put("total",eligible.subtract(pointDiscount).add(fee));
        q.put("etaHours",etaHours);
        q.put("fingerprint",fingerprint+"|"+address+"|"+fee+"|"+discounts+"|"+points+"|"+method+"|"+etaHours);return q;
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
                if(!Objects.equals(expected,q.get("fingerprint")))throw Messages.error(Messages.MSG34);
                long oid=((Number)q.get("cartId")).longValue();
                @SuppressWarnings("unchecked") Map<String,Object> a=(Map<String,Object>)q.get("address");
                String delivery=a.get("address_line")+", "+a.get("ward")+", "+a.get("district")+", "+a.get("city");
                if(delivery.length()>500)throw Messages.error(Messages.MSG03,"delivery address",500);
                Database.update(c,"DELETE FROM seller_orders WHERE order_id=? AND NOT EXISTS(SELECT 1 FROM order_items WHERE seller_order_id=seller_orders.id)",oid);
                Database.update(c,"UPDATE orders SET status=?,order_code=?,recipient_name=?,recipient_phone=?,delivery_address=?,delivery_address_id=?,payment_method=?,placed_at=SYSUTCDATETIME(),checkout_key=CONVERT(uniqueidentifier,?),item_total=?,delivery_fee=?,discount_amount=?,points_discount=?,points_used=?,voucher_id=?,estimated_delivery_at=DATEADD(hour,?,SYSUTCDATETIME()),payment_expires_at=CASE WHEN ?='COD' THEN NULL ELSE DATEADD(minute,?,SYSUTCDATETIME()) END,payable_amount=?,updated_at=SYSUTCDATETIME() WHERE id=? AND status='CART'",method.equals("COD")?"CONFIRMED":"AWAITING_PAYMENT","FR"+oid,a.get("recipient_name"),a.get("phone"),delivery,addressId,method,key,q.get("subtotal"),q.get("fee"),q.get("discount"),q.get("pointsDiscount"),points,q.get("voucherId"),q.get("etaHours"),method,FulfillmentService.policy(c,"reservation_timeout_minutes",15),q.get("total"),oid);
                Database.update(c,"UPDATE so SET status='NEW',subtotal=(SELECT SUM(quantity*unit_price) FROM order_items WHERE seller_order_id=so.id),updated_at=SYSUTCDATETIME() FROM seller_orders so WHERE order_id=?",oid);
                @SuppressWarnings("unchecked") Map<Long,Map<String,Object>> groups=(Map<Long,Map<String,Object>>)q.get("groups");
                for(var entry:groups.entrySet()){Map<String,Object> g=entry.getValue();Database.update(c,"UPDATE so SET delivery_fee=?,discount_amount=?,points_discount=?,points_used=?,cod_amount=?,commission_rate=COALESCE((SELECT TRY_CONVERT(decimal(9,6),JSON_VALUE(CASE WHEN ISJSON(t.value)=1 THEN t.value ELSE '{}' END,'$.commission_rate')) FROM stores s LEFT JOIN settings t ON t.id=s.seller_tier_id WHERE s.id=so.store_id),0) FROM seller_orders so WHERE id=?",g.get("fee"),g.get("discount"),g.get("pointsDiscount"),g.get("points"),method.equals("COD")?((BigDecimal)g.get("subtotal")).subtract((BigDecimal)g.get("discount")).subtract((BigDecimal)g.get("pointsDiscount")).add((BigDecimal)g.get("fee")):BigDecimal.ZERO,entry.getKey());}
                Database.update(c,"UPDATE oi SET product_title=l.title,variant_name=v.option_name FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id WHERE so.order_id=?",oid);
                Database.update(c,"UPDATE so SET store_name_snapshot=s.store_name FROM seller_orders so JOIN stores s ON s.id=so.store_id WHERE so.order_id=?",oid);
                // Errors of the shared services are reported with checkout messages (BA-17); the whole transaction rolls back.
                try{new StockReservationService().reserveOrder(c,oid);}catch(IllegalArgumentException changed){throw Messages.error(Messages.MSG34);}
                @SuppressWarnings("unchecked") Map<Long,BigDecimal> discounts=(Map<Long,BigDecimal>)q.get("discounts");
                try{PromotionService.redeem(c,uid,oid,discounts);}catch(IllegalArgumentException ended){throw Messages.error(Messages.MSG37,reason(ended));}
                try{LoyaltyService.redeem(c,uid,oid,points);}catch(IllegalArgumentException short_){throw Messages.error(Messages.MSG38,q.get("maxPoints"));}
                if(method.equals("COD")) Database.update(c,"INSERT transactions(type,user_id,order_id,method,amount,status,idempotency_key) VALUES('COD',?,?,'COD',?,'PENDING',?)",uid,oid,q.get("total"),"cod:"+oid);
                c.commit();return oid;
            }catch(Exception e){c.rollback();throw e;}
        }
    }

    /** MSG39 (COD) or MSG40 (online, deadline in Vietnam time) for an order the customer just placed; null otherwise (BA-18). */
    public String placedNotice(long uid,long oid) throws Exception {
        try(Connection c=new DBConnect().getConnection()){
            Map<String,Object> o=dao.findPlaced(c,oid,uid);
            if(o==null)return null;
            if("COD".equals(o.get("payment_method")) && "CONFIRMED".equals(o.get("status")))
                return Messages.get(Messages.MSG39,o.get("order_code"),new DecimalFormat("#,##0").format(o.get("payable_amount"))+" VND");
            if("AWAITING_PAYMENT".equals(o.get("status")) && o.get("payment_expires_at")!=null)
                return Messages.get(Messages.MSG40,o.get("order_code"),vietnamTime(o.get("payment_expires_at")));
            return null;
        }
    }

    /** Stored timestamps are UTC (NFR-DATA-01); customers see Vietnam time. */
    static String vietnamTime(Object utc){
        LocalDateTime t=utc instanceof Timestamp ts?ts.toLocalDateTime():(LocalDateTime)utc;
        return t.atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
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
                if(Database.one(c,"SELECT id FROM orders WHERE id=? AND placed_at>=DATEADD(minute,-?,SYSUTCDATETIME())",oid,FulfillmentService.policy(c,"cancellation_window_minutes",30))==null)throw new IllegalArgumentException("The cancellation window has expired.");
                List<Map<String,Object>> sellers=Database.rows(c,"SELECT id,status FROM seller_orders WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? ORDER BY store_id,id",oid);
                if(!"CONFIRMED".equals(o.get("status")) || !"COD".equals(o.get("payment_method")) || sellers.stream().anyMatch(x->!"NEW".equals(x.get("status"))))throw new IllegalArgumentException("This order can no longer be cancelled.");
                if(Database.one(c,"SELECT id FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? AND status IN ('PAID','COMPLETED')",oid)!=null)throw new IllegalArgumentException("Paid orders require a refund review.");
                Database.update(c,"UPDATE orders SET status='CANCELLED',updated_at=SYSUTCDATETIME() WHERE id=?",oid);
                Database.update(c,"UPDATE seller_orders SET status='CANCELLED',updated_at=SYSUTCDATETIME() WHERE order_id=?",oid);
                for(Map<String,Object> so:sellers)new StockReservationService().release(c,((Number)so.get("id")).longValue());
                Database.update(c,"UPDATE transactions SET status='EXPIRED',updated_at=SYSUTCDATETIME() WHERE order_id=? AND type='COD' AND status='PENDING'",oid);
                BenefitsService.returnAll(c,o);
                c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }
}
