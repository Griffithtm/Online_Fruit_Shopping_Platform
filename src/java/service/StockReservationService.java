package service;

import dao.Database;
import java.sql.*;
import java.util.*;

/**
 * Used by Iteration 2 checkout in its existing transaction; carts never reserve
 * stock.
 */
public class StockReservationService {

    /** Checkout must update order state and call this before committing its transaction. */
    public void reserveOrder(Connection c, long orderId) throws Exception {
        if (c.getAutoCommit()) throw new IllegalStateException("Checkout transaction required.");
        Savepoint save = c.setSavepoint();
        try {
            if (Database.one(c,"SELECT id FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status IN ('PLACED','AWAITING_PAYMENT','CONFIRMED')",orderId)==null) throw new IllegalArgumentException("Order is not ready for reservation.");
            java.util.List<Map<String,Object>> sellers=Database.rows(c,"SELECT so.id FROM seller_orders so WITH(UPDLOCK,HOLDLOCK) WHERE so.order_id=? AND EXISTS(SELECT 1 FROM order_items WHERE seller_order_id=so.id) ORDER BY so.store_id,so.id",orderId);
            if(sellers.isEmpty()) throw new IllegalArgumentException("Cannot reserve an empty order.");
            for(Map<String,Object> seller:sellers) reserve(c,((Number)seller.get("id")).longValue());
        } catch(Exception e) {c.rollback(save);throw e;}
    }

