package controller;

import service.AccountService;
import model.SessionUser;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;

@WebServlet(urlPatterns = {"/login", "/register/customer", "/register/seller", "/logout", "/forgot", "/reset"})
public class AuthServlet extends BaseServlet {

    private final AccountService service = new AccountService();

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        if (r.getServletPath().equals("/logout")) {
            s.sendError(405);
            return;
        }
        r.setAttribute("mode", r.getServletPath());
        view(r, s, "auth");
    }

    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        String p = r.getServletPath();
        try {
            if (p.equals("/logout")) {
                r.getSession().invalidate();
                redirect(r, s, "/home");
                return;
            }
            if (p.equals("/login")) {
                Long next = (Long) r.getSession().getAttribute("nextLogin");
                if (next != null && next > System.currentTimeMillis()) {
                    throw new IllegalArgumentException("Please wait a few seconds before trying again.");
                }
                r.getSession().setAttribute("nextLogin", System.currentTimeMillis() + 1500);
                SessionUser user = service.login(r.getParameter("email"), r.getParameter("password"));
                r.changeSessionId();
                r.getSession().setAttribute("user", user);
                r.getSession().setAttribute("csrf", java.util.UUID.randomUUID().toString());
                r.getSession().setMaxInactiveInterval(1800);
                redirect(r, s, user.getRole().equals("SELLER") ? "/seller" : user.getRole().equals("ADMIN") ? "/admin" : "/home");
                return;
            }
            if (p.startsWith("/register/")) {
                service.register(p.endsWith("seller") ? "SELLER" : "CUSTOMER", r.getParameter("email"), r.getParameter("password"), r.getParameter("confirm"), r.getParameter("name"), r.getParameter("phone"), r.getParameter("store"));
            } else if (p.equals("/forgot")) {
                Long last = (Long) r.getSession().getAttribute("lastRecovery");
                if (last == null || last < System.currentTimeMillis() - 60000) {
                    service.forgot(r.getParameter("email"));
                    r.getSession().setAttribute("lastRecovery", System.currentTimeMillis());
                }
                r.setAttribute("message", "If the email exists, recovery instructions will be generated. Local mode saves the email in the server outbox.");
                doGet(r, s);
                return;
            } else if (p.equals("/reset")) {
                service.reset(r.getParameter("token"), r.getParameter("password"), r.getParameter("confirm"));
            }
            redirect(r, s, "/login?success=1");
        } catch (Exception e) {
            r.setAttribute("error", error(e));
            doGet(r, s);
        }
    }
}