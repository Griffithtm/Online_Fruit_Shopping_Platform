package controller;

import model.*;

import service.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.util.*;
import java.math.BigDecimal;

@WebServlet("/cart")
public class CartServlet extends BaseServlet {

    private final CartService service = new CartService();

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            List<CartLine> lines = service.lines(uid(r));
            Map<String, BigDecimal> totals = new LinkedHashMap<>();
            BigDecimal total = BigDecimal.ZERO;
            for (CartLine l : lines) {
                BigDecimal n = l.getLineTotal();
                total = total.add(n);
                totals.merge(l.getStoreName(), n, BigDecimal::add);
            }
            r.setAttribute("lines", lines);
            r.setAttribute("totals", totals);
            r.setAttribute("total", total);
            view(r, s, "cart");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            String a = r.getParameter("action");
            service.change(uid(r), a, Validation.id(r.getParameter("id")), a.equals("remove") ? 1 : Validation.quantity(r.getParameter("quantity")));
            redirect(r, s, "/cart");
        } catch (Exception e) {
            r.setAttribute("error", error(e));
            doGet(r, s);
        }
    }
}
