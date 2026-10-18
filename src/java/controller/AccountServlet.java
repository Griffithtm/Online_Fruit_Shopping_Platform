package controller;

import service.AccountService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import model.SessionUser;
import model.User;
import model.AddressForm;
import util.FieldErrors;
import util.Messages;

@WebServlet("/account")
public class AccountServlet extends BaseServlet {

    private final AccountService service = new AccountService();
    private static final String OTHER_CITY = "__other";

    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            Object flash = r.getSession().getAttribute("flash");
            if (flash != null) {
                r.getSession().removeAttribute("flash");
                r.setAttribute("message", flash);
            }
            User profile = service.profile(uid(r));
            r.setAttribute("profile", profile);
            boolean canManage = AccountService.canManageAddresses((String) profile.getRole());
            r.setAttribute("canManageAddresses", canManage);
            if (canManage) {
                r.setAttribute("addresses", service.addresses(uid(r)));
                r.setAttribute("serviceAreas", service.serviceAreas());
                r.setAttribute("deleteAddressConfirm", Messages.get(Messages.MSG06, "xóa", "địa chỉ này"));
            }
            view(r, s, "account");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /** Province/city from the service-area list, or the free text typed after choosing "Other". */
    private static String city(HttpServletRequest r) {
        String selected = r.getParameter("citySelect");
        return OTHER_CITY.equals(selected) ? r.getParameter("cityOther") : selected;
    }

    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        try {
            String a = r.getParameter("action");
            if ("profile".equals(a)) {
                String name = service.profile(uid(r), r.getParameter("name"), r.getParameter("email"), r.getParameter("phone"));
                HttpSession session = r.getSession();
                session.setAttribute("user", ((SessionUser) session.getAttribute("user")).withName(name));
                session.setAttribute("flash", Messages.get(Messages.MSG05, "Hồ sơ"));
                redirect(r, s, "/account");
                return;
            } else if ("password".equals(a)) {
                service.password(uid(r), r.getParameter("old"), r.getParameter("password"), r.getParameter("confirm"));
                // Sign out as before, then carry MSG17 to the login page in a fresh session (header.jsp shows it once).
                r.getSession().invalidate();
                r.getSession(true).setAttribute("flash", Messages.get(Messages.MSG17));
                redirect(r, s, "/login");
                return;
            } else if (java.util.Set.of("saveAddress", "deleteAddress", "defaultAddress").contains(a)) {
                String message = service.address(uid(r), a, r.getParameter("id"), r.getParameter("name"), r.getParameter("phone"), r.getParameter("line"), r.getParameter("ward"), r.getParameter("district"), city(r), "on".equals(r.getParameter("primary")));
                r.getSession().setAttribute("flash", message);
                redirect(r, s, "/account");
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
        } catch (Exception e) {
            if ("profile".equals(r.getParameter("action"))) {
                // Keep what the user typed (NFR-UX-01); keys match the profile row used by the JSP.
                User form = new User();
                form.setFullName(r.getParameter("name"));
                form.setEmail(r.getParameter("email"));
                form.setPhone(r.getParameter("phone"));
                r.setAttribute("profileForm", form);
            } else if ("password".equals(r.getParameter("action"))) {
                // Passwords are never sent back to the page; only the errors are shown.
                r.setAttribute("passwordFailed", true);
            } else if ("saveAddress".equals(r.getParameter("action"))) {
                // Keys match the address row used by the JSP; addressFormId tells which address form failed.
                AddressForm form = new AddressForm();
                form.setRecipientName(r.getParameter("name"));
                form.setPhone(r.getParameter("phone"));
                form.setAddressLine(r.getParameter("line"));
                form.setWard(r.getParameter("ward"));
                form.setDistrict(r.getParameter("district"));
                form.setCity(city(r));
                form.setOtherCity(OTHER_CITY.equals(r.getParameter("citySelect")));
                form.setIsDefault("on".equals(r.getParameter("primary")));
                String id = r.getParameter("id");
                r.setAttribute("addressForm", form);
                r.setAttribute("addressFormId", id == null || id.isBlank() ? "new" : id.trim());
            }
            if (e instanceof FieldErrors fe) {
                r.setAttribute("fieldErrors", fe.getErrors());
            }
            r.setAttribute("error", error(e));
            doGet(r, s);
        }
    }
}
