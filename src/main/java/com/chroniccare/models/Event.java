package com.chroniccare.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Event {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private int id;
    private String statut;
    private String titre;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private LocalDateTime createdAt;
    private int coachId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getCoachId() {
        return coachId;
    }

    public void setCoachId(int coachId) {
        this.coachId = coachId;
    }

    public String getDateDebutDisplay() {
        return dateDebut != null ? dateDebut.format(DATE_TIME_FORMATTER) : "-";
    }

    public String getDateFinDisplay() {
        return dateFin != null ? dateFin.format(DATE_TIME_FORMATTER) : "-";
    }

    public String getDateRangeDisplay() {
        return getDateDebutDisplay() + " -> " + getDateFinDisplay();
    }

    @Override
    public String toString() {
        return titre != null ? titre : "Evenement";
    }
}
