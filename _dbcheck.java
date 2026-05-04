import java.sql.*;
public class _dbcheck {
  public static void main(String[] args) throws Exception {
    Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/chroniccare","root","");
    String[] queries = {
      "SHOW TABLES LIKE 'suivi_chat_%'",
      "SELECT id, nom, prenom, email, roles FROM users ORDER BY id LIMIT 30",
      "SELECT id, sender_id, receiver_id, message_text, created_at FROM suivi_chat_messages ORDER BY id DESC LIMIT 30",
      "SELECT user_id, display_name, role_label, last_seen FROM suivi_chat_presence ORDER BY user_id",
      "SELECT user_id, contact_id, last_read_at FROM suivi_chat_read_state ORDER BY user_id, contact_id"
    };
    for (String q : queries) {
      System.out.println("QUERY: " + q);
      try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(q)) {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        while (rs.next()) {
          for (int i=1;i<=n;i++) System.out.print(md.getColumnLabel(i)+"="+rs.getString(i)+" ");
          System.out.println();
        }
      } catch (Exception e) {
        System.out.println("ERR="+e.getMessage());
      }
      System.out.println();
    }
  }
}
