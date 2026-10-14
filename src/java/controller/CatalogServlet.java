package controller;

import model.*;

import service.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.util.*;

@WebServlet(urlPatterns = {"/products", "/product", "/shops", "/shop", "/compare"})
public class CatalogServlet extends BaseServlet {

    private final CatalogService service = new CatalogService();

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            r.setAttribute("categories", service.categories());
            r.setAttribute("shops", service.shops());
            if (r.getServletPath().equals("/compare")) r.setAttribute("deliveryAreas",service.deliveryAreas());
            String p = r.getServletPath();
            if (p.equals("/shops")) {
                r.setAttribute("shops", service.shops());
                view(r, s, "shops");
                return;
            }
            if (p.equals("/product")) {
                long id = Validation.id(r.getParameter("id"));
                ListingDetails product = service.product(id);
                if (product == null) {
                    s.sendError(404);
                    return;
                }
                r.setAttribute("product", product);
                r.setAttribute("variants", service.variants(id));
                r.setAttribute("similar", service.products(null, product.getCategoryId().toString(), null, null, null, null, "priceDesc", false));
                view(r, s, "product");
                return;
            }
            String store = r.getParameter("store");
            if (p.equals("/shop")) {
                store = r.getParameter("id");
                Store shop = service.shop(Validation.id(store));
                if (shop == null) {
                    s.sendError(404);
                    return;
                }
                r.setAttribute("shop", shop);
            }
            r.setAttribute("products", service.products(r.getParameter("q"), r.getParameter("category"), store, r.getParameter("min"), r.getParameter("max"), r.getParameter("rating"), r.getParameter("sort"), "1".equals(r.getParameter("promo")), r.getParameter("availability")));
            view(r, s, p.equals("/compare") ? "compare" : "products");
        } catch (IllegalArgumentException e) {
            r.setAttribute("error", e.getMessage());
            r.setAttribute("products", List.of());
            view(r, s, r.getServletPath().equals("/compare") ? "compare" : "products");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
