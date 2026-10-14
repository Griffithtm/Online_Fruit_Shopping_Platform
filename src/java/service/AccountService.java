package service;

import config.DBConnect;
import dao.AddressDAO;
import dao.Database;
import dao.UserDAO;
import model.SessionUser;
import org.mindrot.jbcrypt.BCrypt;
import util.FieldErrors;
import util.Messages;
import java.sql.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class AccountService {

    private final UserDAO users = new UserDAO();
    private final AddressDAO addressDao = new AddressDAO();

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
            return users.findProfile(c, id);
        }
    }

    public List<Map<String, Object>> addresses(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return addressDao.findByUser(c, id);
        }
    }

    /** Updates name, email and phone of the current user; returns the saved name. Errors are reported per field. */
    public String profile(long id, String name, String email, String phone) throws Exception {
        Map<String, String> errors = new LinkedHashMap<>();
        String n = FieldErrors.check(errors, "name", () -> Validation.field(name, 100, "full name", true));
        String e = FieldErrors.check(errors, "email", () -> Validation.email(email));
        String p = FieldErrors.check(errors, "phone", () -> Validation.phone(phone, false));
        FieldErrors.throwIfAny(errors);
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                users.lockUser(c, id);
                if (users.emailTakenByOther(c, e, id)) {
                    throw FieldErrors.of("email", Messages.get(Messages.MSG13));
                }
                users.updateProfile(c, id, n, e, p);
                c.commit();
                return n;
            } catch (SQLException ex) {
                c.rollback();
                // 2627/2601: unique key violation; email is the only unique column this UPDATE writes.
                if (ex.getErrorCode() == 2627 || ex.getErrorCode() == 2601) {
                    throw FieldErrors.of("email", Messages.get(Messages.MSG13));
                }
                throw ex;
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public void password(long id, String old, String pass, String confirm) throws Exception {
        Validation.password(pass, confirm);
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                if (!matches(old, users.lockPasswordHash(c, id))) {
                    throw Messages.error(Messages.MSG04, "current password");
                }
                users.updatePasswordHash(c, id, BCrypt.hashpw(pass, BCrypt.gensalt(12)));
                users.invalidateResetTokens(c, id);
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Parses an address id; a malformed id is reported like a foreign one (MSG12). */
    private static Long addressId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return Validation.id(id);
        } catch (IllegalArgumentException e) {
            throw Messages.error(Messages.MSG12);
        }
    }

    public void address(long uid, String action, String id, String name, String phone, String line, String ward, String district, String city, boolean primary) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                users.lockUser(c, uid);
                Long aid = addressId(id);
                if (aid != null && !addressDao.existsForUser(c, aid, uid)) {
                    throw Messages.error(Messages.MSG12);
                }
                if ((action.equals("deleteAddress") || action.equals("saveAddress")) && aid != null && addressDao.hasOpenOrder(c, uid)) {
                    throw Messages.error(Messages.MSG19);
                }
                if (action.equals("deleteAddress")) {
                    addressDao.delete(c, aid, uid);
                } else if (action.equals("defaultAddress")) {
                    addressDao.clearDefault(c, uid);
                    addressDao.setDefault(c, aid, uid);
                } else {
                    name = Validation.text(name, 100);
                    phone = Validation.text(phone, 20);
                    line = Validation.text(line, 255);
                    ward = Validation.text(ward, 100);
                    district = Validation.text(district, 100);
                    city = Validation.text(city, 100);
                    if (primary) {
                        addressDao.clearDefault(c, uid);
                    }
                    if (aid == null) {
                        addressDao.insert(c, uid, name, phone, line, ward, district, city, primary);
                    } else {
                        addressDao.update(c, aid, uid, name, phone, line, ward, district, city, primary);
                    }
                }
                if (!addressDao.hasDefault(c, uid)) {
                    addressDao.makeFirstDefault(c, uid);
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
