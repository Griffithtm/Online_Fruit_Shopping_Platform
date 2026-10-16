package service;

import model.*;

import config.DBConnect;
import dao.Database;
import dao.UserDAO;
import dao.AddressDAO;
import dao.SettingDAO;
import dao.AuditDAO;
import util.Messages;
import util.FieldErrors;
import org.mindrot.jbcrypt.BCrypt;
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
            User u = Database.one(User.class, c, "SELECT * FROM users WHERE email=? AND status='ACTIVE'", email == null ? "" : email.trim());
            if (u == null || !matches(password, u.getPasswordHash())) {
                throw new IllegalArgumentException("Thư điện tử hoặc mật khẩu không đúng.");
            }
            Database.update(c, "UPDATE users SET last_login_at=SYSUTCDATETIME() WHERE id=?", u.getId());
            return new SessionUser(u.getId().longValue(), u.getFullName(), u.getRole(), u.getPasswordHash());
        }
    }

    public void register(String role, String email, String pass, String confirm, String name, String phone, String store) throws Exception {
        email = Validation.text(email, 254).toLowerCase(Locale.ROOT);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Thư điện tử không hợp lệ.");
        }
        String hash = BCrypt.hashpw(Validation.password(pass, confirm), BCrypt.gensalt(12));
        name = Validation.text(name, 100);
        phone = Validation.text(phone, 20);
        try (Connection c = new DBConnect().getConnection()) {
            if ("SELLER".equals(role)) {
                Database.rows(RegistrationResult.class, c, "EXEC dbo.register_seller ?,?,?,?,?", email, hash, name, Validation.text(store, 100), phone);
            } else if ("CUSTOMER".equals(role)) {
                Database.rows(RegistrationResult.class, c, "EXEC dbo.register_customer ?,?,?,?", email, hash, name, phone);
            } else {
                throw new IllegalArgumentException("Loại tài khoản không hợp lệ.");
            }
        }
    }

    public User profile(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return users.findProfile(c, id);
        }
    }

    public List<Address> addresses(long id) throws Exception {
        try (Connection c = new DBConnect().getConnection()) {
            return addressDao.findByUser(c, id);
        }
    }

    /** Updates name, email and phone of the current user; returns the saved name. Errors are reported per field. */
    public String profile(long id, String name, String email, String phone) throws Exception {
        Map<String, String> errors = new LinkedHashMap<>();
        String n = FieldErrors.check(errors, "name", () -> Validation.field(name, 100, "họ tên", true));
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
                User before = users.findProfile(c, id);
                users.updateProfile(c, id, n, e, p);
                audit.log(c, id, "User", id, "PROFILE_UPDATED", profileJson(before.getFullName(), before.getEmail(), before.getPhone()), profileJson(n, e, p), null);
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

    private static String addressJson(Address a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("recipient_name", a.getRecipientName());
        m.put("phone", a.getPhone());
        m.put("address_line", a.getAddressLine());
        m.put("ward", a.getWard());
        m.put("district", a.getDistrict());
        m.put("city", a.getCity());
        m.put("is_default", a.getIsDefault());
        return AuditDAO.json(m);
    }

    /**
     * Changes the password after checking the current one (FE-01.2, BA-06). Passwords are never trimmed.
     * Errors are reported per field: old, password, confirm. Validation.password is shared and left unchanged.
     */
    public void password(long id, String old, String pass, String confirm) throws Exception {
        Map<String, String> errors = new LinkedHashMap<>();
        if (old == null || old.isEmpty()) {
            errors.put("old", Messages.get(Messages.MSG02, "mật khẩu hiện tại"));
        }
        if (pass == null || pass.isEmpty()) {
            errors.put("password", Messages.get(Messages.MSG02, "mật khẩu mới"));
        } else if (pass.length() > 64) {
            errors.put("password", Messages.get(Messages.MSG03, "mật khẩu mới", 64));
        } else if (pass.length() < 8 || pass.getBytes(StandardCharsets.UTF_8).length > 72) {
            errors.put("password", Messages.get(Messages.MSG04, "mật khẩu mới (8–64 ký tự)"));
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
                    throw FieldErrors.of("old", Messages.get(Messages.MSG04, "mật khẩu hiện tại"));
                }
                if (matches(pass, hash)) {
                    throw FieldErrors.of("password", Messages.get(Messages.MSG04, "mật khẩu mới") + " Mật khẩu mới phải khác mật khẩu hiện tại.");
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
                Address before = aid == null ? null : addressDao.findById(c, aid, uid);
                if (delete) {
                    boolean wasDefault = Boolean.TRUE.equals(before.getIsDefault());
                    addressDao.delete(c, aid, uid);
                    message = Messages.get(Messages.MSG81);
                    String note = null;
                    if (wasDefault) {
                        // The oldest remaining address takes over, and the user is told which one.
                        addressDao.makeFirstDefault(c, uid);
                        Address next = addressDao.findDefault(c, uid);
                        if (next != null) {
                            message = Messages.get(Messages.MSG82, next.getRecipientName() + " - " + next.getAddressLine() + ", "
                                    + next.getWard() + ", " + next.getDistrict() + ", " + next.getCity());
                            note = "Default moved to address #" + next.getId();
                        }
                    }
                    audit.log(c, uid, "Address", aid, "ADDRESS_DELETED", addressJson(before), null, note);
                } else if (action.equals("defaultAddress")) {
                    Address previous = addressDao.findDefault(c, uid);
                    addressDao.clearDefault(c, uid);
                    addressDao.setDefault(c, aid, uid);
                    audit.log(c, uid, "Address", aid, "ADDRESS_DEFAULT_SET", previous == null ? null : previous.getId().toString(), aid.toString(), null);
                    message = Messages.get(Messages.MSG18);
                } else {
                    Map<String, String> errors = new LinkedHashMap<>();
                    String n = name, p = phone, l = line, w = ward, d = district, ct = city;
                    name = FieldErrors.check(errors, "name", () -> Validation.field(n, 100, "tên người nhận", true));
                    phone = FieldErrors.check(errors, "phone", () -> Validation.phone(p, true));
                    line = FieldErrors.check(errors, "line", () -> Validation.field(l, 255, "địa chỉ", true));
                    ward = FieldErrors.check(errors, "ward", () -> Validation.field(w, 100, "phường/xã", true));
                    district = FieldErrors.check(errors, "district", () -> Validation.field(d, 100, "quận/huyện", true));
                    city = FieldErrors.check(errors, "city", () -> Validation.field(ct, 100, "tỉnh/thành phố", true));
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
                    message = Messages.get(Messages.MSG05, "Địa chỉ");
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
            User u = Database.one(User.class, c, "SELECT id,email FROM users WHERE email=? AND status='ACTIVE'", email);
            if (u == null) {
                return;
            }
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            String token = HexFormat.of().formatHex(random);
            Database.update(c, "INSERT password_reset_tokens(user_id,token_hash,expires_at) VALUES(?,?,DATEADD(minute,20,SYSUTCDATETIME()))", u.getId(), digest(token));
            String base = System.getenv().getOrDefault("FRESHORA_PUBLIC_URL", "http://localhost:9999/Freshora");
            try {
                new MailService().recovery(u.getEmail(),base+"/reset?token="+token);
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
                PasswordResetToken owner=Database.one(PasswordResetToken.class, c, "SELECT user_id FROM password_reset_tokens WHERE token_hash=?",tokenHash);
                if(owner==null) throw new IllegalArgumentException("Liên kết đã hết hạn hoặc đã được sử dụng.");
                Database.one(User.class, c, "SELECT id FROM users WITH(UPDLOCK,HOLDLOCK) WHERE id=?",owner.getUserId());
                PasswordResetToken t = Database.one(PasswordResetToken.class, c, "SELECT id,user_id FROM password_reset_tokens WITH(UPDLOCK,HOLDLOCK) WHERE token_hash=? AND used_at IS NULL AND expires_at>SYSUTCDATETIME()", tokenHash);
                if (t == null) {
                    throw new IllegalArgumentException("Liên kết đã hết hạn hoặc đã được sử dụng.");
                }
                Database.update(c, "UPDATE users SET password_hash=?,updated_at=SYSUTCDATETIME() WHERE id=?", BCrypt.hashpw(pass, BCrypt.gensalt(12)), t.getUserId());
                Database.update(c, "UPDATE password_reset_tokens SET used_at=SYSUTCDATETIME() WHERE user_id=? AND used_at IS NULL", t.getUserId());
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }
}
