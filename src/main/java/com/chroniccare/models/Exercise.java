package com.chroniccare.models;

public class Exercise {
    private int id;
    private String nom;
    private String description;
    private int duree;
    private Integer repetitions;
    private Integer evenementId;
    private String evenementTitre;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public Integer getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(Integer repetitions) {
        this.repetitions = repetitions;
    }

    public Integer getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(Integer evenementId) {
        this.evenementId = evenementId;
    }

    public String getEvenementTitre() {
        return evenementTitre;
    }

    public void setEvenementTitre(String evenementTitre) {
        this.evenementTitre = evenementTitre;
    }

    public String getRepetitionsDisplay() {
        return repetitions != null ? String.valueOf(repetitions) : "-";
    }
}
