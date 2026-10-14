package service;

import config.DBConnect;
import dao.AddressDAO;
import dao.AuditDAO;
import dao.Database;
import dao.SettingDAO;
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
    private final AuditDAO audit = new AuditDAO();

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
                Map<String, Object> before = users.findProfile(c, id);
                users.updateProfile(c, id, n, e, p);
                audit.log(c, id, "User", id, "PROFILE_UPDATED", profileJson(before.get("full_name"), before.get("email"), before.get("phone")), profileJson(n, e, p), null);
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

    private static String profileJson(Object name, Object email, Object phone) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("full_name", name);
        m.put("email", email);
        m.put("phone", phone);
        return AuditDAO.json(m);
    }

    private static String addressJson(Map<String, Object> a) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String k : new String[]{"recipient_name", "phone", "address_line", "ward", "district", "city", "is_default"}) {
            m.put(k, a.get(k));
        }
        return AuditDAO.json(m);
    }

    /**
     * Changes the password after checking the current one (FE-01.2, BA-06). Passwords are never trimmed.
     * Errors are reported per field: old, password, confirm. Validation.password is shared and left unchanged.
     */
    public void password(long id, String old, String pass, String confirm) throws Exception {
        Map<String, String> errors = new LinkedHashMap<>();
        if (old == null || old.isEmpty()) {
            errors.put("old", Messages.get(Messages.MSG02, "current password"));
        }
        if (pass == null || pass.isEmpty()) {
            errors.put("password", Messages.get(Messages.MSG02, "new password"));
        } else if (pass.length() > 64) {
            errors.put("password", Messages.get(Messages.MSG03, "new password", 64));
        } else if (pass.length() < 8 || pass.getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.put("password", Messages.get(Messages.MSG04, "new password (8–64 characters)"));
        }
        if (!Objects.equals(pass, confirm)) {
            errors.put("confirm", Messages.get(Messages.MSG16));
        }
        FieldErrors.throwIfAny(errors);
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                String hash = users.lockPasswordHash(c, id);
                if (!matches(old, hash)) {
                    throw FieldErrors.of("old", Messages.get(Messages.MSG04, "current password"));
                }
                if (matches(pass, hash)) {
                    throw FieldErrors.of("password", Messages.get(Messages.MSG04, "new password") + " It must be different from the current password.");
                }
                users.updatePasswordHash(c, id, BCrypt.hashpw(pass, BCrypt.gensalt(12)));
                users.invalidateResetTokens(c, id);
                // Never log the password or its hash.
                audit.log(c, id, "User", id, "PASSWORD_CHANGED", null, null, null);
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

    /** Provinces/cities with delivery service, for the address form. */
    public List<String> serviceAreas() throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return new SettingDAO().activeServiceAreas(c);
        }
    }

    /** Only Customers and Sellers keep an address book (Permission Matrix, Address). */
    public static boolean canManageAddresses(String role) {
        return "CUSTOMER".equals(role) || "SELLER".equals(role);
    }

    /**
     * Saves, deletes or sets the default address of the current user and returns the success message.
     * {@code primary} only applies to a new address; an edit never changes which address is the default (BA-04).
     */
    public String address(long uid, String action, String id, String name, String phone, String line, String ward, String district, String city, boolean primary) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                // Locking the user row also serialises this with CheckoutService.place for the same user.
                if (!canManageAddresses(users.lockRole(c, uid))) {
                    throw Messages.error(Messages.MSG12);
                }
                boolean delete = action.equals("deleteAddress");
                Long aid = addressId(id);
                if ((aid == null && (delete || action.equals("defaultAddress"))) || (aid != null && !addressDao.existsForUser(c, aid, uid))) {
                    throw Messages.error(Messages.MSG12);
                }
                if ((delete || action.equals("saveAddress")) && aid != null) {
                    if (addressDao.usedByOpenOrder(c, aid, uid)) {
                        throw Messages.error(Messages.MSG19);
                    }
                    if (addressDao.isStorePickup(c, aid, uid) && (delete || addressDao.pickupInActiveDelivery(c, aid, uid))) {
                        throw Messages.error(Messages.MSG19);
                    }
                }
                String message;
                Map<String, Object> before = aid == null ? null : addressDao.findById(c, aid, uid);
                if (delete) {
                    boolean wasDefault = Boolean.TRUE.equals(before.get("is_default"));
                    addressDao.delete(c, aid, uid);
                    message = Messages.get(Messages.MSG81);
                    String note = null;
                    if (wasDefault) {
                        // The oldest remaining address takes over, and the user is told which one.
                        addressDao.makeFirstDefault(c, uid);
                        Map<String, Object> next = addressDao.findDefault(c, uid);
                        if (next != null) {
                            message = Messages.get(Messages.MSG82, next.get("recipient_name") + " - " + next.get("address_line") + ", "
                                    + next.get("ward") + ", " + next.get("district") + ", " + next.get("city"));
                            note = "Default moved to address #" + next.get("id");
                        }
                    }
                    audit.log(c, uid, "Address", aid, "ADDRESS_DELETED", addressJson(before), null, note);
                } else if (action.equals("defaultAddress")) {
                    Map<String, Object> previous = addressDao.findDefault(c, uid);
                    addressDao.clearDefault(c, uid);
                    addressDao.setDefault(c, aid, uid);
                    audit.log(c, uid, "Address", aid, "ADDRESS_DEFAULT_SET", previous == null ? null : previous.get("id").toString(), aid.toString(), null);
                    message = Messages.get(Messages.MSG18);
                } else {
                    Map<String, String> errors = new LinkedHashMap<>();
                    String n = name, p = phone, l = line, w = ward, d = district, ct = city;
                    name = FieldErrors.check(errors, "name", () -> Validation.field(n, 100, "recipient name", true));
                    phone = FieldErrors.check(errors, "phone", () -> Validation.phone(p, true));
                    line = FieldErrors.check(errors, "line", () -> Validation.field(l, 255, "address", true));
                    ward = FieldErrors.check(errors, "ward", () -> Validation.field(w, 100, "ward", true));
                    district = FieldErrors.check(errors, "district", () -> Validation.field(d, 100, "district", true));
                    city = FieldErrors.check(errors, "city", () -> Validation.field(ct, 100, "province/city", true));
                    FieldErrors.throwIfAny(errors);
                    if (aid == null) {
                        // The first address is always the default.
                        boolean makeDefault = primary || !addressDao.hasDefault(c, uid);
                        if (makeDefault) {
                            addressDao.clearDefault(c, uid);
                        }
                        long newId = addressDao.insert(c, uid, name, phone, line, ward, district, city, makeDefault);
                        audit.log(c, uid, "Address", newId, "ADDRESS_CREATED", null, addressJson(addressDao.findById(c, newId, uid)), null);
                    } else {
                        addressDao.update(c, aid, uid, name, phone, line, ward, district, city);
                        audit.log(c, uid, "Address", aid, "ADDRESS_UPDATED", addressJson(before), addressJson(addressDao.findById(c, aid, uid)), null);
                    }
                    message = Messages.get(Messages.MSG05, "Address");
                }
                c.commit();
                return message;
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
