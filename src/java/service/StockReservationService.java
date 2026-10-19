package service;

import model.*;

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
        if (c.getAutoCommit()) throw new IllegalStateException("Cần có giao dịch đặt hàng.");
        Savepoint save = c.setSavepoint();
        try {
            if (Database.one(Order.class, c, "SELECT id FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status IN ('PLACED','AWAITING_PAYMENT','CONFIRMED')",orderId)==null) throw new IllegalArgumentException("Đơn hàng chưa sẵn sàng để giữ tồn kho.");
            java.util.List<SellerOrder> sellers=Database.rows(SellerOrder.class, c, "SELECT so.id FROM seller_orders so WITH(UPDLOCK,HOLDLOCK) WHERE so.order_id=? AND EXISTS(SELECT 1 FROM order_items WHERE seller_order_id=so.id) ORDER BY so.store_id,so.id",orderId);
            if(sellers.isEmpty()) throw new IllegalArgumentException("Không thể giữ tồn kho cho đơn hàng trống.");
            for(SellerOrder seller:sellers) reserve(c,seller.getId().longValue());
        } catch(Exception e) {c.rollback(save);throw e;}
    }

    public void reserve(Connection c, long sellerOrderId) throws Exception {
        if (c.getAutoCommit()) {
            throw new IllegalStateException("Giữ tồn kho phải được thực hiện trong giao dịch đặt hàng.");
        }
        Savepoint save = c.setSavepoint();
        try {
            SellerOrder order = Database.one(SellerOrder.class, c, "SELECT so.id FROM seller_orders so WITH(UPDLOCK,HOLDLOCK) JOIN orders o ON o.id=so.order_id WHERE so.id=? AND so.status IN ('NEW','ACCEPTED') AND o.status IN ('PLACED','AWAITING_PAYMENT','CONFIRMED')", sellerOrderId);
            if (order == null) {
                throw new IllegalArgumentException("Chỉ giữ kho cho đơn đã đặt.");
            }
            List<OrderItem> lines = Database.rows(OrderItem.class, c, "SELECT * FROM order_items WITH(UPDLOCK,HOLDLOCK) WHERE seller_order_id=? AND reservation_status='NONE' ORDER BY variant_id,id", sellerOrderId);
            for (OrderItem line : lines) {
                StockProduct product = Database.one(StockProduct.class, c, "SELECT v.price,l.status AS listing_status,v.status AS variant_status,s.status AS store_status,u.status AS owner_status,l.listing_type FROM variants v WITH(UPDLOCK,HOLDLOCK) JOIN listings l WITH(UPDLOCK,HOLDLOCK) ON l.id=v.listing_id JOIN stores s WITH(UPDLOCK,HOLDLOCK) ON s.id=l.store_id JOIN users u WITH(UPDLOCK,HOLDLOCK) ON u.id=s.owner_id WHERE v.id=?", line.getVariantId());
                if (product == null || !"APPROVED".equals(product.getListingStatus()) || !"ACTIVE".equals(product.getVariantStatus()) || !"ACTIVE".equals(product.getStoreStatus()) || !"ACTIVE".equals(product.getOwnerStatus()) || !"PRODUCT".equals(product.getListingType())) throw new IllegalArgumentException("Có sản phẩm đã ngừng bán. Vui lòng kiểm tra giỏ hàng.");
                if ((product.getPrice()).compareTo(line.getUnitPrice()) != 0) throw new IllegalArgumentException("Giá đã thay đổi. Vui lòng kiểm tra và cập nhật giỏ hàng.");
                int left = line.getQuantity().intValue();
                boolean first = true;
                // BA-13, ThanhNC: a batch must still be fresh on the order's estimated delivery date (today when the order has none).
                List<Batch> batches = Database.rows(Batch.class, c, "SELECT * FROM batches WITH(UPDLOCK,HOLDLOCK) WHERE variant_id=? AND status='AVAILABLE' AND expiry_date>(SELECT COALESCE(CONVERT(date,o.estimated_delivery_at),CONVERT(date,SYSUTCDATETIME())) FROM seller_orders so JOIN orders o ON o.id=so.order_id WHERE so.id=?) AND remaining_qty>reserved_qty ORDER BY expiry_date,id", line.getVariantId(), sellerOrderId);
                for (Batch b : batches) {
                    int take = Math.min(left, b.getRemainingQty().intValue() - b.getReservedQty().intValue());
                    if (take <= 0) {
                        continue;
                    }
                    if (Database.update(c, "UPDATE batches SET reserved_qty=reserved_qty+? WHERE id=? AND remaining_qty-reserved_qty>=?", take, b.getId(), take) != 1) {
                        throw new IllegalArgumentException("Tồn kho đã thay đổi. Vui lòng thử lại.");
                    }
                    long item;
                    if (first) {
                        item = line.getId().longValue();
                        Database.update(c, "UPDATE order_items SET batch_id=?,quantity=?,unit_cost=?,reservation_status='RESERVED' WHERE id=?", b.getId(), take, b.getUnitCost(), item);
                        first = false;
                    } else {
                        item = Database.insert(c, "INSERT order_items(seller_order_id,variant_id,batch_id,quantity,unit_price,unit_cost,reservation_status,product_title,variant_name) VALUES(?,?,?,?,?,?,'RESERVED',?,?)", sellerOrderId, line.getVariantId(), b.getId(), take, line.getUnitPrice(), b.getUnitCost(),line.getProductTitle(),line.getVariantName());
                    }
                    Database.update(c, "INSERT stock_movements(batch_id,order_item_id,movement_type,quantity,idempotency_key) VALUES(?,?,'RESERVE',?,?)", b.getId(), item, take, "reserve:" + item);
                    left -= take;
                    if (left == 0) {
                        break;
                    }
                }
                if (left > 0) {
                    throw new IllegalArgumentException("Không đủ hàng để giữ toàn bộ đơn.");
                }
            }
        } catch (Exception e) {
            c.rollback(save);
            throw e;
        }
    }
    /** Release only after the lifecycle service has changed the order status in this transaction. */
    public void release(Connection c, long sellerOrderId) throws Exception {
        if (c.getAutoCommit()) throw new IllegalStateException("Giải phóng tồn kho phải được thực hiện trong giao dịch đơn hàng.");
        Savepoint save = c.setSavepoint();
        try {
            SellerOrder order = Database.one(SellerOrder.class, c, "SELECT so.id FROM seller_orders so WITH(UPDLOCK,HOLDLOCK) JOIN orders o WITH(UPDLOCK,HOLDLOCK) ON o.id=so.order_id WHERE so.id=? AND (so.status IN ('CANCELLED','REJECTED') OR o.status IN ('CANCELLED','EXPIRED'))", sellerOrderId);
            if (order == null) throw new IllegalArgumentException("Chỉ đơn đã hủy, hết hạn hoặc bị từ chối mới được giải phóng tồn kho.");
            for (OrderItem line : Database.rows(OrderItem.class, c, "SELECT id,batch_id,quantity FROM order_items WITH(UPDLOCK,HOLDLOCK) WHERE seller_order_id=? AND reservation_status='RESERVED' ORDER BY batch_id,id", sellerOrderId)) {
                if (Database.update(c,"UPDATE batches SET reserved_qty=reserved_qty-? WHERE id=? AND reserved_qty>=?",line.getQuantity(),line.getBatchId(),line.getQuantity()) != 1) throw new IllegalStateException("Dữ liệu tồn kho đã giữ không nhất quán.");
                Database.update(c,"UPDATE order_items SET reservation_status='RELEASED' WHERE id=?",line.getId());
                Database.update(c,"INSERT stock_movements(batch_id,order_item_id,movement_type,quantity,idempotency_key) VALUES(?,?,'RELEASE',?,?)",line.getBatchId(),line.getId(),line.getQuantity(),"release:"+line.getId());
            }
        } catch(Exception e) { c.rollback(save); throw e; }
    }
}
