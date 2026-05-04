package com.chroniccare.services;

import com.chroniccare.models.PanierItem;
import com.chroniccare.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour persister le panier dans la base de données
 * Permet de sauvegarder et charger le panier des utilisateurs
 */
public class PanierPersistantService {

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    private PanierItem map(ResultSet rs) throws SQLException {
        return new PanierItem(
                rs.getInt("id"),
                rs.getInt("utilisateur_id"),
                rs.getInt("produit_id"),
                rs.getInt("quantite"),
                rs.getDouble("prix_unitaire"),
                rs.getTimestamp("date_ajout") != null ? rs.getTimestamp("date_ajout").toLocalDateTime() : null,
                rs.getTimestamp("date_modification") != null ? rs.getTimestamp("date_modification").toLocalDateTime() : null
        );
    }

    // ====== READ ======

    /**
     * Récupère tous les articles du panier pour un utilisateur
     */
    public List<PanierItem> findByUtilisateurId(int utilisateurId) throws SQLException {
        String sql = "SELECT * FROM panier WHERE utilisateur_id = ? ORDER BY date_ajout DESC";
        List<PanierItem> items = new ArrayList<>();

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        }
        return items;
    }

    /**
     * Récupère un article spécifique du panier
     */
    public PanierItem findById(int id) throws SQLException {
        String sql = "SELECT * FROM panier WHERE id = ?";

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
     * Trouve un article du panier par utilisateur et produit
     */
    public PanierItem findByUtilisateurAndProduit(int utilisateurId, int produitId) throws SQLException {
        String sql = "SELECT * FROM panier WHERE utilisateur_id = ? AND produit_id = ?";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ps.setInt(2, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    /**
     * Calcule le total du panier pour un utilisateur
     */
    public double getTotalPanier(int utilisateurId) throws SQLException {
        String sql = "SELECT SUM(quantite * prix_unitaire) as total FROM panier WHERE utilisateur_id = ?";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }

    /**
     * Compte le nombre d'articles du panier
     */
    public int countItems(int utilisateurId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM panier WHERE utilisateur_id = ?";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        }
        return 0;
    }

    // ====== CREATE/UPDATE ======

    /**
     * Ajoute ou met à jour un article dans le panier
     */
    public int addOrUpdate(int utilisateurId, int produitId, int quantite, double prixUnitaire) throws SQLException {
        // Vérifier si l'article existe déjà
        PanierItem existing = findByUtilisateurAndProduit(utilisateurId, produitId);

        if (existing != null) {
            // Mise à jour de la quantité
            return update(existing.getId(), quantite, prixUnitaire);
        } else {
            // Création d'un nouvel article
            return add(utilisateurId, produitId, quantite, prixUnitaire);
        }
    }

    /**
     * Crée un nouvel article dans le panier
     */
    public int add(int utilisateurId, int produitId, int quantite, double prixUnitaire) throws SQLException {
        String sql = "INSERT INTO panier (utilisateur_id, produit_id, quantite, prix_unitaire) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, utilisateurId);
            ps.setInt(2, produitId);
            ps.setInt(3, quantite);
            ps.setDouble(4, prixUnitaire);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Met à jour la quantité d'un article du panier
     */
    public int update(int panierItemId, int quantite, double prixUnitaire) throws SQLException {
        String sql = "UPDATE panier SET quantite = ?, prix_unitaire = ?, date_modification = NOW() WHERE id = ?";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, quantite);
            ps.setDouble(2, prixUnitaire);
            ps.setInt(3, panierItemId);
            return ps.executeUpdate();
        }
    }

    /**
     * Supprime un article du panier
     */
    public int delete(int panierItemId) throws SQLException {
        String sql = "DELETE FROM panier WHERE id = ?";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, panierItemId);
            return ps.executeUpdate();
        }
    }

    /**
     * Supprime un article du panier par utilisateur et produit
     */
    public int deleteByUtilisateurAndProduit(int utilisateurId, int produitId) throws SQLException {
        String sql = "DELETE FROM panier WHERE utilisateur_id = ? AND produit_id = ?";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            ps.setInt(2, produitId);
            return ps.executeUpdate();
        }
    }

    /**
     * Vide complètement le panier d'un utilisateur
     */
    public int clearPanier(int utilisateurId) throws SQLException {
        String sql = "DELETE FROM panier WHERE utilisateur_id = ?";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            return ps.executeUpdate();
        }
    }

    /**
     * Supprime les anciens paniers (ex: plus de 30 jours)
     */
    public int deleteOldPanier(int daysOld) throws SQLException {
        String sql = "DELETE FROM panier WHERE date_ajout < DATE_SUB(NOW(), INTERVAL ? DAY)";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, daysOld);
            return ps.executeUpdate();
        }
    }
}


