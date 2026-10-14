package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** SQL for checkout quotes and placement notices (FE-05.1). The caller owns the connection/transaction. */
public class CheckoutDAO {

    public Map<String, Object> findCart(Connection c, long uid) throws SQLException {
        return Database.one(c, "SELECT id FROM orders WHERE customer_id=? AND status='CART'", uid);
    }

    /** Cart lines with their store owner and variant details; price is null when the option is no longer public. */
    public List<Map<String, Object>> cartLines(Connection c, long cartId) throws SQLException {
        return Database.rows(c, "SELECT oi.id,oi.variant_id,oi.quantity,oi.unit_price,so.id AS seller_order_id,l.campaign_id,p.price,l.title,s.store_name,s.owner_id,v.unit,v.option_name"
                + " FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN stores s ON s.id=so.store_id JOIN variants v ON v.id=oi.variant_id"
                + " JOIN listings l ON l.id=v.listing_id LEFT JOIN v_public_products p ON p.variant_id=oi.variant_id WHERE so.order_id=? ORDER BY so.store_id,oi.id", cartId);
    }

    /**
     * Stock that can still be reserved for delivery after {@code etaHours}: only batches that expire after
     * the estimated delivery date (BA-13). Same rule as StockReservationService.reserve.
     */
    public long availableUntilDelivery(Connection c, Object variantId, int etaHours) throws SQLException {
        Map<String, Object> r = Database.one(c, "SELECT COALESCE(SUM(CONVERT(BIGINT,remaining_qty-reserved_qty)),0) AS n FROM batches"
                + " WHERE variant_id=? AND status='AVAILABLE' AND remaining_qty>reserved_qty AND expiry_date>CONVERT(date,DATEADD(hour,?,SYSUTCDATETIME()))", variantId, etaHours);
        return ((Number) r.get("n")).longValue();
    }

    /** Points the user can redeem now: the balance minus lots that have already expired but not yet been swept (read-only, BA-10). */
    public long usablePoints(Connection c, long uid) throws SQLException {
        Map<String, Object> r = Database.one(c, "SELECT u.points_balance-COALESCE((SELECT SUM(t.remaining_points) FROM transactions t WHERE t.user_id=u.id AND t.type='POINTS_EARN'"
                + " AND t.status='AVAILABLE' AND t.remaining_points>0 AND t.points_expire_at<CONVERT(date,SYSUTCDATETIME())),0) AS n FROM users u WHERE u.id=?", uid);
        return r == null ? 0 : Math.max(0, ((Number) r.get("n")).longValue());
    }

    /** A placed order of this customer, for the confirmation notice; null for someone else's order or a cart. */
    public Map<String, Object> findPlaced(Connection c, long oid, long uid) throws SQLException {
        return Database.one(c, "SELECT order_code,status,payment_method,payable_amount,payment_expires_at FROM orders WHERE id=? AND customer_id=? AND status<>'CART'", oid, uid);
    }
}
