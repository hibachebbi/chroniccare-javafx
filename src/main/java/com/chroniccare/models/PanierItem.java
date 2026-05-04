package com.chroniccare.models;

import java.time.LocalDateTime;

public class PanierItem {
    private int id;
    private int utilisateurId;
    private int produitId;
    private int quantite;
    private double prixUnitaire;
    private LocalDateTime dateAjout;
    private LocalDateTime dateModification;

    // Constructeurs
    public PanierItem() {
    }

    public PanierItem(int utilisateurId, int produitId, int quantite, double prixUnitaire) {
        this.utilisateurId = utilisateurId;
        this.produitId = produitId;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    public PanierItem(int id, int utilisateurId, int produitId, int quantite,
                      double prixUnitaire, LocalDateTime dateAjout, LocalDateTime dateModification) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.produitId = produitId;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.dateAjout = dateAjout;
        this.dateModification = dateModification;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public int getProduitId() {
        return produitId;
    }

    public void setProduitId(int produitId) {
        this.produitId = produitId;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public double getTotalLigne() {
        return prixUnitaire * quantite;
    }

    @Override
    public String toString() {
        return "PanierItem{" +
                "id=" + id +
                ", utilisateurId=" + utilisateurId +
                ", produitId=" + produitId +
                ", quantite=" + quantite +
                ", prixUnitaire=" + prixUnitaire +
                ", dateAjout=" + dateAjout +
                ", dateModification=" + dateModification +
                '}';
    }
}

