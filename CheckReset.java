import java.sql.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
public class CheckReset {
  static String sha256Hex(String value) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) sb.append(String.format("%02x", b));
    return sb.toString();
  }
  public static void main(String[] args) throws Exception {
    Class.forName("com.mysql.cj.jdbc.Driver");
    try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/chroniccare?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC","root","")) {
      String email = "hamaamri006@gmail.com";
      String code = "620137";
      String hash = sha256Hex(code);
      System.out.println("HASH=" + hash);
      try (PreparedStatement ps = c.prepareStatement("SELECT id, email, is_active, password FROM users WHERE email=?")) {
        ps.setString(1, email);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            System.out.println("USER id="+rs.getInt("id")+" email="+rs.getString("email")+" active="+rs.getBoolean("is_active")+" password="+rs.getString("password"));
          }
        }
      }
      try (PreparedStatement ps = c.prepareStatement(
        "SELECT prt.id, prt.user_id, prt.token_hash, prt.expires_at, prt.used_at, prt.created_at, u.email " +
        "FROM password_reset_tokens prt JOIN users u ON u.id=prt.user_id " +
        "WHERE u.email=? ORDER BY prt.created_at DESC LIMIT 10")) {
        ps.setString(1, email);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            System.out.println("TOKEN id="+rs.getInt("id")+" user_id="+rs.getInt("user_id")+" email="+rs.getString("email")+" expires="+rs.getTimestamp("expires_at")+" used="+rs.getTimestamp("used_at")+" created="+rs.getTimestamp("created_at")+" hash="+rs.getString("token_hash"));
          }
        }
      }
      try (PreparedStatement ps = c.prepareStatement(
        "SELECT prt.id, prt.user_id, prt.expires_at, prt.used_at, prt.created_at, u.email FROM password_reset_tokens prt JOIN users u ON u.id=prt.user_id WHERE prt.token_hash=? ORDER BY prt.created_at DESC LIMIT 10")) {
        ps.setString(1, hash);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            System.out.println("MATCH id="+rs.getInt("id")+" user_id="+rs.getInt("user_id")+" email="+rs.getString("email")+" expires="+rs.getTimestamp("expires_at")+" used="+rs.getTimestamp("used_at")+" created="+rs.getTimestamp("created_at"));
          }
        }
      }
    }
  }
}
