package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Notification;
import model.SessionUser;
import service.NotificationService;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;


@WebServlet(urlPatterns = {"/notifications", "/notifications/*"})
public final class NotificationServlet extends HttpServlet {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_RECENT_LIMIT = 10;
    private static final int MAX_RECENT_LIMIT = 50;

    private final NotificationService notifications =
            new NotificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String path = normalizedPath(request);
        SessionUser user = sessionUser(request);
        if (user == null) {
            if ("/".equals(path)) {
                response.sendRedirect(request.getContextPath() + "/login");
            } else {
                setJsonHeaders(response);
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized");
            }
            return;
        }

        if ("/".equals(path)) {
            request.getRequestDispatcher("/WEB-INF/views/notifications.jsp")
                    .forward(request, response);
            return;
        }

        setJsonHeaders(response);
        try {
            long userId = user.getId();
            switch (path) {
                case "/list" -> {
                    int page = intParam(request, "page", 0, 0, Integer.MAX_VALUE);
                    int size = intParam(request, "size", DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE);
                    // Query one extra entry to determine whether another page exists.
                    List<Notification> rows = notifications.getInbox(userId, page, size + 1);
                    boolean hasMore = rows.size() > size;
                    if (hasMore) rows = rows.subList(0, size);
                    long unread = notifications.countUnread(userId);
                    response.getWriter().write(inboxJson(rows, page, size, unread, hasMore));
                }
                case "/unread" -> {
                    long unread = notifications.countUnread(userId);
                    response.getWriter().write("{\"unread\":" + unread + "}");
                }
                case "/recent" -> {
                    int limit = intParam(request, "limit", DEFAULT_RECENT_LIMIT, 1,
                            MAX_RECENT_LIMIT);
                    List<Notification> rows = notifications.getUnread(userId, limit);
                    response.getWriter().write("{\"notifications\":" + arrayJson(rows)
                            + ",\"unread\":" + notifications.countUnread(userId) + "}");
                }
                case "/item" -> {
                    long id = positiveId(request.getParameter("id"));
                    Notification n = notifications.getById(userId, id);
                    if (n == null) {
                        sendError(response, HttpServletResponse.SC_NOT_FOUND, "not_found");
                        return;
                    }
                    response.getWriter().write("{\"notification\":" + notificationJson(n) + "}");
                }
                default -> sendError(response, HttpServletResponse.SC_NOT_FOUND, "not_found");
            }
        } catch (IllegalArgumentException ex) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "invalid_parameters");
        } catch (SQLException ex) {
            getServletContext().log("Cannot load notifications", ex);
            sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "database_unavailable");
        } catch (RuntimeException ex) {
            getServletContext().log("Unexpected notification GET failure", ex);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internal_error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        setJsonHeaders(response);
        HttpSession session = request.getSession(false);
        SessionUser user = sessionUser(session);
        if (user == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized");
            return;
        }
        // Defense-in-depth. SecurityFilter checks the same CSRF field globally.
        Object expected = session.getAttribute("csrf");
        String submitted = request.getParameter("csrf");
        if (!(expected instanceof String) || submitted == null
                || !Objects.equals(expected, submitted)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "invalid_csrf");
            return;
        }

        try {
            long userId = user.getId();
            switch (normalizedPath(request)) {
                case "/read" -> {
                    long id = positiveId(request.getParameter("id"));
                    boolean updated = notifications.markAsRead(userId, id);
                    // False covers both already-read and not-owned/not-found IDs.
                    long unread = notifications.countUnread(userId);
                    response.getWriter().write("{\"updated\":" + updated
                            + ",\"unread\":" + unread + "}");
                }
                case "/read-all" -> {
                    int updated = notifications.markAllAsRead(userId);
                    long unread = notifications.countUnread(userId);
                    response.getWriter().write("{\"updated\":" + updated
                            + ",\"unread\":" + unread + "}");
                }
                default -> sendError(response, HttpServletResponse.SC_NOT_FOUND, "not_found");
            }
        } catch (IllegalArgumentException ex) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "invalid_parameters");
        } catch (SQLException ex) {
            getServletContext().log("Cannot update notifications", ex);
            sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "database_unavailable");
        } catch (RuntimeException ex) {
            getServletContext().log("Unexpected notification POST failure", ex);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internal_error");
        }
    }

    private static SessionUser sessionUser(HttpServletRequest request) {
        return sessionUser(request.getSession(false));
    }

    private static SessionUser sessionUser(HttpSession session) {
        if (session == null) return null;
        try {
            Object value = session.getAttribute("user");
            return value instanceof SessionUser ? (SessionUser) value : null;
        } catch (IllegalStateException ex) {
            return null; // Expired/invalidated session.
        }
    }

    private static String normalizedPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.isEmpty() ? "/" : path;
    }

    private static int intParam(HttpServletRequest request, String key,
                                int defaultValue, int min, int max) {
        String raw = request.getParameter(key);
        if (raw == null || raw.isBlank()) return defaultValue;
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed < min || parsed > max) throw new IllegalArgumentException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + key, ex);
        }
    }

    private static long positiveId(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Missing id");
        try {
            long value = Long.parseLong(raw);
            if (value <= 0) throw new IllegalArgumentException("Invalid id");
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid id", ex);
        }
    }

    private static void setJsonHeaders(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private static void sendError(HttpServletResponse response, int status, String code)
            throws IOException {
        response.setStatus(status);
        response.getWriter().write("{\"error\":" + jsonString(code) + "}");
    }

    private static String inboxJson(List<Notification> rows, int page,
                                    int size, long unread, boolean hasMore) {
        return "{\"notifications\":" + arrayJson(rows)
                + ",\"page\":" + page
                + ",\"size\":" + size
                + ",\"hasMore\":" + hasMore
                + ",\"unread\":" + unread + "}";
    }

    private static String arrayJson(List<Notification> notifications) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < notifications.size(); i++) {
            if (i > 0) out.append(',');
            out.append(notificationJson(notifications.get(i)));
        }
        return out.append(']').toString();
    }

    private static String notificationJson(Notification n) {
        // Do not expose send_email, email_status, dedup_key or provider identifiers.
        return "{\"id\":" + n.getId()
                + ",\"eventType\":" + jsonString(n.getEventType())
                + ",\"title\":" + jsonString(n.getTitle())
                + ",\"content\":" + jsonString(n.getContent())
                + ",\"linkUrl\":" + jsonString(safeLocalPath(n.getLinkUrl()))
                + ",\"read\":" + (n.getReadAt() != null)
                + ",\"createdAt\":" + jsonTimestamp(n.getCreatedAt())
                + ",\"readAt\":" + jsonTimestamp(n.getReadAt())
                + "}";
    }

    private static String safeLocalPath(String path) {
        if (path == null || !path.startsWith("/") || path.startsWith("//")
                || path.indexOf('\\') >= 0 || path.indexOf('\r') >= 0
                || path.indexOf('\n') >= 0) return null;
        return path;
    }

    private static String jsonTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "null";
        // Freshora stores notification timestamps with SYSUTCDATETIME().
        String utc = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                timestamp.toLocalDateTime().atOffset(ZoneOffset.UTC));
        return jsonString(utc);
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20 || ch == '\u2028' || ch == '\u2029') {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
