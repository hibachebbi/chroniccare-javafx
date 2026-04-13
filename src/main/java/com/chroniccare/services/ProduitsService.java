package com.chroniccare.services;

import com.chroniccare.entities.Produit;
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

public class ProduitsService {

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    public List<Produit> findAll() throws SQLException {
        String sql = "SELECT * FROM produit ORDER BY created_at DESC, id DESC";
        List<Produit> produits = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                produits.add(map(rs));
            }
        }
        return produits;
    }

    public Produit findById(int id) throws SQLException {
        String sql = "SELECT * FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public Produit save(Produit produit) throws SQLException {
        String sql = "INSERT INTO produit (nom, description, prix, stock, categorie, is_active, created_at, popularite) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindInsert(ps, produit);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    produit.setId(keys.getInt(1));
                }
            }
        }
        return produit;
    }

    public void update(Produit produit) throws SQLException {
        String sql = "UPDATE produit SET nom = ?, description = ?, prix = ?, stock = ?, categorie = ?, is_active = ?, popularite = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, produit.getNom());
            ps.setString(2, produit.getDescription());
            ps.setDouble(3, produit.getPrix());
            ps.setInt(4, produit.getStock());
            ps.setString(5, produit.getCategorie());
            ps.setBoolean(6, produit.isActive());
            ps.setInt(7, produit.getPopularite());
            ps.setInt(8, produit.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countAll() throws SQLException {
        return countQuery("SELECT COUNT(*) FROM produit");
    }

    public int countActive() throws SQLException {
        return countQuery("SELECT COUNT(*) FROM produit WHERE is_active = 1");
    }

    public int sumStock() throws SQLException {
        String sql = "SELECT COALESCE(SUM(stock), 0) FROM produit";
        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countLowStock(int threshold) throws SQLException {
        String sql = "SELECT COUNT(*) FROM produit WHERE stock <= ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void bindInsert(PreparedStatement ps, Produit produit) throws SQLException {
        ps.setString(1, produit.getNom());
        ps.setString(2, produit.getDescription());
        ps.setDouble(3, produit.getPrix());
        ps.setInt(4, produit.getStock());
        ps.setString(5, produit.getCategorie());
        ps.setBoolean(6, produit.isActive());
        LocalDateTime createdAt = produit.getCreatedAt() != null ? produit.getCreatedAt() : LocalDateTime.now();
        ps.setTimestamp(7, Timestamp.valueOf(createdAt));
        ps.setInt(8, produit.getPopularite());
    }

    private Produit map(ResultSet rs) throws SQLException {
        Produit produit = new Produit();
        produit.setId(rs.getInt("id"));
        produit.setNom(rs.getString("nom"));
        produit.setDescription(rs.getString("description"));
        produit.setPrix(rs.getDouble("prix"));
        produit.setStock(rs.getInt("stock"));
        produit.setCategorie(rs.getString("categorie"));
        produit.setActive(rs.getBoolean("is_active"));
        Timestamp timestamp = rs.getTimestamp("created_at");
        produit.setCreatedAt(timestamp != null ? timestamp.toLocalDateTime() : null);
        produit.setPopularite(rs.getInt("popularite"));
        return produit;
    }

    private int countQuery(String sql) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
