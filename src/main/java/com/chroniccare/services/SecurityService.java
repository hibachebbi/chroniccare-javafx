package com.chroniccare.services;

import com.chroniccare.models.User;
import com.chroniccare.utils.MyDatabase;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SecurityService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int PROFILE_WINDOW_MINUTES = 10;

    private final Connection conn = MyDatabase.getInstance().getConnection();

    public boolean isAccountLocked(User user) {
        if (user == null) return false;

        if (user.isSecurityManualLocked()) {
            return true;
        }

        if (user.getSecurityBlockedUntil() == null) {
            return false;
        }

        return user.getSecurityBlockedUntil()
                .toLocalDateTime()
                .isAfter(LocalDateTime.now());
    }

    public String getRemainingLockMessage(User user) {
        if (user == null) {
            return "Compte bloqué.";
        }

        if (user.isSecurityManualLocked()) {
            return "Compte bloqué par l'administrateur. Contactez l'administrateur.";
        }

        if (user.getSecurityBlockedUntil() == null) {
            return "Compte bloqué.";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = user.getSecurityBlockedUntil().toLocalDateTime();

        if (!until.isAfter(now)) {
            return "Compte bloqué.";
        }

        long minutes = Duration.between(now, until).toMinutes();
        if (minutes <= 0) {
            minutes = 1;
        }

        return "Compte temporairement bloqué. Réessayez dans " + minutes + " minute(s).";
    }

    public void logInvalidEmailFormat(String email) {
        logEvent(null, email, "INVALID_EMAIL_FORMAT", "MEDIUM",
                "Tentative avec format d'email invalide : " + email);
    }

    public void logBlockedLogin(User user, String email) {
        Integer userId = user != null ? user.getId() : null;
        logEvent(userId, email, "LOGIN_BLOCKED", "HIGH",
                "Tentative de connexion sur un compte temporairement bloqué.");
    }

    public void handleFailedLogin(User existingUser, String email) throws SQLException {
        if (existingUser == null) {
            logEvent(null, email, "LOGIN_FAILED", "MEDIUM",
                    "Tentative échouée sur email inexistant ou non reconnu.");
            return;
        }

        int newCount = existingUser.getFailedLoginCount() + 1;

        String sql = "UPDATE users " +
                "SET failed_login_count = ?, last_failed_login_at = NOW() " +
                "WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newCount);
            ps.setInt(2, existingUser.getId());
            ps.executeUpdate();
        }

        logEvent(existingUser.getId(), email, "LOGIN_FAILED",
                newCount >= MAX_FAILED_ATTEMPTS ? "HIGH" : "MEDIUM",
                "Échec de connexion. Nombre d'échecs consécutifs = " + newCount);

        if (newCount >= MAX_FAILED_ATTEMPTS) {
            lockUser(existingUser.getId(), email);
        }
    }

    private void lockUser(int userId, String email) throws SQLException {
        String reason = "Trop de tentatives de connexion avec mot de passe incorrect.";

        String sql = "UPDATE users " +
                "SET security_blocked_until = DATE_ADD(NOW(), INTERVAL " + LOCK_MINUTES + " MINUTE), " +
                "security_block_reason = ?, " +
                "security_manual_locked = 0, " +
                "security_locked_at = NOW(), " +
                "security_locked_by_admin = NULL " +
                "WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }

        logEvent(userId, email, "LOGIN_BLOCKED", "HIGH", reason);
    }

    public void handleSuccessfulLogin(User user) throws SQLException {
        if (user == null) return;

        String sql = "UPDATE users " +
                "SET failed_login_count = 0, " +
                "last_failed_login_at = NULL, " +
                "security_blocked_until = NULL, " +
                "security_block_reason = NULL, " +
                "security_manual_locked = 0, " +
                "security_locked_at = NULL, " +
                "security_locked_by_admin = NULL " +
                "WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
        }

        logEvent(user.getId(), user.getEmail(), "LOGIN_SUCCESS", "LOW", "Connexion réussie.");
    }

    public void analyzeProfileUpdate(User oldUser, User newUser) throws SQLException {
        if (oldUser == null || newUser == null) return;

        List<String> changedFields = new ArrayList<>();
        List<String> sensitiveFields = new ArrayList<>();

        if (!Objects.equals(oldUser.getNom(), newUser.getNom())) {
            changedFields.add("nom");
        }
        if (!Objects.equals(oldUser.getPrenom(), newUser.getPrenom())) {
            changedFields.add("prenom");
        }
        if (!Objects.equals(oldUser.getGenre(), newUser.getGenre())) {
            changedFields.add("genre");
        }
        if (!Objects.equals(oldUser.getEmail(), newUser.getEmail())) {
            changedFields.add("email");
            sensitiveFields.add("email");
        }
        if (!Objects.equals(oldUser.getTelephone(), newUser.getTelephone())) {
            changedFields.add("telephone");
            sensitiveFields.add("telephone");
        }
        if (!Objects.equals(oldUser.getMedicalCondition(), newUser.getMedicalCondition())) {
            changedFields.add("medical_condition");
            sensitiveFields.add("medical_condition");
        }

        if (changedFields.isEmpty()) {
            return;
        }

        String description = "Champs modifiés : " + String.join(", ", changedFields);

        logEvent(newUser.getId(), newUser.getEmail(), "PROFILE_UPDATE",
                sensitiveFields.isEmpty() ? "LOW" : "MEDIUM",
                description);

        int recentSensitiveEvents = countRecentSensitiveProfileEvents(newUser.getId());

        boolean suspicious = sensitiveFields.size() >= 2 || recentSensitiveEvents >= 2;

        if (suspicious) {
            String reason = "Modification suspecte du profil : " + String.join(", ", sensitiveFields);

            String sql = "UPDATE users SET " +
                    "security_blocked_until = DATE_ADD(NOW(), INTERVAL 30 MINUTE), " +
                    "security_block_reason = ?, " +
                    "security_manual_locked = 0, " +
                    "security_locked_at = NOW(), " +
                    "security_locked_by_admin = NULL " +
                    "WHERE id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, reason);
                ps.setInt(2, newUser.getId());
                ps.executeUpdate();
            }

            logEvent(newUser.getId(), newUser.getEmail(), "PROFILE_SUSPICIOUS_UPDATE", "HIGH", reason);
        }
    }

    private int countRecentSensitiveProfileEvents(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM security_events " +
                "WHERE user_id = ? " +
                "AND event_type IN ('PROFILE_UPDATE', 'PROFILE_SUSPICIOUS_UPDATE') " +
                "AND created_at >= DATE_SUB(NOW(), INTERVAL " + PROFILE_WINDOW_MINUTES + " MINUTE) " +
                "AND severity IN ('MEDIUM', 'HIGH')";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    private void logEvent(Integer userId, String email, String eventType, String severity, String description) {
        String sql = "INSERT INTO security_events (user_id, email_attempted, event_type, severity, description, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }

            ps.setString(2, email);
            ps.setString(3, eventType);
            ps.setString(4, severity);
            ps.setString(5, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void adminUnlockUser(int userId, int adminId) throws SQLException {
        String sql = "UPDATE users SET " +
                "failed_login_count = 0, " +
                "last_failed_login_at = NULL, " +
                "security_blocked_until = NULL, " +
                "security_block_reason = NULL, " +
                "security_manual_locked = 0, " +
                "security_locked_at = NULL, " +
                "security_locked_by_admin = NULL " +
                "WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }

        logEvent(userId, null, "ADMIN_UNLOCK", "MEDIUM",
                "Compte débloqué par l'administrateur ID=" + adminId);
    }

    public void adminKeepBlocked(int userId, int adminId, String reason) throws SQLException {
        String finalReason = (reason == null || reason.isBlank())
                ? "Compte maintenu bloqué par l'administrateur après vérification."
                : reason;

        String sql = "UPDATE users SET " +
                "security_manual_locked = 1, " +
                "security_block_reason = ?, " +
                "security_locked_at = NOW(), " +
                "security_locked_by_admin = ?, " +
                "security_blocked_until = NULL " +
                "WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, finalReason);
            ps.setInt(2, adminId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }

        logEvent(userId, null, "ADMIN_KEEP_BLOCKED", "HIGH",
                "Compte maintenu bloqué par admin ID=" + adminId + " | Motif : " + finalReason);
    }
}