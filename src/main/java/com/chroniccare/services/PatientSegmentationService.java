package com.chroniccare.services;

import com.chroniccare.models.PatientSegmentationRow;
import com.chroniccare.utils.MyDatabase;

import java.sql.*;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatientSegmentationService {

    private final Connection conn = MyDatabase.getInstance().getConnection();
    private volatile boolean schemaChecked = false;

    public List<PatientSegmentationRow> getPatientsSegmentation() throws SQLException {
        ensureAuditSchema();
        if (conn == null) return List.of();

        String sql = """
                SELECT u.id,
                       u.nom,
                       u.prenom,
                       u.genre,
                       u.medical_condition,
                       u.activity_score,
                       pmr.main_condition AS record_main_condition,
                       pmr.imported_pdf_text,
                       pmr.generated_record,
                       (SELECT COUNT(*) FROM patient_medical_condition pmc WHERE pmc.user_id = u.id) AS secondary_count,
                       COALESCE(a.views30, 0) AS views30,
                       COALESCE(a.updates30, 0) AS updates30,
                       a.last_activity
                FROM users u
                LEFT JOIN patient_medical_record pmr ON pmr.user_id = u.id
                LEFT JOIN (
                  SELECT actor_user_id,
                         SUM(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                                   AND event_action LIKE '%_VIEW'
                                  THEN 1 ELSE 0 END) AS views30,
                         SUM(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                                   AND (event_action LIKE '%_UPDATE'
                                        OR event_action LIKE '%_PDF_%')
                                  THEN 1 ELSE 0 END) AS updates30,
                         MAX(created_at) AS last_activity
                  FROM medical_audit_events
                  GROUP BY actor_user_id
                ) a ON a.actor_user_id = u.id
                WHERE u.roles LIKE '%ROLE_PATIENT%'
                ORDER BY u.id DESC
                """;

        List<PatientSegmentationRow> list = new ArrayList<>();

        try {
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                list.addAll(readRows(rs));
            }
        } catch (SQLException ignored) {
            String fallbackSql = """
                    SELECT u.id,
                           u.nom,
                           u.prenom,
                           u.genre,
                           u.medical_condition,
                           u.activity_score,
                           '' AS record_main_condition,
                           '' AS imported_pdf_text,
                           '' AS generated_record,
                           0 AS secondary_count,
                           COALESCE(a.views30, 0) AS views30,
                           COALESCE(a.updates30, 0) AS updates30,
                           a.last_activity
                    FROM users u
                    LEFT JOIN (
                      SELECT actor_user_id,
                             SUM(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                                       AND event_action LIKE '%_VIEW'
                                      THEN 1 ELSE 0 END) AS views30,
                             SUM(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                                       AND (event_action LIKE '%_UPDATE'
                                            OR event_action LIKE '%_PDF_%')
                                      THEN 1 ELSE 0 END) AS updates30,
                             MAX(created_at) AS last_activity
                      FROM medical_audit_events
                      GROUP BY actor_user_id
                    ) a ON a.actor_user_id = u.id
                    WHERE u.roles LIKE '%ROLE_PATIENT%'
                    ORDER BY u.id DESC
                    """;

            try (PreparedStatement ps = conn.prepareStatement(fallbackSql);
                 ResultSet rs = ps.executeQuery()) {
                list.addAll(readRows(rs));
            }
        }

        return list;
    }

    private List<PatientSegmentationRow> readRows(ResultSet rs) throws SQLException {
        List<PatientSegmentationRow> list = new ArrayList<>();
        while (rs.next()) {
            int userId = rs.getInt("id");
            String patientName = joinName(rs.getString("prenom"), rs.getString("nom"));
            String genre = safe(rs.getString("genre"));
            String mainCondition = safe(rs.getString("record_main_condition"));
            if (mainCondition.isBlank()) mainCondition = safe(rs.getString("medical_condition"));

            String importedPdfText = safe(rs.getString("imported_pdf_text"));
            String generatedRecord = safe(rs.getString("generated_record"));

            int secondaryCount = rs.getInt("secondary_count");
            int activityScore = rs.getInt("activity_score");
            int views30 = rs.getInt("views30");
            int updates30 = rs.getInt("updates30");
            Timestamp lastActivityAt = rs.getTimestamp("last_activity");

            RiskInfo risk = computeRisk(mainCondition, secondaryCount, importedPdfText, generatedRecord);
            BehaviorInfo behavior = computeBehavior(views30, updates30, activityScore, lastActivityAt);

            list.add(new PatientSegmentationRow(
                    userId,
                    patientName.isBlank() ? ("Patient #" + userId) : patientName,
                    genre.isBlank() ? "Non renseigné" : genre,
                    mainCondition.isBlank() ? "Non renseignée" : mainCondition,
                    secondaryCount,
                    activityScore,
                    views30,
                    updates30,
                    lastActivityAt,
                    risk.level,
                    risk.score,
                    risk.details,
                    behavior.segment,
                    behavior.details
            ));
        }
        return list;
    }

    private BehaviorInfo computeBehavior(int views30, int updates30, int activityScore, Timestamp lastActivityAt) {
        int engagement = views30 + (updates30 * 2) + Math.min(Math.max(activityScore, 0), 100) / 10;
        String details = "Vues30=" + views30 + ", Maj30=" + updates30 + ", Score=" + activityScore;

        if (lastActivityAt == null) {
            return new BehaviorInfo("Inactif", details);
        }

        Instant last = lastActivityAt.toInstant();
        long days = Duration.between(last, Instant.now()).toDays();

        if (days > 30) {
            return new BehaviorInfo("Inactif", details);
        }

        if (engagement >= 25 || updates30 >= 10) {
            return new BehaviorInfo("Très actif", details);
        }

        if (engagement >= 10) {
            return new BehaviorInfo("Actif", details);
        }

        return new BehaviorInfo("Occasionnel", details);
    }

    private RiskInfo computeRisk(String mainCondition, int secondaryCount, String importedPdfText, String generatedRecord) {
        if (mainCondition == null || mainCondition.isBlank()) {
            return new RiskInfo("Inconnu", 0, "Condition principale non renseignée.");
        }

        String normalized = normalize(mainCondition);
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (containsAny(normalized, "cancer", "insuffisance", "cardiaque", "infarctus", "avc", "dialyse", "renal", "respiratoire")) {
            score += 3;
            reasons.add("Condition potentiellement sévère");
        } else if (containsAny(normalized, "diabete")) {
            score += 2;
            reasons.add("Diabète");
        } else if (containsAny(normalized, "hypertension", "tension")) {
            score += 2;
            reasons.add("Hypertension");
        } else if (containsAny(normalized, "asthme")) {
            score += 1;
            reasons.add("Asthme");
        } else if (containsAny(normalized, "obesite", "cholesterol")) {
            score += 1;
            reasons.add("Facteur de risque");
        } else {
            score += 1;
            reasons.add("Condition chronique");
        }

        if (secondaryCount >= 2) {
            score += 2;
            reasons.add("Comorbidités multiples (" + secondaryCount + ")");
        } else if (secondaryCount == 1) {
            score += 1;
            reasons.add("Comorbidité (1)");
        }

        if (importedPdfText != null && !importedPdfText.isBlank()) {
            score += 1;
            reasons.add("Documents PDF importés");
        }

        if (generatedRecord != null && !generatedRecord.isBlank()) {
            reasons.add("Dossier généré");
        }

        String level;
        if (score >= 5) level = "Élevé";
        else if (score >= 3) level = "Moyen";
        else level = "Faible";

        return new RiskInfo(level, score, String.join(" · ", reasons));
    }

    private String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String normalized, String... needles) {
        if (normalized == null) return false;
        for (String n : needles) {
            if (n == null || n.isBlank()) continue;
            if (normalized.contains(n)) return true;
        }
        return false;
    }

    private String joinName(String prenom, String nom) {
        String p = safe(prenom);
        String n = safe(nom);
        String full = (p + " " + n).trim();
        return full;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void ensureAuditSchema() {
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

    private static class RiskInfo {
        private final String level;
        private final int score;
        private final String details;

        private RiskInfo(String level, int score, String details) {
            this.level = level;
            this.score = score;
            this.details = details;
        }
    }

    private static class BehaviorInfo {
        private final String segment;
        private final String details;

        private BehaviorInfo(String segment, String details) {
            this.segment = segment;
            this.details = details;
        }
    }
}
