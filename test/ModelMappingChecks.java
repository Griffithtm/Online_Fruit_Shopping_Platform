import config.DBConnect;
import dao.Database;
import model.*;
import java.sql.*;
import java.math.BigDecimal;

/** Run only against an isolated verification database. */
public class ModelMappingChecks {
    static int checks;
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("PASS " + message);
    }

    public static void main(String[] args) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            if (!c.getCatalog().contains("Verify")) throw new IllegalStateException("Use a verification database");
            String[] tables = {"addresses", "audit_logs", "batches", "integration_events", "listings", "notifications", "order_items", "orders", "password_reset_tokens", "promotion_redemptions", "promotions", "review_reports", "reviews", "schema_versions", "seller_orders", "settings", "stock_movements", "stores", "subscriptions", "transactions", "users", "variants", "v_available_stock", "v_public_products"};
            Class<?>[] types = {Address.class, AuditLog.class, Batch.class, IntegrationEvent.class, Listing.class, Notification.class, OrderItem.class, Order.class, PasswordResetToken.class, PromotionRedemption.class, Promotion.class, ReviewReport.class, Review.class, SchemaVersion.class, SellerOrder.class, Setting.class, StockMovement.class, Store.class, Subscription.class, Transaction.class, User.class, Variant.class, AvailableStock.class, ProductOffer.class};
            for (int i = 0; i < tables.length; i++) {
                Database.rows(types[i], c, "SELECT TOP 1 * FROM dbo.[" + tables[i] + "]");
                check(true, "schema mapping " + tables[i]);
            }
            User user = Database.one(User.class, c, "SELECT CAST(7 AS bigint) id,N'Khách hàng' full_name,CAST(NULL AS int) points_balance,CAST(SYSUTCDATETIME() AS datetime2) created_at");
            check(user.getId() == 7L && "Khách hàng".equals(user.getFullName()) && user.getPointsBalance() == null && user.getCreatedAt() != null, "snake_case, Unicode, NULL and timestamp mapping");
            MoneyTotal money = Database.one(MoneyTotal.class, c, "SELECT CAST(123456.78 AS decimal(15,2)) total");
            check(money.getTotal().equals(new BigDecimal("123456.78")), "money retains exact decimal precision");
            check(Database.one(User.class, c, "SELECT id FROM users WHERE 1=0") == null, "missing row returns null");
            check(Database.one(CountResult.class, c, "SELECT CAST(3 AS int) n").getN() == 3L, "SQL numeric conversion is exact");
            try {
                Database.one(User.class, c, "SELECT 1 unexpected_column");
                throw new AssertionError("Unknown column must fail");
            } catch (SQLException expected) { check(true, "unknown column rejected rather than silently lost"); }
            try {
                Database.one(CountResult.class, c, "SELECT CAST(1.25 AS decimal(5,2)) n");
                throw new AssertionError("Fractional count must fail");
            } catch (SQLException expected) { check(true, "lossy numeric conversion rejected"); }
            Address first = Database.one(Address.class, c, "SELECT CAST(1 AS bigint) id,N'Địa chỉ' address_line");
            Address second = Database.one(Address.class, c, "SELECT CAST(1 AS bigint) id,N'Địa chỉ' address_line");
            check(first.toString().equals(second.toString()), "checkout address fingerprint is independent of Java object identity");
            second.setAddressLine("Địa chỉ mới");
            check(!first.toString().equals(second.toString()), "checkout address fingerprint detects edits");
        }
        System.out.println(checks + " model mapping checks passed");
    }
}
