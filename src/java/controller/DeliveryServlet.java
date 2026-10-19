package controller;

import config.DBConnect;
import dao.OrderDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import model.SellerOrderDetails;
import model.SessionUser;
import service.FulfillmentService;
import service.Validation;
import util.EvidenceImages;
import util.Messages;
import util.OrderCodes;

/**
 * Delivery Staff screen and delivery evidence photos (FE-07.2, ThanhNC 2.6a: BA-35, BA-37, BA-38, BA-47).
 * The container limit is above 5 MB so that a large photo reaches EvidenceImages and gets MSG60 instead of a parsing error.
 */
@WebServlet(urlPatterns = {"/delivery", "/delivery/evidence/*"})
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 10L * 1024 * 1024, maxRequestSize = 11L * 1024 * 1024)
public class DeliveryServlet extends BaseServlet {

    private static final Set<String> ACTIONS = Set.of("pickup", "transit", "arrive", "deliver", "fail");
    private final FulfillmentService service = new FulfillmentService();
    private final OrderDAO orders = new OrderDAO();

    @Override
    protected void doGet(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        if ("/delivery/evidence".equals(r.getServletPath())) {
            evidence(r, s);
            return;
        }
        try {
            Object flash = r.getSession().getAttribute("flash");
            if (flash != null) {
                r.getSession().removeAttribute("flash");
                r.setAttribute("message", flash);
            }
            r.setAttribute("sellerOrders", service.list(uid(r), "DELIVERY_STAFF"));
            r.setAttribute("allocations", service.allocations(uid(r), "DELIVERY_STAFF"));
            r.setAttribute("evidenceTooLarge", Messages.get(Messages.MSG60)); // shown by the page before uploading a file over 5 MB
            view(r, s, "fulfillment");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * BA-47: a photo is shown only to the customer of the order, the assigned courier and Admin.
     * SecurityFilter covers only "/delivery", so access is checked here.
     */
    private void evidence(HttpServletRequest r, HttpServletResponse s) throws IOException, ServletException {
        SessionUser user = (SessionUser) r.getSession().getAttribute("user");
        if (user == null) {
            s.sendError(HttpServletResponse.SC_FORBIDDEN, Messages.get(Messages.MSG12));
            return;
        }
        String name = r.getPathInfo() == null ? null : r.getPathInfo().substring(1);
        if (!EvidenceImages.validName(name)) { // UUID + ".png" only, so "../" never reaches the file system
            s.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String path = EvidenceImages.PREFIX + name;
        try (Connection c = new DBConnect().getConnection()) {
            List<Long> viewers = orders.evidenceViewers(c, path);
            if (viewers == null) {
                s.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (!"ADMIN".equals(user.getRole()) && !viewers.contains(user.getId())) {
                s.sendError(HttpServletResponse.SC_FORBIDDEN, Messages.get(Messages.MSG12));
                return;
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        Path file = EvidenceImages.file(path);
        if (file == null || !Files.isRegularFile(file)) {
            s.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        s.setContentType("image/png");
        s.setHeader("Cache-Control", "private, no-store");
        s.setHeader("X-Content-Type-Options", "nosniff");
        Files.copy(file, s.getOutputStream());
    }

    @Override
    protected void doPost(HttpServletRequest r, HttpServletResponse s) throws ServletException, IOException {
        if (!"/delivery".equals(r.getServletPath())) {
            s.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String saved = null;
        try {
            String action = r.getParameter("action");
            if (!ACTIONS.contains(action)) {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            long sid = Validation.id(r.getParameter("id"));
            String detail = null;
            BigDecimal cod = null;
            if ("deliver".equals(action)) {
                saved = savePhoto(r);
                detail = saved;
                cod = codAmount(r.getParameter("codAmount"));
            } else if ("fail".equals(action)) {
                detail = r.getParameter("failureReason"); // BA-37: its own field, never the evidence input
            }
            service.change(uid(r), "DELIVERY_STAFF", sid, action, detail, null, false, cod);
            SellerOrderDetails so;
            try (Connection c = new DBConnect().getConnection()) {
                so = orders.sellerOrderHeader(c, sid);
            }
            if (saved != null && !saved.equals(so.getEvidenceUrl())) {
                EvidenceImages.delete(saved); // repeated confirmation (BA-41): the first photo stays
            }
            saved = null;
            String code = OrderCodes.sellerOrderCode(so.getOrderCode(), so.getStoreId());
            String message = switch (so.getStatus()) {
                case "DELIVERED" -> Messages.get(Messages.MSG61, code);
                case "FAILED" -> Messages.get(Messages.MSG63, code, so.getFailureReason());
                default -> Messages.get(Messages.MSG49, code, util.Vietnamese.label(so.getStatus()));
            };
            r.getSession().setAttribute("flash", message);
            redirect(r, s, "/delivery");
        } catch (Exception e) {
            if (saved != null) {
                EvidenceImages.delete(saved);
            }
            r.setAttribute("error", error(e));
            doGet(r, s);
        }
    }

    /** The uploaded photo (BA-35), or null when none was chosen. Over-size or unreadable files give MSG60. */
    private static String savePhoto(HttpServletRequest r) throws IOException, ServletException {
        Part part;
        try {
            part = r.getPart("evidence");
        } catch (IllegalStateException tooLarge) {
            throw Messages.error(Messages.MSG60);
        }
        if (part == null || part.getSize() == 0) {
            return null;
        }
        try (InputStream in = part.getInputStream()) {
            return EvidenceImages.save(in, part.getSize());
        }
    }

    /**
     * COD amount typed by the courier (BA-38), whole VND: "120000" or with thousand separators "120.000" / "120,000".
     * Blank means "not entered"; anything else gives MSG60.
     */
    static BigDecimal codAmount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.matches("\\d{1,3}([.,]\\d{3})+")) {
            v = v.replaceAll("[.,]", "");
        }
        if (!v.matches("\\d{1,15}")) {
            throw Messages.error(Messages.MSG60);
        }
        return new BigDecimal(v);
    }
}
