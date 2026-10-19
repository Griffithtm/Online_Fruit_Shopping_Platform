package service;

import model.*;

import config.DBConnect;
import dao.AddressDAO;
import dao.AuditDAO;
import dao.CheckoutDAO;
import dao.NotificationDAO;
import dao.OrderDAO;
import dao.Database;
import dao.SettingDAO;
import util.Messages;
import util.OrderCodes;
import java.sql.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Checkout. All totals and stock are verified on the server.
 */
public class CheckoutService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final CheckoutDAO dao = new CheckoutDAO();
    private final AddressDAO addresses = new AddressDAO();
    private final SettingDAO settings = new SettingDAO();
    private final OrderDAO orders = new OrderDAO();
    private final AuditDAO audit = new AuditDAO();
    private final NotificationDAO notifications = new NotificationDAO();

    public CheckoutQuote quote(long uid, long addressId) throws Exception {
        return quote(uid, addressId, "", 0, "COD");
    }

    /** Read-only quote for GET /checkout (BA-10): no points expiry, no commit. */
    public CheckoutQuote quote(long uid, long addressId, String voucher, int points, String method) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return quote(c, uid, addressId, voucher, points, method);
        }
    }

    /** Lower case, trimmed, single spaces (BA-16). Accents are kept, so "Ha Noi" is not "Hà Nội". */
    static String normalizeArea(Object name) {
        return name == null ? "" : name.toString().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private ServiceArea serviceArea(Connection c, String city) throws Exception {
        String wanted = normalizeArea(city);
        for (ServiceArea area : settings.activeServiceAreaRows(c)) {
            if (!wanted.isEmpty() && normalizeArea(area.getName()).equals(wanted)) {
                return area;
            }
        }
        return null;
    }

    /** MSG37 reason from a promotion error: "Ngân sách khuyến mãi đã hết." becomes "ngân sách khuyến mãi đã hết". */
    private static String reason(Exception e) {
        String m = e.getMessage() == null ? "" : e.getMessage().trim();
        if (m.endsWith(".")) {
            m = m.substring(0, m.length() - 1);
        }
        return m.isEmpty() ? m : Character.toLowerCase(m.charAt(0)) + m.substring(1);
    }

    private CheckoutQuote quote(Connection c, long uid, long addressId, String voucher, int points, String method) throws Exception {
        if (method == null || !Set.of("COD", "CARD", "E_WALLET").contains(method)) {
            throw Messages.error(Messages.MSG04, "phương thức thanh toán");
        }
        if (!method.equals("COD")) {
            new VnpayGateway().requireConfigured();
        }
        if (points < 0) {
            throw Messages.error(Messages.MSG04, "điểm thưởng");
        }
        Address address = addresses.findById(c, addressId, uid);
        if (address == null) {
            throw Messages.error(Messages.MSG12);
        }
        ServiceArea area = serviceArea(c, address.getCity());
        if (area == null || area.getFee() == null || area.getFee().signum() < 0) {
            throw Messages.error(Messages.MSG36);
        }
        // BA-11: the area may define its own eta_hours; otherwise the delivery_eta_hours policy applies.
        int etaHours = area.getEtaHours() != null && area.getEtaHours() > 0 ? area.getEtaHours() : FulfillmentService.policy(c, "delivery_eta_hours", 24);
        Order cart = dao.findCart(c, uid);
        if (cart == null) {
            throw Messages.error(Messages.MSG35);
        }
        List<CheckoutLine> lines = dao.cartLines(c, cart.getId());
        if (lines.isEmpty()) {
            throw Messages.error(Messages.MSG35);
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        StringBuilder fingerprint = new StringBuilder(cart.getId().toString());
        for (CheckoutLine line : lines) {
            // BA-09: a Seller may buy, but not from their own store.
            if (line.getOwnerId() != null && line.getOwnerId() == uid) {
                throw Messages.error(Messages.MSG12);
            }
            if (line.getPrice() == null) {
                throw Messages.error(Messages.MSG32);
            }
            if (line.getPrice().compareTo(line.getUnitPrice()) != 0) {
                throw Messages.error(Messages.MSG34);
            }
            // BA-13: only stock from batches that are still fresh on the estimated delivery date counts.
            long available = dao.availableUntilDelivery(c, line.getVariantId(), etaHours);
            if (available < line.getQuantity().longValue()) {
                throw Messages.error(Messages.MSG33, available, line.getUnit(), line.getOptionName());
            }
            subtotal = subtotal.add(line.getPrice().multiply(new BigDecimal(line.getQuantity().toString())));
            fingerprint.append('|').append(line.getId()).append(':').append(line.getQuantity()).append(':').append(line.getPrice());
        }
        Map<Long, BigDecimal> discounts = new TreeMap<>();
        Map<Long, CheckoutSellerTotal> groups = new LinkedHashMap<>();
        Map<Long, BigDecimal> campaignBases = new TreeMap<>();
        Map<Long, Long> campaignSeller = new HashMap<>();
        for (CheckoutLine line : lines) {
            long sid = line.getSellerOrderId();
            CheckoutSellerTotal g = groups.computeIfAbsent(sid, k -> {
                CheckoutSellerTotal m = new CheckoutSellerTotal();
                m.setSubtotal(BigDecimal.ZERO);
                m.setDiscount(BigDecimal.ZERO);
                m.setStoreName(line.getStoreName());
                return m;
            });
            BigDecimal amount = line.getPrice().multiply(new BigDecimal(line.getQuantity().toString()));
            g.setSubtotal(g.getSubtotal().add(amount));
            if (line.getCampaignId() != null) {
                long pid = line.getCampaignId();
                campaignBases.merge(pid, amount, BigDecimal::add);
                campaignSeller.put(pid, sid);
            }
        }
        for (var entry : campaignBases.entrySet()) {
            Promotion promo = PromotionService.active(c, entry.getKey());
            if (promo != null) {
                BigDecimal d;
                try {
                    d = PromotionService.discount(c, uid, promo, entry.getValue());
                } catch (IllegalArgumentException ineligible) {
                    continue;
                }
                if (d.signum() == 0) {
                    continue;
                }
                discounts.put(entry.getKey(), d);
                CheckoutSellerTotal g = groups.get(campaignSeller.get(entry.getKey()));
                g.setDiscount(g.getDiscount().add(d));
            }
        }
        BigDecimal campaignTotal = discounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add), voucherDiscount = BigDecimal.ZERO;
        Long voucherId = null;
        if (voucher != null && !voucher.isBlank()) {
            Promotion ref = Database.one(Promotion.class, c, "SELECT id FROM promotions WHERE type='VOUCHER' AND code=?", voucher.trim().toUpperCase(Locale.ROOT));
            Promotion promo = ref == null ? null : PromotionService.active(c, ref.getId());
            if (promo == null) {
                throw Messages.error(Messages.MSG37, "mã không hợp lệ hoặc ngoài thời gian áp dụng");
            }
            voucherId = promo.getId();
            try {
                voucherDiscount = PromotionService.discount(c, uid, promo, subtotal.subtract(campaignTotal));
            } catch (IllegalArgumentException ineligible) {
                throw Messages.error(Messages.MSG37, reason(ineligible));
            }
            discounts.put(voucherId, voucherDiscount);
        }
        BigDecimal eligible = subtotal.subtract(campaignTotal).subtract(voucherDiscount);
        int rate = FulfillmentService.policy(c, "points_vnd_per_point", 1000);
        BigDecimal rateValue = new BigDecimal(rate);
        // Voucher share per seller order first; what is left is where points and their capacity come from.
        BigDecimal voucherLeft = voucherDiscount;
        int index = 0;
        long capacity = 0;
        Map<Long, BigDecimal> afterVoucher = new LinkedHashMap<>();
        for (var entry : groups.entrySet()) {
            CheckoutSellerTotal g = entry.getValue();
            boolean last = ++index == groups.size();
            BigDecimal after = g.getSubtotal().subtract(g.getDiscount());
            BigDecimal vd = last ? voucherLeft : (subtotal.subtract(campaignTotal).signum() == 0 ? BigDecimal.ZERO : voucherDiscount.multiply(after).divide(subtotal.subtract(campaignTotal), 0, RoundingMode.DOWN));
            voucherLeft = voucherLeft.subtract(vd);
            g.setDiscount(g.getDiscount().add(vd));
            BigDecimal av = after.subtract(vd);
            afterVoucher.put(entry.getKey(), av);
            capacity += av.divide(rateValue, 0, RoundingMode.DOWN).longValueExact();
        }
        // BA-12: maximum = balance, policy share of the discounted item total, and what fits into the seller orders.
        long usable = dao.usablePoints(c, uid);
        int percent = Math.min(100, FulfillmentService.policy(c, "points_max_percent", 50));
        long policyMax = eligible.multiply(new BigDecimal(percent)).divide(new BigDecimal(100L * rate), 0, RoundingMode.DOWN).longValueExact();
        long maxPoints = Math.max(0, Math.min(usable, Math.min(policyMax, capacity)));
        if (points > maxPoints) {
            throw Messages.error(Messages.MSG38, maxPoints);
        }
        // Points are split in proportion to each group's amount, then any remainder goes to groups that still have room.
        Map<Long, Integer> pointShare = new LinkedHashMap<>();
        int given = 0;
        for (var entry : afterVoucher.entrySet()) {
            int room = entry.getValue().divide(rateValue, 0, RoundingMode.DOWN).intValueExact();
            int share = eligible.signum() == 0 ? 0 : new BigDecimal(points).multiply(entry.getValue()).divide(eligible, 0, RoundingMode.DOWN).intValue();
            share = Math.min(share, room);
            pointShare.put(entry.getKey(), share);
            given += share;
        }
        for (var entry : afterVoucher.entrySet()) {
            if (given == points) {
                break;
            }
            int room = entry.getValue().divide(rateValue, 0, RoundingMode.DOWN).intValueExact() - pointShare.get(entry.getKey());
            int extra = Math.min(room, points - given);
            pointShare.merge(entry.getKey(), extra, Integer::sum);
            given += extra;
        }
        // BA-11: every seller order is a separate delivery and pays the area fee.
        BigDecimal areaFee = area.getFee();
        for (var entry : groups.entrySet()) {
            CheckoutSellerTotal g = entry.getValue();
            int gp = pointShare.get(entry.getKey());
            g.setPoints(gp);
            g.setPointsDiscount(new BigDecimal(gp).multiply(rateValue));
            g.setFee(areaFee);
        }
        BigDecimal fee = areaFee.multiply(new BigDecimal(groups.size()));
        BigDecimal pointDiscount = new BigDecimal(points).multiply(rateValue);
        CheckoutQuote q = new CheckoutQuote();
        q.setCartId(cart.getId());
        q.setAddress(address);
        q.setLines(lines);
        q.setSubtotal(subtotal);
        q.setFee(fee);
        q.setAreaFee(areaFee);
        q.setDiscount(campaignTotal.add(voucherDiscount));
        q.setPointsDiscount(pointDiscount);
        q.setPoints(points);
        q.setMaxPoints(maxPoints);
        q.setUsablePoints(usable);
        q.setVoucherId(voucherId);
        q.setDiscounts(discounts);
        q.setGroups(groups);
        q.setMethod(method);
        q.setTotal(eligible.subtract(pointDiscount).add(fee));
        q.setEtaHours(etaHours);
        q.setFingerprint(fingerprint + "|" + address + "|" + fee + "|" + discounts + "|" + points + "|" + method + "|" + etaHours);
        return q;
    }

    public long place(long uid, long addressId, String key, String expected) throws Exception {
        return place(uid, addressId, key, expected, "", 0, "COD");
    }

    public long place(long uid, long addressId, String key, String expected, String voucher, int points, String method) throws Exception {
        UUID.fromString(key);
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                Database.one(User.class, c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status='ACTIVE'", uid);
                Order previous = Database.one(Order.class, c, "SELECT id FROM orders WHERE customer_id=? AND checkout_key=CONVERT(uniqueidentifier,?)", uid, key);
                if (previous != null) {
                    c.commit();
                    return previous.getId();
                }
                LoyaltyService.expire(c, uid);
                CheckoutQuote q = quote(c, uid, addressId, voucher, points, method);
                if (!Objects.equals(expected, q.getFingerprint())) {
                    throw Messages.error(Messages.MSG34);
                }
                long oid = q.getCartId();
                Address a = q.getAddress();
                String delivery = a.getAddressLine() + ", " + a.getWard() + ", " + a.getDistrict() + ", " + a.getCity();
                if (delivery.length() > 500) {
                    throw Messages.error(Messages.MSG03, "địa chỉ giao hàng", 500);
                }
                Database.update(c, "DELETE FROM seller_orders WHERE order_id=? AND NOT EXISTS(SELECT 1 FROM order_items WHERE seller_order_id=seller_orders.id)", oid);
                Database.update(c, "UPDATE orders SET status=?,order_code=?,recipient_name=?,recipient_phone=?,delivery_address=?,delivery_address_id=?,delivery_city=?,payment_method=?,placed_at=SYSUTCDATETIME(),checkout_key=CONVERT(uniqueidentifier,?),item_total=?,delivery_fee=?,discount_amount=?,points_discount=?,points_used=?,voucher_id=?,estimated_delivery_at=DATEADD(hour,?,SYSUTCDATETIME()),payment_expires_at=CASE WHEN ?='COD' THEN NULL ELSE DATEADD(minute,?,SYSUTCDATETIME()) END,payable_amount=?,updated_at=SYSUTCDATETIME() WHERE id=? AND status='CART'",
                        method.equals("COD") ? "CONFIRMED" : "AWAITING_PAYMENT", "FR" + oid, a.getRecipientName(), a.getPhone(), delivery, addressId, a.getCity(), method, key, q.getSubtotal(), q.getFee(), q.getDiscount(), q.getPointsDiscount(), points, q.getVoucherId(), q.getEtaHours(), method, FulfillmentService.policy(c, "reservation_timeout_minutes", 15), q.getTotal(), oid);
                Database.update(c, "UPDATE so SET status='NEW',subtotal=(SELECT SUM(quantity*unit_price) FROM order_items WHERE seller_order_id=so.id),updated_at=SYSUTCDATETIME() FROM seller_orders so WHERE order_id=?", oid);
                for (var entry : q.getGroups().entrySet()) {
                    CheckoutSellerTotal g = entry.getValue();
                    BigDecimal cod = method.equals("COD") ? g.getSubtotal().subtract(g.getDiscount()).subtract(g.getPointsDiscount()).add(g.getFee()) : BigDecimal.ZERO;
                    Database.update(c, "UPDATE so SET delivery_fee=?,discount_amount=?,points_discount=?,points_used=?,cod_amount=?,commission_rate=COALESCE((SELECT TRY_CONVERT(decimal(9,6),JSON_VALUE(CASE WHEN ISJSON(t.value)=1 THEN t.value ELSE '{}' END,'$.commission_rate')) FROM stores s LEFT JOIN settings t ON t.id=s.seller_tier_id WHERE s.id=so.store_id),0) FROM seller_orders so WHERE id=?",
                            g.getFee(), g.getDiscount(), g.getPointsDiscount(), g.getPoints(), cod, entry.getKey());
                }
                Database.update(c, "UPDATE oi SET product_title=l.title,variant_name=v.option_name FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id WHERE so.order_id=?", oid);
                Database.update(c, "UPDATE so SET store_name_snapshot=s.store_name FROM seller_orders so JOIN stores s ON s.id=so.store_id WHERE so.order_id=?", oid);
                // Errors of the shared services are reported with checkout messages (BA-17); the whole transaction rolls back.
                try {
                    new StockReservationService().reserveOrder(c, oid);
                } catch (IllegalArgumentException changed) {
                    throw Messages.error(Messages.MSG34);
                }
                try {
                    PromotionService.redeem(c, uid, oid, q.getDiscounts());
                } catch (IllegalArgumentException ended) {
                    throw Messages.error(Messages.MSG37, reason(ended));
                }
                try {
                    LoyaltyService.redeem(c, uid, oid, points);
                } catch (IllegalArgumentException notEnough) {
                    throw Messages.error(Messages.MSG38, q.getMaxPoints());
                }
                if (method.equals("COD")) {
                    Database.update(c, "INSERT transactions(type,user_id,order_id,method,amount,status,idempotency_key) VALUES('COD',?,?,'COD',?,'PENDING',?)", uid, oid, q.getTotal(), "cod:" + oid);
                }
                // ThanhNC BA-15: cancellation deadline = this order's placed_at + the window in force now.
                orders.setCancelDeadline(c, oid, FulfillmentService.policy(c, "cancellation_window_minutes", 30));
                // ThanhNC BA-44: placement is an order status change (Cart → Confirmed / Awaiting Payment).
                String placedStatus = method.equals("COD") ? "CONFIRMED" : "AWAITING_PAYMENT";
                audit.log(c, uid, "Order", oid, "ORDER_PLACED", "CART", placedStatus, null);
                // ThanhNC BA-14: a COD order is ready for acceptance now; online orders are announced after payment (FE-06).
                if (method.equals("COD")) {
                    for (SellerOrderDetails so : orders.sellerOrdersWithOwner(c, oid)) {
                        notifications.send(c, so.getOwnerId(), "SELLER_ORDER_NEW", "Đơn hàng mới",
                                Messages.get(Messages.MSG83, OrderCodes.sellerOrderCode("FR" + oid, so.getStoreId())), "/seller/orders", "seller-order:new:" + so.getId());
                    }
                }
                c.commit();
                return oid;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** MSG39 (COD) or MSG40 (online, deadline in Vietnam time) for an order the customer just placed; null otherwise (BA-18). */
    public String placedNotice(long uid, long oid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            Order o = dao.findPlaced(c, oid, uid);
            if (o == null) {
                return null;
            }
            if ("COD".equals(o.getPaymentMethod()) && "CONFIRMED".equals(o.getStatus())) {
                return Messages.get(Messages.MSG39, o.getOrderCode(), new DecimalFormat("#,##0").format(o.getPayableAmount()) + " đ");
            }
            if ("AWAITING_PAYMENT".equals(o.getStatus()) && o.getPaymentExpiresAt() != null) {
                return Messages.get(Messages.MSG40, o.getOrderCode(), vietnamTime(o.getPaymentExpiresAt()));
            }
            return null;
        }
    }

    /** Stored timestamps are UTC (NFR-DATA-01); customers see Vietnam time. */
    static String vietnamTime(Timestamp utc) {
        return utc.toLocalDateTime().atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    public List<Transaction> payments(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(Transaction.class, c, "SELECT id,order_id,method,amount,status,created_at FROM transactions WHERE user_id=? AND type IN ('PAYMENT','COD') ORDER BY id", uid);
        }
    }

    public List<OrderSummary> orders(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(OrderSummary.class, c, "SELECT o.*,(SELECT COALESCE(SUM(subtotal-discount_amount-points_discount+delivery_fee),0) FROM seller_orders WHERE order_id=o.id AND status NOT IN ('CANCELLED','REJECTED')) AS current_payable,(SELECT TOP 1 status FROM transactions WHERE order_id=o.id AND type IN ('PAYMENT','COD') ORDER BY id DESC) AS payment_status,CASE WHEN o.status='CONFIRMED' AND SYSUTCDATETIME()<=COALESCE(o.cancel_deadline_at,DATEADD(minute,?,o.placed_at)) THEN 1 ELSE 0 END AS cancellable FROM orders o WHERE customer_id=? AND status<>'CART' ORDER BY id DESC",
                    // ThanhNC BA-26/BA-28: "cancellable" now means the order is Confirmed and still within its cancellation deadline.
                    FulfillmentService.policy(c, "cancellation_window_minutes", 30), uid);
        }
    }

    public List<OrderItemDetails> items(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(OrderItemDetails.class, c, "SELECT so.order_id,so.id AS seller_order_id,so.rejection_reason,so.status AS seller_status,COALESCE(so.store_name_snapshot,s.store_name) AS store_name,COALESCE(oi.product_title,l.title) AS title,oi.quantity,oi.unit_price FROM orders o JOIN seller_orders so ON so.order_id=o.id JOIN stores s ON s.id=so.store_id JOIN order_items oi ON oi.seller_order_id=so.id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id WHERE o.customer_id=? AND o.status<>'CART' ORDER BY so.id,oi.id", uid);
        }
    }

    /**
     * ThanhNC BA-20 (replaces the unused cancel(), BA-28): an online order still awaiting payment is cancelled as a whole,
     * because the gateway charges the full order amount. Returns the codes of its seller orders for MSG48.
     * A payment that still arrives later is refunded by PaymentService.callback (order no longer Awaiting Payment).
     */
    public List<String> cancelAwaiting(long uid, long oid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                Order o = orders.lockOwnedOrder(c, oid, uid);
                if (o == null) {
                    throw Messages.error(Messages.MSG12);
                }
                List<SellerOrder> sellers = orders.lockSellerOrders(c, oid);
                String reason = "Khách hàng hủy đơn chờ thanh toán";
                if (orders.cancelAwaitingOrder(c, oid, reason) != 1) {
                    throw Messages.error(Messages.MSG07); // paid, expired or cancelled meanwhile
                }
                for (SellerOrder so : sellers) {
                    new StockReservationService().release(c, so.getId());
                }
                orders.expireInitiatedPayments(c, oid);
                BenefitsService.returnAll(c, o);
                audit.log(c, uid, "Order", oid, "ORDER_CANCELLED", o.getStatus(), "CANCELLED", reason);
                List<String> codes = new ArrayList<>();
                for (SellerOrder so : sellers) {
                    String code = OrderCodes.sellerOrderCode(o.getOrderCode(), so.getStoreId());
                    long auditId = audit.log(c, uid, "SellerOrder", so.getId(), "cancel", so.getStatus(), "CANCELLED", reason);
                    notifications.send(c, uid, "ORDER_UPDATE", "Cập nhật đơn hàng", Messages.get(Messages.MSG48, code), "/orders", "seller-order:" + so.getId() + ":CANCELLED:" + auditId);
                    codes.add(code);
                }
                c.commit();
                return codes;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }
}
