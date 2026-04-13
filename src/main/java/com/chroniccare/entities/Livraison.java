package com.chroniccare.entities;

import java.time.LocalDateTime;

public class Livraison {
    private int id;
    private int commandeId;
    private String statut;
    private String adresse;
    private String ville;
    private String codePostal;
    private LocalDateTime datePrevue;
    private LocalDateTime dateLivraison;
    private String trackingCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Livraison() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public LocalDateTime getDatePrevue() { return datePrevue; }
    public void setDatePrevue(LocalDateTime datePrevue) { this.datePrevue = datePrevue; }

    public LocalDateTime getDateLivraison() { return dateLivraison; }
    public void setDateLivraison(LocalDateTime dateLivraison) { this.dateLivraison = dateLivraison; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

