package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/** SQL for the users table used by FE-01.2 (profile and password). The caller owns the connection/transaction. */
public class UserDAO {

    public Map<String, Object> findProfile(Connection c, long id) throws SQLException {
        return Database.one(c, "SELECT id,email,full_name,phone,role FROM users WHERE id=?", id);
    }

    /** Only name, email and phone are writable here; role, status, tier and points never are. */
    public int updateProfile(Connection c, long id, String name, String email, String phone) throws SQLException {
        return Database.update(c, "UPDATE users SET full_name=?,email=?,phone=?,updated_at=SYSUTCDATETIME() WHERE id=?", name, email, phone, id);
    }

    /** True if another user already has this email (collation is case-insensitive); locks the key range. */
    public boolean emailTakenByOther(Connection c, String email, long id) throws SQLException {
        return Database.one(c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE email=? AND id<>?", email, id) != null;
    }

    /** Locks the user row for the rest of the transaction. */
    public Map<String, Object> lockUser(Connection c, long id) throws SQLException {
        return Database.one(c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", id);
    }

    /** Locks the user row and returns its password hash, or null if the user does not exist. */
    public String lockPasswordHash(Connection c, long id) throws SQLException {
        Map<String, Object> u = Database.one(c, "SELECT password_hash FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", id);
        return u == null ? null : (String) u.get("password_hash");
    }

    public int updatePasswordHash(Connection c, long id, String hash) throws SQLException {
        return Database.update(c, "UPDATE users SET password_hash=?,updated_at=SYSUTCDATETIME() WHERE id=?", hash, id);
    }

    public int invalidateResetTokens(Connection c, long id) throws SQLException {
        return Database.update(c, "UPDATE password_reset_tokens SET used_at=SYSUTCDATETIME() WHERE user_id=? AND used_at IS NULL", id);
    }
}
