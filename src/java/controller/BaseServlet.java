package controller;

import jakarta.servlet.http.*;
import jakarta.servlet.*;
import model.SessionUser;
import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {

    protected long uid(HttpServletRequest r) {
        return ((SessionUser) r.getSession().getAttribute("user")).getId();
    }

    protected void view(HttpServletRequest r, HttpServletResponse s, String name) throws ServletException, IOException {
        String path = switch (name) {
            case "admin" ->
                "admin/admin";
            case "auth" ->
                "auth/auth";
            case "seller" ->
                "seller/seller";
            case "checkout", "orders", "account", "cart", "compare", "product", "products", "shops" ->
                "customer/" + name;
            case "fulfillment", "promotions", "wallet", "refunds", "payment" -> name;
            case "home" ->
                "home";
            default ->
                throw new IllegalArgumentException("Trang không hợp lệ: " + name);
        };
        r.getRequestDispatcher("/WEB-INF/views/" + path + ".jsp").forward(r, s);
    }

    protected void redirect(HttpServletRequest r, HttpServletResponse s, String path) throws IOException {
        s.sendRedirect(r.getContextPath() + path);
    }

    protected String error(Exception e) {
        getServletContext().log("Freshora request failed", e);
        return e instanceof IllegalArgumentException && e.getMessage() != null && e.getMessage().matches("(?s).*[À-ỹ].*") ? e.getMessage() : "Không thể lưu dữ liệu. Kiểm tra dữ liệu trùng, ràng buộc hoặc thử lại.";
    }
}
