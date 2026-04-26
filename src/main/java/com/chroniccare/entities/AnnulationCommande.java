package com.chroniccare.entities;

import java.time.LocalDateTime;

/**
 * Entité pour tracker les annulations de commande
 * Permet de garder un historique complet de chaque annulation
 */
public class AnnulationCommande {
    private int id;
    private int commandeId;
    private String raison;
    private LocalDateTime dateAnnulation;
    private int utilisateurId;
    private String statutAvantAnnulation;
    private double montantRembourse;
    private boolean stockRestitue;

    public AnnulationCommande() {
    }

    public AnnulationCommande(int commandeId, String raison, int utilisateurId,
                             String statutAvantAnnulation, double montantRembourse) {
        this.commandeId = commandeId;
        this.raison = raison;
        this.utilisateurId = utilisateurId;
        this.statutAvantAnnulation = statutAvantAnnulation;
        this.montantRembourse = montantRembourse;
        this.dateAnnulation = LocalDateTime.now();
        this.stockRestitue = false;
    }

    // ===== GETTERS / SETTERS =====
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }

    public String getRaison() { return raison; }
    public void setRaison(String raison) { this.raison = raison; }

    public LocalDateTime getDateAnnulation() { return dateAnnulation; }
    public void setDateAnnulation(LocalDateTime dateAnnulation) { this.dateAnnulation = dateAnnulation; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getStatutAvantAnnulation() { return statutAvantAnnulation; }
    public void setStatutAvantAnnulation(String statutAvantAnnulation) { this.statutAvantAnnulation = statutAvantAnnulation; }

    public double getMontantRembourse() { return montantRembourse; }
    public void setMontantRembourse(double montantRembourse) { this.montantRembourse = montantRembourse; }

    public boolean isStockRestitue() { return stockRestitue; }
    public void setStockRestitue(boolean stockRestitue) { this.stockRestitue = stockRestitue; }
}

