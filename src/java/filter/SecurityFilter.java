package filter;

import config.DBConnect;
import dao.Database;
import model.SessionUser;
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
                Map<String, Object> db = Database.one(c, "SELECT id,full_name,role,status,password_hash FROM users WHERE id=?", u.getId());
                if (db == null || (!"ACTIVE".equals(db.get("status")) || !u.credentialsMatch((String)db.get("password_hash")))) {
                    session.removeAttribute("user");
                    u = null;
                } else {
                    u = new SessionUser(u.getId(), (String) db.get("full_name"), (String) db.get("role"),(String)db.get("password_hash"));
                    session.setAttribute("user", u);
                }
            } catch (Exception e) {
                throw new ServletException("Database unavailable", e);
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
            s.sendError(403, "Invalid form token. Reload the page.");
            return;
        }
        chain.doFilter(r, s);
    }
}
