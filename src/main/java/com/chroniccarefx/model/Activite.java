package com.chroniccarefx.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Activite {
    public static final List<String> TYPE_CHOICES = List.of("marche", "course", "velo");

    private Long id;
    private Long utilisateurId;
    private Long etatId;
    private String type;
    private Integer duree;
    private Integer calories;
    private Double distanceKm;
    private Integer heuresRepos;
    private LocalDateTime dateActivite;
    private String notes;
    private LocalDateTime createdAt;

    public Activite(
            Long id,
            Long utilisateurId,
            Long etatId,
            String type,
            Integer duree,
            Integer calories,
            Double distanceKm,
            Integer heuresRepos,
            LocalDateTime dateActivite,
            String notes,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.etatId = etatId;
        this.type = type;
        this.duree = duree;
        this.calories = calories;
        this.distanceKm = distanceKm;
        this.heuresRepos = heuresRepos;
        this.dateActivite = dateActivite;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public Long getEtatId() {
        return etatId;
    }

    public void setEtatId(Long etatId) {
        this.etatId = etatId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getDuree() {
        return duree;
    }

    public void setDuree(Integer duree) {
        this.duree = duree;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getHeuresRepos() {
        return heuresRepos;
    }

    public void setHeuresRepos(Integer heuresRepos) {
        this.heuresRepos = heuresRepos;
    }

    public LocalDateTime getDateActivite() {
        return dateActivite;
    }

    public void setDateActivite(LocalDateTime dateActivite) {
        this.dateActivite = dateActivite;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Activite activite)) {
            return false;
        }
        return Objects.equals(id, activite.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
