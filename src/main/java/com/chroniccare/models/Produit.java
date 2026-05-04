package com.chroniccare.models;

import java.time.LocalDateTime;

public class Produit {
    private int id;
    private String nom;
    private String description;
    private double prix;
    private int stock;
    private String categorie;
    private boolean active;
    private LocalDateTime createdAt;
    private int popularite;

    public Produit() {
    }

    public Produit(String nom, String description, double prix, int stock, String categorie, boolean active, LocalDateTime createdAt, int popularite) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.stock = stock;
        this.categorie = categorie;
        this.active = active;
        this.createdAt = createdAt;
        this.popularite = popularite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getPopularite() { return popularite; }
    public void setPopularite(int popularite) { this.popularite = popularite; }
}