    public void reserve(Connection c, long sellerOrderId) throws Exception {
        if (c.getAutoCommit()) {
            throw new IllegalStateException("Stock reservation requires the checkout transaction.");
        }
        Savepoint save = c.setSavepoint();
        try {
            Map<String, Object> order = Database.one(c, "SELECT so.id FROM seller_orders so WITH(UPDLOCK,HOLDLOCK) JOIN orders o ON o.id=so.order_id WHERE so.id=? AND so.status IN ('NEW','ACCEPTED') AND o.status IN ('PLACED','AWAITING_PAYMENT','CONFIRMED')", sellerOrderId);
            if (order == null) {
                throw new IllegalArgumentException("Stock can only be reserved for placed orders.");
            }
            List<Map<String, Object>> lines = Database.rows(c, "SELECT * FROM order_items WITH(UPDLOCK,HOLDLOCK) WHERE seller_order_id=? AND reservation_status='NONE' ORDER BY variant_id,id", sellerOrderId);
            for (Map<String, Object> line : lines) {
                Map<String,Object> product = Database.one(c, "SELECT v.price,l.status AS listing_status,v.status AS variant_status,s.status AS store_status,u.status AS owner_status,l.listing_type FROM variants v WITH(UPDLOCK,HOLDLOCK) JOIN listings l WITH(UPDLOCK,HOLDLOCK) ON l.id=v.listing_id JOIN stores s WITH(UPDLOCK,HOLDLOCK) ON s.id=l.store_id JOIN users u WITH(UPDLOCK,HOLDLOCK) ON u.id=s.owner_id WHERE v.id=?", line.get("variant_id"));
                if (product == null || !"APPROVED".equals(product.get("listing_status")) || !"ACTIVE".equals(product.get("variant_status")) || !"ACTIVE".equals(product.get("store_status")) || !"ACTIVE".equals(product.get("owner_status")) || !"PRODUCT".equals(product.get("listing_type"))) throw new IllegalArgumentException("A product is no longer available. Review your cart.");
                if (((java.math.BigDecimal)product.get("price")).compareTo((java.math.BigDecimal)line.get("unit_price")) != 0) throw new IllegalArgumentException("A price has changed. Review and update your cart.");
                int left = ((Number) line.get("quantity")).intValue();
                boolean first = true;
                // BA-13, ThanhNC: a batch must still be fresh on the order's estimated delivery date (today when the order has none).
                List<Map<String, Object>> batches = Database.rows(c, "SELECT * FROM batches WITH(UPDLOCK,HOLDLOCK) WHERE variant_id=? AND status='AVAILABLE' AND expiry_date>(SELECT COALESCE(CONVERT(date,o.estimated_delivery_at),CONVERT(date,SYSUTCDATETIME())) FROM seller_orders so JOIN orders o ON o.id=so.order_id WHERE so.id=?) AND remaining_qty>reserved_qty ORDER BY expiry_date,id", line.get("variant_id"), sellerOrderId);
                for (Map<String, Object> b : batches) {
                    int take = Math.min(left, ((Number) b.get("remaining_qty")).intValue() - ((Number) b.get("reserved_qty")).intValue());
                    if (take <= 0) {
                        continue;
                    }
                    if (Database.update(c, "UPDATE batches SET reserved_qty=reserved_qty+? WHERE id=? AND remaining_qty-reserved_qty>=?", take, b.get("id"), take) != 1) {
                        throw new IllegalArgumentException("Stock has changed. Please try again.");
                    }
                    long item;
                    if (first) {
                        item = ((Number) line.get("id")).longValue();
                        Database.update(c, "UPDATE order_items SET batch_id=?,quantity=?,unit_cost=?,reservation_status='RESERVED' WHERE id=?", b.get("id"), take, b.get("unit_cost"), item);
                        first = false;
                    } else {
                        item = Database.insert(c, "INSERT order_items(seller_order_id,variant_id,batch_id,quantity,unit_price,unit_cost,reservation_status,product_title,variant_name) VALUES(?,?,?,?,?,?,'RESERVED',?,?)", sellerOrderId, line.get("variant_id"), b.get("id"), take, line.get("unit_price"), b.get("unit_cost"),line.get("product_title"),line.get("variant_name"));
                    }
                    Database.update(c, "INSERT stock_movements(batch_id,order_item_id,movement_type,quantity,idempotency_key) VALUES(?,?,'RESERVE',?,?)", b.get("id"), item, take, "reserve:" + item);
                    left -= take;
                    if (left == 0) {
                        break;
                    }
                }
                if (left > 0) {
                    throw new IllegalArgumentException("Insufficient stock to reserve the entire order.");
                }
            }
        } catch (Exception e) {
            c.rollback(save);
            throw e;
        }
    }
    /** Release only after the lifecycle service has changed the order status in this transaction. */
    public void release(Connection c, long sellerOrderId) throws Exception {
        if (c.getAutoCommit()) throw new IllegalStateException("Stock release requires an order transaction.");
        Savepoint save = c.setSavepoint();
        try {
            Map<String,Object> order = Database.one(c, "SELECT so.id FROM seller_orders so WITH(UPDLOCK,HOLDLOCK) JOIN orders o WITH(UPDLOCK,HOLDLOCK) ON o.id=so.order_id WHERE so.id=? AND (so.status IN ('CANCELLED','REJECTED') OR o.status IN ('CANCELLED','EXPIRED'))", sellerOrderId);
            if (order == null) throw new IllegalArgumentException("Only cancelled, expired or rejected orders can release stock.");
            for (Map<String,Object> line : Database.rows(c, "SELECT id,batch_id,quantity FROM order_items WITH(UPDLOCK,HOLDLOCK) WHERE seller_order_id=? AND reservation_status='RESERVED' ORDER BY batch_id,id", sellerOrderId)) {
                if (Database.update(c,"UPDATE batches SET reserved_qty=reserved_qty-? WHERE id=? AND reserved_qty>=?",line.get("quantity"),line.get("batch_id"),line.get("quantity")) != 1) throw new IllegalStateException("Reserved stock is inconsistent.");
                Database.update(c,"UPDATE order_items SET reservation_status='RELEASED' WHERE id=?",line.get("id"));
                Database.update(c,"INSERT stock_movements(batch_id,order_item_id,movement_type,quantity,idempotency_key) VALUES(?,?,'RELEASE',?,?)",line.get("batch_id"),line.get("id"),line.get("quantity"),"release:"+line.get("id"));
            }
        } catch(Exception e) { c.rollback(save); throw e; }
    }
}
