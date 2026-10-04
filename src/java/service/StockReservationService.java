package service;

import dao.Database;
import java.sql.*;
import java.util.*;

/**
 * Used by Iteration 2 checkout in its existing transaction; carts never reserve
 * stock.
 */
public class StockReservationService {

    public void reserve(Connection c, long sellerOrderId) throws Exception {
        if (c.getAutoCommit()) {
            throw new IllegalStateException("Stock reservation requires the checkout transaction.");
        }
        Savepoint save = c.setSavepoint();
        try {
            Map<String, Object> order = Database.one(c, "SELECT so.id FROM seller_orders so WITH(UPDLOCK,HOLDLOCK) JOIN orders o ON o.id=so.order_id WHERE so.id=? AND o.status IN ('PLACED','AWAITING_PAYMENT','CONFIRMED')", sellerOrderId);
            if (order == null) {
                throw new IllegalArgumentException("Stock can only be reserved for placed orders.");
            }
            List<Map<String, Object>> lines = Database.rows(c, "SELECT * FROM order_items WITH(UPDLOCK,HOLDLOCK) WHERE seller_order_id=? AND reservation_status='NONE' ORDER BY variant_id,id", sellerOrderId);
            for (Map<String, Object> line : lines) {
                int left = ((Number) line.get("quantity")).intValue();
                boolean first = true;
                List<Map<String, Object>> batches = Database.rows(c, "SELECT * FROM batches WITH(UPDLOCK,HOLDLOCK) WHERE variant_id=? AND status='AVAILABLE' AND expiry_date>CONVERT(date,SYSUTCDATETIME()) AND remaining_qty>reserved_qty ORDER BY expiry_date,id", line.get("variant_id"));
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
                        item = Database.insert(c, "INSERT order_items(seller_order_id,variant_id,batch_id,quantity,unit_price,unit_cost,reservation_status) VALUES(?,?,?,?,?,?,'RESERVED')", sellerOrderId, line.get("variant_id"), b.get("id"), take, line.get("unit_price"), b.get("unit_cost"));
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
}
