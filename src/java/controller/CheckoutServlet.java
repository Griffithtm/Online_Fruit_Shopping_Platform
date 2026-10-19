package controller;

import model.*;

import service.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.util.*;
import util.Messages;

@WebServlet(urlPatterns = {"/checkout", "/orders"})
public class CheckoutServlet extends BaseServlet {

    private int parsePoints(String value) {
        try {
            int n = value == null || value.isBlank() ? 0 : Integer.parseInt(value.trim());
            if (n < 0) {
                throw new NumberFormatException();
            }
            return n;
        } catch (NumberFormatException e) {
            throw Messages.error(Messages.MSG04, "điểm thưởng");
        }
    }
    private final CheckoutService service = new CheckoutService();

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            if (r.getServletPath().equals("/orders")) {
                // BA-18: MSG39/MSG40 after placement; only for the customer's own order.
                String placed = r.getParameter("placed");
                if (placed != null) {
                    try {
                        String notice = service.placedNotice(uid(r), Validation.id(placed));
                        if (notice != null) {
                            r.setAttribute("message", notice);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                Object flash = r.getSession().getAttribute("flash");
                if (flash != null) {
                    r.getSession().removeAttribute("flash");
                    r.setAttribute("message", flash);
                }
                List<OrderSummary> orders = service.orders(uid(r));
                List<OrderItemDetails> items = service.items(uid(r));
                // ThanhNC BA-26: cancellation deadline in Vietnam time and MSG06 confirmations prepared for the page.
                Map<Long, String> deadlines = new HashMap<>(), orderConfirms = new HashMap<>(), sellerConfirms = new HashMap<>();
                for (OrderSummary o : orders) {
                    String deadline = util.VietnamTime.format(o.getCancelDeadlineAt(), "dd/MM/yyyy HH:mm");
                    if (deadline != null) {
                        deadlines.put(o.getId(), deadline);
                    }
                    orderConfirms.put(o.getId(), Messages.get(Messages.MSG06, "hủy", "đơn hàng " + o.getOrderCode()));
                }
                for (OrderItemDetails i : items) {
                    sellerConfirms.put(i.getSellerOrderId(), Messages.get(Messages.MSG06, "hủy", "đơn hàng của cửa hàng " + i.getStoreName()));
                }
                r.setAttribute("orders", orders);
                r.setAttribute("payments", service.payments(uid(r)));
                r.setAttribute("items", items);
                r.setAttribute("cancelDeadlines", deadlines);
                r.setAttribute("orderCancelConfirms", orderConfirms);
                r.setAttribute("sellerCancelConfirms", sellerConfirms);
                view(r, s, "orders");
                return;
            }
            render(r, s, r.getParameter("address"), r.getParameter("voucher"), r.getParameter("points"), r.getParameter("method"));
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /** Shows the checkout form with the given choices and, when an address is chosen, a fresh quote. */
    private void render(HttpServletRequest r, HttpServletResponse s, String address, String voucher, String points, String method) throws Exception {
        r.setAttribute("addresses", new AccountService().addresses(uid(r)));
        Map<String, Object> form = new HashMap<>();
        form.put("voucher", voucher);
        form.put("points", points);
        form.put("method", method == null ? "COD" : method);
        r.setAttribute("form", form);
        if (address != null) {
            try {
                long addressId;
                try {
                    addressId = Validation.id(address);
                } catch (IllegalArgumentException e) {
                    throw Messages.error(Messages.MSG12);
                }
                r.setAttribute("addressId", addressId);
                int p = parsePoints(points);
                String m = method == null ? "COD" : method;
                CheckoutQuote q = service.quote(uid(r), addressId, voucher, p, m);
                String key = UUID.randomUUID().toString();
                HttpSession session = r.getSession();
                session.setAttribute("checkoutKey", key);
                session.setAttribute("checkoutQuote", q.getFingerprint());
                session.setAttribute("checkoutAddress", addressId);
                session.setAttribute("checkoutVoucher", voucher);
                session.setAttribute("checkoutPoints", p);
                session.setAttribute("checkoutMethod", q.getMethod());
                r.setAttribute("quote", q);
                r.setAttribute("checkoutKey", key);
            } catch (IllegalArgumentException e) {
                if (r.getAttribute("error") == null) {
                    r.setAttribute("error", e.getMessage());
                }
            }
        }
        view(r, s, "checkout");
    }

    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        if (r.getServletPath().equals("/orders")) {
            try {
                String action = r.getParameter("action");
                String cancelled;
                if ("cancel".equals(action)) {
                    // ThanhNC BA-26: one seller order; "status" is what the page showed (MSG07 if it changed).
                    cancelled = new FulfillmentService().cancelByCustomer(uid(r), ((model.SessionUser) r.getSession().getAttribute("user")).getRole(), Validation.id(r.getParameter("id")), r.getParameter("status"));
                } else if ("cancelOrder".equals(action)) {
                    // ThanhNC BA-20: the whole order while it awaits online payment.
                    cancelled = String.join(", ", service.cancelAwaiting(uid(r), Validation.id(r.getParameter("id"))));
                } else {
                    throw new IllegalArgumentException("Thao tác không hợp lệ.");
                }
                r.getSession().setAttribute("flash", Messages.get(Messages.MSG48, cancelled));
                redirect(r, s, "/orders");
                return;
            } catch (Exception e) {
                r.setAttribute("error", error(e));
                doGet(r, s);
                return;
            }
        }
        HttpSession session = r.getSession();
        Long address = (Long) session.getAttribute("checkoutAddress");
        String voucher = (String) session.getAttribute("checkoutVoucher");
        Integer points = (Integer) session.getAttribute("checkoutPoints");
        String method = (String) session.getAttribute("checkoutMethod");
        try {
            String key = r.getParameter("key");
            // A missing or replaced review (session expired, another tab) must be reviewed again (MSG07).
            if (key == null || !key.equals(session.getAttribute("checkoutKey")) || address == null || points == null || method == null) {
                throw Messages.error(Messages.MSG07);
            }
            long id = service.place(uid(r), address, key, (String) session.getAttribute("checkoutQuote"), voucher, points, method);
            redirect(r, s, "/orders?placed=" + id);
        } catch (Exception e) {
            r.setAttribute("error", error(e));
            // BA-18: keep the customer's choices; the page is quoted again with them.
            try {
                render(r, s, address == null ? null : address.toString(), voucher, points == null ? null : points.toString(), method);
            } catch (Exception again) {
                throw new ServletException(again);
            }
        }
    }
}
