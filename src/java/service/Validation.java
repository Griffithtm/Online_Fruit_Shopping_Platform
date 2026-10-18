package service;

import java.math.BigDecimal;
import java.util.Locale;
import util.Messages;

public final class Validation {

    public static String text(String v, int max) {
        if (v == null || v.trim().isEmpty() || v.trim().length() > max) {
            throw new IllegalArgumentException("Vui lòng nhập đủ thông tin, đúng độ dài.");
        }
        return v.trim();
    }

    /** Trimmed text checked against MSG02/MSG03 for {@code field}; an optional blank value returns null. */
    public static String field(String v, int max, String field, boolean required) {
        String t = v == null ? "" : v.trim();
        if (t.isEmpty()) {
            if (required) {
                throw Messages.error(Messages.MSG02, field);
            }
            return null;
        }
        if (t.length() > max) {
            throw Messages.error(Messages.MSG03, field, max);
        }
        return t;
    }

    /** Lower-case email, at most 254 characters (MSG02/MSG03/MSG04). */
    public static String email(String v) {
        String t = field(v, 254, "địa chỉ thư điện tử", true).toLowerCase(Locale.ROOT);
        if (!t.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw Messages.error(Messages.MSG04, "địa chỉ thư điện tử");
        }
        return t;
    }

    /** Phone of 7–15 digits with an optional leading +; spaces, dots and hyphens are removed first. */
    public static String phone(String v, boolean required) {
        String t = v == null ? "" : v.trim().replaceAll("[\\s.\\-]", "");
        if (t.isEmpty()) {
            if (required) {
                throw Messages.error(Messages.MSG02, "số điện thoại");
            }
            return null;
        }
        if (!t.matches("\\+?[0-9]{7,15}")) {
            throw Messages.error(Messages.MSG04, "số điện thoại");
        }
        return t;
    }

    public static long id(String v) {
        try {
            long n = Long.parseLong(v);
            if (n > 0) {
                return n;
            }
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("Mã dữ liệu không hợp lệ.");
    }

    public static int quantity(String v) {
        long n = id(v);
        if (n > 10000) {
            throw new IllegalArgumentException("Số lượng tối đa 10.000.");
        }
        return (int) n;
    }

    public static BigDecimal money(String v, boolean zero) {
        try {
            BigDecimal n = new BigDecimal(v);
            if (n.scale() <= 2 && n.precision() <= 15 && (zero ? n.signum() >= 0 : n.signum() > 0)) {
                return n;
            }
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("Số tiền không hợp lệ.");
    }

    public static String password(String p, String confirm) {
        if (p == null || p.length() < 8 || p.length() > 64 || p.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72 || !p.equals(confirm)) {
            throw new IllegalArgumentException("Mật khẩu cần 8–64 ký tự, tối đa 72 byte, và nhập lại trùng khớp.");
        }
        return p;
    }
}
