package service;

import model.*;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.util.*;
import java.math.*;

public class PaymentService {

    public String start(long uid, long oid, String ip) throws Exception {
        VnpayGateway gateway = new VnpayGateway();
        gateway.requireConfigured();
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                Order o = Database.one(Order.class, c, "SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND customer_id=? AND status='AWAITING_PAYMENT' AND payment_method IN ('CARD','E_WALLET') AND payment_expires_at>SYSUTCDATETIME()", oid, uid);
                if (o == null) {
                    throw new IllegalArgumentException("Đơn hàng chưa đủ điều kiện thanh toán trực tuyến.");
                }
                Transaction t = Database.one(Transaction.class, c, "SELECT TOP 1 id FROM transactions WHERE order_id=? AND type='PAYMENT' AND status='INITIATED' ORDER BY id DESC", oid);
                long ref = t == null ? Database.insert(c, "INSERT transactions(type,user_id,order_id,method,amount,status,provider) VALUES('PAYMENT',?,?,?,?,'INITIATED','VNPAY')", uid, oid, o.getPaymentMethod(), o.getPayableAmount()) : FulfillmentService.id(t.getId());
                String created = t == null ? null : (String) Database.one(Transaction.class, c, "SELECT provider_create_date FROM transactions WHERE id=?", ref).getProviderCreateDate();
                if (created == null) {
                    created = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(java.time.Instant.now());
                    Database.update(c, "UPDATE transactions SET provider_create_date=? WHERE id=?", created, ref);
                }
                String url = gateway.payment(ref, o.getPayableAmount(), o.getPaymentMethod(), ip, (Timestamp) o.getPaymentExpiresAt(), created);
                c.commit();
                return url;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public String callback(Map<String, String> p) throws Exception {
        if (!new VnpayGateway().verify(p)) {
            try (Connection log = new DBConnect().getConnection()) {
                Database.update(log, "INSERT integration_events(provider,event_key,event_type,status,error_message) VALUES('VNPAY',?,'PAYMENT_IPN','REJECTED','Signature or merchant validation failed')", UUID.randomUUID().toString());
            }
            return "97";
        }
        long ref;
        BigDecimal amount;
        try {
            ref = Long.parseLong(p.get("vnp_TxnRef"));
            amount = new BigDecimal(p.get("vnp_Amount")).movePointLeft(2);
        } catch (Exception e) {
            return "01";
        }
        String gatewayRef = p.get("vnp_TransactionNo");
        if (gatewayRef == null || !gatewayRef.matches("[0-9]{1,15}")) {
            return "99";
        }
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                Transaction pointer = Database.one(Transaction.class, c, "SELECT order_id FROM transactions WHERE id=? AND type='PAYMENT' AND provider='VNPAY'", ref);
                if (pointer == null) {
                    c.rollback();
                    return "01";
                }
                PaymentOrder o = Database.one(PaymentOrder.class, c, "SELECT *,CASE WHEN payment_expires_at<=SYSUTCDATETIME() THEN 1 ELSE 0 END AS expired FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?", pointer.getOrderId());
                Transaction t = Database.one(Transaction.class, c, "SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE id=?", ref);
                if ((t.getAmount()).compareTo(amount) != 0) {
                    c.rollback();
                    return "04";
                }
                if ("PAID".equals(t.getStatus())) {
                    c.commit();
                    return Objects.equals(gatewayRef, t.getGatewayRef()) ? "02" : "99";
                }
                if (Database.one(IntegrationEvent.class, c, "SELECT id FROM integration_events WHERE provider='VNPAY' AND event_key=?", ref + ":" + gatewayRef + ":" + p.get("vnp_ResponseCode") + ":" + p.get("vnp_TransactionStatus")) != null) {
                    c.commit();
                    return "02";
                }
                boolean success = "00".equals(p.get("vnp_ResponseCode")) && "00".equals(p.get("vnp_TransactionStatus"));
                if (success && Database.one(Transaction.class, c, "SELECT id FROM transactions WHERE provider='VNPAY' AND gateway_ref=? AND id<>?", gatewayRef, ref) != null) {
                    c.rollback();
                    return "99";
                }
                Database.update(c, "UPDATE transactions SET status=?,gateway_ref=?,reconciled=1,updated_at=SYSUTCDATETIME() WHERE id=?", success ? "PAID" : Set.of("01", "04", "05", "06", "07").contains(p.getOrDefault("vnp_TransactionStatus", "")) ? "INITIATED" : "FAILED", success ? gatewayRef : null, ref);
                if (success) {
                    if ("AWAITING_PAYMENT".equals(o.getStatus()) && o.getExpired().intValue() == 0) {
                        Database.update(c, "UPDATE orders SET status='CONFIRMED',updated_at=SYSUTCDATETIME() WHERE id=?", o.getId());
                    } else {
                        if ("AWAITING_PAYMENT".equals(o.getStatus())) {
                            expire(c, o);
                        }
                        // A late payment is recorded, never used to revive an expired order.
                        for (SellerOrder so : Database.rows(SellerOrder.class, c, "SELECT * FROM seller_orders WHERE order_id=?", o.getId())) {
                            FinanceService.requestAutomatic(c, o, so, ref, "AWAITING_PAYMENT".equals(o.getStatus()) || "EXPIRED".equals(o.getStatus()) || "CANCELLED".equals(o.getStatus()) ? "Thanh toán đến sau khi đơn hết hạn hoặc bị hủy" : "Thanh toán trùng cho đơn đã xác nhận", Set.of("CONFIRMED", "COMPLETED").contains(o.getStatus()));
                        }
                    }
                }
                Database.update(c, "INSERT integration_events(provider,event_key,event_type,status) VALUES('VNPAY',?,'PAYMENT_IPN','PROCESSED')", ref + ":" + gatewayRef + ":" + p.get("vnp_ResponseCode") + ":" + p.get("vnp_TransactionStatus"));
                c.commit();
                return "00";
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static void expire(Connection c, Order o) throws Exception {
        Database.update(c, "UPDATE orders SET status='EXPIRED',updated_at=SYSUTCDATETIME() WHERE id=?", o.getId());
        for (SellerOrder so : Database.rows(SellerOrder.class, c, "SELECT id FROM seller_orders WITH(UPDLOCK,HOLDLOCK) WHERE order_id=? ORDER BY store_id,id", o.getId())) {
            new StockReservationService().release(c, FulfillmentService.id(so.getId()));
        }
        Database.update(c, "UPDATE seller_orders SET status='CANCELLED' WHERE order_id=?", o.getId());
        Database.update(c, "UPDATE transactions SET status='EXPIRED' WHERE order_id=? AND type='PAYMENT' AND status='INITIATED'", o.getId());
        BenefitsService.returnAll(c, o);
    }

    public void expireDue() throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            List<Order> due = Database.rows(Order.class, c, "SELECT TOP 100 id FROM orders WHERE status='AWAITING_PAYMENT' AND payment_expires_at<=SYSUTCDATETIME() ORDER BY id");
            for (var id : due) {
                c.setAutoCommit(false);
                try {
                    Order o = Database.one(Order.class, c, "SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status='AWAITING_PAYMENT' AND payment_expires_at<=SYSUTCDATETIME()", id.getId());
                    if (o != null) {
                        expire(c, o);
                    }
                    c.commit();
                } catch (Exception e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(true);
                }
            }
        }
    }
}
