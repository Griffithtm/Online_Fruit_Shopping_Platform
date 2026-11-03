package controller;

import model.*;

import config.DBConnect;
import dao.Database;
import service.NotificationService;
import service.Validation;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.sql.*;
import java.io.*;
import java.util.*;

@WebServlet("/admin")
public class AdminServlet extends BaseServlet {

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try (Connection c = new DBConnect().getConnection()) {
            r.setAttribute("users", Database.rows(User.class, c, "SELECT id,email,full_name,role,status FROM users ORDER BY id"));
            // Restore the legacy review queue, now containing only seller resubmissions.
            r.setAttribute("pending", Database.rows(ListingDetails.class, c,
                "SELECT l.*,s.store_name,JSON_VALUE(l.image_urls,'$[0]') AS image FROM listings l JOIN stores s ON s.id=l.store_id WHERE l.listing_type='PRODUCT' AND l.status='PENDING_REVIEW' ORDER BY l.updated_at ASC,l.id ASC"));
            view(r, s, "admin");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try (Connection c = new DBConnect().getConnection()) {
            long id = Validation.id(r.getParameter("id"));
            String a = r.getParameter("action");
            if ("role".equals(a)) {
                String role = r.getParameter("role");
                if (id == uid(r) || !Set.of("CUSTOMER", "SELLER", "DELIVERY_STAFF", "ADMIN").contains(role)) {
                    throw new IllegalArgumentException("Không được đổi quyền bản thân hoặc vai trò không hợp lệ.");
                }
                if (role.equals("SELLER")) throw new IllegalArgumentException("Người bán đăng ký riêng để tạo đầy đủ cửa hàng.");
                if (Database.one(Store.class, c, "SELECT id FROM stores WHERE owner_id=?", id) != null)
                    throw new IllegalArgumentException("Chủ cửa hàng phải giữ vai trò người bán.");
                Database.update(c, "UPDATE users SET role=?,updated_at=SYSUTCDATETIME() WHERE id=?", role, id);
            } else if ("review".equals(a)) {
                String decision = r.getParameter("status");
                String reason = r.getParameter("reason");
                c.setAutoCommit(false);
                try {
                    Store owner = Database.one(Store.class, c,
                        "SELECT s.owner_id AS owner_id FROM listings l JOIN stores s ON s.id=l.store_id WHERE l.id=? AND l.listing_type='PRODUCT'", id);
                    if (owner == null) throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
                    if ("APPROVED".equals(decision)) {
                        if (Database.update(c, "UPDATE listings SET status='PUBLISHED',rejection_reason=NULL,updated_at=SYSUTCDATETIME() WHERE id=? AND status='PENDING_REVIEW'", id) != 1)
                            throw new IllegalArgumentException("Sản phẩm không còn chờ duyệt.");
                        new NotificationService().create(c, owner.getOwnerId(), "LISTING_MODERATION", "Sản phẩm đã được duyệt",
                            "Sản phẩm #" + id + " đã được duyệt và công khai trở lại.", "/seller", false, "listing-approved:" + id + ":" + System.currentTimeMillis());
                    } else if ("REJECTED".equals(decision)) {
                        String why = Validation.text(reason, 500);
                        if (Database.update(c, "UPDATE listings SET status='SUSPENDED',rejection_reason=?,updated_at=SYSUTCDATETIME() WHERE id=? AND status='PENDING_REVIEW'", why, id) != 1)
                            throw new IllegalArgumentException("Sản phẩm không còn chờ duyệt.");
                        new NotificationService().create(c, owner.getOwnerId(), "LISTING_MODERATION", "Yêu cầu đăng lại bị từ chối",
                            "Sản phẩm #" + id + " chưa được duyệt. Lý do: " + why, "/seller", false, "listing-resubmit-rejected:" + id + ":" + System.currentTimeMillis());
                    } else throw new IllegalArgumentException("Quyết định duyệt không hợp lệ.");
                    c.commit();
                } catch (Exception ex) { c.rollback(); throw ex; }
            } else throw new IllegalArgumentException("Thao tác không hợp lệ.");
            redirect(r, s, "/admin?saved=1");
        } catch (Exception e) {
            r.setAttribute("error", error(e));
            doGet(r, s);
        }
    }
}
