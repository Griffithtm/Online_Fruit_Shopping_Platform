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

    /** BA-39: evidence is required for COD or above the threshold; decided when the Seller marks the order ready. */
    public int setEvidenceRequired(Connection c, long sid, boolean required) throws SQLException {
        return Database.update(c, "UPDATE seller_orders SET evidence_required=? WHERE id=?", required, sid);
    }

    /** BA-40: In Transit and Arriving event times. */
    public int markInTransit(Connection c, long sid) throws SQLException {
        return Database.update(c, "UPDATE seller_orders SET in_transit_at=SYSUTCDATETIME() WHERE id=?", sid);
    }

    public int markArriving(Connection c, long sid) throws SQLException {
        return Database.update(c, "UPDATE seller_orders SET arriving_at=SYSUTCDATETIME() WHERE id=?", sid);
    }

    /** BA-38: when the courier recorded the exact COD amount. */
    public int markCodCollected(Connection c, long sid) throws SQLException {
        return Database.update(c, "UPDATE seller_orders SET cod_collected_at=SYSUTCDATETIME() WHERE id=?", sid);
    }

    /**
     * BA-47: users who may see an evidence photo besides Admin: the customer of the order and the assigned courier.
     * Null when no seller order uses this photo.
     */
    public List<Long> evidenceViewers(Connection c, String evidencePath) throws SQLException {
        SellerOrder so = Database.one(SellerOrder.class, c, "SELECT order_id,delivery_staff_id FROM seller_orders WHERE evidence_url=?", evidencePath);
        if (so == null) {
            return null;
        }
        Order o = Database.one(Order.class, c, "SELECT customer_id FROM orders WHERE id=?", so.getOrderId());
        List<Long> viewers = new java.util.ArrayList<>();
        viewers.add(o.getCustomerId());
        if (so.getDeliveryStaffId() != null) {
            viewers.add(so.getDeliveryStaffId());
        }
        return viewers;
    }

    /** Seller order with its order code, current status and evidence path (for the courier's result message). */
    public SellerOrderDetails sellerOrderHeader(Connection c, long sid) throws SQLException {
        return Database.one(SellerOrderDetails.class, c, "SELECT so.id,so.store_id,so.status,so.evidence_url,so.failure_reason,o.order_code FROM seller_orders so JOIN orders o ON o.id=so.order_id WHERE so.id=?", sid);
    }

    /** BA-20: the customer's own order, locked for the rest of the transaction (null when it is not theirs). */
    public Order lockOwnedOrder(Connection c, long oid, long uid) throws SQLException {
        return Database.one(Order.class, c, "SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND customer_id=?", oid, uid);
    }

    /** Seller orders of an order, locked, in the usual lock order (store, id). */
    public List<SellerOrder> lockSellerOrders(Connection c, long oid) throws SQLException {
        return Database.rows(SellerOrder.class, c, "SELECT * FROM seller_orders WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? ORDER BY store_id,id", oid);
    }

    /** BA-20: an unpaid online order is cancelled as a whole. Returns 1 only while it is still awaiting payment. */
    public int cancelAwaitingOrder(Connection c, long oid, String reason) throws SQLException {
        int n = Database.update(c, "UPDATE orders SET status='CANCELLED',updated_at=SYSUTCDATETIME() WHERE id=? AND status='AWAITING_PAYMENT'", oid);
        if (n == 1) {
            Database.update(c, "UPDATE seller_orders SET status='CANCELLED',rejection_reason=?,updated_at=SYSUTCDATETIME() WHERE order_id=?", reason, oid);
        }
        return n;
    }

    /** BA-20: open payment attempts of a cancelled order can no longer be completed (status only; amounts are immutable). */
    public int expireInitiatedPayments(Connection c, long oid) throws SQLException {
        return Database.update(c, "UPDATE transactions SET status='EXPIRED',updated_at=SYSUTCDATETIME() WHERE order_id=? AND type='PAYMENT' AND status='INITIATED'", oid);
    }

    /** Seller orders of a placed order with their store owner, for new-order notifications (BA-14). */
    public List<SellerOrderDetails> sellerOrdersWithOwner(Connection c, long oid) throws SQLException {
        return Database.rows(SellerOrderDetails.class, c, "SELECT so.id,so.store_id,s.owner_id FROM seller_orders so JOIN stores s ON s.id=so.store_id WHERE so.order_id=? ORDER BY so.store_id,so.id", oid);
    }
}
