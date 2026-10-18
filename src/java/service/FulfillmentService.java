package service;

import model.*;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.math.*;
import java.util.*;

/**
 * Every order mutation locks the parent order before seller orders and batches.
 */
public class FulfillmentService {

    public static int policy(Connection c, String key, int fallback) throws Exception {
        CountResult p = Database.one(CountResult.class, c, "SELECT TRY_CONVERT(int,value) AS n FROM settings WHERE type='POLICY' AND name=? AND status='ACTIVE'", key);
        return p != null && p.getN() != null && p.getN().intValue() > 0 ? p.getN().intValue() : fallback;
    }

    public static long id(Object n) {
        return ((Number) n).longValue();
    }

    public static BigDecimal money(Object n) {
        return n == null ? BigDecimal.ZERO : (BigDecimal) n;
    }

    public static void role(Connection c, long uid, String required) throws Exception {
        if (Database.one(User.class, c, "SELECT id FROM users WHERE id=? AND role=? AND status='ACTIVE'", uid, required) == null) {
            throw new IllegalArgumentException("Bạn không có quyền thực hiện thao tác này.");
        }
    }

    public List<SellerOrderDetails> list(long uid, String role) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            role(c, uid, role);
            String condition = switch (role) {
                case "SELLER" ->
                    "s.owner_id=?";
                case "DELIVERY_STAFF" ->
                    "so.delivery_staff_id=?";
                case "ADMIN" ->
                    "? > 0";
                default ->
                    throw new IllegalArgumentException("Bạn không có quyền thực hiện thao tác này.");
            };
            return Database.rows(SellerOrderDetails.class, c, "SELECT so.*,o.order_code,o.payment_method,o.recipient_name,o.recipient_phone,o.delivery_address,o.status AS order_status,s.store_name,a.address_line AS pickup_address,a.city AS pickup_city FROM seller_orders so JOIN orders o ON o.id=so.order_id JOIN stores s ON s.id=so.store_id LEFT JOIN addresses a ON a.id=s.pickup_address_id WHERE o.status<>'CART' AND " + condition + " ORDER BY so.id DESC", uid);
        }
    }

    public List<OrderItemDetails> allocations(long uid, String role) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            if (!Set.of("SELLER", "ADMIN", "DELIVERY_STAFF").contains(role)) {
                throw new IllegalArgumentException("Bạn không có quyền thực hiện thao tác này.");
            }
            role(c, uid, role);
            String condition = role.equals("SELLER") ? "s.owner_id=?" : role.equals("DELIVERY_STAFF") ? "so.delivery_staff_id=?" : "?>0";
            return Database.rows(OrderItemDetails.class, c, "SELECT oi.*,l.title,v.option_name,b.batch_no,b.expiry_date FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN orders o ON o.id=so.order_id JOIN stores s ON s.id=so.store_id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id LEFT JOIN batches b ON b.id=oi.batch_id WHERE o.status<>'CART' AND " + condition + " ORDER BY so.id,oi.id", uid);
        }
    }

    public void change(long actor, String role, long sid, String action, String detail, Long courier, boolean collected) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                SellerOrder ref = Database.one(SellerOrder.class, c, "SELECT order_id FROM seller_orders WHERE id=?", sid);
                if (ref == null) {
                    throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
                }
                Order o = Database.one(Order.class, c, "SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?", ref.getOrderId());
                SellerOrderDetails so = Database.one(SellerOrderDetails.class, c, "SELECT so.*,s.owner_id FROM seller_orders so WITH(UPDLOCK,HOLDLOCK) JOIN stores s ON s.id=so.store_id WHERE so.id=?", sid);
                role(c, actor, role);
                boolean customer = "CUSTOMER".equals(role) || "SELLER".equals(role) && action.equals("cancel");
                if (customer) {
                    if (id(o.getCustomerId()) != actor || !action.equals("cancel")) {
                        throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
                    }
                } else if (role.equals("SELLER") && id(so.getOwnerId()) != actor || role.equals("DELIVERY_STAFF") && (so.getDeliveryStaffId() == null || id(so.getDeliveryStaffId()) != actor)) {
                    throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
                }
                String old = so.getStatus(), next;
                if (!"CONFIRMED".equals(o.getStatus())) {
                    throw new IllegalArgumentException("Đơn hàng chưa được xác nhận để xử lý.");
                }
                switch (action) {
                    case "accept":
                        require(role.equals("SELLER") && old.equals("NEW"));
                        next = "ACCEPTED";
                        break;
                    case "reject":
                        require(role.equals("SELLER") && Set.of("NEW", "ACCEPTED").contains(old));
                        detail = Validation.text(detail, 500);
                        next = "REJECTED";
                        break;
                    case "cancel":
                        require(customer && Set.of("NEW", "ACCEPTED").contains(old));
                        if (Database.one(Order.class, c, "SELECT id FROM orders WHERE id=? AND placed_at>=DATEADD(minute,-?,SYSUTCDATETIME())", o.getId(), policy(c, "cancellation_window_minutes", 30)) == null) {
                            throw new IllegalArgumentException("Đã hết thời hạn cho phép hủy đơn.");
                        }
                        detail = "Khách hàng hủy đơn";
                        next = "CANCELLED";
                        break;
                    case "ready":
                        require(role.equals("SELLER") && old.equals("ACCEPTED"));
                        if (Database.one(OrderItem.class, c, "SELECT TOP 1 oi.id FROM order_items oi LEFT JOIN batches b ON b.id=oi.batch_id WHERE oi.seller_order_id=? AND (oi.reservation_status<>'RESERVED' OR b.id IS NULL OR b.status<>'AVAILABLE' OR b.expiry_date<=CONVERT(date,SYSUTCDATETIME()))", sid) != null) {
                            throw new IllegalArgumentException("Hàng đã phân bổ không khả dụng hoặc hết hạn. Vui lòng liên hệ quản trị viên.");
                        }
                        if (Database.one(Store.class, c, "SELECT id FROM stores WHERE id=? AND pickup_address_id IS NOT NULL", so.getStoreId()) == null) {
                            throw new IllegalArgumentException("Vui lòng đặt địa chỉ lấy hàng của cửa hàng trước khi bàn giao.");
                        }
                        next = "READY";
                        break;
                    case "assign":
                        require(role.equals("ADMIN") && old.equals("READY") && courier != null);
                        role(c, courier, "DELIVERY_STAFF");
                        Database.update(c, "UPDATE seller_orders SET delivery_staff_id=?,assigned_at=SYSUTCDATETIME(),cold_chain_note=?,evidence_required=1 WHERE id=?", courier, Validation.text(detail, 255), sid);
                        next = "ASSIGNED";
                        break;
                    case "pickup":
                        require(role.equals("DELIVERY_STAFF") && old.equals("ASSIGNED"));
                        next = "PICKED_UP";
                        Database.update(c, "UPDATE seller_orders SET picked_up_at=SYSUTCDATETIME() WHERE id=?", sid);
                        break;
                    case "transit":
                        require(role.equals("DELIVERY_STAFF") && old.equals("PICKED_UP"));
                        next = "IN_TRANSIT";
                        break;
                    case "arrive":
                        require(role.equals("DELIVERY_STAFF") && old.equals("IN_TRANSIT"));
                        next = "ARRIVING";
                        break;
                    case "deliver":
                        require(role.equals("DELIVERY_STAFF") && old.equals("ARRIVING"));
                        detail = evidence(detail);
                        if ("COD".equals(o.getPaymentMethod()) && !collected) {
                            throw new IllegalArgumentException("Vui lòng xác nhận đã thu tiền trước khi đánh dấu giao thành công.");
                        }
                        consume(c, sid);
                        Database.update(c, "UPDATE seller_orders SET evidence_url=?,delivered_at=SYSUTCDATETIME(),commission=ROUND(subtotal*COALESCE(commission_rate,0),2) WHERE id=?", detail, sid);
                        next = "DELIVERED";
                        break;
                    case "fail":
                        require(role.equals("DELIVERY_STAFF") && Set.of("ASSIGNED", "PICKED_UP", "IN_TRANSIT", "ARRIVING").contains(old));
                        detail = Validation.text(detail, 500);
                        Database.update(c, "UPDATE seller_orders SET failure_reason=?,failed_at=SYSUTCDATETIME() WHERE id=?", detail, sid);
                        next = "FAILED";
                        break;
                    default:
                        throw new IllegalArgumentException("Thao tác không hợp lệ.");
                }
                Database.update(c, "UPDATE seller_orders SET status=?,rejection_reason=CASE WHEN ? IN ('REJECTED','CANCELLED') THEN ? ELSE rejection_reason END,updated_at=SYSUTCDATETIME() WHERE id=?", next, next, detail, sid);
                if (Set.of("REJECTED", "CANCELLED").contains(next)) {
                    new StockReservationService().release(c, sid);
                    BenefitsService.returnSeller(c, o, so);
                    Transaction paid = Database.one(Transaction.class, c, "SELECT TOP 1 id FROM transactions WHERE order_id=? AND type='PAYMENT' AND status='PAID' ORDER BY id", o.getId());
                    if (paid != null) {
                        FinanceService.requestAutomatic(c, o, so, FulfillmentService.id(paid.getId()), detail);
                    }
                    // Amounts remain immutable snapshots. Uncollected COD is replaced by a revised payable attempt.
                    if ("COD".equals(o.getPaymentMethod())) {
                        Database.update(c, "UPDATE transactions SET status='EXPIRED',updated_at=SYSUTCDATETIME() WHERE order_id=? AND type='COD' AND status='PENDING'", o.getId());
                        BigDecimal remaining = money(Database.one(MoneyTotal.class, c, "SELECT COALESCE(SUM(subtotal-discount_amount-points_discount+delivery_fee),0) AS total FROM seller_orders WHERE order_id=? AND status NOT IN ('CANCELLED','REJECTED','DELIVERED')", o.getId()).getTotal());
                        if (remaining.signum() > 0) {
                            Database.update(c, "INSERT transactions(type,user_id,order_id,amount,status,idempotency_key,method) VALUES('COD',?,?,?,'PENDING',?,'COD')", o.getCustomerId(), o.getId(), remaining, "cod-revision:" + sid);
                        }
                    }
                    if (Database.one(SellerOrder.class, c, "SELECT id FROM seller_orders WHERE order_id=? AND status NOT IN ('CANCELLED','REJECTED')", o.getId()) == null) {
                        Database.update(c, "UPDATE orders SET status='CANCELLED',updated_at=SYSUTCDATETIME() WHERE id=?", o.getId());
                        BenefitsService.returnAll(c, o);
                    }
                }
                Database.update(c, "INSERT audit_logs(actor_id,entity_type,entity_id,action,old_value,new_value,note) VALUES(?,'SellerOrder',?,?,?,?,?)", actor, sid, action, old, next, detail);
                Database.update(c, "INSERT notifications(user_id,event_type,title,content,link_url,dedup_key) VALUES(?,'ORDER_UPDATE',N'Đơn hàng đã cập nhật',?,'/orders',?)", o.getCustomerId(), "Đơn hàng cửa hàng " + sid + ": " + util.Vietnamese.label(next), "seller-order:" + sid + ":" + next);
                if (next.equals("DELIVERED")) {
                    LoyaltyService.earn(c, FulfillmentService.id(o.getCustomerId()), sid, FulfillmentService.money(so.getSubtotal()).subtract(FulfillmentService.money(so.getDiscountAmount())).subtract(FulfillmentService.money(so.getPointsDiscount())));
                    if ("COD".equals(o.getPaymentMethod())) {
                        Database.update(c, "INSERT transactions(type,user_id,order_id,seller_order_id,method,amount,status,reconciled,idempotency_key) VALUES('COD',?,?,?,'COD',?,'PAID',1,?)", o.getCustomerId(), o.getId(), sid, money(so.getSubtotal()).subtract(money(so.getDiscountAmount())).subtract(money(so.getPointsDiscount())).add(money(so.getDeliveryFee())), "cod-collected:" + sid);
                    }
                    if (Database.one(SellerOrder.class, c, "SELECT id FROM seller_orders WHERE order_id=? AND status NOT IN ('DELIVERED','CANCELLED','REJECTED')", o.getId()) == null) {
                        Database.update(c, "UPDATE orders SET status='COMPLETED',updated_at=SYSUTCDATETIME() WHERE id=?", o.getId());
                        Database.update(c, "UPDATE transactions SET status='COMPLETED' WHERE order_id=? AND type='COD' AND seller_order_id IS NULL AND status='PENDING'", o.getId());
                    }
                }
                if (Database.one(SellerOrder.class, c, "SELECT id FROM seller_orders WHERE order_id=? AND status NOT IN ('DELIVERED','CANCELLED','REJECTED')", o.getId()) == null && Database.one(SellerOrder.class, c, "SELECT id FROM seller_orders WHERE order_id=? AND status='DELIVERED'", o.getId()) != null) {
                    Database.update(c, "UPDATE orders SET status='COMPLETED',updated_at=SYSUTCDATETIME() WHERE id=?", o.getId());
                    Database.update(c, "UPDATE transactions SET status='COMPLETED' WHERE order_id=? AND type='COD' AND seller_order_id IS NULL AND status='PENDING'", o.getId());
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    static void require(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException("Trạng thái đơn hiện tại không cho phép thao tác này.");
        }
    }

    public static String evidence(String url) {
        url = Validation.text(url, 500);
        if (!url.startsWith("https://") && !url.startsWith("/media/")) {
            throw new IllegalArgumentException("Cung cấp đường dẫn bằng chứng HTTPS hoặc đường dẫn tệp đã tải lên.");
        }
        return url;
    }

    private void consume(Connection c, long sid) throws Exception {
        for (OrderItem line : Database.rows(OrderItem.class, c, "SELECT * FROM order_items WITH(UPDLOCK,HOLDLOCK) WHERE seller_order_id=? ORDER BY batch_id,id", sid)) {
            if (!"RESERVED".equals(line.getReservationStatus())) {
                throw new IllegalArgumentException("Đơn hàng chưa được giữ tồn kho.");
            }
            if (Database.update(c, "UPDATE batches SET remaining_qty=remaining_qty-?,reserved_qty=reserved_qty-? WHERE id=? AND reserved_qty>=? AND remaining_qty>=? AND status='AVAILABLE' AND expiry_date>CONVERT(date,SYSUTCDATETIME())", line.getQuantity(), line.getQuantity(), line.getBatchId(), line.getQuantity(), line.getQuantity()) != 1) {
                throw new IllegalArgumentException("Không thể giao hàng từ lô đã phân bổ. Vui lòng liên hệ quản trị viên.");
            }
            Database.update(c, "UPDATE order_items SET reservation_status='CONSUMED' WHERE id=?", line.getId());
            Database.update(c, "INSERT stock_movements(batch_id,order_item_id,movement_type,quantity,idempotency_key) VALUES(?,?,'CONSUME',?,?)", line.getBatchId(), line.getId(), line.getQuantity(), "consume:" + line.getId());
        }
    }
}
