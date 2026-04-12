package com.chroniccare.services;

import com.chroniccare.models.User;
import com.chroniccare.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private Connection conn = MyDatabase.getInstance().getConnection();

    // -------- AJOUTER --------
    public void insert(User u) throws SQLException {
        String sql = "INSERT INTO users (nom, prenom, email, password, " +
                "roles, telephone, genre, created_at, " +
                "approval_status, medical_condition, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?)";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getPassword());
        ps.setString(5, u.getRoles());
        ps.setString(6, u.getTelephone());
        ps.setString(7, u.getGenre());
        ps.setString(8, u.getApprovalStatus());
        ps.setString(9, u.getMedicalCondition());
        ps.setBoolean(10, u.isActive());
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
        String sql = "SELECT * FROM users WHERE email=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, email);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String dbPassword = rs.getString("password");

            // Cas simple : mot de passe stocké en clair
            if (dbPassword != null && dbPassword.equals(password)) {
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
        return u;
    }
}
