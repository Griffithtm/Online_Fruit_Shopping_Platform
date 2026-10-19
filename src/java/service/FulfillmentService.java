package service;

import model.*;

import config.DBConnect;
import dao.AuditDAO;
import dao.Database;
import dao.NotificationDAO;
import dao.OrderDAO;
import java.sql.*;
import java.math.*;
import java.util.*;
import util.Messages;
import util.OrderCodes;

/**
 * Every order mutation locks the parent order before seller orders and batches.
 */
public class FulfillmentService {

    // ThanhNC 2.2 (BA-15, BA-44, BA-45)
    private final OrderDAO orders = new OrderDAO();
    private final AuditDAO audit = new AuditDAO();
    private final NotificationDAO notifications = new NotificationDAO();

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
            if (role.equals("SELLER")) {
                return orders.sellerAll(c, uid); // ThanhNC BA-34: no recipient phone or full address for Sellers
            }
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
            // ThanhNC BA-51: couriers do not receive settlement, commission or seller net amounts.
            String columns = role.equals("DELIVERY_STAFF")
                    ? "so.id,so.order_id,so.store_id,so.status,so.subtotal,so.discount_amount,so.points_discount,so.points_used,so.delivery_fee,so.cod_amount,so.cold_chain_note,so.delivery_staff_id,so.assigned_at,so.picked_up_at,so.in_transit_at,so.arriving_at,so.delivered_at,so.failed_at,so.failure_reason,so.rejection_reason,so.evidence_url,so.evidence_required,so.cod_collected_at,so.packing_note,so.packing_confirmed_at"
                    : "so.*";
            return Database.rows(SellerOrderDetails.class, c, "SELECT " + columns + ",o.order_code,o.payment_method,o.recipient_name,o.recipient_phone,o.delivery_address,o.status AS order_status,s.store_name,a.address_line AS pickup_address,a.city AS pickup_city FROM seller_orders so JOIN orders o ON o.id=so.order_id JOIN stores s ON s.id=so.store_id LEFT JOIN addresses a ON a.id=s.pickup_address_id WHERE o.status<>'CART' AND " + condition + " ORDER BY so.id DESC", uid);
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

