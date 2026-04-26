package com.chroniccare.services;

import com.chroniccare.utils.MyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les alertes de stock faible et rupture
 */
public class StockAlertService {

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Seuils de stock
     */
    public static final int STOCK_FAIBLE_SEUIL = 5;   // Alerte
    public static final int STOCK_RUPTURE_SEUIL = 1;  // Rupture

    /**
     * Vérifier si un produit a un stock faible
     */
    public boolean isStockFaible(int produitId) throws SQLException {
        String sql = "SELECT stock FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock") <= STOCK_FAIBLE_SEUIL && rs.getInt("stock") > 0;
                }
            }
        }
        return false;
    }

    /**
     * Vérifier si un produit est en rupture de stock
     */
    public boolean isRupture(int produitId) throws SQLException {
        String sql = "SELECT stock FROM produit WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock") == 0;
                }
            }
        }
        return false;
    }

    /**
     * Compter tous les produits en stock faible
     */
    public int countStockFaible() throws SQLException {
        String sql = "SELECT COUNT(*) FROM produit WHERE stock > 0 AND stock <= ? AND is_active = 1";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, STOCK_FAIBLE_SEUIL);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Compter tous les produits en rupture de stock
     */
    public int countRupture() throws SQLException {
        String sql = "SELECT COUNT(*) FROM produit WHERE stock = 0 AND is_active = 1";
        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Récupérer tous les produits en stock faible (pour affichage détaillé)
     */
    public List<String> getProduitsStockFaible() throws SQLException {
        List<String> produits = new ArrayList<>();
        String sql = "SELECT nom, stock FROM produit WHERE stock > 0 AND stock <= ? AND is_active = 1 ORDER BY stock ASC";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, STOCK_FAIBLE_SEUIL);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produits.add(rs.getString("nom") + " (" + rs.getInt("stock") + " u)");
                }
            }
        }
        return produits;
    }

    /**
     * Récupérer tous les produits en rupture (pour affichage détaillé)
     */
    public List<String> getProduitsRupture() throws SQLException {
        List<String> produits = new ArrayList<>();
        String sql = "SELECT nom FROM produit WHERE stock = 0 AND is_active = 1 ORDER BY nom ASC";
        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                produits.add(rs.getString("nom") + " (Rupture)");
            }
        }
        return produits;
    }

    /**
     * Obtenir le status visuel du stock
     */
    public String getStockStatus(int produitId) throws SQLException {
        if (isRupture(produitId)) {
            return "RUPTURE";
        }
        if (isStockFaible(produitId)) {
            return "FAIBLE";
        }
        return "OK";
    }

    /**
     * Obtenir la couleur CSS pour le status
     */
    public String getStockStatusColor(int produitId) throws SQLException {
        String status = getStockStatus(produitId);
        return switch (status) {
            case "RUPTURE" -> "#ef4444";  // Rouge
            case "FAIBLE" -> "#f59e0b";   // Amber/Orange
            default -> "#10b981";         // Vert
        };
    }
}

