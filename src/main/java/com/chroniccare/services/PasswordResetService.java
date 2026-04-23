package com.chroniccare.services;

import com.chroniccare.models.User;
import com.chroniccare.utils.MyDatabase;
import com.chroniccare.utils.PasswordUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;

public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_TTL_MINUTES = 30;

    private final Connection conn = MyDatabase.getInstance().getConnection();
    private volatile boolean schemaChecked = false;

    public static final class ResetToken {
        private final int userId;
        private final String email;
        private final String token;
        private final Timestamp expiresAt;

        public ResetToken(int userId, String email, String token, Timestamp expiresAt) {
            this.userId = userId;
            this.email = email;
            this.token = token;
            this.expiresAt = expiresAt;
        }

        public int getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getToken() {
            return token;
        }

        public Timestamp getExpiresAt() {
            return expiresAt;
        }
    }

    private void ensureSchema() {
        if (schemaChecked) return;
        synchronized (this) {
            if (schemaChecked) return;
            if (conn == null) return;

            String sql = """
                    CREATE TABLE IF NOT EXISTS password_reset_tokens (
                      id INT AUTO_INCREMENT PRIMARY KEY,
                      user_id INT NOT NULL,
                      token_hash CHAR(64) NOT NULL,
                      expires_at TIMESTAMP NOT NULL,
                      used_at TIMESTAMP NULL,
                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      INDEX idx_prt_token_hash (token_hash),
                      INDEX idx_prt_user_id (user_id),
                      INDEX idx_prt_expires_at (expires_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """;

            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                schemaChecked = true;
            }
        }
    }

    public ResetToken createResetToken(String email) throws SQLException {
        ensureSchema();
        if (conn == null) return null;
        if (email == null || email.isBlank()) return null;

        UserService userService = new UserService();
        User user = userService.findByEmail(email.trim());
        if (user == null) return null;
        if (!user.isActive()) return null;

        String token = generateToken();
        String tokenHash = sha256Hex(token);
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(TOKEN_TTL_MINUTES));

        String sql = "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, created_at) VALUES (?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();
        }

        return new ResetToken(user.getId(), user.getEmail(), token, expiresAt);
    }

    public boolean resetPassword(String token, String newPassword) throws SQLException {
        ensureSchema();
        if (conn == null) return false;
        if (token == null || token.isBlank()) return false;
        if (newPassword == null || newPassword.length() < 8) return false;

        String tokenHash = sha256Hex(token.trim());
        String hashedPassword = PasswordUtils.hash(newPassword);

        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            Integer tokenId = null;
            Integer userId = null;

            String selectSql = """
                    SELECT id, user_id
                    FROM password_reset_tokens
                    WHERE token_hash = ?
                      AND used_at IS NULL
                      AND expires_at > NOW()
                    ORDER BY created_at DESC
                    LIMIT 1
                    FOR UPDATE
                    """;

            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, tokenHash);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tokenId = rs.getInt("id");
                        userId = rs.getInt("user_id");
                    }
                }
            }

            if (tokenId == null || userId == null) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET password = ? WHERE id = ?")) {
                ps.setString(1, hashedPassword);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE password_reset_tokens SET used_at = NOW() WHERE id = ?")) {
                ps.setInt(1, tokenId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

