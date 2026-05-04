package com.chroniccare.modules.suivi.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Etat {
    public static final List<String> TEMPERATURE_CORPORELLE_CHOICES = List.of(
            "35.0", "35.5", "36.0", "36.5", "37.0", "37.5", "38.0", "38.5", "39.0", "39.5", "40.0"
    );
    public static final List<String> NIVEAU_HYDRATATION_CHOICES = List.of(
            "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0"
    );

    private Long id;
    private Long utilisateurId;
    private String traitementEnCours;
    private String remarquesCliniques;
    private String temperatureCorporelle;
    private String niveauHydratation;
    private LocalDateTime dateReleve;

    public Etat(Long id, Long utilisateurId, String traitementEnCours, String remarquesCliniques,
                String temperatureCorporelle, String niveauHydratation, LocalDateTime dateReleve) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.traitementEnCours = traitementEnCours;
        this.remarquesCliniques = remarquesCliniques;
        this.temperatureCorporelle = temperatureCorporelle;
        this.niveauHydratation = niveauHydratation;
        this.dateReleve = dateReleve;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Long utilisateurId) { this.utilisateurId = utilisateurId; }
    public String getTraitementEnCours() { return traitementEnCours; }
    public void setTraitementEnCours(String traitementEnCours) { this.traitementEnCours = traitementEnCours; }
    public String getRemarquesCliniques() { return remarquesCliniques; }
    public void setRemarquesCliniques(String remarquesCliniques) { this.remarquesCliniques = remarquesCliniques; }
    public String getTemperatureCorporelle() { return temperatureCorporelle; }
    public void setTemperatureCorporelle(String temperatureCorporelle) { this.temperatureCorporelle = temperatureCorporelle; }
    public String getNiveauHydratation() { return niveauHydratation; }
    public void setNiveauHydratation(String niveauHydratation) { this.niveauHydratation = niveauHydratation; }
    public LocalDateTime getDateReleve() { return dateReleve; }
    public void setDateReleve(LocalDateTime dateReleve) { this.dateReleve = dateReleve; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Etat etat)) return false;
        return Objects.equals(id, etat.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
