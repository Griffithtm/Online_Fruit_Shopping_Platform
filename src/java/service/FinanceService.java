package service;

import model.*;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.util.*;
import java.math.*;

public class FinanceService {

    public static BigDecimal paidShare(SellerOrder so) {
        return FulfillmentService.money(so.getSubtotal()).subtract(FulfillmentService.money(so.getDiscountAmount())).subtract(FulfillmentService.money(so.getPointsDiscount())).add(FulfillmentService.money(so.getDeliveryFee()));
    }

    public static void requestAutomatic(Connection c, Order o, SellerOrder so, long parent, String reason) throws Exception {
        requestAutomatic(c, o, so, parent, reason, false);
    }

    public static boolean requestAutomatic(Connection c, Order o, SellerOrder so, long parent, String reason, boolean excess) throws Exception {
        if (Database.one(Transaction.class, c, "SELECT id FROM transactions WHERE type='REFUND' AND seller_order_id=? AND parent_transaction_id=? AND status<>'REJECTED'", so.getId(), parent) != null) {
            return false;
        }
        Transaction payment = Database.one(Transaction.class, c, "SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status='PAID'", parent);
        if (payment == null) {
            throw new IllegalArgumentException("Chưa có khoản thanh toán được xác nhận để hoàn tiền.");
        }
        BigDecimal reserved = FulfillmentService.money(Database.one(MoneyTotal.class, c, "SELECT COALESCE(SUM(amount),0) AS total FROM transactions WHERE parent_transaction_id=? AND type='REFUND' AND status<>'REJECTED'", parent).getTotal());
        BigDecimal amount = paidShare(so);
        if (reserved.add(amount).compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Số tiền hoàn vượt số tiền đã trả còn lại.");
        }
        Database.update(c, "INSERT transactions(type,user_id,order_id,seller_order_id,parent_transaction_id,amount,method,status,reason,idempotency_key,refund_scope) VALUES('REFUND',?,?,?,?,?,?,'REQUESTED',?,?,?)", o.getCustomerId(), o.getId(), so.getId(), parent, amount, o.getPaymentMethod(), reason, "refund:" + so.getId() + ":" + parent + ":" + UUID.randomUUID(), excess ? "EXCESS_PAYMENT" : "PURCHASE");
        return true;
    }

    public void request(long uid, long sid, String reason) throws Exception {
        reason = Validation.text(reason, 500);
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                SellerOrder ref = Database.one(SellerOrder.class, c, "SELECT order_id FROM seller_orders WHERE id=?", sid);
                if (ref == null) {
                    throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
                }
                Order o = Database.one(Order.class, c, "SELECT * FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND customer_id=?", ref.getOrderId(), uid);
                if (o == null) {
                    throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
                }
                SellerOrder so = Database.one(SellerOrder.class, c, "SELECT * FROM seller_orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND (status IN ('CANCELLED','REJECTED') OR (status='DELIVERED' AND delivered_at>=DATEADD(day,-?,SYSUTCDATETIME())))", sid, FulfillmentService.policy(c, "refund_window_days", 7));
                if (so == null || "SETTLED".equals(so.getSettlementStatus())) {
                    throw new IllegalArgumentException("Đơn hàng đã ngoài thời hạn được yêu cầu hoàn tiền.");
                }
                Transaction payment = Database.one(Transaction.class, c, "SELECT TOP 1 id FROM transactions WHERE order_id=? AND status='PAID' AND (type='PAYMENT' OR (type='COD' AND seller_order_id=?)) ORDER BY id", o.getId(), sid);
                if (payment == null) {
                    throw new IllegalArgumentException("Chưa thu được tiền để hoàn trả.");
                }
                boolean created = requestAutomatic(c, o, so, FulfillmentService.id(payment.getId()), reason, false);
                if (created) {
                    // A duplicate requestAutomatic() call does not generate another alert.
                    new NotificationService().create(c, uid, "REFUND_REQUESTED",
                            "Đã tiếp nhận yêu cầu hoàn tiền",
                            "Yêu cầu hoàn tiền cho đơn cửa hàng #" + sid + " đang chờ xử lý.",
                            "/refunds", false, "refund-requested:" + sid + ":" + UUID.randomUUID());
                }
                if (created && "DELIVERED".equals(so.getStatus())) {
                    Database.update(c, "UPDATE seller_orders SET dispute_status='OPEN',dispute_note=? WHERE id=?", reason, sid);
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<TransactionDetails> list(long uid, boolean admin) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            if (admin) {
                FulfillmentService.role(c, uid, "ADMIN");
            }
            return Database.rows(TransactionDetails.class, c, "SELECT t.*,o.order_code FROM transactions t JOIN orders o ON o.id=t.order_id WHERE t.type='REFUND' AND " + (admin ? "?>0" : "t.user_id=?") + " ORDER BY t.id DESC", uid);
        }
    }

