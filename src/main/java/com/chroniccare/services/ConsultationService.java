package com.chroniccare.services;

import com.chroniccare.models.Appointment;
import com.chroniccare.models.Consultation;
import com.chroniccare.utils.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConsultationService {
    private final Connection conn = MyDatabase.getInstance().getConnection();
    private final AppointmentService appointmentService = new AppointmentService();

    public List<Consultation> getConsultationsForPatient(int patientId) throws SQLException {
        String sql = "SELECT c.*, a.status AS appointment_status, " +
                "CONCAT(p.prenom, ' ', p.nom) AS patient_name, " +
                "CONCAT(n.prenom, ' ', n.nom) AS nutritionist_name " +
                "FROM consultations c " +
                "JOIN appointments a ON a.id = c.appointment_id " +
                "JOIN users p ON p.id = c.patient_id " +
                "JOIN users n ON n.id = c.nutritionist_id " +
                "WHERE c.patient_id = ? " +
                "ORDER BY c.consultation_datetime DESC";
        return fetchConsultations(sql, patientId);
    }

    public List<Consultation> getConsultationsForNutritionistPatient(int nutritionistId, int patientId) throws SQLException {
        String sql = "SELECT c.*, a.status AS appointment_status, " +
                "CONCAT(p.prenom, ' ', p.nom) AS patient_name, " +
                "CONCAT(n.prenom, ' ', n.nom) AS nutritionist_name " +
                "FROM consultations c " +
                "JOIN appointments a ON a.id = c.appointment_id " +
                "JOIN users p ON p.id = c.patient_id " +
                "JOIN users n ON n.id = c.nutritionist_id " +
                "WHERE c.nutritionist_id = ? AND c.patient_id = ? " +
                "ORDER BY c.consultation_datetime DESC";
        List<Consultation> consultations = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nutritionistId);
            ps.setInt(2, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    consultations.add(mapConsultation(rs));
                }
            }
        }
        return consultations;
    }

    public Consultation getConsultationByAppointmentId(int appointmentId) throws SQLException {
        String sql = "SELECT c.*, a.status AS appointment_status, " +
                "CONCAT(p.prenom, ' ', p.nom) AS patient_name, " +
                "CONCAT(n.prenom, ' ', n.nom) AS nutritionist_name " +
                "FROM consultations c " +
                "JOIN appointments a ON a.id = c.appointment_id " +
                "JOIN users p ON p.id = c.patient_id " +
                "JOIN users n ON n.id = c.nutritionist_id " +
                "WHERE c.appointment_id = ?";
        List<Consultation> consultations = fetchConsultations(sql, appointmentId);
        return consultations.isEmpty() ? null : consultations.get(0);
    }

    public void saveConsultation(Appointment appointment,
                                 String theme,
                                 String summary,
                                 String recommendations,
                                 String mealPlan,
                                 String objectives,
                                 String medicalNotes,
                                 LocalDate followUpDate) throws SQLException {
        Consultation existing = getConsultationByAppointmentId(appointment.getId());
        if (existing == null) {
            String sql = "INSERT INTO consultations " +
                    "(appointment_id, patient_id, nutritionist_id, theme, summary, recommendations, meal_plan, objectives, medical_notes, consultation_datetime, follow_up_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, appointment.getId());
                ps.setInt(2, appointment.getPatientId());
                ps.setInt(3, appointment.getNutritionistId());
                ps.setString(4, theme);
                ps.setString(5, summary);
                ps.setString(6, recommendations);
                ps.setString(7, mealPlan);
                ps.setString(8, objectives);
                ps.setString(9, emptyToNull(medicalNotes));
                ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
                if (followUpDate != null) {
                    ps.setDate(11, Date.valueOf(followUpDate));
                } else {
                    ps.setDate(11, null);
                }
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE consultations SET theme = ?, summary = ?, recommendations = ?, " +
                    "meal_plan = ?, objectives = ?, medical_notes = ?, follow_up_date = ?, consultation_datetime = ?, is_read = 0 " +
                    "WHERE appointment_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, theme);
                ps.setString(2, summary);
                ps.setString(3, recommendations);
                ps.setString(4, mealPlan);
                ps.setString(5, objectives);
                ps.setString(6, emptyToNull(medicalNotes));
                if (followUpDate != null) {
                    ps.setDate(7, Date.valueOf(followUpDate));
                } else {
                    ps.setDate(7, null);
                }
                ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(9, appointment.getId());
                ps.executeUpdate();
            }
        }
        appointmentService.markCompleted(appointment.getId());
    }

    public void markAsRead(int consultationId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE consultations SET is_read = 1 WHERE id = ?")) {
            ps.setInt(1, consultationId);
            ps.executeUpdate();
        }
    }

    private List<Consultation> fetchConsultations(String sql, int parameter) throws SQLException {
        List<Consultation> consultations = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, parameter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    consultations.add(mapConsultation(rs));
                }
            }
        }
        return consultations;
    }

    private Consultation mapConsultation(ResultSet rs) throws SQLException {
        Consultation consultation = new Consultation();
        consultation.setId(rs.getInt("id"));
        consultation.setAppointmentId(rs.getInt("appointment_id"));
        consultation.setPatientId(rs.getInt("patient_id"));
        consultation.setNutritionistId(rs.getInt("nutritionist_id"));
        consultation.setPatientName(rs.getString("patient_name"));
        consultation.setNutritionistName(rs.getString("nutritionist_name"));
        consultation.setAppointmentStatus(rs.getString("appointment_status"));
        consultation.setTheme(rs.getString("theme"));
        consultation.setSummary(rs.getString("summary"));
        consultation.setRecommendations(rs.getString("recommendations"));
        consultation.setMealPlan(rs.getString("meal_plan"));
        consultation.setObjectives(rs.getString("objectives"));
        consultation.setMedicalNotes(rs.getString("medical_notes"));
        consultation.setRead(rs.getBoolean("is_read"));
        Timestamp consultationDateTime = rs.getTimestamp("consultation_datetime");
        if (consultationDateTime != null) {
            consultation.setConsultationDateTime(consultationDateTime.toLocalDateTime());
        }
        Date followUpDate = rs.getDate("follow_up_date");
        if (followUpDate != null) {
            consultation.setFollowUpDate(followUpDate.toLocalDate());
        }
        return consultation;
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
