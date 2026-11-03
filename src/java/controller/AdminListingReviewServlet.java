package controller;

import config.DBConnect;
import dao.Database;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.ListingDetails;
import model.Store;
import model.User;
import service.NotificationService;
import service.Validation;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;

@WebServlet("/admin/listing-reviews")
public class AdminListingReviewServlet extends BaseServlet {
    private static final int PAGE_SIZE = 20;
    private static final Set<String> STATUSES = Set.of("ALL", "PENDING_REVIEW", "PUBLISHED", "SUSPENDED", "HIDDEN", "DRAFT", "RETIRED");

    @Override
    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try (Connection c = new DBConnect().getConnection()) {
            String status = Optional.ofNullable(r.getParameter("status")).orElse("PUBLISHED").trim().toUpperCase(Locale.ROOT);
            if (!STATUSES.contains(status)) status = "PUBLISHED";
            String storeText = r.getParameter("store");
            Long storeId = storeText == null || storeText.isBlank() ? null : Validation.id(storeText);
            String keyword = r.getParameter("q");
            keyword = keyword == null ? "" : keyword.trim();
            if (keyword.length() > 100) keyword = keyword.substring(0, 100);
            int page = 1;
            try { page = Math.max(1, Integer.parseInt(r.getParameter("page"))); } catch (Exception ignored) {}

            String where = " WHERE l.listing_type='PRODUCT' ";
            List<Object> args = new ArrayList<>();
            if (!"ALL".equals(status)) { where += " AND l.status=? "; args.add(status); }
            if (storeId != null) { where += " AND l.store_id=? "; args.add(storeId); }
            if (!keyword.isBlank()) { where += " AND (l.title LIKE ? OR CONVERT(varchar(20),l.id)=?) "; args.add("%" + keyword + "%"); args.add(keyword); }

            int total;
            try (var ps = c.prepareStatement("SELECT COUNT(*) FROM listings l" + where)) {
                for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
                try (var rs = ps.executeQuery()) { rs.next(); total = rs.getInt(1); }
            }
            int pages = Math.max(1, (int)Math.ceil(total / (double)PAGE_SIZE));
            page = Math.min(page, pages);
            List<Object> listArgs = new ArrayList<>(args);
            listArgs.add(PAGE_SIZE); listArgs.add((page - 1) * PAGE_SIZE);
            String sql = "SELECT l.*,s.store_name,JSON_VALUE(l.image_urls,'$[0]') AS image FROM listings l JOIN stores s ON s.id=l.store_id" + where + " ORDER BY l.updated_at DESC,l.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            // SQL Server OFFSET parameters are offset then page size.
            listArgs.set(listArgs.size()-2, (page - 1) * PAGE_SIZE);
            listArgs.set(listArgs.size()-1, PAGE_SIZE);
            r.setAttribute("listings", Database.rows(ListingDetails.class, c, sql, listArgs.toArray()));
            r.setAttribute("stores", Database.rows(Store.class, c, "SELECT id,store_name FROM stores ORDER BY store_name"));
            r.setAttribute("filterStatus", status); r.setAttribute("filterStore", storeText == null ? "" : storeText);
            r.setAttribute("keyword", keyword); r.setAttribute("page", page); r.setAttribute("pages", pages); r.setAttribute("total", total);
            view(r, s, "admin-listing-reviews");
        } catch (Exception e) { throw new ServletException(e); }
    }

    @Override
    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        String action = r.getParameter("action");
        try (Connection c = new DBConnect().getConnection()) {
            long id = Validation.id(r.getParameter("id"));
            if ("suspend".equals(action)) {
                String reason = Validation.text(r.getParameter("reason"), 500);
                c.setAutoCommit(false);
                try {
                    Store owner = Database.one(Store.class, c, "SELECT s.owner_id AS owner_id FROM listings l JOIN stores s ON s.id=l.store_id WHERE l.id=?", id);
                    if (owner == null || Database.update(c, "UPDATE listings SET status='SUSPENDED',rejection_reason=?,updated_at=SYSUTCDATETIME() WHERE id=? AND listing_type='PRODUCT' AND status='PUBLISHED'", reason, id) != 1)
                        throw new IllegalArgumentException("Sản phẩm không còn ở trạng thái đang bán hoặc đã được xử lý.");
                    new NotificationService().create(c, owner.getOwnerId(), "LISTING_MODERATION", "Sản phẩm bị đình chỉ",
                            "Sản phẩm #" + id + " đã bị đình chỉ. Lý do: " + reason, "/seller", false, "listing-suspended:" + id + ":" + System.currentTimeMillis());
                    c.commit();
                } catch (Exception ex) { c.rollback(); throw ex; }
            } else if ("restore".equals(action)) {
                c.setAutoCommit(false);
                try {
                    Store owner = Database.one(Store.class, c, "SELECT s.owner_id AS owner_id FROM listings l JOIN stores s ON s.id=l.store_id WHERE l.id=?", id);
                    if (owner == null || Database.update(c, "UPDATE listings SET status='PUBLISHED',rejection_reason=NULL,updated_at=SYSUTCDATETIME() WHERE id=? AND listing_type='PRODUCT' AND status='SUSPENDED'", id) != 1)
                        throw new IllegalArgumentException("Không thể mở đình chỉ sản phẩm này.");
                    new NotificationService().create(c, owner.getOwnerId(), "LISTING_MODERATION", "Sản phẩm được mở đình chỉ",
                            "Sản phẩm #" + id + " đã được mở đình chỉ và công khai trở lại.", "/seller", false, "listing-restored:" + id + ":" + System.currentTimeMillis());
                    c.commit();
                } catch (Exception ex) { c.rollback(); throw ex; }
            } else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            redirect(r, s, "/admin/listing-reviews?saved=1&status=" + ("restore".equals(action) ? "SUSPENDED" : "PUBLISHED"));
        } catch (Exception e) {
            r.setAttribute("error", error(e)); doGet(r, s);
        }
    }
}