    public void decide(long admin, long tid, String action, String reason) throws Exception {
        reason = Validation.text(reason, 500);
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                FulfillmentService.role(c, admin, "ADMIN");
                Transaction ref = Database.one(Transaction.class, c, "SELECT order_id FROM transactions WHERE id=? AND type='REFUND'", tid);
                if (ref == null) {
                    throw new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền.");
                }
                Database.one(Order.class, c, "SELECT id FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?", ref.getOrderId());
                Transaction t = Database.one(Transaction.class, c, "SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE id=?", tid);
                if (action.equals("approve") || action.equals("reject")) {
                    if (!"REQUESTED".equals(t.getStatus())) {
                        throw new IllegalArgumentException("Yêu cầu hoàn tiền đã được xét duyệt.");
                    }
                    Database.update(c, "UPDATE transactions SET status=?,reason=?,updated_at=SYSUTCDATETIME() WHERE id=?", action.equals("approve") ? "APPROVED" : "REJECTED", reason, tid);
                    String result = action.equals("approve") ? "APPROVED" : "REJECTED";
                    new NotificationService().create(c, FulfillmentService.id(t.getUserId()),
                            action.equals("approve") ? "REFUND_APPROVED" : "REFUND_REJECTED",
                            action.equals("approve") ? "Yêu cầu hoàn tiền đã được duyệt" : "Yêu cầu hoàn tiền bị từ chối",
                            "Yêu cầu hoàn tiền #" + tid + " có kết quả: "
                            + (action.equals("approve") ? "Đã duyệt." : "Từ chối. Vui lòng xem chi tiết."),
                            "/refunds", false, "refund-decision:" + tid + ":" + result);
                    if (!"EXCESS_PAYMENT".equals(t.getRefundScope())) {
                        Database.update(c, "UPDATE seller_orders SET dispute_status='RESOLVED',dispute_decision=? WHERE id=?", reason, t.getSellerOrderId());
                    }
                } else if (action.equals("complete")) {
                    if (!"COD".equals(t.getMethod()) || !"APPROVED".equals(t.getStatus())) {
                        throw new IllegalArgumentException("Chỉ yêu cầu hoàn tiền thanh toán khi nhận hàng đã duyệt mới được hoàn tất thủ công.");
                    }
                    complete(c, t, FulfillmentService.evidence(reason), null);
                } else {
                    throw new IllegalArgumentException("Thao tác hoàn tiền không hợp lệ.");
                }
                Database.update(c, "INSERT audit_logs(actor_id,entity_type,entity_id,action,note) VALUES(?,'Refund',?,?,?)", admin, tid, action, reason);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    static void complete(Connection c, Transaction t, String evidence, String gatewayRef) throws Exception {
        Database.update(c, "UPDATE transactions SET status='REFUNDED',evidence_url=?,gateway_ref=?,provider=?,reconciled=1,updated_at=SYSUTCDATETIME() WHERE id=?", evidence, gatewayRef, gatewayRef == null ? null : "VNPAY", t.getId());
        SellerOrder so = Database.one(SellerOrder.class, c, "SELECT * FROM seller_orders WHERE id=?", t.getSellerOrderId());
        if (!"EXCESS_PAYMENT".equals(t.getRefundScope())) {
            LoyaltyService.restore(c, FulfillmentService.id(t.getUserId()), FulfillmentService.id(so.getId()), so.getPointsUsed().intValue());
            Transaction earned = Database.one(Transaction.class, c, "SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE idempotency_key=?", "earn:" + so.getId());
            if (earned != null && earned.getRemainingPoints() != null && earned.getRemainingPoints().intValue() > 0) {
                int n = earned.getRemainingPoints().intValue();
                Database.update(c, "UPDATE transactions SET remaining_points=0,status='EXPIRED' WHERE id=?", earned.getId());
                Database.update(c, "UPDATE users SET points_balance=points_balance-? WHERE id=?", n, t.getUserId());
                Database.update(c, "INSERT transactions(type,user_id,seller_order_id,points,status,reason,idempotency_key) VALUES('POINTS_EXPIRE',?,?,?,'COMPLETED',N'Thu hồi điểm do hoàn tiền',?)", t.getUserId(), so.getId(), -n, "refund-points:" + t.getId());
            }
            LoyaltyService.tier(c, FulfillmentService.id(t.getUserId()));
        }
        new NotificationService().create(c, FulfillmentService.id(t.getUserId()),
                "REFUND", "Hoàn tiền thành công",
                "Khoản hoàn tiền của bạn đã được xác nhận.",
                "/refunds", false, "refund-complete:" + t.getId());
    }

    public void settle(long admin, long sid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                FulfillmentService.role(c, admin, "ADMIN");
                SellerOrder ref = Database.one(SellerOrder.class, c, "SELECT order_id FROM seller_orders WHERE id=?", sid);
                if (ref == null) {
                    throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
                }
                Database.one(Order.class, c, "SELECT id FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?", ref.getOrderId());
                SellerOrder so = Database.one(SellerOrder.class, c, "SELECT * FROM seller_orders WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status='DELIVERED' AND settlement_status='UNSETTLED' AND delivered_at<DATEADD(day,-?,SYSUTCDATETIME())", sid, FulfillmentService.policy(c, "refund_window_days", 7));
                if (so == null) {
                    throw new IllegalArgumentException("Cần hết thời hạn yêu cầu hoàn tiền trước khi quyết toán.");
                }
                if (Database.one(Transaction.class, c, "SELECT id FROM transactions WHERE seller_order_id=? AND type='REFUND' AND status NOT IN ('REFUNDED','REJECTED')", sid) != null || "OPEN".equals(so.getDisputeStatus())) {
                    throw new IllegalArgumentException("Vui lòng xử lý các yêu cầu hoàn tiền hoặc tranh chấp còn chờ trước.");
                }
                if (Database.one(Transaction.class, c, "SELECT id FROM transactions WHERE order_id=? AND status='PAID' AND reconciled=1 AND (type='PAYMENT' OR (type='COD' AND seller_order_id=?))", so.getOrderId(), sid) == null) {
                    throw new IllegalArgumentException("Khoản thanh toán chưa được đối soát.");
                }
                BigDecimal refunds = FulfillmentService.money(Database.one(MoneyTotal.class, c, "SELECT COALESCE(SUM(amount),0) total FROM transactions WHERE seller_order_id=? AND type='REFUND' AND status='REFUNDED' AND refund_scope='PURCHASE'", sid).getTotal());
                BigDecimal adjustments = FulfillmentService.money(Database.one(MoneyTotal.class, c, "SELECT COALESCE(SUM(amount),0) total FROM transactions WHERE seller_order_id=? AND type='ADJUSTMENT' AND status='COMPLETED' AND reconciled=1", sid).getTotal());
                BigDecimal net = FulfillmentService.money(so.getSubtotal()).subtract(refunds).subtract(FulfillmentService.money(so.getCommission())).add(adjustments);
                Database.update(c, "UPDATE seller_orders SET settlement_status='SETTLED',settled_at=SYSUTCDATETIME(),net_payable=? WHERE id=?", net, sid);
                Database.update(c, "INSERT audit_logs(actor_id,entity_type,entity_id,action,note) VALUES(?,'SellerOrder',?,'SETTLED','Settlement summary recorded; not a bank payout')", admin, sid);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }
}
