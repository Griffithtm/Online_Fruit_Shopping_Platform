package service;

import config.DBConnect;
import dao.NotificationDAO;
import model.Notification;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;


public class NotificationService {

    private final NotificationDAO dao;

    public NotificationService() {
        this.dao = new NotificationDAO();
    }


    private Connection getConnection() throws SQLException {
        return new DBConnect().getConnection();
    }

    public long create(Connection connection,
                       long recipientId,
                       String eventType,
                       String title,
                       String content,
                       String linkUrl,
                       boolean sendEmail,
                       String dedupKey) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        validateId(recipientId, "recipientId");
        eventType = required(eventType, 50, "eventType");
        title = required(title, 200, "title");
        content = required(content, 1000, "content");
        dedupKey = required(dedupKey, 150, "dedupKey");
        if (!eventType.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("eventType must use A-Z, 0-9 and _");
        }
        if (!dedupKey.matches("[A-Za-z0-9:_-]+")) {
            throw new IllegalArgumentException("dedupKey must use A-Z, a-z, 0-9, :, - or _");
        }
        linkUrl = validLocalLink(linkUrl);

        Notification previous = dao.findByDedupKey(connection, dedupKey);
        if (previous != null) {
            return existingId(previous, recipientId, eventType);
        }
        return dao.create(connection, recipientId, eventType,
                title, content, linkUrl, sendEmail, dedupKey);
    }

    
    public long notifyUser(long recipientId,
                           String eventType,
                           String title,
                           String content,
                           String linkUrl,
                           boolean sendEmail,
                           String dedupKey) throws SQLException {
        SQLException duplicateFailure = null;
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                long id = create(c, recipientId, eventType, title, content,
                        linkUrl, sendEmail, dedupKey);
                c.commit();
                return id;
            } catch (SQLException failure) {
                rollback(c, failure);
                if (!isDuplicateKey(failure)) throw failure;
                duplicateFailure = failure;
            } catch (RuntimeException failure) {
                rollback(c, failure);
                throw failure;
            }
        }

        // The earlier transaction is finished. Do not query through a failed
        // SQL Server transaction after a unique-key violation.
        try (Connection verify = getConnection()) {
            Notification existing = dao.findByDedupKey(verify, dedupKey);
            if (existing != null) {
                return existingId(existing, recipientId, eventType);
            }
        }
        throw duplicateFailure;
    }

    /**
     * Returns an inbox page (zero-based page numbering).
     *
     * @param recipientId logged-in user's ID
     * @param page zero-based page number
     * @param pageSize number of rows to load (1 to 100)
     * @return most recent notifications, including read items
     * @throws SQLException if the query fails
     */
    public List<Notification> getInbox(long recipientId, int page, int pageSize)
            throws SQLException {
        validateId(recipientId, "recipientId");
        try (Connection c = getConnection()) {
            return dao.findByUser(c, recipientId, page, pageSize);
        }
    }

    
    public List<Notification> getUnread(long recipientId, int limit)
            throws SQLException {
        validateId(recipientId, "recipientId");
        try (Connection c = getConnection()) {
            return dao.findUnread(c, recipientId, limit);
        }
    }

    
    public List<Notification> getAfterId(long recipientId, long lastKnownId,
                                          int limit) throws SQLException {
        validateId(recipientId, "recipientId");
        try (Connection c = getConnection()) {
            return dao.findAfterId(c, recipientId, lastKnownId, limit);
        }
    }

    
    public long countUnread(long recipientId) throws SQLException {
        validateId(recipientId, "recipientId");
        try (Connection c = getConnection()) {
            return dao.countUnread(c, recipientId);
        }
    }

    
    public Notification getById(long recipientId, long notificationId)
            throws SQLException {
        validateId(recipientId, "recipientId");
        validateId(notificationId, "notificationId");
        try (Connection c = getConnection()) {
            return dao.findById(c, notificationId, recipientId);
        }
    }

    
    public boolean markAsRead(long recipientId, long notificationId)
            throws SQLException {
        validateId(recipientId, "recipientId");
        validateId(notificationId, "notificationId");
        try (Connection c = getConnection()) {
            return dao.markAsRead(c, notificationId, recipientId) == 1;
        }
    }


    public int markAllAsRead(long recipientId) throws SQLException {
        validateId(recipientId, "recipientId");
        try (Connection c = getConnection()) {
            return dao.markAllAsRead(c, recipientId);
        }
    }


       public List<Notification> getDueEmails(int limit) throws SQLException {
        try (Connection c = getConnection()) {
            return dao.findDueEmails(c, limit);
        }
    }

 
    public boolean markEmailSent(long notificationId, String providerReference)
            throws SQLException {
        validateId(notificationId, "notificationId");
        if (providerReference != null && providerReference.length() > 150) {
            throw new IllegalArgumentException("providerReference too long");
        }
        try (Connection c = getConnection()) {
            return dao.markEmailSent(c, notificationId, providerReference) == 1;
        }
    }

  
    public boolean scheduleEmailRetry(long notificationId) throws SQLException {
        validateId(notificationId, "notificationId");
        try (Connection c = getConnection()) {
            return dao.scheduleEmailRetry(c, notificationId) == 1;
        }
    }

    public boolean markEmailDelivered(long notificationId) throws SQLException {
        validateId(notificationId, "notificationId");
        try (Connection c = getConnection()) {
            return dao.markEmailDelivered(c, notificationId) == 1;
        }
    }


    public boolean markEmailBounced(long notificationId) throws SQLException {
        validateId(notificationId, "notificationId");
        try (Connection c = getConnection()) {
            return dao.markEmailBounced(c, notificationId) == 1;
        }
    }

    private static long existingId(Notification existing, long recipientId,
                                   String eventType) throws SQLException {
        if (!Objects.equals(existing.getUserId(), recipientId)
                || !Objects.equals(existing.getEventType(), eventType)) {
            throw new SQLException("dedupKey belongs to a different recipient/event");
        }
        return existing.getId();
    }

    private static void rollback(Connection c, Exception failure) {
        try {
            c.rollback();
        } catch (SQLException rollbackError) {
            failure.addSuppressed(rollbackError);
        }
    }

    private static boolean isDuplicateKey(SQLException exception) {
        for (SQLException e = exception; e != null; e = e.getNextException()) {
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                return true;
            }
        }
        return false;
    }

    private static void validateId(long id, String name) {
        if (id <= 0) throw new IllegalArgumentException(name + " must be > 0");
    }

    private static String required(String value, int maxLen, String name) {
        if (value == null || value.isBlank() || value.length() > maxLen) {
            throw new IllegalArgumentException(name + " required, max " + maxLen + " characters");
        }
        return value.trim();
    }

    private static String validLocalLink(String url) {
        if (url == null || url.isBlank()) return null;
        if (url.length() > 500 || !url.startsWith("/") || url.startsWith("//")
                || url.indexOf('\\') >= 0 || url.indexOf('\r') >= 0
                || url.indexOf('\n') >= 0 || url.indexOf('\t') >= 0) {
            throw new IllegalArgumentException("linkUrl must be an application-local path");
        }
        return url;
    }
}