    /** ThanhNC BA-33: one page of the Seller's orders for a tab (see OrderDAO.SELLER_TABS; unknown tab = all). */
    public List<SellerOrderDetails> sellerPage(long uid, String tab, int page, int size) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            role(c, uid, "SELLER");
            return orders.sellerPage(c, uid, tab, page, size);
        }
    }

    public long sellerCount(long uid, String tab) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            role(c, uid, "SELLER");
            return orders.sellerCount(c, uid, tab);
        }
    }

    /** ThanhNC BA-33: allocations of the seller orders shown on the current page only. */
    public List<OrderItemDetails> sellerAllocations(long uid, List<Long> sids) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            role(c, uid, "SELLER");
            return orders.sellerAllocations(c, uid, sids);
        }
    }

    /** ThanhNC BA-31: "ready" attempts blocked during the last 24 hours, for the Admin fulfilment screen. */
    public List<AuditLog> readyBlockedLastDay(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            role(c, uid, "ADMIN");
            return orders.readyBlockedLastDay(c);
        }
    }

    /**
     * ThanhNC BA-32: "packed and ready" from the Seller form. Packing must be confirmed; the storage note is optional (≤ 255).
     * Internal callers of change(..., "ready", ...) keep the earlier behaviour without the packing form.
     */
    public void ready(long actor, long sid, boolean packed, String packingNote) throws Exception {
        apply(actor, "SELLER", sid, "ready", null, null, null, null, packed, packingNote);
    }

    public void change(long actor, String role, long sid, String action, String detail, Long courier, boolean collected) throws Exception {
        change(actor, role, sid, action, detail, courier, collected, null);
    }

    /**
     * ThanhNC 2.6a: {@code codAmount} is the amount the courier collected (BA-38); for "deliver", {@code detail} is the
     * stored evidence path from DeliveryServlet or blank (BA-35). Other actions ignore {@code codAmount}.
     */
    public void change(long actor, String role, long sid, String action, String detail, Long courier, boolean collected, BigDecimal codAmount) throws Exception {
        apply(actor, role, sid, action, detail, courier, codAmount, null, null, null);
    }

    /**
     * ThanhNC BA-26: customer cancellation of one SellerOrder. {@code expectedStatus} is the status the page showed;
     * if the order changed since then the customer gets MSG07, otherwise an ineligible cancellation gets MSG46.
     */
    public String cancelByCustomer(long actor, String role, long sid, String expectedStatus) throws Exception {
        apply(actor, role, sid, "cancel", null, null, null, expectedStatus, null, null);
        try (Connection c = new DBConnect().getConnection()) {
            SellerOrderDetails so = orders.sellerOrderHeader(c, sid);
            return OrderCodes.sellerOrderCode(so.getOrderCode(), so.getStoreId()); // for MSG48
        }
    }

    private void apply(long actor, String role, long sid, String action, String detail, Long courier, BigDecimal codAmount, String expectedStatus, Boolean packed, String packingNote) throws Exception {
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
                // ThanhNC BA-41: a repeated confirmation by the assigned courier returns the existing outcome and writes nothing.
                // Checked before the order-status rule because the order may already be Completed.
                if (action.equals("deliver") && role.equals("DELIVERY_STAFF") && "DELIVERED".equals(old)) {
                    c.commit();
                    return;
                }
                // ThanhNC BA-26: a customer cancellation reports MSG07 when the page was stale, MSG46 when not eligible.
                if (customer && action.equals("cancel")) {
                    if (expectedStatus != null && !expectedStatus.equals(old)) {
                        throw Messages.error(Messages.MSG07);
                    }
                    if (!"CONFIRMED".equals(o.getStatus()) || !Set.of("NEW", "ACCEPTED").contains(old)) {
                        throw Messages.error(Messages.MSG46);
                    }
                }
                // ThanhNC BA-30: a suspended store cannot accept or hand over orders (reject stays allowed to release stock and refund).
                if (role.equals("SELLER") && !customer && Set.of("accept", "ready").contains(action) && !orders.storeActive(c, so.getStoreId())) {
                    throw Messages.error(Messages.MSG12);
                }
                if (!"CONFIRMED".equals(o.getStatus())) {
                    throw new IllegalArgumentException("Đơn hàng chưa được xác nhận để xử lý.");
                }
                switch (action) {
                    case "accept":
                        require(role.equals("SELLER") && old.equals("NEW"));
                        next = "ACCEPTED";
                        break;
                    case "reject":
                        // ThanhNC BA-29: a Seller may reject only a New seller order, with a reason (MSG31).
                        require(role.equals("SELLER") && old.equals("NEW"));
                        if (detail == null || detail.isBlank()) {
                            throw Messages.error(Messages.MSG31);
                        }
                        detail = Validation.field(detail, 500, "lý do từ chối", true);
                        next = "REJECTED";
                        break;
                    case "cancel":
                        require(customer && Set.of("NEW", "ACCEPTED").contains(old));
                        // ThanhNC BA-15: compare with the deadline snapshotted at placement (old orders: placed_at + current window).
                        if (!orders.withinCancelDeadline(c, o.getId(), policy(c, "cancellation_window_minutes", 30))) {
                            throw Messages.error(Messages.MSG46); // ThanhNC BA-26
                        }
                        detail = "Khách hàng hủy đơn";
                        next = "CANCELLED";
                        break;
                    case "ready":
                        require(role.equals("SELLER") && old.equals("ACCEPTED"));
                        // ThanhNC BA-31: the blocked batches are recorded and Admins notified after this transaction rolls back.
                        List<OrderItemDetails> blocked = orders.blockedAllocations(c, sid);
                        if (!blocked.isEmpty()) {
                            throw new ReadyBlocked(sid, OrderCodes.sellerOrderCode(o.getOrderCode(), so.getStoreId()), describe(blocked));
                        }
                        if (Database.one(Store.class, c, "SELECT id FROM stores WHERE id=? AND pickup_address_id IS NOT NULL", so.getStoreId()) == null) {
                            throw new IllegalArgumentException("Vui lòng đặt địa chỉ lấy hàng của cửa hàng trước khi bàn giao.");
                        }
                        // ThanhNC BA-32: from the Seller form, packing must be confirmed; the storage note is optional.
                        if (packed != null) {
                            if (!packed) {
                                throw Messages.error(Messages.MSG02, "xác nhận đóng gói");
                            }
                            String note = packingNote;
                            orders.markPacked(c, sid, Validation.field(note, 255, "ghi chú bảo quản", false));
                        }
                        // ThanhNC BA-39: evidence is required for COD or when the seller order value is above the policy threshold.
                        BigDecimal value = money(so.getSubtotal()).subtract(money(so.getDiscountAmount())).subtract(money(so.getPointsDiscount()));
                        orders.setEvidenceRequired(c, sid, "COD".equals(o.getPaymentMethod()) || value.compareTo(BigDecimal.valueOf(policy(c, "evidence_threshold_vnd", 500000))) > 0);
                        next = "READY";
                        break;
                    case "assign":
                        require(role.equals("ADMIN") && old.equals("READY") && courier != null);
                        role(c, courier, "DELIVERY_STAFF");
                        // ThanhNC BA-39: evidence_required is no longer forced to 1 here; it is decided at "ready".
                        Database.update(c, "UPDATE seller_orders SET delivery_staff_id=?,assigned_at=SYSUTCDATETIME(),cold_chain_note=? WHERE id=?", courier, Validation.text(detail, 255), sid);
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
                        orders.markInTransit(c, sid); // ThanhNC BA-40
                        break;
                    case "arrive":
                        require(role.equals("DELIVERY_STAFF") && old.equals("IN_TRANSIT"));
                        next = "ARRIVING";
                        orders.markArriving(c, sid); // ThanhNC BA-40
                        break;
                    case "deliver":
                        require(role.equals("DELIVERY_STAFF") && old.equals("ARRIVING"));
                        // ThanhNC BA-35/BA-39: an uploaded photo is mandatory only when evidence_required; typed URLs are not accepted.
                        detail = detail == null || detail.isBlank() ? null : deliveryEvidence(detail);
                        if (Boolean.TRUE.equals(so.getEvidenceRequired()) && detail == null) {
                            throw Messages.error(Messages.MSG60);
                        }
                        // ThanhNC BA-38: COD needs the exact amount due.
                        boolean cod = "COD".equals(o.getPaymentMethod());
                        if (cod && (codAmount == null || codAmount.compareTo(money(so.getCodAmount())) != 0)) {
                            throw Messages.error(Messages.MSG60);
                        }
                        consume(c, sid);
                        Database.update(c, "UPDATE seller_orders SET evidence_url=?,delivered_at=SYSUTCDATETIME(),commission=ROUND(subtotal*COALESCE(commission_rate,0),2) WHERE id=?", detail, sid);
                        if (cod) {
                            orders.markCodCollected(c, sid);
                        }
                        next = "DELIVERED";
                        break;
                    case "fail":
                        require(role.equals("DELIVERY_STAFF") && Set.of("ASSIGNED", "PICKED_UP", "IN_TRANSIT", "ARRIVING").contains(old));
                        // ThanhNC BA-37: a reason is required (MSG62).
                        if (detail == null || detail.isBlank()) {
                            throw Messages.error(Messages.MSG62);
                        }
                        detail = Validation.field(detail, 500, "lý do giao thất bại", true);
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
                    // ThanhNC BA-21: this store's campaign is returned now; the marketplace voucher stays with the order.
                    BenefitsService.returnCampaign(c, o.getId(), so.getStoreId());
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
                        // ThanhNC BA-44: the parent order changes status too.
                        audit.log(c, actor, "Order", o.getId(), "ORDER_CANCELLED", o.getStatus(), "CANCELLED", detail);
                        BenefitsService.returnAll(c, o);
                    }
                }
                // ThanhNC BA-45: the audit id makes the dedup key unique even when a seller order returns to an earlier status.
                long auditId = audit.log(c, actor, "SellerOrder", sid, action, old, next, detail);
                notifyChange(c, o, so, next, detail, auditId, customer);
                if (next.equals("DELIVERED")) {
                    LoyaltyService.earn(c, FulfillmentService.id(o.getCustomerId()), sid, FulfillmentService.money(so.getSubtotal()).subtract(FulfillmentService.money(so.getDiscountAmount())).subtract(FulfillmentService.money(so.getPointsDiscount())));
                    if ("COD".equals(o.getPaymentMethod())) {
                        // ThanhNC BA-38: the ledger records the snapshotted amount due (the amount the courier confirmed).
                        Database.update(c, "INSERT transactions(type,user_id,order_id,seller_order_id,method,amount,status,reconciled,idempotency_key) VALUES('COD',?,?,?,'COD',?,'PAID',1,?)", o.getCustomerId(), o.getId(), sid, money(so.getCodAmount()), "cod-collected:" + sid);
                    }
                }
                // ThanhNC BA-44: the two completion checks that were here had the same rule (every seller order final, at least one Delivered);
                // one check now completes the order once and audits it.
                if (orders.completeIfDone(c, o.getId())) {
                    audit.log(c, actor, "Order", o.getId(), "ORDER_COMPLETED", "CONFIRMED", "COMPLETED", null);
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                if (e instanceof ReadyBlocked blocked) {
                    recordReadyBlocked(actor, blocked); // ThanhNC BA-31: own transaction, after the rollback
                }
                throw e;
            }
        }
    }

    /** ThanhNC BA-31: "ready" refused because allocated batches are expired or unavailable; the Seller sees MSG85. */
    private static final class ReadyBlocked extends IllegalArgumentException {
        final long sid;
        final String code, batches;

        ReadyBlocked(long sid, String code, String batches) {
            super(Messages.get(Messages.MSG85, code));
            this.sid = sid;
            this.code = code;
            this.batches = batches;
        }
    }

    /** Batch list for the audit note and MSG84, e.g. "L001 (HSD 19/10/2026), chưa giữ hàng". */
    private static String describe(List<OrderItemDetails> blocked) {
        StringJoiner out = new StringJoiner(", ");
        for (OrderItemDetails i : blocked) {
            if (i.getBatchNo() == null) {
                out.add("chưa giữ hàng");
            } else {
                out.add(i.getBatchNo() + (i.getExpiryDate() == null ? "" : " (HSD " + new java.text.SimpleDateFormat("dd/MM/yyyy").format(i.getExpiryDate()) + ")"));
            }
        }
        return out.toString();
    }

    /**
     * ThanhNC BA-31: audit every blocked attempt and notify each active Admin at most once per seller order per day (UTC).
     * A failure here is only logged, so the Seller still gets MSG85.
     */
    private void recordReadyBlocked(long actor, ReadyBlocked blocked) {
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                audit.log(c, actor, "SellerOrder", blocked.sid, "READY_BLOCKED", "ACCEPTED", "ACCEPTED", blocked.code + ": " + blocked.batches);
                String day = java.time.LocalDate.now(java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                String content = Messages.get(Messages.MSG84, blocked.code, blocked.batches);
                for (User admin : notifications.activeAdmins(c)) {
                    notifications.send(c, admin.getId(), "READY_BLOCKED", "Không thể chuyển sang sẵn sàng giao", content, "/admin/orders",
                            "ready-blocked:" + blocked.sid + ":" + day + ":admin:" + admin.getId());
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(FulfillmentService.class.getName()).log(java.util.logging.Level.WARNING, "Could not record a blocked ready attempt", e);
        }
    }

    /**
     * ThanhNC BA-45: status messages by event (MSG47/48/49/61/63) with the seller order code. The customer is always told;
     * the Seller hears about a customer cancellation and every active Admin about a failed delivery.
     */
    private void notifyChange(Connection c, Order o, SellerOrderDetails so, String next, String reason, long auditId, boolean byCustomer) throws Exception {
        String code = OrderCodes.sellerOrderCode(o.getOrderCode(), so.getStoreId());
        String content = switch (next) {
            case "REJECTED" -> Messages.get(Messages.MSG47, code, reason);
            case "CANCELLED" -> Messages.get(Messages.MSG48, code);
            case "DELIVERED" -> Messages.get(Messages.MSG61, code);
            case "FAILED" -> Messages.get(Messages.MSG63, code, reason);
            default -> Messages.get(Messages.MSG49, code, util.Vietnamese.label(next));
        };
        String key = "seller-order:" + so.getId() + ":" + next + ":" + auditId;
        notifications.send(c, o.getCustomerId(), "ORDER_UPDATE", "Cập nhật đơn hàng", content, "/orders", key);
        if ("CANCELLED".equals(next) && byCustomer) {
            notifications.send(c, so.getOwnerId(), "SELLER_ORDER_CANCELLED", "Đơn hàng bị hủy", content, "/seller/orders", key + ":seller");
        }
        if ("FAILED".equals(next)) {
            for (User admin : notifications.activeAdmins(c)) {
                notifications.send(c, admin.getId(), "DELIVERY_FAILED", "Giao hàng thất bại", content, "/admin/orders", key + ":admin:" + admin.getId());
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

    /** ThanhNC BA-35: only a photo stored by DeliveryServlet is accepted as delivery evidence (MSG60 otherwise). */
    public static String deliveryEvidence(String path) {
        if (!util.EvidenceImages.validPath(path)) {
            throw Messages.error(Messages.MSG60);
        }
        return path;
    }

    private void consume(Connection c, long sid) throws Exception {
        for (OrderItem line : Database.rows(OrderItem.class, c, "SELECT * FROM order_items WITH(UPDLOCK,HOLDLOCK) WHERE seller_order_id=? ORDER BY batch_id,id", sid)) {
            if (!"RESERVED".equals(line.getReservationStatus())) {
                throw new IllegalArgumentException("Đơn hàng chưa được giữ tồn kho.");
            }
            // ThanhNC BA-48: goods already handed over are consumed even if the batch expired meanwhile
            // (no expiry-date check; status may be AVAILABLE or EXPIRED, any other status is still refused).
            if (Database.update(c, "UPDATE batches SET remaining_qty=remaining_qty-?,reserved_qty=reserved_qty-? WHERE id=? AND reserved_qty>=? AND remaining_qty>=? AND status IN ('AVAILABLE','EXPIRED')", line.getQuantity(), line.getQuantity(), line.getBatchId(), line.getQuantity(), line.getQuantity()) != 1) {
                throw new IllegalArgumentException("Không thể giao hàng từ lô đã phân bổ. Vui lòng liên hệ quản trị viên.");
            }
            Database.update(c, "UPDATE order_items SET reservation_status='CONSUMED' WHERE id=?", line.getId());
            Database.update(c, "INSERT stock_movements(batch_id,order_item_id,movement_type,quantity,idempotency_key) VALUES(?,?,'CONSUME',?,?)", line.getBatchId(), line.getId(), line.getQuantity(), "consume:" + line.getId());
        }
    }
}
