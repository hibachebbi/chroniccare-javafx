package com.chroniccare.services;

import com.chroniccare.models.Appointment;
import com.chroniccare.models.User;
import com.chroniccare.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentService {
    private final Connection conn = MyDatabase.getInstance().getConnection();

    public AppointmentService() {
        ensureSchema();
    }

    public void ensureSchema() {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS appointments (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "patient_id INT NOT NULL," +
                            "nutritionist_id INT NOT NULL," +
                            "motif TEXT NOT NULL," +
                            "requested_datetime DATETIME NOT NULL," +
                            "scheduled_datetime DATETIME NULL," +
                            "urgency_level VARCHAR(50) NULL," +
                            "follow_up_type VARCHAR(100) NULL," +
                            "medical_snapshot TEXT NULL," +
                            "status VARCHAR(40) NOT NULL DEFAULT 'PENDING'," +
                            "nutritionist_response TEXT NULL," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE," +
                            "CONSTRAINT fk_appointments_nutritionist FOREIGN KEY (nutritionist_id) REFERENCES users(id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci"
            );
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS consultations (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "appointment_id INT NOT NULL UNIQUE," +
                            "patient_id INT NOT NULL," +
                            "nutritionist_id INT NOT NULL," +
                            "theme VARCHAR(255) NOT NULL," +
                            "summary TEXT NOT NULL," +
                            "recommendations TEXT NOT NULL," +
                            "meal_plan TEXT NOT NULL," +
                            "objectives TEXT NOT NULL," +
                            "medical_notes TEXT NULL," +
                            "consultation_datetime DATETIME NOT NULL," +
                            "follow_up_date DATE NULL," +
                            "is_read TINYINT(1) NOT NULL DEFAULT 0," +
                            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "CONSTRAINT fk_consultations_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE," +
                            "CONSTRAINT fk_consultations_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE CASCADE," +
                            "CONSTRAINT fk_consultations_nutritionist FOREIGN KEY (nutritionist_id) REFERENCES users(id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci"
            );
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'initialiser le schema RDV/consultations", e);
        }
    }

    public List<User> getNutritionists() throws SQLException {
        List<User> nutritionists = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, email, telephone, genre, roles, approval_status, " +
                "medical_condition, photo_profil, is_active " +
                "FROM users " +
                "WHERE roles LIKE '%ROLE_NUTRITIONNISTE%' AND is_active = 1 " +
                "ORDER BY prenom, nom";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                user.setTelephone(rs.getString("telephone"));
                user.setGenre(rs.getString("genre"));
                user.setRoles(rs.getString("roles"));
                user.setApprovalStatus(rs.getString("approval_status"));
                user.setMedicalCondition(rs.getString("medical_condition"));
                user.setPhotoProfil(rs.getString("photo_profil"));
                user.setActive(rs.getBoolean("is_active"));
                nutritionists.add(user);
            }
        }
        return nutritionists;
    }

    public void createAppointment(int patientId,
                                  int nutritionistId,
                                  String motif,
                                  LocalDateTime requestedDateTime,
                                  String urgencyLevel,
                                  String followUpType,
                                  String medicalSnapshot) throws SQLException {
        String sql = "INSERT INTO appointments " +
                "(patient_id, nutritionist_id, motif, requested_datetime, urgency_level, follow_up_type, medical_snapshot, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setInt(2, nutritionistId);
            ps.setString(3, motif);
            ps.setTimestamp(4, Timestamp.valueOf(requestedDateTime));
            ps.setString(5, emptyToNull(urgencyLevel));
            ps.setString(6, emptyToNull(followUpType));
            ps.setString(7, emptyToNull(medicalSnapshot));
            ps.setString(8, Appointment.STATUS_PENDING);
            ps.executeUpdate();
        }
    }

    public List<Appointment> getAppointmentsForPatient(int patientId) throws SQLException {
        String sql = baseAppointmentQuery() +
                " WHERE a.patient_id = ? ORDER BY " +
                "CASE a.status " +
                " WHEN 'PENDING' THEN 0 " +
                " WHEN 'RESCHEDULE_PROPOSED' THEN 1 " +
                " WHEN 'PLANNED' THEN 2 " +
                " WHEN 'COMPLETED' THEN 3 " +
                " ELSE 4 END, a.requested_datetime DESC";
        return fetchAppointments(sql, patientId);
    }

    public List<Appointment> getAppointmentsForNutritionist(int nutritionistId) throws SQLException {
        String sql = baseAppointmentQuery() +
                " WHERE a.nutritionist_id = ? ORDER BY " +
                "CASE a.status " +
                " WHEN 'PENDING' THEN 0 " +
                " WHEN 'RESCHEDULE_PROPOSED' THEN 1 " +
                " WHEN 'PLANNED' THEN 2 " +
                " WHEN 'COMPLETED' THEN 3 " +
                " ELSE 4 END, a.requested_datetime ASC";
        return fetchAppointments(sql, nutritionistId);
    }

    public Appointment getAppointmentById(int appointmentId) throws SQLException {
        String sql = baseAppointmentQuery() + " WHERE a.id = ?";
        List<Appointment> appointments = fetchAppointments(sql, appointmentId);
        return appointments.isEmpty() ? null : appointments.get(0);
    }

    public int countUpcomingForPatient(int patientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE patient_id = ? AND status IN (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, Appointment.STATUS_PENDING);
            ps.setString(3, Appointment.STATUS_PLANNED);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countPendingForNutritionist(int nutritionistId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM appointments WHERE nutritionist_id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nutritionistId);
            ps.setString(2, Appointment.STATUS_PENDING);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void acceptAppointment(int appointmentId, LocalDateTime scheduledDateTime, String response) throws SQLException {
        updateAppointmentStatus(appointmentId, Appointment.STATUS_PLANNED, scheduledDateTime, response);
    }

    public void refuseAppointment(int appointmentId, String response) throws SQLException {
        updateAppointmentStatus(appointmentId, Appointment.STATUS_REFUSED, null, response);
    }

    public void proposeReschedule(int appointmentId, LocalDateTime scheduledDateTime, String response) throws SQLException {
        updateAppointmentStatus(appointmentId, Appointment.STATUS_RESCHEDULE_PROPOSED, scheduledDateTime, response);
    }

    public void markCompleted(int appointmentId) throws SQLException {
        String sql = "UPDATE appointments SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Appointment.STATUS_COMPLETED);
            ps.setInt(2, appointmentId);
            ps.executeUpdate();
        }
    }

    private void updateAppointmentStatus(int appointmentId,
                                         String status,
                                         LocalDateTime scheduledDateTime,
                                         String response) throws SQLException {
        String sql = "UPDATE appointments SET status = ?, scheduled_datetime = ?, nutritionist_response = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (scheduledDateTime != null) {
                ps.setTimestamp(2, Timestamp.valueOf(scheduledDateTime));
            } else {
                ps.setTimestamp(2, null);
            }
            ps.setString(3, emptyToNull(response));
            ps.setInt(4, appointmentId);
            ps.executeUpdate();
        }
    }

    private List<Appointment> fetchAppointments(String sql, int parameter) throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parameter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    appointments.add(mapAppointment(rs));
                }
            }
        }
        return appointments;
    }

    private Appointment mapAppointment(ResultSet rs) throws SQLException {
        Appointment appointment = new Appointment();
        appointment.setId(rs.getInt("id"));
        appointment.setPatientId(rs.getInt("patient_id"));
        appointment.setNutritionistId(rs.getInt("nutritionist_id"));
        appointment.setPatientName(rs.getString("patient_name"));
        appointment.setNutritionistName(rs.getString("nutritionist_name"));
        appointment.setMotif(rs.getString("motif"));
        Timestamp requested = rs.getTimestamp("requested_datetime");
        if (requested != null) {
            appointment.setRequestedDateTime(requested.toLocalDateTime());
        }
        Timestamp scheduled = rs.getTimestamp("scheduled_datetime");
        if (scheduled != null) {
            appointment.setScheduledDateTime(scheduled.toLocalDateTime());
        }
        appointment.setUrgencyLevel(rs.getString("urgency_level"));
        appointment.setFollowUpType(rs.getString("follow_up_type"));
        appointment.setMedicalSnapshot(rs.getString("medical_snapshot"));
        appointment.setStatus(rs.getString("status"));
        appointment.setNutritionistResponse(rs.getString("nutritionist_response"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            appointment.setCreatedAt(created.toLocalDateTime());
        }
        return appointment;
    }

    private String baseAppointmentQuery() {
        return "SELECT a.*, " +
                "CONCAT(p.prenom, ' ', p.nom) AS patient_name, " +
                "CONCAT(n.prenom, ' ', n.nom) AS nutritionist_name " +
                "FROM appointments a " +
                "JOIN users p ON p.id = a.patient_id " +
                "JOIN users n ON n.id = a.nutritionist_id";
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
