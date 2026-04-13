package com.chroniccare.models;

import java.time.LocalDateTime;

public class Publication {
    private int id;
    private String titre;
    private String contenu;
    private String categorie;
    private int nbVues;
    private int nbLikes;
    private String statut;
    private LocalDateTime createdAt;
    private Integer auteurId;
    private String imagePath;
    private String likedBy;


    public Publication() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public int getNbVues() { return nbVues; }
    public void setNbVues(int nbVues) { this.nbVues = nbVues; }
    public int getNbLikes() { return nbLikes; }
    public void setNbLikes(int nbLikes) { this.nbLikes = nbLikes; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Integer getAuteurId() { return auteurId; }
    public void setAuteurId(Integer auteurId) { this.auteurId = auteurId; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getLikedBy() { return likedBy; }
    public void setLikedBy(String likedBy) { this.likedBy = likedBy; }
}