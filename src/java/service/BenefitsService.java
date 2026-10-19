package service;

import model.*;

import dao.Database;
import java.sql.*;
import java.util.*;
import java.math.*;

public class BenefitsService {

    public static void returnSeller(Connection c, Order order, SellerOrder so) throws Exception {
        int points = so.getPointsUsed().intValue();
        LoyaltyService.restore(c, FulfillmentService.id(order.getCustomerId()), FulfillmentService.id(so.getId()), points);
    }

    public static void returnAll(Connection c, Order o) throws Exception {
        for (SellerOrder so : Database.rows(SellerOrder.class, c, "SELECT * FROM seller_orders WHERE order_id=?", o.getId())) {
            returnSeller(c, o, so);
        }
        for (PromotionRedemption r : Database.rows(PromotionRedemption.class, c, "SELECT * FROM promotion_redemptions WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? AND status='APPLIED'", o.getId())) {
            Database.update(c, "UPDATE promotions SET used_count=used_count-1,budget_used=budget_used-? WHERE id=? AND used_count>0 AND budget_used>=?", r.getDiscountAmount(), r.getPromotionId(), r.getDiscountAmount());
            Database.update(c, "UPDATE promotion_redemptions SET status='RELEASED' WHERE id=?", r.getId());
        }
    }

    /**
     * ThanhNC BA-21: when one SellerOrder is rejected or cancelled, return the usage and budget of that store's campaign
     * for this order, exactly as returnAll does. The marketplace voucher stays applied to the remaining order.
     * Released rows are no longer APPLIED, so a later returnAll for the whole order does not return them twice.
     */
    public static void returnCampaign(Connection c, long orderId, long storeId) throws Exception {
        for (PromotionRedemption r : Database.rows(PromotionRedemption.class, c, "SELECT r.* FROM promotion_redemptions r WITH(UPDLOCK,HOLDLOCK) JOIN promotions p ON p.id=r.promotion_id WHERE r.order_id=? AND p.store_id=? AND p.type='SELLER_CAMPAIGN' AND r.status='APPLIED'", orderId, storeId)) {
            Database.update(c, "UPDATE promotions SET used_count=used_count-1,budget_used=budget_used-? WHERE id=? AND used_count>0 AND budget_used>=?", r.getDiscountAmount(), r.getPromotionId(), r.getDiscountAmount());
            Database.update(c, "UPDATE promotion_redemptions SET status='RELEASED' WHERE id=?", r.getId());
        }
    }
}
