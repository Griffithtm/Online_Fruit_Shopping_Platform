package controller;

import model.*;

import service.*;
import config.DBConnect;
import dao.Database;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.*;
import java.util.*;
import java.sql.*;

// ThanhNC BA-35: "/delivery" moved to DeliveryServlet (multipart photo upload, evidence route).
@WebServlet(urlPatterns = {"/seller/orders", "/admin/orders", "/seller/promotions", "/admin/promotions", "/wallet", "/refunds", "/admin/refunds"})
public class CommerceServlet extends BaseServlet {

    private String role(HttpServletRequest r) {
        return ((SessionUser) r.getSession().getAttribute("user")).getRole();
    }

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            String path = r.getServletPath();
            if (path.endsWith("promotions")) {
                r.setAttribute("promotions", new PromotionService().list(uid(r), role(r)));
                view(r, s, "promotions");
            } else if (path.equals("/wallet")) {
                r.setAttribute("wallet", new LoyaltyService().wallet(uid(r)));
                view(r, s, "wallet");
            } else if (path.endsWith("refunds")) {
                r.setAttribute("refunds", new FinanceService().list(uid(r), path.startsWith("/admin")));
                view(r, s, "refunds");
            } else {
                FulfillmentService fulfillment = new FulfillmentService();
                if (role(r).equals("SELLER")) {
                    r.setAttribute("pickupAddresses", new AccountService().addresses(uid(r)));
                    // ThanhNC BA-33: status tabs and paging (10 per page); allocations only for the orders on this page.
                    String tab = dao.OrderDAO.SELLER_TABS.containsKey(r.getParameter("status")) ? r.getParameter("status") : "";
                    int size = 10, pages = (int) Math.max(1, (fulfillment.sellerCount(uid(r), tab) + size - 1) / size), page = 1;
                    try {
                        page = Math.min(pages, Math.max(1, Integer.parseInt(r.getParameter("page"))));
                    } catch (NumberFormatException ignored) {
                    }
                    List<SellerOrderDetails> rows = fulfillment.sellerPage(uid(r), tab, page, size);
                    r.setAttribute("sellerOrders", rows);
                    r.setAttribute("allocations", fulfillment.sellerAllocations(uid(r), rows.stream().map(SellerOrderDetails::getId).toList()));
                    r.setAttribute("sellerTab", tab);
                    r.setAttribute("page", page);
                    r.setAttribute("pages", pages);
                } else {
                    r.setAttribute("sellerOrders", fulfillment.list(uid(r), role(r)));
                    r.setAttribute("allocations", fulfillment.allocations(uid(r), role(r)));
                }
                if (role(r).equals("ADMIN"))try (Connection c = new DBConnect().getConnection()) {
                    r.setAttribute("couriers", Database.rows(User.class, c, "SELECT id,full_name FROM users WHERE role='DELIVERY_STAFF' AND status='ACTIVE'"));
                    // ThanhNC BA-31: seller orders whose "ready" was blocked in the last 24 hours.
                    r.setAttribute("readyBlocked", fulfillment.readyBlockedLastDay(uid(r)));
                }
                view(r, s, "fulfillment");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            String path = r.getServletPath(), action = r.getParameter("action");
            if (path.endsWith("promotions")) {
                PromotionForm f = new PromotionForm();
                f.setId(r.getParameter("id"));
                f.setAction(r.getParameter("action"));
                f.setState(r.getParameter("state"));
                f.setName(r.getParameter("name"));
                f.setDiscountType(r.getParameter("discountType"));
                f.setValue(r.getParameter("value"));
                f.setBudget(r.getParameter("budget"));
                f.setMinimum(r.getParameter("minimum"));
                f.setLimit(r.getParameter("limit"));
                f.setPerCustomer(r.getParameter("perCustomer"));
                f.setStart(r.getParameter("start"));
                f.setEnd(r.getParameter("end"));
                f.setCode(r.getParameter("code"));
                f.setListings(r.getParameter("listings"));
                new PromotionService().save(uid(r), role(r), f);
            } else if (path.endsWith("refunds")) {
                if (path.startsWith("/admin")) {
                    if ("reconcile".equals(action)) {
                        new OnlineRefundService().confirm(uid(r), Validation.id(r.getParameter("id")), r.getParameter("signedResponse"));
                    } else if ("online".equals(action)) {
                        new OnlineRefundService().execute(uid(r), Validation.id(r.getParameter("id")), r.getRemoteAddr());
                    } else {
                        new FinanceService().decide(uid(r), Validation.id(r.getParameter("id")), action, r.getParameter("reason"));
                    }
                } else {
                    if (!"request".equals(action)) {
                        throw new IllegalArgumentException("Thao tác không hợp lệ.");
                    }
                    new FinanceService().request(uid(r), Validation.id(r.getParameter("id")), r.getParameter("reason"));
                }
            } else if (path.equals("/wallet")) {
                throw new IllegalArgumentException("Vui lòng sử dụng điểm thưởng tại bước thanh toán.");
            } else if ("pickupAddress".equals(action)) {
                try (Connection c = new DBConnect().getConnection()) {
                    FulfillmentService.role(c, uid(r), "SELLER");
                    long aid = Validation.id(r.getParameter("address"));
                    if (Database.update(c, "UPDATE stores SET pickup_address_id=? WHERE owner_id=? AND EXISTS(SELECT 1 FROM addresses WHERE id=? AND user_id=?)", aid, uid(r), aid, uid(r)) != 1) {
                        throw new IllegalArgumentException("Vui lòng chọn địa chỉ lấy hàng thuộc tài khoản của bạn.");
                    }
                }
            } else if ("settle".equals(action)) {
                new FinanceService().settle(uid(r), Validation.id(r.getParameter("id")));
            } else if ("ready".equals(action) && "SELLER".equals(role(r))) {
                // ThanhNC BA-32: packing confirmation (required) and the Seller's storage note.
                new FulfillmentService().ready(uid(r), Validation.id(r.getParameter("id")), "on".equals(r.getParameter("packed")), r.getParameter("packingNote"));
            } else {
                String courier = r.getParameter("courier");
                new FulfillmentService().change(uid(r), role(r), Validation.id(r.getParameter("id")), action, r.getParameter("detail"), courier == null || courier.isBlank() ? null : Validation.id(courier), "on".equals(r.getParameter("collected")));
            }
            redirect(r, s, r.getServletPath() + "?saved=1");
        } catch (Exception e) {
            r.setAttribute("error", error(e));
            doGet(r, s);
        }
    }
}
