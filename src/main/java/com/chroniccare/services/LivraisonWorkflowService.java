package com.chroniccare.services;

import com.chroniccare.utils.MyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service pour les livraisons avec statuts avancés
 */
public class LivraisonWorkflowService {

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Statuts valides pour une livraison
     */
    public enum LivraisonStatut {
        EN_PREPARATION("en_preparation"),
        EN_TRANSIT("en_transit"),
        LIVREE("livree"),
        RETARD("retard"),
        ANNULEE("annulee");

        private final String label;

        LivraisonStatut(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static LivraisonStatut fromLabel(String label) {
            for (LivraisonStatut s : values()) {
                if (s.label.equals(label)) return s;
            }
            return EN_PREPARATION;
        }
    }

    /**
     * Compter les livraisons en retard (date prévue passée et pas livrée)
     */
    public int countLivraisonsEnRetard() throws SQLException {
        String sql = "SELECT COUNT(*) FROM livraison WHERE date_prevue < NOW() AND statut != ? AND date_livraison IS NULL";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, LivraisonStatut.ANNULEE.getLabel());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Compter par statut
     */
    public int countByStatut(LivraisonStatut statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM livraison WHERE statut = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, statut.getLabel());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Compter toutes les livraisons
     */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM livraison";
        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Transitionner le statut d'une livraison
     */
    public void transitionStatut(int livraisonId, LivraisonStatut newStatut) throws SQLException {
        if (livraisonId <= 0) {
            throw new IllegalArgumentException("livraisonId invalide");
        }
        if (newStatut == null) {
            throw new IllegalArgumentException("statut obligatoire");
        }

        String sql = "UPDATE livraison SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, newStatut.getLabel());
            ps.setInt(2, livraisonId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Livraison introuvable (id=" + livraisonId + ")");
            }
        }
    }
}

