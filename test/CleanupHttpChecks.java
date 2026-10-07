import config.DBConnect;
import dao.Database;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
public class CleanupHttpChecks {
 public static void main(String[] args)throws Exception {
  Set<String> emails=new HashSet<>();
  for(String path:args)if(Files.exists(Path.of(path)))emails.addAll(Files.readAllLines(Path.of(path)));
  try(Connection c=new DBConnect().getConnection()){
   c.setAutoCommit(false);
   for(String email:emails){
    if(!email.matches("iter1-http-[0-9]+@test\\.invalid"))throw new IllegalArgumentException("Unexpected cleanup target");
    Database.update(c,"DELETE FROM password_reset_tokens WHERE user_id IN (SELECT id FROM users WHERE email=?)",email);
    Database.update(c,"DELETE FROM users WHERE email=?",email);
   }
   c.commit();System.out.println("Removed "+emails.size()+" HTTP test accounts only.");
  }
 }
}
