package com.chroniccare.services;

import com.chroniccare.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientMedicalRecordService {

    private final Connection conn = MyDatabase.getInstance().getConnection();

    public boolean hasRecord(int userId) throws SQLException {
        String sql = "SELECT 1 FROM patient_medical_record WHERE user_id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void ensureRecordExists(int userId, String mainCondition) throws SQLException {
        if (hasRecord(userId)) return;
        createInitialRecord(userId, mainCondition);
    }

    public void createInitialRecord(int userId, String mainCondition) throws SQLException {
        String sql = "INSERT INTO patient_medical_record (user_id, main_condition, created_at) VALUES (?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, mainCondition);
            ps.executeUpdate();
        }
    }

    public void saveGeneratedRecord(int userId, String generatedRecord, String questionnaireAnswersJson) throws SQLException {
        String sql = "UPDATE patient_medical_record " +
                "SET generated_record = ?, questionnaire_answers = ?, updated_at = NOW() " +
                "WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, generatedRecord);
            ps.setString(2, questionnaireAnswersJson);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public void saveImportedPdfText(int userId, String importedPdfText) throws SQLException {
        String sql = "UPDATE patient_medical_record " +
                "SET imported_pdf_text = ?, updated_at = NOW() " +
                "WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, importedPdfText);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void saveQuestionnaireAnswers(int userId, String questionnaireAnswersJson) throws SQLException {
        String sql = "UPDATE patient_medical_record " +
                "SET questionnaire_answers = ?, updated_at = NOW() " +
                "WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, questionnaireAnswersJson);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateGeneratedRecord(int userId, String generatedRecord) throws SQLException {
        String sql = "UPDATE patient_medical_record " +
                "SET generated_record = ?, updated_at = NOW() " +
                "WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, generatedRecord);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateMainCondition(int userId, String mainCondition) throws SQLException {
        String sql = "UPDATE patient_medical_record " +
                "SET main_condition = ?, updated_at = NOW() " +
                "WHERE user_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mainCondition);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public String getGeneratedRecord(int userId) throws SQLException {
        String sql = "SELECT generated_record FROM patient_medical_record WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("generated_record");
            }
        }
        return null;
    }

    public String getQuestionnaireAnswers(int userId) throws SQLException {
        String sql = "SELECT questionnaire_answers FROM patient_medical_record WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("questionnaire_answers");
            }
        }
        return null;
    }

    public String getImportedPdfText(int userId) throws SQLException {
        String sql = "SELECT imported_pdf_text FROM patient_medical_record WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("imported_pdf_text");
            }
        }
        return null;
    }

    public String getMainCondition(int userId) throws SQLException {
        String sql = "SELECT main_condition FROM patient_medical_record WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("main_condition");
            }
        }
        return null;
    }

    public void addCondition(int userId, String conditionName) throws SQLException {
        String sql = "INSERT INTO patient_medical_condition (user_id, condition_name, created_at) VALUES (?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, conditionName);
            ps.executeUpdate();
        }
    }

    public List<String> getSecondaryConditions(int userId) throws SQLException {
        String sql = "SELECT condition_name FROM patient_medical_condition WHERE user_id = ?";
        List<String> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("condition_name"));
                }
            }
        }
        return list;
    }

    public void removeCondition(int userId, String conditionName) throws SQLException {
        String sql = "DELETE FROM patient_medical_condition WHERE user_id = ? AND condition_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, conditionName);
            ps.executeUpdate();
        }
    }
}
