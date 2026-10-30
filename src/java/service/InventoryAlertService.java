package service;

import config.DBConnect;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodic inventory alerts for Freshora sellers.
 * Low-stock threshold: fewer than 10 sellable units.
 * Expiry warning window: 3 days, including today only for batches not yet expired.
 */
public class InventoryAlertService {
    private static final Logger LOG = Logger.getLogger(InventoryAlertService.class.getName());
    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final int EXPIRY_WARNING_DAYS = 1;

    private final NotificationService notifications = new NotificationService();

    public int checkLowStock() throws SQLException {
        int createdOrFound = 0;
        String sql =
            "SELECT u.id AS seller_id, v.id AS variant_id, l.id AS listing_id, " +
            "l.title, v.option_name, v.unit, COALESCE(stock.available_qty, 0) AS available_qty " +
            "FROM dbo.variants v " +
            "JOIN dbo.listings l ON l.id = v.listing_id " +
            "JOIN dbo.stores s ON s.id = l.store_id " +
            "JOIN dbo.users u ON u.id = s.owner_id " +
            "LEFT JOIN ( " +
            "  SELECT variant_id, SUM(CONVERT(BIGINT, remaining_qty - reserved_qty)) AS available_qty " +
            "  FROM dbo.batches " +
            "  WHERE status = 'AVAILABLE' AND expiry_date > CONVERT(date, SYSUTCDATETIME()) " +
            "    AND remaining_qty > reserved_qty " +
            "  GROUP BY variant_id " +
            ") stock ON stock.variant_id = v.id " +
            "WHERE l.listing_type = 'PRODUCT' AND l.status = 'PUBLISHED' " +
            "AND s.status = 'ACTIVE' AND u.status = 'ACTIVE' AND v.status = 'ACTIVE' " +
            "AND u.role = 'SELLER' AND COALESCE(stock.available_qty, 0) < ?";

        LocalDate utcDate;
        try (Connection c = new DBConnect().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, LOW_STOCK_THRESHOLD);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long sellerId = rs.getLong("seller_id");
                    long variantId = rs.getLong("variant_id");
                    long listingId = rs.getLong("listing_id");
                    String title = rs.getString("title");
                    String option = rs.getString("option_name");
                    String unit = rs.getString("unit");
                    long qty = rs.getLong("available_qty");
                    utcDate = LocalDate.now(java.time.ZoneOffset.UTC);

                    String label = title + (option == null || option.isBlank() ? "" : " - " + option);
                    String content = "Sản phẩm \"" + label + "\" chỉ còn " + qty +
                        (unit == null || unit.isBlank() ? " đơn vị" : " " + unit) +
                        " có thể bán. Vui lòng kiểm tra và bổ sung tồn kho.";
                    String dedup = "LOW_STOCK:" + variantId + ":" + utcDate;
                    notifications.notifyUser(sellerId, "LOW_STOCK", "Cảnh báo tồn kho thấp",
                        content, "/seller", false, dedup);
                    createdOrFound++;
                }
            }
        }
        return createdOrFound;
    }

    public int checkExpiringBatches() throws SQLException {
        int createdOrFound = 0;
        String sql =
            "SELECT u.id AS seller_id, b.id AS batch_id, b.batch_no, b.expiry_date, " +
            "b.remaining_qty, b.reserved_qty, l.id AS listing_id, l.title, " +
            "v.option_name, v.unit " +
            "FROM dbo.batches b " +
            "JOIN dbo.variants v ON v.id = b.variant_id " +
            "JOIN dbo.listings l ON l.id = v.listing_id " +
            "JOIN dbo.stores s ON s.id = l.store_id " +
            "JOIN dbo.users u ON u.id = s.owner_id " +
            "WHERE b.status = 'AVAILABLE' AND b.expiry_date > CONVERT(date, SYSUTCDATETIME()) " +
            "AND b.expiry_date <= DATEADD(day, ?, CONVERT(date, SYSUTCDATETIME())) " +
            "AND b.remaining_qty > b.reserved_qty " +
            "AND l.listing_type = 'PRODUCT' AND s.status = 'ACTIVE' " +
            "AND u.status = 'ACTIVE' AND u.role = 'SELLER'";

        try (Connection c = new DBConnect().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, EXPIRY_WARNING_DAYS);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long sellerId = rs.getLong("seller_id");
                    long batchId = rs.getLong("batch_id");
                    long listingId = rs.getLong("listing_id");
                    String batchNo = rs.getString("batch_no");
                    java.sql.Date expiry = rs.getDate("expiry_date");
                    String title = rs.getString("title");
                    String option = rs.getString("option_name");
                    String unit = rs.getString("unit");
                    long available = rs.getLong("remaining_qty") - rs.getLong("reserved_qty");

                    String label = title + (option == null || option.isBlank() ? "" : " - " + option);
                    String content = "Lô " + batchNo + " của sản phẩm \"" + label +
                        "\" còn " + available + (unit == null || unit.isBlank() ? " đơn vị" : " " + unit) +
                        " chưa được giữ và sẽ hết hạn ngày " + expiry +
                        ". Vui lòng ưu tiên xử lý lô hàng này.";
                    String dedup = "BATCH_EXPIRING:" + batchId + ":" + expiry.toLocalDate();
                    notifications.notifyUser(sellerId, "BATCH_EXPIRING", "Lô hàng sắp hết hạn",
                        content, "/seller", false, dedup);
                    createdOrFound++;
                }
            }
        }
        return createdOrFound;
    }

    public void runChecksSafely() {
        try {
            int low = checkLowStock();
            int expiring = checkExpiringBatches();
            LOG.info("Inventory alerts checked: low-stock=" + low + ", expiring-batches=" + expiring);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to run Freshora inventory alert checks", e);
        }
    }
}
