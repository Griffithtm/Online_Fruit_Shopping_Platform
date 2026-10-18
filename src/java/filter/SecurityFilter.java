package filter;

import model.*;

import config.DBConnect;
import dao.Database;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;

@WebFilter("/*")
public class SecurityFilter implements Filter {

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) request;
        HttpServletResponse s = (HttpServletResponse) response;
        r.setCharacterEncoding("UTF-8");
        s.setCharacterEncoding("UTF-8");
        s.setHeader("X-Content-Type-Options", "nosniff");
        s.setHeader("X-Frame-Options", "DENY");
        s.setHeader("Referrer-Policy", "same-origin");
        String path = r.getServletPath();
        if (path.startsWith("/assets/")) {
            chain.doFilter(r, s);
            return;
        }
        s.setHeader("Cache-Control", "no-store");
        HttpSession session = r.getSession();
        if (session.getAttribute("csrf") == null) {
            session.setAttribute("csrf", UUID.randomUUID().toString());
        }
        SessionUser u = (SessionUser) session.getAttribute("user");
        if (u != null) {
            try (Connection c = new DBConnect().getConnection()) {
                User db = Database.one(User.class, c, "SELECT id,full_name,role,status,password_hash FROM users WHERE id=?", u.getId());
                if (db == null || (!"ACTIVE".equals(db.getStatus()) || !u.credentialsMatch(db.getPasswordHash()))) {
                    session.removeAttribute("user");
                    u = null;
                } else {
                    u = new SessionUser(u.getId(), db.getFullName(), db.getRole(),db.getPasswordHash());
                    session.setAttribute("user", u);
                }
            } catch (Exception e) {
                throw new ServletException("Cơ sở dữ liệu tạm thời không khả dụng", e);
            }
        }
        if (path.startsWith("/demo/")) {
            s.sendError(404);
            return;
        }
        boolean privatePath = path.startsWith("/account") || path.startsWith("/seller") || path.startsWith("/admin") || path.startsWith("/cart") || path.equals("/checkout") || path.equals("/orders") || path.equals("/wallet") || path.equals("/refunds") || path.equals("/delivery") || path.equals("/payment/start");
        if (privatePath && u == null) {
            s.sendRedirect(r.getContextPath() + "/login");
            return;
        }
        if ((path.startsWith("/seller") && !"SELLER".equals(u == null ? null : u.getRole())) || (path.startsWith("/admin") && !"ADMIN".equals(u == null ? null : u.getRole()))) {
            s.sendError(403);
            return;
        }
        if(path.equals("/delivery") && (u==null || !"DELIVERY_STAFF".equals(u.getRole()))){s.sendError(403);return;}
        if ((path.startsWith("/cart") || path.equals("/checkout") || path.equals("/orders") || path.equals("/wallet") || path.equals("/refunds") || path.equals("/payment/start")) && u != null && !Set.of("CUSTOMER", "SELLER").contains(u.getRole())) {
            s.sendError(403);
            return;
        }
        if ("POST".equals(r.getMethod()) && !Objects.equals(session.getAttribute("csrf"), r.getParameter("csrf"))) {
            s.sendError(403, "Phiên gửi biểu mẫu không hợp lệ. Vui lòng tải lại trang.");
            return;
        }
        chain.doFilter(r, s);
    }
}
