package service;

import model.*;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Checkout. All totals and stock are verified on the server.
 */
public class CheckoutService {

    public CheckoutQuote quote(long uid, long addressId) throws Exception {
        return quote(uid, addressId, "", 0, "COD");
    }

    public CheckoutQuote quote(long uid, long addressId, String voucher, int points, String method) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                LoyaltyService.expire(c, uid);
                CheckoutQuote q = quote(c, uid, addressId, voucher, points, method);
                c.commit();
                return q;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    private CheckoutQuote quote(Connection c, long uid, long addressId, String voucher, int points, String method) throws Exception {
        if (!Set.of("COD", "CARD", "E_WALLET").contains(method)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
        }
        if (!method.equals("COD")) {
            new VnpayGateway().requireConfigured();
        }
        if (points < 0) {
            throw new IllegalArgumentException("Số điểm thưởng không hợp lệ.");
        }
        Address address = Database.one(Address.class, c, "SELECT * FROM addresses WHERE id=? AND user_id=?", addressId, uid);
        if (address == null) {
            throw new IllegalArgumentException("Vui lòng chọn một địa chỉ giao hàng của bạn.");
        }
        ServiceArea area = Database.one(ServiceArea.class, c, "SELECT TRY_CONVERT(decimal(15,2),JSON_VALUE(CASE WHEN ISJSON(value)=1 THEN value ELSE '{}' END,'$.delivery_fee')) AS fee FROM settings WHERE type='SERVICE_AREA' AND status='ACTIVE' AND name=?", address.getCity());
        if (area == null || area.getFee() == null || (area.getFee()).signum() < 0) {
            throw new IllegalArgumentException("Địa chỉ này nằm ngoài khu vực giao hàng.");
        }
        Order cart = Database.one(Order.class, c, "SELECT id FROM orders WHERE customer_id=? AND status='CART'", uid);
        if (cart == null) {
            throw new IllegalArgumentException("Giỏ hàng đang trống.");
        }
        List<CheckoutLine> lines = Database.rows(CheckoutLine.class, c, "SELECT oi.id,oi.variant_id,oi.quantity,oi.unit_price,so.id AS seller_order_id,l.campaign_id,p.price,p.available_qty,l.title,s.store_name FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN stores s ON s.id=so.store_id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id LEFT JOIN v_public_products p ON p.variant_id=oi.variant_id WHERE so.order_id=? ORDER BY so.store_id,oi.id", cart.getId());
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng đang trống.");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        StringBuilder fingerprint = new StringBuilder(cart.getId().toString());
        for (CheckoutLine line : lines) {
            if (line.getPrice() == null || line.getAvailableQty().longValue() < line.getQuantity().longValue()) {
                throw new IllegalArgumentException("Có sản phẩm ngừng bán hoặc không đủ tồn kho. Vui lòng kiểm tra giỏ hàng.");
            }
            if ((line.getPrice()).compareTo(line.getUnitPrice()) != 0) {
                throw new IllegalArgumentException("Giá sản phẩm đã thay đổi. Vui lòng cập nhật sản phẩm tương ứng trong giỏ hàng trước.");
            }
            subtotal = subtotal.add((line.getPrice()).multiply(new BigDecimal(line.getQuantity().toString())));
            fingerprint.append('|').append(line.getId()).append(':').append(line.getQuantity()).append(':').append(line.getPrice());
        }
        Map<Long, BigDecimal> discounts = new TreeMap<>();
        Map<Long, CheckoutSellerTotal> groups = new LinkedHashMap<>();
        Map<Long, BigDecimal> campaignBases = new TreeMap<>();
        Map<Long, Long> campaignSeller = new HashMap<>();
        for (CheckoutLine line : lines) {
            long sid = FulfillmentService.id(line.getSellerOrderId());
            CheckoutSellerTotal g = groups.computeIfAbsent(sid, k -> {
                CheckoutSellerTotal m = new CheckoutSellerTotal();
                m.setSubtotal(BigDecimal.ZERO);
                m.setDiscount(BigDecimal.ZERO);
                m.setStoreName(line.getStoreName());
                return m;
            });
            BigDecimal amount = (line.getPrice()).multiply(new BigDecimal(line.getQuantity().toString()));
            g.setSubtotal((g.getSubtotal()).add(amount));
            if (line.getCampaignId() != null) {
                long pid = FulfillmentService.id(line.getCampaignId());
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
                g.setDiscount((g.getDiscount()).add(d));
            }
        }
        BigDecimal campaignTotal = discounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add), voucherDiscount = BigDecimal.ZERO;
        Long voucherId = null;
        if (voucher != null && !voucher.isBlank()) {
            Promotion ref = Database.one(Promotion.class, c, "SELECT id FROM promotions WHERE type='VOUCHER' AND code=?", voucher.trim().toUpperCase(Locale.ROOT));
            Promotion promo = ref == null ? null : PromotionService.active(c, FulfillmentService.id(ref.getId()));
            if (promo == null) {
                throw new IllegalArgumentException("Mã giảm giá không hợp lệ hoặc ngoài thời gian áp dụng.");
            }
            voucherId = FulfillmentService.id(promo.getId());
            voucherDiscount = PromotionService.discount(c, uid, promo, subtotal.subtract(campaignTotal));
            discounts.put(voucherId, voucherDiscount);
        }
        BigDecimal eligible = subtotal.subtract(campaignTotal).subtract(voucherDiscount);
        int rate = FulfillmentService.policy(c, "points_vnd_per_point", 1000);
        BigDecimal pointDiscount = new BigDecimal(points).multiply(new BigDecimal(rate));
        long available = FulfillmentService.id(Database.one(User.class, c, "SELECT points_balance FROM users WHERE id=?", uid).getPointsBalance());
        if (points > available || pointDiscount.compareTo(eligible) > 0) {
            throw new IllegalArgumentException("Điểm sử dụng vượt số dư hoặc giá trị mua hàng đủ điều kiện.");
        }
        BigDecimal fee = area.getFee(), voucherLeft = voucherDiscount, feeLeft = fee;
        int pointLeft = points, index = 0;
        for (var entry : groups.entrySet()) {
            CheckoutSellerTotal g = entry.getValue();
            boolean last = ++index == groups.size();
            BigDecimal after = (g.getSubtotal()).subtract(g.getDiscount());
            BigDecimal vd = last ? voucherLeft : (subtotal.subtract(campaignTotal).signum() == 0 ? BigDecimal.ZERO : voucherDiscount.multiply(after).divide(subtotal.subtract(campaignTotal), 0, java.math.RoundingMode.DOWN));
            voucherLeft = voucherLeft.subtract(vd);
            g.setDiscount((g.getDiscount()).add(vd));
            BigDecimal afterVoucher = after.subtract(vd);
            int gp = last ? pointLeft : (eligible.signum() == 0 ? 0 : new BigDecimal(points).multiply(afterVoucher).divide(eligible, 0, java.math.RoundingMode.DOWN).intValue());
            // Allocate whole point units only where their monetary value fits.
            gp = Math.min(gp, afterVoucher.divide(new BigDecimal(rate), 0, java.math.RoundingMode.DOWN).intValue());
            pointLeft -= gp;
            g.setPoints(gp);
            g.setPointsDiscount(new BigDecimal(gp).multiply(new BigDecimal(rate)));
            BigDecimal gf = last ? feeLeft : fee.divide(new BigDecimal(groups.size()), 0, java.math.RoundingMode.DOWN);
            feeLeft = feeLeft.subtract(gf);
            g.setFee(gf);
        }
        if (pointLeft != 0) {
            throw new IllegalArgumentException("Vui lòng giảm điểm thưởng để phù hợp với phần phân bổ cho từng cửa hàng.");
        }
        CheckoutQuote q = new CheckoutQuote();
        q.setCartId(cart.getId());
        q.setAddress(address);
        q.setLines(lines);
        q.setSubtotal(subtotal);
        q.setFee(fee);
        q.setDiscount(campaignTotal.add(voucherDiscount));
        q.setPointsDiscount(pointDiscount);
        q.setPoints(points);
        q.setVoucherId(voucherId);
        q.setDiscounts(discounts);
        q.setGroups(groups);
        q.setMethod(method);
        q.setTotal(eligible.subtract(pointDiscount).add(fee));
        q.setEtaHours(FulfillmentService.policy(c, "delivery_eta_hours", 24));
        q.setFingerprint(fingerprint + "|" + address + "|" + fee + "|" + discounts + "|" + points + "|" + method + "|" + q.getEtaHours());
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
                    return previous.getId().longValue();
                }
                LoyaltyService.expire(c, uid);
                CheckoutQuote q = quote(c, uid, addressId, voucher, points, method);
                if (!Objects.equals(expected, q.getFingerprint())) {
                    throw new IllegalArgumentException("Giỏ hàng hoặc thông tin giao hàng đã thay đổi. Vui lòng kiểm tra lại trước khi thanh toán.");
                }
                long oid = q.getCartId().longValue();
                Address a = q.getAddress();
                String delivery = a.getAddressLine() + ", " + a.getWard() + ", " + a.getDistrict() + ", " + a.getCity();
                if (delivery.length() > 500) {
                    throw new IllegalArgumentException("Địa chỉ giao hàng quá dài.");
                }
                Database.update(c, "DELETE FROM seller_orders WHERE order_id=? AND NOT EXISTS(SELECT 1 FROM order_items WHERE seller_order_id=seller_orders.id)", oid);
                Database.update(c, "UPDATE orders SET status=?,order_code=?,recipient_name=?,recipient_phone=?,delivery_address=?,payment_method=?,placed_at=SYSUTCDATETIME(),checkout_key=CONVERT(uniqueidentifier,?),item_total=?,delivery_fee=?,discount_amount=?,points_discount=?,points_used=?,voucher_id=?,estimated_delivery_at=DATEADD(hour,?,SYSUTCDATETIME()),payment_expires_at=CASE WHEN ?='COD' THEN NULL ELSE DATEADD(minute,?,SYSUTCDATETIME()) END,payable_amount=?,updated_at=SYSUTCDATETIME() WHERE id=? AND status='CART'", method.equals("COD") ? "CONFIRMED" : "AWAITING_PAYMENT", "FR" + oid, a.getRecipientName(), a.getPhone(), delivery, method, key, q.getSubtotal(), q.getFee(), q.getDiscount(), q.getPointsDiscount(), points, q.getVoucherId(), q.getEtaHours(), method, FulfillmentService.policy(c, "reservation_timeout_minutes", 15), q.getTotal(), oid);
                Database.update(c, "UPDATE so SET status='NEW',subtotal=(SELECT SUM(quantity*unit_price) FROM order_items WHERE seller_order_id=so.id),updated_at=SYSUTCDATETIME() FROM seller_orders so WHERE order_id=?", oid);
                Map<Long, CheckoutSellerTotal> groups = q.getGroups();
                for (var entry : groups.entrySet()) {
                    CheckoutSellerTotal g = entry.getValue();
                    Database.update(c, "UPDATE so SET delivery_fee=?,discount_amount=?,points_discount=?,points_used=?,cod_amount=?,commission_rate=COALESCE((SELECT TRY_CONVERT(decimal(9,6),JSON_VALUE(CASE WHEN ISJSON(t.value)=1 THEN t.value ELSE '{}' END,'$.commission_rate')) FROM stores s LEFT JOIN settings t ON t.id=s.seller_tier_id WHERE s.id=so.store_id),0) FROM seller_orders so WHERE id=?", g.getFee(), g.getDiscount(), g.getPointsDiscount(), g.getPoints(), method.equals("COD") ? (g.getSubtotal()).subtract(g.getDiscount()).subtract(g.getPointsDiscount()).add(g.getFee()) : BigDecimal.ZERO, entry.getKey());
                }
                Database.update(c, "UPDATE oi SET product_title=l.title,variant_name=v.option_name FROM order_items oi JOIN seller_orders so ON so.id=oi.seller_order_id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id WHERE so.order_id=?", oid);
                Database.update(c, "UPDATE so SET store_name_snapshot=s.store_name FROM seller_orders so JOIN stores s ON s.id=so.store_id WHERE so.order_id=?", oid);
                new StockReservationService().reserveOrder(c, oid);
                Map<Long, BigDecimal> discounts = q.getDiscounts();
                PromotionService.redeem(c, uid, oid, discounts);
                LoyaltyService.redeem(c, uid, oid, points);
                if (method.equals("COD")) {
                    Database.update(c, "INSERT transactions(type,user_id,order_id,method,amount,status,idempotency_key) VALUES('COD',?,?,'COD',?,'PENDING',?)", uid, oid, q.getTotal(), "cod:" + oid);
                }
                c.commit();
                return oid;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<Transaction> payments(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(Transaction.class, c, "SELECT id,order_id,method,amount,status,created_at FROM transactions WHERE user_id=? AND type IN ('PAYMENT','COD') ORDER BY id", uid);
        }
    }

    public List<OrderSummary> orders(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(OrderSummary.class, c, "SELECT o.*,(SELECT COALESCE(SUM(subtotal-discount_amount-points_discount+delivery_fee),0) FROM seller_orders WHERE order_id=o.id AND status NOT IN ('CANCELLED','REJECTED')) AS current_payable,(SELECT TOP 1 status FROM transactions WHERE order_id=o.id AND type IN ('PAYMENT','COD') ORDER BY id DESC) AS payment_status,CASE WHEN o.status='CONFIRMED' AND o.payment_method='COD' AND NOT EXISTS(SELECT 1 FROM seller_orders WHERE order_id=o.id AND status<>'NEW') THEN 1 ELSE 0 END AS cancellable FROM orders o WHERE customer_id=? AND status<>'CART' ORDER BY id DESC", uid);
        }
    }

    public List<OrderItemDetails> items(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(OrderItemDetails.class, c, "SELECT so.order_id,so.id AS seller_order_id,so.rejection_reason,so.status AS seller_status,COALESCE(so.store_name_snapshot,s.store_name) AS store_name,COALESCE(oi.product_title,l.title) AS title,oi.quantity,oi.unit_price FROM orders o JOIN seller_orders so ON so.order_id=o.id JOIN stores s ON s.id=so.store_id JOIN order_items oi ON oi.seller_order_id=so.id JOIN variants v ON v.id=oi.variant_id JOIN listings l ON l.id=v.listing_id WHERE o.customer_id=? AND o.status<>'CART' ORDER BY so.id,oi.id", uid);
        }
    }

    public void cancel(long uid, long oid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                Database.one(User.class, c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", uid);
                Order o = Database.one(Order.class, c, "SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND customer_id=?", oid, uid);
                if (o == null) {
                    throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
                }
                if ("CANCELLED".equals(o.getStatus())) {
                    c.commit();
                    return;
                }
                if (Database.one(Order.class, c, "SELECT id FROM orders WHERE id=? AND placed_at>=DATEADD(minute,-?,SYSUTCDATETIME())", oid, FulfillmentService.policy(c, "cancellation_window_minutes", 30)) == null) {
                    throw new IllegalArgumentException("Đã hết thời hạn cho phép hủy đơn.");
                }
                List<SellerOrder> sellers = Database.rows(SellerOrder.class, c, "SELECT id,status FROM seller_orders WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? ORDER BY store_id,id", oid);
                if (!"CONFIRMED".equals(o.getStatus()) || !"COD".equals(o.getPaymentMethod()) || sellers.stream().anyMatch(x -> !"NEW".equals(x.getStatus()))) {
                    throw new IllegalArgumentException("Không thể hủy đơn hàng này nữa.");
                }
                if (Database.one(Transaction.class, c, "SELECT id FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? AND status IN ('PAID','COMPLETED')", oid) != null) {
                    throw new IllegalArgumentException("Đơn đã thanh toán cần được duyệt hoàn tiền.");
                }
                Database.update(c, "UPDATE orders SET status='CANCELLED',updated_at=SYSUTCDATETIME() WHERE id=?", oid);
                Database.update(c, "UPDATE seller_orders SET status='CANCELLED',updated_at=SYSUTCDATETIME() WHERE order_id=?", oid);
                for (SellerOrder so : sellers) {
                    new StockReservationService().release(c, so.getId().longValue());
                }
                Database.update(c, "UPDATE transactions SET status='EXPIRED',updated_at=SYSUTCDATETIME() WHERE order_id=? AND type='COD' AND status='PENDING'", oid);
                BenefitsService.returnAll(c, o);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }
}
