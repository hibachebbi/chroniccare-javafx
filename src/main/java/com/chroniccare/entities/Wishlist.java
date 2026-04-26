package com.chroniccare.entities;

import java.time.LocalDateTime;

/**
 * Entity pour la liste de souhaits (Wishlist)
 * Permet aux clients de marquer les produits qu'ils souhaitent
 */
public class Wishlist {
    private int id;
    private int utilisateurId;
    private int produitId;
    private LocalDateTime dateAjout;

    // Constructors
    public Wishlist() {}

    public Wishlist(int utilisateurId, int produitId) {
        this.utilisateurId = utilisateurId;
        this.produitId = produitId;
        this.dateAjout = LocalDateTime.now();
    }

    public Wishlist(int id, int utilisateurId, int produitId, LocalDateTime dateAjout) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.produitId = produitId;
        this.dateAjout = dateAjout;
    }

    // Getters & Setters
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

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }

    @Override
    public String toString() {
        return "Wishlist{" +
                "id=" + id +
                ", utilisateurId=" + utilisateurId +
                ", produitId=" + produitId +
                ", dateAjout=" + dateAjout +
                '}';
    }
}

