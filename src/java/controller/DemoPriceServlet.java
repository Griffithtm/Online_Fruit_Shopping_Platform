package controller;

import dao.PriceDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/demo/prices")
public class DemoPriceServlet extends HttpServlet {

    // Local classroom demo only. Replace with the authenticated seller from session.
    private static final String DEMO_SELLER = "seller@freshora.demo";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        if (session.getAttribute("priceToken") == null) {
            session.setAttribute("priceToken", UUID.randomUUID().toString());
        }
        request.setAttribute("message", session.getAttribute("priceMessage"));
        session.removeAttribute("priceMessage");
        try {
            request.setAttribute("variants", new PriceDAO().findBySeller(DEMO_SELLER));
        } catch (SQLException e) {
            log("Cannot load demo prices", e);
            request.setAttribute("message", "Không thể kết nối cơ sở dữ liệu. Vui lòng kiểm tra cấu hình và nhật ký máy chủ.");
        }
        request.getRequestDispatcher("/WEB-INF/views/seller/demo-prices.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("priceToken") == null
                || !session.getAttribute("priceToken").equals(request.getParameter("token"))) {
            response.sendError(403);
            return;
        }
        String message;
        try {
            int id = Integer.parseInt(request.getParameter("variantId"));
            String input = request.getParameter("price");
            if (id <= 0 || input == null || !input.matches("[0-9]{1,16}(\\.[0-9]{1,2})?")) {
                throw new IllegalArgumentException();
            }
            BigDecimal price = new BigDecimal(input);
            if (price.signum() <= 0) {
                throw new IllegalArgumentException();
            }
            boolean updated = new PriceDAO().updatePrice(id, price, DEMO_SELLER);
            message = updated ? "Đã cập nhật giá thành công." : "Biến thể không tồn tại hoặc không thuộc cửa hàng này.";
        } catch (IllegalArgumentException e) {
            message = "Nhập giá lớn hơn 0, tối đa 16 chữ số phần nguyên và 2 chữ số thập phân.";
        } catch (SQLException e) {
            log("Cannot update demo price", e);
            message = "Update failed. Check the database and Tomcat log.";
        }
        session.setAttribute("priceMessage", message);
        response.sendRedirect(request.getContextPath() + "/demo/prices");
    }
}
