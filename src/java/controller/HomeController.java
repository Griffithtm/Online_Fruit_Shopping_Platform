package controller;

import service.CatalogService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;

@WebServlet("/home")
public class HomeController extends BaseServlet {

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            CatalogService c = new CatalogService();
            r.setAttribute("products", c.products(null, null, null, null, null, null, null, false));
            r.setAttribute("categories", c.categories());
            view(r, s, "home");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
