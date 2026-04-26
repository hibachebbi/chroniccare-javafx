package com.chroniccare.services;

import com.chroniccare.entities.Wishlist;
import com.chroniccare.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer la liste de souhaits (Wishlist)
 * Responsabilités:
 * - Ajouter un produit à la wishlist
 * - Retirer un produit de la wishlist
 * - Récupérer tous les produits d'une wishlist
 * - Vérifier si un produit est dans la wishlist
 * - Supprimer une wishlist
 */
public class WishlistService {

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Ajouter un produit à la wishlist d'un utilisateur
     */
    public void addToWishlist(int utilisateurId, int produitId) throws SQLException {
        // Vérifier si déjà dans wishlist
        if (isInWishlist(utilisateurId, produitId)) {
            throw new IllegalArgumentException("Ce produit est déjà dans votre wishlist");
        }

        String sql = "INSERT INTO wishlist (utilisateur_id, produit_id, date_ajout) VALUES (?, ?, ?)";
        
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ps.setInt(2, produitId);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    /**
     * Retirer un produit de la wishlist
     */
    public void removeFromWishlist(int utilisateurId, int produitId) throws SQLException {
        String sql = "DELETE FROM wishlist WHERE utilisateur_id = ? AND produit_id = ?";
        
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ps.setInt(2, produitId);
            ps.executeUpdate();
        }
    }

    /**
     * Vérifier si un produit est dans la wishlist
     */
    public boolean isInWishlist(int utilisateurId, int produitId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM wishlist WHERE utilisateur_id = ? AND produit_id = ?";
        
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ps.setInt(2, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Récupérer tous les éléments de la wishlist d'un utilisateur
     */
    public List<Wishlist> findByUtilisateurId(int utilisateurId) throws SQLException {
        List<Wishlist> wishlist = new ArrayList<>();
        String sql = "SELECT * FROM wishlist WHERE utilisateur_id = ? ORDER BY date_ajout DESC";
        
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    wishlist.add(map(rs));
                }
            }
        }
        return wishlist;
    }

    /**
     * Récupérer un élément spécifique de la wishlist
     */
    public Wishlist findById(int id) throws SQLException {
        String sql = "SELECT * FROM wishlist WHERE id = ?";
        
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

    /**
     * Supprimer toute la wishlist d'un utilisateur
     */
    public void clearWishlist(int utilisateurId) throws SQLException {
        String sql = "DELETE FROM wishlist WHERE utilisateur_id = ?";
        
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ps.executeUpdate();
        }
    }

    /**
     * Obtenir le nombre d'éléments dans la wishlist
     */
    public int countWishlist(int utilisateurId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM wishlist WHERE utilisateur_id = ?";
        
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Mapper un ResultSet en Wishlist
     */
    private Wishlist map(ResultSet rs) throws SQLException {
        Wishlist w = new Wishlist();
        w.setId(rs.getInt("id"));
        w.setUtilisateurId(rs.getInt("utilisateur_id"));
        w.setProduitId(rs.getInt("produit_id"));
        Timestamp ts = rs.getTimestamp("date_ajout");
        w.setDateAjout(ts != null ? ts.toLocalDateTime() : null);
        return w;
    }
}

