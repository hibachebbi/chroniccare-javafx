package com.chroniccare.services;

import com.chroniccare.entities.AnnulationCommande;
import com.chroniccare.entities.Commande;
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
 * Service de gestion des annulations de commande
 * Responsabilités:
 * - Annuler une commande
 * - Restituer le stock
 * - Tracker l'annulation en BD
 * - Lister les annulations
 */
public class AnnulationCommandeService {

    private final CommandeService commandeService = new CommandeService();
    private final ProduitsService produitsService = new ProduitsService();

    private Connection connection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Annuler une commande
     * Étapes:
     * 1. Vérifier que la commande peut être annulée (statut en_attente)
     * 2. Restituer le stock de tous les produits
     * 3. Mettre à jour le statut de la commande à "annulee"
     * 4. Enregistrer l'annulation en BD
     */
    public void annulerCommande(int commandeId, String raison, int utilisateurId) throws SQLException {
        Commande commande = commandeService.findById(commandeId);
        if (commande == null) {
            throw new IllegalArgumentException("Commande non trouvée");
        }

        // Vérifier le statut
        if (commande.getStatut() == null ||
            (!commande.getStatut().equals("en_attente") && !commande.getStatut().equals("confirmee"))) {
            throw new IllegalArgumentException("Impossible d'annuler une commande avec le statut: " + commande.getStatut());
        }

        // Restituer le stock (récupérer les lignes de commande)
        List<LigneCommandeService.LigneCommande> lignes = getLignesCommande(commandeId);
        for (LigneCommandeService.LigneCommande ligne : lignes) {
            augmenterStock(ligne.getProduitId(), ligne.getQuantite());
        }

        // Mettre à jour le statut de la commande
        updateStatutCommande(commandeId, "annulee");

        // Enregistrer l'annulation
        enregistrerAnnulation(commandeId, raison, utilisateurId, commande.getStatut(), commande.getTotal());
    }

    /**
     * Enregistrer l'annulation en BD
     */
    private void enregistrerAnnulation(int commandeId, String raison, int utilisateurId,
                                       String statutAvant, double montantRembourse) throws SQLException {
        String sql = "INSERT INTO annulation_commande (commande_id, raison, utilisateur_id, statut_avant_annulation, montant_rembourse, stock_restitue, date_annulation) VALUES (?, ?, ?, ?, ?, 1, ?)";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, commandeId);
            ps.setString(2, raison == null ? "Non spécifiée" : raison.trim());
            ps.setInt(3, utilisateurId);
            ps.setString(4, statutAvant);
            ps.setDouble(5, montantRembourse);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }

        // Ajouter note dans la colonne motif_annulation de la commande
        String updateSql = "UPDATE commande SET motif_annulation = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(updateSql)) {
            ps.setString(1, raison == null ? "Non spécifiée" : raison.trim());
            ps.setInt(2, commandeId);
            ps.executeUpdate();
        }
    }

    /**
     * Augmenter le stock d'un produit
     */
    private void augmenterStock(int produitId, int quantite) throws SQLException {
        String sql = "UPDATE produit SET stock = stock + ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, quantite);
            ps.setInt(2, produitId);
            ps.executeUpdate();
        }
    }

    /**
     * Mettre à jour le statut de la commande
     */
    private void updateStatutCommande(int commandeId, String nouveauStatut) throws SQLException {
        String sql = "UPDATE commande SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, commandeId);
            ps.executeUpdate();
        }
    }

    /**
     * Récupérer les lignes de commande (produits)
     * Cette méthode suppose que la table ligne_commande existe
     */
    private List<LigneCommandeService.LigneCommande> getLignesCommande(int commandeId) throws SQLException {
        List<LigneCommandeService.LigneCommande> lignes = new ArrayList<>();
        String sql = "SELECT produit_id, quantite FROM ligne_commande WHERE commande_id = ?";

        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, commandeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int produitId = rs.getInt("produit_id");
                    int quantite = rs.getInt("quantite");
                    lignes.add(new LigneCommandeService.LigneCommande(produitId, quantite));
                }
            }
        }
        return lignes;
    }

    /**
     * Récupérer les annulations d'une commande
     */
    public AnnulationCommande findByCommandeId(int commandeId) throws SQLException {
        String sql = "SELECT * FROM annulation_commande WHERE commande_id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, commandeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    /**
     * Lister toutes les annulations (pour audit admin)
     */
    public List<AnnulationCommande> findAll() throws SQLException {
        String sql = "SELECT * FROM annulation_commande ORDER BY date_annulation DESC LIMIT 100";
        List<AnnulationCommande> annulations = new ArrayList<>();

        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                annulations.add(map(rs));
            }
        }
        return annulations;
    }

    /**
     * Compter les annulations d'une période
     */
    public int countAnnulationsThisMonth() throws SQLException {
        String sql = "SELECT COUNT(*) FROM annulation_commande WHERE YEAR(date_annulation) = YEAR(NOW()) AND MONTH(date_annulation) = MONTH(NOW())";
        try (PreparedStatement ps = connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Mapper un ResultSet en AnnulationCommande
     */
    private AnnulationCommande map(ResultSet rs) throws SQLException {
        AnnulationCommande a = new AnnulationCommande();
        a.setId(rs.getInt("id"));
        a.setCommandeId(rs.getInt("commande_id"));
        a.setRaison(rs.getString("raison"));
        Timestamp ts = rs.getTimestamp("date_annulation");
        a.setDateAnnulation(ts != null ? ts.toLocalDateTime() : null);
        a.setUtilisateurId(rs.getInt("utilisateur_id"));
        a.setStatutAvantAnnulation(rs.getString("statut_avant_annulation"));
        a.setMontantRembourse(rs.getDouble("montant_rembourse"));
        a.setStockRestitue(rs.getBoolean("stock_restitue"));
        return a;
    }
}

// Classe interne pour les lignes de commande
class LigneCommande {
    private int produitId;
    private int quantite;

    public LigneCommande(int produitId, int quantite) {
        this.produitId = produitId;
        this.quantite = quantite;
    }

    public int getProduitId() { return produitId; }
    public int getQuantite() { return quantite; }
}

