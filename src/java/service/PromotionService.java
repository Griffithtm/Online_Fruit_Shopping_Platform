package service;

import model.*;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.math.*;
import java.util.*;

public class PromotionService {

    public List<PromotionDetails> list(long uid, String role) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            FulfillmentService.role(c, uid, role);
            List<PromotionDetails> result = Database.rows(PromotionDetails.class, c, "SELECT p.*,(SELECT COUNT(*) FROM promotion_redemptions r WHERE r.promotion_id=p.id AND r.status='APPLIED') AS redemptions FROM promotions p LEFT JOIN stores s ON s.id=p.store_id WHERE " + (role.equals("ADMIN") ? "p.type='VOUCHER' AND ?>0" : "p.type='SELLER_CAMPAIGN' AND s.owner_id=?") + " ORDER BY p.id DESC", uid);
            for (PromotionDetails p : result) {
                p.setHistory(Database.rows(RedemptionDetails.class, c, "SELECT r.order_id,o.order_code,r.discount_amount,r.status,r.created_at FROM promotion_redemptions r JOIN orders o ON o.id=r.order_id WHERE r.promotion_id=? ORDER BY r.id DESC", p.getId()));
            }
            return result;
        }
    }

    public void save(long uid, String role, PromotionForm f) throws Exception {
        if (!Set.of("ADMIN", "SELLER").contains(role)) {
            throw new IllegalArgumentException("Bạn không có quyền thực hiện thao tác này.");
        }
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                FulfillmentService.role(c, uid, role);
                boolean admin = role.equals("ADMIN");
                Store store = admin ? null : Database.one(Store.class, c, "SELECT id FROM stores WHERE owner_id=? AND status='ACTIVE'", uid);
                if (!admin && store == null) {
                    throw new IllegalArgumentException("Cửa hàng phải đang hoạt động.");
                }
                Long id = f.getId() == null || f.getId().isBlank() ? null : Validation.id(f.getId());
                Promotion old = id == null ? null : Database.one(Promotion.class, c, "SELECT * FROM promotions WITH(UPDLOCK,HOLDLOCK) WHERE id=?", id);
                if (id != null && (old == null || admin && !"VOUCHER".equals(old.getType()) || !admin && !Objects.equals(old.getStoreId(), store.getId()))) {
                    throw new IllegalArgumentException("Không tìm thấy khuyến mãi.");
                }
                String action = f.getAction();
                if ("state".equals(action)) {
                    String state = f.getState();
                    if (!Set.of("SCHEDULED", "RUNNING", "PAUSED", "ENDED").contains(state) || id == null) {
                        throw new IllegalArgumentException("Trạng thái khuyến mãi không hợp lệ.");
                    }
                    if (Set.of("ENDED", "DISABLED").contains(old.getStatus())) {
                        throw new IllegalArgumentException("Khuyến mãi này đã kết thúc.");
                    }
                    Database.update(c, "UPDATE promotions SET status=?,updated_at=SYSUTCDATETIME() WHERE id=?", state, id);
                } else {
                    if (old != null && old.getUsedCount().intValue() > 0) {
                        throw new IllegalArgumentException("Không thể sửa khuyến mãi đã được sử dụng. Vui lòng tạm dừng và tạo khuyến mãi mới.");
                    }
                    String name = Validation.text(f.getName(), 150), kind = f.getDiscountType();
                    if (!Set.of("PERCENT", "AMOUNT").contains(kind)) {
                        throw new IllegalArgumentException("Loại giảm giá không hợp lệ.");
                    }
                    BigDecimal amount = Validation.money(f.getValue(), false), budget = Validation.money(f.getBudget(), false), minimum = Validation.money(f.getMinimum(), true);
                    if (kind.equals("PERCENT") && amount.compareTo(new BigDecimal("100")) > 0) {
                        throw new IllegalArgumentException("Tỷ lệ phần trăm không được vượt quá 100.");
                    }
                    int limit = Validation.quantity(f.getLimit()), per = Validation.quantity(f.getPerCustomer());
                    Timestamp start = Timestamp.valueOf(java.time.LocalDateTime.parse(f.getStart()).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime());
                    Timestamp end = Timestamp.valueOf(java.time.LocalDateTime.parse(f.getEnd()).atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).withZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime());
                    if (!end.after(start)) {
                        throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
                    }
                    String code = admin ? Validation.text(f.getCode(), 30).toUpperCase(Locale.ROOT) : null;
                    if (admin && !code.matches("[A-Z0-9_-]+")) {
                        throw new IllegalArgumentException("Mã giảm giá chỉ được chứa chữ cái, chữ số, dấu gạch dưới hoặc gạch ngang.");
                    }
                    if (id == null) {
                        id = Database.insert(c, "INSERT promotions(type,store_id,name,code,discount_type,discount_value,min_spend,budget_limit,usage_limit,per_customer_limit,start_at,end_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)", admin ? "VOUCHER" : "SELLER_CAMPAIGN", admin ? null : store.getId(), name, code, kind, amount, minimum, budget, limit, per, start, end);
                    } else {
                        Database.update(c, "UPDATE promotions SET name=?,code=?,discount_type=?,discount_value=?,min_spend=?,budget_limit=?,usage_limit=?,per_customer_limit=?,start_at=?,end_at=?,updated_at=SYSUTCDATETIME() WHERE id=?", name, code, kind, amount, minimum, budget, limit, per, start, end, id);
                    }
                    if (!admin) {
                        String raw = Validation.text(f.getListings(), 1000);
                        List<Long> ids = new ArrayList<>();
                        for (String token : raw.split(",")) {
                            long lid = Validation.id(token.trim());
                            if (Database.one(Listing.class, c, "SELECT id FROM listings WHERE id=? AND store_id=?", lid, store.getId()) == null) {
                                throw new IllegalArgumentException("Tất cả tin đăng phải thuộc cửa hàng của bạn.");
                            }
                            ids.add(lid);
                        }
                        Database.update(c, "UPDATE listings SET campaign_id=NULL WHERE campaign_id=?", id);
                        for (long lid : ids) {
                            Database.update(c, "UPDATE listings SET campaign_id=? WHERE id=?", id, lid);
                        }
                    }
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public static BigDecimal discount(Connection c, long uid, Promotion p, BigDecimal base) throws Exception {
        if (base.compareTo(p.getMinSpend()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu của khuyến mãi.");
        }
        if (p.getUsageLimit() != null && FulfillmentService.id(p.getUsedCount()) >= FulfillmentService.id(p.getUsageLimit())) {
            throw new IllegalArgumentException("Khuyến mãi đã đạt giới hạn sử dụng.");
        }
        long uses = FulfillmentService.id(Database.one(CountResult.class, c, "SELECT COUNT(*) n FROM promotion_redemptions WHERE promotion_id=? AND customer_id=? AND status='APPLIED'", p.getId(), uid).getN());
        if (p.getPerCustomerLimit() != null && uses >= FulfillmentService.id(p.getPerCustomerLimit())) {
            throw new IllegalArgumentException("Bạn đã dùng hết lượt áp dụng khuyến mãi.");
        }
        BigDecimal d = p.getDiscountType().equals("PERCENT") ? base.multiply(p.getDiscountValue()).divide(new BigDecimal("100"), 0, RoundingMode.DOWN) : p.getDiscountValue();
        if (p.getMaxDiscount() != null) {
            d = d.min(p.getMaxDiscount());
        }
        d = d.min(base).setScale(0, RoundingMode.DOWN);
        if (p.getBudgetLimit() != null && (p.getBudgetUsed()).add(d).compareTo(p.getBudgetLimit()) > 0) {
            throw new IllegalArgumentException("Ngân sách khuyến mãi đã hết.");
        }
        return d;
    }

    public static Promotion active(Connection c, long id) throws Exception {
        return Database.one(Promotion.class, c, "SELECT * FROM promotions WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND status IN ('RUNNING','SCHEDULED') AND start_at<=SYSUTCDATETIME() AND end_at>SYSUTCDATETIME()", id);
    }

    public static void redeem(Connection c, long uid, long oid, Map<Long, BigDecimal> discounts) throws Exception {
        for (var entry : discounts.entrySet()) {
            Promotion p = active(c, entry.getKey());
            if (p == null) {
                throw new IllegalArgumentException("Có khuyến mãi không còn hiệu lực.");
            }
            Database.update(c, "UPDATE promotions SET used_count=used_count+1,budget_used=budget_used+?,updated_at=SYSUTCDATETIME() WHERE id=?", entry.getValue(), entry.getKey());
            Database.update(c, "INSERT promotion_redemptions(promotion_id,order_id,customer_id,discount_amount) VALUES(?,?,?,?)", entry.getKey(), oid, uid, entry.getValue());
        }
    }
}
