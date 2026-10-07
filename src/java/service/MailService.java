package service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.nio.file.*;
import java.util.*;

public class MailService {
    public void recovery(String recipient, String link) throws Exception {
        String mode=System.getenv().getOrDefault("FRESHORA_MAIL_MODE","smtp");
        String user=System.getenv("FRESHORA_SMTP_USER"), password=System.getenv("FRESHORA_SMTP_PASSWORD");
        Properties p=new Properties();
        String host=System.getenv().getOrDefault("FRESHORA_SMTP_HOST","smtp.gmail.com");
        p.setProperty("mail.smtp.host",host);
        p.setProperty("mail.smtp.port",System.getenv().getOrDefault("FRESHORA_SMTP_PORT","587"));
        boolean local=Set.of("localhost","127.0.0.1","::1").contains(host);
        p.setProperty("mail.smtp.auth",String.valueOf(user!=null && !user.isBlank()));
        p.setProperty("mail.smtp.starttls.enable",String.valueOf(!local));
        p.setProperty("mail.smtp.starttls.required",String.valueOf(!local));
        p.setProperty("mail.smtp.ssl.checkserveridentity","true");
        p.setProperty("mail.smtp.connectiontimeout","10000");
        p.setProperty("mail.smtp.timeout","10000");
        p.setProperty("mail.smtp.writetimeout","10000");
        Session session=Session.getInstance(p);
        MimeMessage message=new MimeMessage(session);
        message.setFrom(new InternetAddress(user==null ? "noreply@freshora.test" : user));
        message.setRecipient(Message.RecipientType.TO,new InternetAddress(recipient,true));
        message.setSubject("Reset your Freshora password","UTF-8");
        message.setText("Use this link to reset your password:\n"+link+"\n\nThe link expires in 20 minutes and can be used once. If you did not request this, ignore this email.","UTF-8");
        message.setSentDate(new Date());
        if("outbox".equals(mode)) {
            Path dir=Path.of(System.getProperty("catalina.base",System.getProperty("user.home")),"freshora-private-outbox");
            Files.createDirectories(dir);
            try(var out=Files.newOutputStream(dir.resolve(UUID.randomUUID()+".eml"))){message.writeTo(out);}
        } else {
            if(!"smtp".equals(mode)) throw new IllegalStateException("Unknown mail mode.");
            if(!local && (user==null || user.isBlank() || password==null || password.isBlank())) throw new IllegalStateException("Configure Gmail SMTP credentials before sending recovery emails.");
            try(Transport transport=session.getTransport("smtp")) {
                transport.connect(host,user,password);
                transport.sendMessage(message,message.getAllRecipients());
            }
        }
    }
}
