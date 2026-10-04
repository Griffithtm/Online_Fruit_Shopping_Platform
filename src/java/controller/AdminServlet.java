package controller;

import config.DBConnect;
import dao.Database;
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
            r.setAttribute("pending", Database.rows(c, "SELECT l.*,s.store_name FROM listings l JOIN stores s ON s.id=l.store_id WHERE l.status='PENDING_REVIEW' ORDER BY l.id"));
            r.setAttribute("users", Database.rows(c, "SELECT id,email,full_name,role,status FROM users ORDER BY id"));
            view(r, s, "admin");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try (Connection c = new DBConnect().getConnection()) {
            long id = Validation.id(r.getParameter("id"));
            String a = r.getParameter("action");
            if (a.equals("review")) {
                String state = r.getParameter("status");
                if (!Set.of("APPROVED", "REJECTED").contains(state)) {
                    throw new IllegalArgumentException("Invalid status.");
                }
                String reason = state.equals("REJECTED") ? Validation.text(r.getParameter("reason"), 500) : null;
                if (Database.update(c, "UPDATE listings SET status=?,rejection_reason=?,updated_at=SYSUTCDATETIME() WHERE id=? AND status='PENDING_REVIEW'", state, reason, id) != 1) {
                    throw new IllegalArgumentException("This listing has already been reviewed.");
                }
            } else if (a.equals("role")) {
                String role = r.getParameter("role");
                if (id == uid(r) || !Set.of("CUSTOMER", "SELLER", "DELIVERY_STAFF", "ADMIN").contains(role)) {
                    throw new IllegalArgumentException("You cannot change your own role or assign an invalid role.");
                }
                if (role.equals("SELLER")) {
                    throw new IllegalArgumentException("Sellers must register separately to create their stores.");
                }
                if (Database.one(c, "SELECT id FROM stores WHERE owner_id=?", id) != null) {
                    throw new IllegalArgumentException("Store owners must retain the Seller role.");
                }
                Database.update(c, "UPDATE users SET role=?,updated_at=SYSUTCDATETIME() WHERE id=?", role, id);
            } else {
                throw new IllegalArgumentException("Invalid action.");
            }
            redirect(r, s, "/admin?saved=1");
        } catch (Exception e) {
            r.setAttribute("error", error(e));
            doGet(r, s);
        }
    }
}
