package controller;

import service.AccountService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;

@WebServlet("/account")
public class AccountServlet extends BaseServlet {

    private final AccountService service = new AccountService();

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            r.setAttribute("profile", service.profile(uid(r)));
            r.setAttribute("addresses", service.addresses(uid(r)));
            view(r, s, "account");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            String a = r.getParameter("action");
            if ("profile".equals(a)) {
                service.profile(uid(r), r.getParameter("name"), r.getParameter("phone"));
            } else if ("password".equals(a)) {
                service.password(uid(r), r.getParameter("old"), r.getParameter("password"), r.getParameter("confirm"));
                r.getSession().invalidate();
                redirect(r,s,"/login?success=1");
                return;
            } else if (java.util.Set.of("saveAddress", "deleteAddress", "defaultAddress").contains(a)) {
                service.address(uid(r), a, r.getParameter("id"), r.getParameter("name"), r.getParameter("phone"), r.getParameter("line"), r.getParameter("ward"), r.getParameter("district"), r.getParameter("city"), "on".equals(r.getParameter("primary")));
            } else {
                throw new IllegalArgumentException("Invalid action.");
            }
            redirect(r, s, "/account?saved=1");
        } catch (Exception e) {
            r.setAttribute("error", error(e));
            doGet(r, s);
        }
    }
}
