package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import model.User;

/** In-app notifications (GB-08). The caller owns the transaction. */
public class NotificationDAO {

    /**
     * Creates a notification unless one with the same dedup key already exists.
     * A duplicate is skipped silently, so a repeated event never fails or rolls back the business change.
     * Returns true when a row was inserted.
     */
    public boolean send(Connection c, long userId, String eventType, String title, String content, String link, String dedupKey) throws SQLException {
        return Database.update(c, "INSERT notifications(user_id,event_type,title,content,link_url,dedup_key) SELECT ?,?,?,?,?,?"
                + " WHERE NOT EXISTS(SELECT 1 FROM notifications WITH(UPDLOCK,HOLDLOCK) WHERE dedup_key=?)",
                userId, eventType, title, content, link, dedupKey, dedupKey) == 1;
    }

    /** Admins who should hear about delivery exceptions. */
    public List<User> activeAdmins(Connection c) throws SQLException {
        return Database.rows(User.class, c, "SELECT id FROM users WHERE role='ADMIN' AND status='ACTIVE' ORDER BY id");
    }
}
