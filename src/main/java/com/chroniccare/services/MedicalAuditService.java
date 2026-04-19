package com.chroniccare.services;

import com.chroniccare.models.MedicalAuditEvent;
import com.chroniccare.models.User;
import com.chroniccare.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalAuditService {

    private final Connection conn = MyDatabase.getInstance().getConnection();
    private volatile boolean schemaChecked = false;

    private void ensureSchema() {
        if (schemaChecked) return;
        synchronized (this) {
            if (schemaChecked) return;
            if (conn == null) return;

            String sql = """
                    CREATE TABLE IF NOT EXISTS medical_audit_events (
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      actor_user_id INT NOT NULL,
                      actor_role VARCHAR(50) NOT NULL,
                      target_user_id INT NOT NULL,
                      event_action VARCHAR(60) NOT NULL,
                      details TEXT NULL,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      INDEX idx_med_audit_created_at (created_at),
                      INDEX idx_med_audit_target (target_user_id),
                      INDEX idx_med_audit_actor (actor_user_id),
                      INDEX idx_med_audit_action (event_action)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """;

            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
                boolean hasEventAction = columnExists("medical_audit_events", "event_action");
                if (!hasEventAction && columnExists("medical_audit_events", "action")) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("ALTER TABLE medical_audit_events ADD COLUMN event_action VARCHAR(60) NOT NULL DEFAULT 'UNKNOWN'");
                        st.executeUpdate("UPDATE medical_audit_events SET event_action = `action` WHERE event_action = 'UNKNOWN'");
                        try {
                            st.executeUpdate("CREATE INDEX idx_med_audit_action ON medical_audit_events(event_action)");
                        } catch (SQLException ignored) {
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                schemaChecked = true;
            }
        }
    }

    public void logEvent(User actor, int targetUserId, String action, String details) {
        if (actor == null) return;
        if (conn == null) return;
        if (action == null || action.isBlank()) return;

        ensureSchema();

        String sql = "INSERT INTO medical_audit_events (actor_user_id, actor_role, target_user_id, event_action, details, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, actor.getId());
            ps.setString(2, extractPrimaryRole(actor));
            ps.setInt(3, targetUserId);
            ps.setString(4, action.trim());
            ps.setString(5, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<MedicalAuditEvent> getRecentEvents(int limit) throws SQLException {
        ensureSchema();
        if (conn == null) return List.of();

        int safeLimit = Math.max(1, Math.min(limit, 1000));

        String sql = """
                SELECT e.id,
                       e.actor_role,
                       e.event_action AS action,
                       e.details,
                       e.created_at,
                       CONCAT(IFNULL(a.prenom,''), ' ', IFNULL(a.nom,'')) AS actor_name,
                       CONCAT(IFNULL(t.prenom,''), ' ', IFNULL(t.nom,'')) AS target_name
                FROM medical_audit_events e
                LEFT JOIN users a ON a.id = e.actor_user_id
                LEFT JOIN users t ON t.id = e.target_user_id
                ORDER BY e.created_at DESC
                LIMIT ?
                """;

        List<MedicalAuditEvent> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, safeLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MedicalAuditEvent(
                            rs.getInt("id"),
                            cleanName(rs.getString("actor_name")),
                            rs.getString("actor_role"),
                            rs.getString("action"),
                            cleanName(rs.getString("target_name")),
                            rs.getString("details"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        }

        return list;
    }

    private String extractPrimaryRole(User user) {
        String roles = user.getRoles();
        if (roles == null) return "UNKNOWN";
        if (roles.contains("ROLE_ADMIN")) return "ROLE_ADMIN";
        if (roles.contains("ROLE_PATIENT")) return "ROLE_PATIENT";
        if (roles.contains("ROLE_COACH")) return "ROLE_COACH";
        if (roles.contains("ROLE_NUTRITIONNISTE")) return "ROLE_NUTRITIONNISTE";
        return roles;
    }

    private String cleanName(String name) {
        if (name == null) return "";
        String n = name.trim().replaceAll("\\s+", " ");
        return n.equals("") ? "" : n;
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }
}
