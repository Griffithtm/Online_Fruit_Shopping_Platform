package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import model.Order;
import model.SellerOrder;
import model.SellerOrderDetails;

/** SQL for the order lifecycle shared by checkout and fulfilment (FE-05.1/05.2). The caller owns the transaction. */
public class OrderDAO {

    /** BA-15: the deadline is computed from the order's own placed_at, so later policy changes do not affect it. */
    public int setCancelDeadline(Connection c, long oid, int windowMinutes) throws SQLException {
        return Database.update(c, "UPDATE orders SET cancel_deadline_at=DATEADD(minute,?,placed_at) WHERE id=?", windowMinutes, oid);
    }

    /** True while the customer may still cancel; orders placed before the snapshot fall back to placed_at + current window. */
    public boolean withinCancelDeadline(Connection c, long oid, int windowMinutes) throws SQLException {
        return Database.one(Order.class, c, "SELECT id FROM orders WHERE id=? AND SYSUTCDATETIME()<=COALESCE(cancel_deadline_at,DATEADD(minute,?,placed_at))", oid, windowMinutes) != null;
    }

    /**
     * Completes the order when every seller order is final (Delivered, Cancelled or Rejected) and at least one was Delivered,
     * which is the rule of the two checks it replaces. Returns true only for the call that actually completed it.
     */
    public boolean completeIfDone(Connection c, long oid) throws SQLException {
        boolean open = Database.one(SellerOrder.class, c, "SELECT TOP 1 id FROM seller_orders WHERE order_id=? AND status NOT IN ('DELIVERED','CANCELLED','REJECTED')", oid) != null;
        boolean delivered = Database.one(SellerOrder.class, c, "SELECT TOP 1 id FROM seller_orders WHERE order_id=? AND status='DELIVERED'", oid) != null;
        if (open || !delivered) {
            return false;
        }
        if (Database.update(c, "UPDATE orders SET status='COMPLETED',updated_at=SYSUTCDATETIME() WHERE id=? AND status='CONFIRMED'", oid) != 1) {
            return false;
        }
        Database.update(c, "UPDATE transactions SET status='COMPLETED' WHERE order_id=? AND type='COD' AND seller_order_id IS NULL AND status='PENDING'", oid);
        return true;
    }

    /** Seller orders of a placed order with their store owner, for new-order notifications (BA-14). */
    public List<SellerOrderDetails> sellerOrdersWithOwner(Connection c, long oid) throws SQLException {
        return Database.rows(SellerOrderDetails.class, c, "SELECT so.id,so.store_id,s.owner_id FROM seller_orders so JOIN stores s ON s.id=so.store_id WHERE so.order_id=? ORDER BY so.store_id,so.id", oid);
    }
}
