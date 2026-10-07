package service;

import config.DBConnect;
import dao.Database;
import model.SessionUser;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class AccountService {

    private static boolean matches(String plain, String hash) {
        return plain != null && hash != null && BCrypt.checkpw(plain, hash.replaceFirst("^\\$2[by]\\$", "\\$2a\\$"));
    }

    public SessionUser login(String email, String password) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            Map<String, Object> u = Database.one(c, "SELECT * FROM users WHERE email=? AND status='ACTIVE'", email == null ? "" : email.trim());
            if (u == null || !matches(password, (String) u.get("password_hash"))) {
                throw new IllegalArgumentException("Incorrect email or password.");
            }
            Database.update(c, "UPDATE users SET last_login_at=SYSUTCDATETIME() WHERE id=?", u.get("id"));
            return new SessionUser(((Number) u.get("id")).longValue(), (String) u.get("full_name"), (String) u.get("role"), (String)u.get("password_hash"));
        }
    }

    public void register(String role, String email, String pass, String confirm, String name, String phone, String store) throws Exception {
        email = Validation.text(email, 254).toLowerCase(Locale.ROOT);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        String hash = BCrypt.hashpw(Validation.password(pass, confirm), BCrypt.gensalt(12));
        name = Validation.text(name, 100);
        phone = Validation.text(phone, 20);
        try (Connection c = new DBConnect().getConnection()) {
            if ("SELLER".equals(role)) {
                Database.rows(c, "EXEC dbo.register_seller ?,?,?,?,?", email, hash, name, Validation.text(store, 100), phone);
            } else if ("CUSTOMER".equals(role)) {
                Database.rows(c, "EXEC dbo.register_customer ?,?,?,?", email, hash, name, phone);
            } else {
                throw new IllegalArgumentException("Invalid account type.");
            }
        }
    }

    public Map<String, Object> profile(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.one(c, "SELECT id,email,full_name,phone,role FROM users WHERE id=?", id);
        }
    }

    public List<Map<String, Object>> addresses(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return Database.rows(c, "SELECT * FROM addresses WHERE user_id=? ORDER BY is_default DESC,id", id);
        }
    }

    public void profile(long id, String name, String phone) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            Database.update(c, "UPDATE users SET full_name=?,phone=?,updated_at=SYSUTCDATETIME() WHERE id=?", Validation.text(name, 100), Validation.text(phone, 20), id);
        }
    }

    public void password(long id, String old, String pass, String confirm) throws Exception {
        Validation.password(pass, confirm);
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
            Map<String, Object> u = Database.one(c, "SELECT password_hash FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", id);
            if (!matches(old, (String) u.get("password_hash"))) {
                throw new IllegalArgumentException("The current password is incorrect.");
            }
            Database.update(c, "UPDATE users SET password_hash=?,updated_at=SYSUTCDATETIME() WHERE id=?", BCrypt.hashpw(pass, BCrypt.gensalt(12)), id);
            Database.update(c, "UPDATE password_reset_tokens SET used_at=SYSUTCDATETIME() WHERE user_id=? AND used_at IS NULL", id);
            c.commit();
            } catch(Exception ex) {c.rollback();throw ex;}
        }
    }

    public void address(long uid, String action, String id, String name, String phone, String line, String ward, String district, String city, boolean primary) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                Database.one(c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?", uid);
                Long aid = (id == null || id.isBlank()) ? null : Validation.id(id);
                if (aid != null && Database.one(c, "SELECT id FROM addresses WHERE id=? AND user_id=?", aid, uid) == null) {
                    throw new IllegalArgumentException("Address not found.");
                }
                if ((action.equals("deleteAddress") || action.equals("saveAddress")) && aid != null && Database.one(c, "SELECT TOP 1 id FROM orders WHERE customer_id=? AND status NOT IN ('CART','COMPLETED','CANCELLED','EXPIRED')", uid) != null) {
                    throw new IllegalArgumentException("You cannot edit or delete an address while orders are being processed.");
                }
                if (action.equals("deleteAddress")) {
                    Database.update(c, "DELETE FROM addresses WHERE id=? AND user_id=?", aid, uid);
                } else if (action.equals("defaultAddress")) {
                    Database.update(c, "UPDATE addresses SET is_default=0 WHERE user_id=?", uid);
                    Database.update(c, "UPDATE addresses SET is_default=1 WHERE id=? AND user_id=?", aid, uid);
                } else {
                    name = Validation.text(name, 100);
                    phone = Validation.text(phone, 20);
                    line = Validation.text(line, 255);
                    ward = Validation.text(ward, 100);
                    district = Validation.text(district, 100);
                    city = Validation.text(city, 100);
                    if (primary) {
                        Database.update(c, "UPDATE addresses SET is_default=0 WHERE user_id=?", uid);
                    }
                    if (aid == null) {
                        Database.update(c, "INSERT addresses(user_id,recipient_name,phone,address_line,ward,district,city,is_default) VALUES(?,?,?,?,?,?,?,?)", uid, name, phone, line, ward, district, city, primary);
                    } else {
                        Database.update(c, "UPDATE addresses SET recipient_name=?,phone=?,address_line=?,ward=?,district=?,city=?,is_default=?,updated_at=SYSUTCDATETIME() WHERE id=? AND user_id=?", name, phone, line, ward, district, city, primary, aid, uid);
                    }
                }
                if (Database.one(c,"SELECT id FROM addresses WHERE user_id=? AND is_default=1",uid)==null) {
                    Database.update(c,"UPDATE addresses SET is_default=1 WHERE id=(SELECT MIN(id) FROM addresses WHERE user_id=?)",uid);
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static byte[] digest(String token) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
    }

    public void forgot(String email) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            Map<String, Object> u = Database.one(c, "SELECT id,email FROM users WHERE email=? AND status='ACTIVE'", email);
            if (u == null) {
                return;
            }
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            String token = HexFormat.of().formatHex(random);
            Database.update(c, "INSERT password_reset_tokens(user_id,token_hash,expires_at) VALUES(?,?,DATEADD(minute,20,SYSUTCDATETIME()))", u.get("id"), digest(token));
            String base = System.getenv().getOrDefault("FRESHORA_PUBLIC_URL", "http://localhost:9999/Freshora");
            try {
                new MailService().recovery((String)u.get("email"),base+"/reset?token="+token);
            } catch(Exception e) {
                Database.update(c,"DELETE FROM password_reset_tokens WHERE token_hash=?",digest(token));
                throw e;
            }
        }
    }

    public void reset(String token, String pass, String confirm) throws Exception {
        Validation.password(pass, confirm);
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                byte[] tokenHash=digest(token == null ? "" : token);
                Map<String,Object> owner=Database.one(c,"SELECT user_id FROM password_reset_tokens WHERE token_hash=?",tokenHash);
                if(owner==null) throw new IllegalArgumentException("This link has expired or has already been used.");
                Database.one(c,"SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?",owner.get("user_id"));
                Map<String, Object> t = Database.one(c, "SELECT id,user_id FROM password_reset_tokens WITH(UPDLOCK,HOLDLOCK) WHERE token_hash=? AND used_at IS NULL AND expires_at>SYSUTCDATETIME()", tokenHash);
                if (t == null) {
                    throw new IllegalArgumentException("This link has expired or has already been used.");
                }
                Database.update(c, "UPDATE users SET password_hash=?,updated_at=SYSUTCDATETIME() WHERE id=?", BCrypt.hashpw(pass, BCrypt.gensalt(12)), t.get("user_id"));
                Database.update(c, "UPDATE password_reset_tokens SET used_at=SYSUTCDATETIME() WHERE user_id=? AND used_at IS NULL", t.get("user_id"));
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }
}
