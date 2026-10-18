package service;

import model.*;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.util.*;
import java.math.*;

public class LoyaltyService {

    public static void expire(Connection c, long uid) throws Exception {
        Database.one(User.class, c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", uid);
        for (Transaction t : Database.rows(Transaction.class, c, "SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE user_id=? AND type='POINTS_EARN' AND status='AVAILABLE' AND remaining_points>0 AND points_expire_at<CONVERT(date,SYSUTCDATETIME()) ORDER BY id", uid)) {
            int n = t.getRemainingPoints().intValue();
            if (Database.update(c, "UPDATE users SET points_balance=points_balance-? WHERE id=? AND points_balance>=?", n, uid, n) != 1) {
                throw new IllegalStateException("Số dư điểm cần được đối soát.");
            }
            Database.update(c, "UPDATE transactions SET remaining_points=0,status='EXPIRED' WHERE id=?", t.getId());
            Database.update(c, "INSERT transactions(type,user_id,points,status,idempotency_key) VALUES('POINTS_EXPIRE',?,?,'COMPLETED',?)", uid, -n, "points-expire:" + t.getId());
        }
    }

    public static void redeem(Connection c, long uid, long oid, int points) throws Exception {
        if (points == 0) {
            return;
        }
        expire(c, uid);
        int left = points;
        for (Transaction lot : Database.rows(Transaction.class, c, "SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE user_id=? AND type='POINTS_EARN' AND status='AVAILABLE' AND remaining_points>0 ORDER BY points_expire_at,id", uid)) {
            int take = Math.min(left, lot.getRemainingPoints().intValue());
            Database.update(c, "UPDATE transactions SET remaining_points=remaining_points-? WHERE id=?", take, lot.getId());
            left -= take;
            if (left == 0) {
                break;
            }
        }
        if (left > 0) {
            throw new IllegalArgumentException("Không đủ điểm thưởng khả dụng.");
        }
        if (Database.update(c, "UPDATE users SET points_balance=points_balance-? WHERE id=? AND points_balance>=?", points, uid, points) != 1) {
            throw new IllegalArgumentException("Số dư điểm đã thay đổi. Vui lòng kiểm tra lại thanh toán.");
        }
        Database.update(c, "INSERT transactions(type,user_id,order_id,points,status,idempotency_key) VALUES('POINTS_REDEEM',?, ?,?,'REDEEMED',?)", uid, oid, -points, "redeem:" + oid);
    }

    public static void earn(Connection c, long uid, long sid, BigDecimal eligible) throws Exception {
        Database.one(User.class, c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", uid);
        if (Database.one(Transaction.class, c, "SELECT id FROM transactions WHERE idempotency_key=?", "earn:" + sid) != null) {
            return;
        }
        int points = eligible.divide(new BigDecimal(FulfillmentService.policy(c, "points_earn_vnd", 10000)), 0, RoundingMode.DOWN).intValueExact();
        if (points > 0) {
            Database.update(c, "INSERT transactions(type,user_id,seller_order_id,points,remaining_points,status,points_expire_at,idempotency_key) VALUES('POINTS_EARN',?,?,?,?,'AVAILABLE',DATEADD(day,?,CONVERT(date,SYSUTCDATETIME())),?)", uid, sid, points, points, FulfillmentService.policy(c, "points_expiry_days", 365), "earn:" + sid);
            Database.update(c, "UPDATE users SET points_balance=points_balance+? WHERE id=?", points, uid);
        }
        tier(c, uid);
    }

    public static MembershipProgress membership(Connection c, long uid) throws Exception {
        int days = FulfillmentService.policy(c, "membership_period_days", 365);
        MembershipProgress progress = new MembershipProgress();
        progress.setPeriodDays(days);
        MoneyTotal spend = Database.one(MoneyTotal.class, c, "SELECT COALESCE(SUM(so.subtotal-so.discount_amount-so.points_discount),0) AS amount FROM seller_orders so JOIN orders o ON o.id=so.order_id WHERE o.customer_id=? AND so.status='DELIVERED' AND so.delivered_at>=DATEADD(day,-?,SYSUTCDATETIME()) AND NOT EXISTS(SELECT 1 FROM transactions t WHERE t.seller_order_id=so.id AND t.type='REFUND' AND t.status='REFUNDED' AND t.refund_scope='PURCHASE')", uid, days);
        BigDecimal amount = spend.getAmount();
        progress.setQualifyingSpend(amount);
        List<MembershipRule> rules = Database.rows(MembershipRule.class, c, "SELECT id,name,TRY_CONVERT(decimal(15,2),JSON_VALUE(CASE WHEN ISJSON(value)=1 THEN value ELSE '{}' END,'$.minimum_spend')) AS threshold FROM settings WHERE type='MEMBERSHIP_TIER' AND status='ACTIVE' ORDER BY threshold,priority,id");
        MembershipRule current = null, next = null;
        for (MembershipRule rule : rules) {
            BigDecimal threshold = rule.getThreshold();
            if (threshold == null || threshold.signum() < 0) {
                continue;
            }
            if (threshold.compareTo(amount) <= 0) {
                current = rule;
            } else if (next == null) {
                next = rule;
            }
        }
        if (current == null) {
            progress.setWarning("Quy tắc hạng thành viên chưa đầy đủ. Hạng hiện tại của bạn được giữ nguyên.");
        } else {
            progress.setTierId(current.getId());
        }
        if (next != null) {
            progress.setNextTier(next.getName());
            progress.setRemainingSpend((next.getThreshold()).subtract(amount));
        }
        return progress;
    }

    public static void tier(Connection c, long uid) throws Exception {
        User user = Database.one(User.class, c, "SELECT membership_tier_id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", uid);
        MembershipProgress progress = membership(c, uid);
        Object target = progress.getTierId();
        if (target != null && !Objects.equals(user.getMembershipTierId(), target)) {
            Database.update(c, "UPDATE users SET membership_tier_id=? WHERE id=?", target, uid);
            new NotificationService().create(c, uid, "MEMBERSHIP_UPDATE",
                    "Hạng thành viên đã cập nhật",
                    "Hạng thành viên của bạn đã thay đổi. Hãy xem quyền lợi và tiến độ tích lũy.",
                    "/wallet", false, "membership:" + uid + ":" + UUID.randomUUID());
        }
    }

    public Wallet wallet(long uid) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                expire(c, uid);
                tier(c, uid);
                Wallet result = Database.one(Wallet.class, c, "SELECT u.points_balance,s.name AS tier,s.description AS benefits FROM users u LEFT JOIN settings s ON s.id=u.membership_tier_id WHERE u.id=?", uid);
                result.setMembership(membership(c, uid));
                result.setTotals(Database.one(WalletTotals.class, c, "SELECT COALESCE(SUM(CASE WHEN type='POINTS_EARN' AND status='PENDING' THEN points ELSE 0 END),0) AS pending,COALESCE(SUM(CASE WHEN type='POINTS_EXPIRE' THEN -points ELSE 0 END),0) AS expired FROM transactions WHERE user_id=?", uid));
                result.setEntries(Database.rows(Transaction.class, c, "SELECT type,points,remaining_points,status,points_expire_at,created_at FROM transactions WHERE user_id=? AND type LIKE 'POINTS_%' ORDER BY id DESC", uid));
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static void restore(Connection c, long uid, long sid, int points) throws Exception {
        if (points <= 0 || Database.one(Transaction.class, c, "SELECT id FROM transactions WHERE idempotency_key=?", "points-return:" + sid) != null) {
            return;
        }
        Database.one(User.class, c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", uid);
        Database.update(c, "INSERT transactions(type,user_id,seller_order_id,points,remaining_points,status,points_expire_at,idempotency_key,reason) VALUES('POINTS_EARN',?,?,?,?,'AVAILABLE',DATEADD(day,?,CONVERT(date,SYSUTCDATETIME())),?,N'Hoàn lại sau khi hủy đơn')", uid, sid, points, points, FulfillmentService.policy(c, "points_expiry_days", 365), "points-return:" + sid);
        Database.update(c, "UPDATE users SET points_balance=points_balance+? WHERE id=?", points, uid);
    }
}
