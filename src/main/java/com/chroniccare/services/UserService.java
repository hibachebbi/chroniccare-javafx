package com.chroniccare.services;

import com.chroniccare.models.User;
import com.chroniccare.utils.MyDatabase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.chroniccare.utils.PasswordUtils;

public class UserService {
    private Connection conn = MyDatabase.getInstance().getConnection();

    // -------- AJOUTER --------
    public void insert(User u) throws SQLException {
        String sql = "INSERT INTO users (nom, prenom, email, password, " +
                "roles, telephone, genre, photo_profil, medical_condition, created_at, " +
                "approval_status, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getPassword());
        ps.setString(5, u.getRoles());
        ps.setString(6, u.getTelephone());
        ps.setString(7, u.getGenre());
        ps.setString(8, u.getPhotoProfil());
        ps.setString(9, u.getMedicalCondition());
        ps.setString(10, u.getApprovalStatus());
        ps.setBoolean(11, u.isActive());
        ps.executeUpdate();

        System.out.println("Utilisateur ajouté !");
    }

    // -------- MODIFIER --------
    public void update(User u) throws SQLException {
        String sql = "UPDATE users SET nom=?, prenom=?, email=?, " +
                "telephone=?, genre=?, roles=?, " +
                "approval_status=?, medical_condition=?, is_active=? " +
                "WHERE id=?";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getTelephone());
        ps.setString(5, u.getGenre());
        ps.setString(6, u.getRoles());
        ps.setString(7, u.getApprovalStatus());
        ps.setString(8, u.getMedicalCondition());
        ps.setBoolean(9, u.isActive());
        ps.setInt(10, u.getId());
        ps.executeUpdate();

        System.out.println("Utilisateur modifié !");
    }

    // -------- SUPPRIMER --------
    public void delete(int id) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM users WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Utilisateur supprimé !");
    }

    // -------- AFFICHER TOUS --------
    public List<User> getAll() throws SQLException {
        List<User> list = new ArrayList<>();
        ResultSet rs = conn.createStatement()
                .executeQuery("SELECT * FROM users");

        while (rs.next()) {
            User u = mapResultSetToUser(rs);
            list.add(u);
        }
        return list;
    }

    // -------- CHERCHER PAR ID --------
    public User getById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapResultSetToUser(rs);
        }
        return null;
    }

    // -------- LOGIN --------
    public User checkLogin(String email, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String dbPassword = rs.getString("password");

            if (PasswordUtils.matches(password, dbPassword)) {
                return mapResultSetToUser(rs);
            }
        }

        return null;
    }
    // -------- COMPTER TOUS --------
    public int countAll() throws SQLException {
        ResultSet rs = conn.createStatement()
                .executeQuery("SELECT COUNT(*) FROM users");
        if (rs.next()) return rs.getInt(1);
        return 0;
    }
    // -------- COMPTER PAR RÔLE --------
    public Map<String, Integer> countByRole() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Patient",        0);
        map.put("Coach",          0);
        map.put("Nutritionniste", 0);
        map.put("Admin",          0);

        ResultSet rs = conn.createStatement()
                .executeQuery("SELECT roles FROM users");

        while (rs.next()) {
            String roles = rs.getString("roles");
            if (roles == null) continue;
            if (roles.contains("ROLE_ADMIN"))               map.merge("Admin",          1, Integer::sum);
            else if (roles.contains("ROLE_NUTRITIONNISTE")) map.merge("Nutritionniste", 1, Integer::sum);
            else if (roles.contains("ROLE_COACH"))          map.merge("Coach",          1, Integer::sum);
            else if (roles.contains("ROLE_PATIENT"))        map.merge("Patient",        1, Integer::sum);
        }
        map.entrySet().removeIf(e -> e.getValue() == 0);
        return map;
    }
    public void assignBadgeManually(int userId) throws SQLException {
        User selectedUser = getById(userId);
        if (selectedUser == null) return;

        resetAllBadges();

        String badge = "Badge attribué par l'administrateur";
        String reward;

        if (selectedUser.getRoles() != null && selectedUser.getRoles().contains("ROLE_PATIENT")) {
            reward = "Remise de 15% sur la boutique ChronicCare";
        } else if (selectedUser.getRoles() != null &&
                (selectedUser.getRoles().contains("ROLE_COACH")
                        || selectedUser.getRoles().contains("ROLE_NUTRITIONNISTE"))) {
            reward = "Prime exceptionnelle sur le salaire de ce mois";
        } else {
            reward = "Récompense spéciale";
        }

        String sql = "UPDATE users SET is_most_active = 1, activity_badge = ?, activity_reward = ? WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, badge);
        ps.setString(2, reward);
        ps.setInt(3, userId);
        ps.executeUpdate();
    }

    public void resetAllBadges() throws SQLException {
        String sql = "UPDATE users SET is_most_active = 0, activity_badge = NULL, activity_reward = NULL";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.executeUpdate();
    }

    public void removeBadge(int userId) throws SQLException {
        String sql = "UPDATE users SET is_most_active = 0, activity_badge = NULL, activity_reward = NULL WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    // -------- COMPTER PAR GENRE --------
    public Map<String, Integer> countByGenre() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        ResultSet rs = conn.createStatement()
                .executeQuery("SELECT genre, COUNT(*) as nb FROM users GROUP BY genre");
        while (rs.next()) {
            String genre = rs.getString("genre");
            int nb = rs.getInt("nb");
            if (genre == null || genre.isBlank()) genre = "Non renseigné";
            map.put(genre, nb);
        }
        return map;
    }
    // -------- MAPPING --------
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setRoles(rs.getString("roles"));
        u.setTelephone(rs.getString("telephone"));
        u.setGenre(rs.getString("genre"));
        u.setApprovalStatus(rs.getString("approval_status"));
        u.setMedicalCondition(rs.getString("medical_condition"));
        u.setPhotoProfil(rs.getString("photo_profil"));
        u.setActive(rs.getBoolean("is_active"));
        u.setActivityScore(rs.getInt("activity_score"));
        u.setMostActive(rs.getBoolean("is_most_active"));
        u.setActivityBadge(rs.getString("activity_badge"));
        u.setActivityReward(rs.getString("activity_reward"));
        u.setFailedLoginCount(rs.getInt("failed_login_count"));
        u.setLastFailedLoginAt(rs.getTimestamp("last_failed_login_at"));
        u.setSecurityBlockedUntil(rs.getTimestamp("security_blocked_until"));
        u.setSecurityBlockReason(rs.getString("security_block_reason"));
        u.setSecurityManualLocked(rs.getBoolean("security_manual_locked"));
        u.setSecurityLockedAt(rs.getTimestamp("security_locked_at"));

        int lockedByAdmin = rs.getInt("security_locked_by_admin");
        u.setSecurityLockedByAdmin(rs.wasNull() ? null : lockedByAdmin);
        return u;
    }

    public List<User> getBlockedUsers() throws SQLException {
        String sql = "SELECT * FROM users " +
                "WHERE security_manual_locked = 1 " +
                "OR (security_blocked_until IS NOT NULL AND security_blocked_until > NOW()) " +
                "ORDER BY security_locked_at DESC, security_blocked_until DESC";

        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        List<User> users = new ArrayList<>();
        while (rs.next()) {
            users.add(mapResultSetToUser(rs));
        }
        return users;
    }
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return mapResultSetToUser(rs);
        }
        return null;
    }
}
