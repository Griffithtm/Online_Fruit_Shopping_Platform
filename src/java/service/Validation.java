package service;

import java.math.BigDecimal;

public final class Validation {

    public static String text(String v, int max) {
        if (v == null || v.trim().isEmpty() || v.trim().length() > max) {
            throw new IllegalArgumentException("Please complete the required fields within the allowed length.");
        }
        return v.trim();
    }

    public static long id(String v) {
        try {
            long n = Long.parseLong(v);
            if (n > 0) {
                return n;
            }
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException("Invalid record ID.");
    }

    public static int quantity(String v) {
        long n = id(v);
        if (n > 10000) {
            throw new IllegalArgumentException("The maximum quantity is 10,000.");
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
        throw new IllegalArgumentException("Invalid amount.");
    }

    public static String password(String p, String confirm) {
        if (p == null || p.length() < 8 || p.length() > 64 || p.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72 || !p.equals(confirm)) {
            throw new IllegalArgumentException("Password must be 8–64 characters, at most 72 bytes, and match the confirmation.");
        }
        return p;
    }
}
