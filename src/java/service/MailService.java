package service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.nio.file.*;
import java.util.*;

public class MailService {

    private static String env(String key) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? null : v.trim();
    }

    /**
     * Gui thu khoi phuc mat khau.
     * Che do gui thu:
     *  - FRESHORA_MAIL_MODE=smtp   : gui that qua SMTP (Gmail)
     *  - FRESHORA_MAIL_MODE=outbox : luu file .eml (chi de phat trien)
     *  - Khong dat bien nay        : co SMTP user + password thi dung smtp, nguoc lai dung outbox
     * Tra ve duong dan file .eml neu dung outbox, tra ve null neu da gui that.
     */
    public String recovery(String recipient, String link) throws Exception {
        String user = env("FRESHORA_SMTP_USER"), password = env("FRESHORA_SMTP_PASSWORD");
        String mode = env("FRESHORA_MAIL_MODE");
        if (mode == null) {
            mode = (user != null && password != null) ? "smtp" : "outbox";
        }
        mode = mode.toLowerCase(Locale.ROOT);
        if (!mode.equals("smtp") && !mode.equals("outbox")) {
            throw new IllegalArgumentException("Chế độ gửi thư không hợp lệ: " + mode + ". Chỉ chấp nhận smtp hoặc outbox.");
        }

        String host = env("FRESHORA_SMTP_HOST") == null ? "smtp.gmail.com" : env("FRESHORA_SMTP_HOST");
        boolean local = Set.of("localhost", "127.0.0.1", "::1").contains(host);
        Properties p = new Properties();
        p.setProperty("mail.smtp.host", host);
        p.setProperty("mail.smtp.port", env("FRESHORA_SMTP_PORT") == null ? "587" : env("FRESHORA_SMTP_PORT"));
        p.setProperty("mail.smtp.auth", String.valueOf(user != null));
        p.setProperty("mail.smtp.starttls.enable", String.valueOf(!local));
        p.setProperty("mail.smtp.starttls.required", String.valueOf(!local));
        p.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        p.setProperty("mail.smtp.connectiontimeout", "10000");
        p.setProperty("mail.smtp.timeout", "10000");
        p.setProperty("mail.smtp.writetimeout", "10000");
        Session session = Session.getInstance(p);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(user == null ? "noreply@freshora.test" : user));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient, true));
        message.setSubject("Đặt lại mật khẩu Freshora", "UTF-8");
        message.setText("Sử dụng liên kết sau để đặt lại mật khẩu:\n" + link
                + "\n\nLiên kết có hiệu lực trong 20 phút và chỉ dùng một lần. "
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua thư này.", "UTF-8");
        message.setSentDate(new Date());

        if (mode.equals("outbox")) {
            Path dir = Path.of(System.getProperty("catalina.base", System.getProperty("user.home")), "freshora-private-outbox");
            Files.createDirectories(dir);
            Path file = dir.resolve(System.currentTimeMillis() + "-" + UUID.randomUUID() + ".eml");
            try (var out = Files.newOutputStream(file)) {
                message.writeTo(out);
            }
            System.out.println("[Freshora] Che do outbox: thu khoi phuc mat khau duoc luu tai " + file);
            return file.toString();
        }

        if (!local && (user == null || password == null)) {
            throw new IllegalArgumentException("Chưa cấu hình tài khoản Gmail gửi thư (FRESHORA_SMTP_USER và FRESHORA_SMTP_PASSWORD).");
        }
        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(host, user, password);
            transport.sendMessage(message, message.getAllRecipients());
        } catch (AuthenticationFailedException e) {
            throw new IllegalArgumentException("Gmail từ chối đăng nhập. Hãy dùng Mật khẩu ứng dụng (App Password) 16 ký tự, không dùng mật khẩu Gmail thường.", e);
        } catch (MessagingException e) {
            throw new IllegalArgumentException("Không gửi được thư. Kiểm tra kết nối mạng và cấu hình SMTP.", e);
        }
        return null;
    }
}