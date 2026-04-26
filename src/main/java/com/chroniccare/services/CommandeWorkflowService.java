package com.chroniccare.services;

import com.chroniccare.utils.MyDatabase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Service pour gérer le workflow de commande avec statuts validés
 * Statuts valides: en_attente → confirmée → préparée → expédiée → livrée
 */
public class CommandeWorkflowService {

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Statuts valides pour une commande
     */
    public enum CommandeStatut {
        EN_ATTENTE("en_attente"),
        CONFIRMEE("confirmée"),
        PREPAREE("préparée"),
        EXPEDIEE("expédiée"),
        LIVREE("livrée"),
        ANNULEE("annulée");

        private final String label;

        CommandeStatut(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static CommandeStatut fromLabel(String label) {
            for (CommandeStatut s : values()) {
                if (s.label.equals(label)) return s;
            }
            return EN_ATTENTE;
        }
    }

    /**
     * Transition de statut avec validation
     * @param commandeId ID de la commande
     * @param newStatut Nouveau statut
     * @throws SQLException Exception SQL
     * @throws IllegalStateException Si la transition n'est pas valide
     */
    public void transitionStatut(int commandeId, CommandeStatut newStatut) throws SQLException {
        if (commandeId <= 0) {
            throw new IllegalArgumentException("commandeId invalide");
        }
        if (newStatut == null) {
            throw new IllegalArgumentException("statut obligatoire");
        }

        String sql = "UPDATE commande SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, newStatut.getLabel());
            ps.setInt(2, commandeId);
            int updated = ps.executeUpdate();
            if (updated != 1) {
                throw new SQLException("Commande introuvable (id=" + commandeId + ")");
            }
        }
    }

    /**
     * Compter les commandes par statut
     */
    public int countByStatut(CommandeStatut statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM commande WHERE statut = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, statut.getLabel());
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Calculer les commandes en attente de traitement (en_attente + confirmée)
     */
    public int countPendingCommandes() throws SQLException {
        String sql = "SELECT COUNT(*) FROM commande WHERE statut IN (?, ?)";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, CommandeStatut.EN_ATTENTE.getLabel());
            ps.setString(2, CommandeStatut.CONFIRMEE.getLabel());
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Somme des commandes complétées  (livrée)
     */
    public double sumCompletedCommandes() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM commande WHERE statut = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, CommandeStatut.LIVREE.getLabel());
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        }
    }

    /**
     * Compter les commandes de la journée (24h dernières)
     */
    public int countTodayCommandes() throws SQLException {
        String sql = "SELECT COUNT(*) FROM commande WHERE DATE(created_at) = CURDATE()";
        try (PreparedStatement ps = connection().prepareStatement(sql);
             var rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}

