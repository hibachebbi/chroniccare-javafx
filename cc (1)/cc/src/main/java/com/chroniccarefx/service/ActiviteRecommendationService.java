package com.chroniccarefx.service;

import com.chroniccarefx.model.Activite;
import com.chroniccarefx.model.Etat;

import java.time.LocalDateTime;
import java.util.Locale;

public class ActiviteRecommendationService {

    public Activite buildRecommendedActivite(Etat etat) {
        if (etat == null) {
            throw new IllegalArgumentException("Etat obligatoire pour generer une activite.");
        }

        RecommendationProfile profile = buildProfile(etat);
        String notes = buildNotes(etat, profile);

        return new Activite(
                null,
                etat.getUtilisateurId(),
                etat.getId(),
                profile.type(),
                profile.duree(),
                profile.calories(),
                profile.distanceKm(),
                profile.heuresRepos(),
                defaultActivityDate(etat),
                notes,
                LocalDateTime.now()
        );
    }

    private RecommendationProfile buildProfile(Etat etat) {
        double temperature = parseDouble(etat.getTemperatureCorporelle());
        double hydratation = parseDouble(etat.getNiveauHydratation());
        String traitement = normalize(etat.getTraitementEnCours());
        String remarques = normalize(etat.getRemarquesCliniques());
        String contexte = (traitement + " " + remarques).toLowerCase(Locale.ROOT);

        if (temperature >= 38.0 || hydratation > 0 && hydratation < 1.5 || containsOneOf(contexte, "fatigue", "douleur", "vertige", "naus", "faible")) {
            return new RecommendationProfile("marche", 20, 90, 1.2, 9, "intensite douce");
        }

        if (temperature >= 37.5 || hydratation > 0 && hydratation < 2.0 || containsOneOf(contexte, "stress", "anx", "suivi", "controle")) {
            return new RecommendationProfile("marche", 30, 140, 2.0, 8, "intensite moderee");
        }

        if (containsOneOf(contexte, "diab", "cardio", "tension", "respir")) {
            return new RecommendationProfile("velo", 35, 220, 6.0, 7, "endurance reguliere");
        }

        return new RecommendationProfile("course", 40, 280, 5.5, 7, "endurance active");
    }

    private String buildNotes(Etat etat, RecommendationProfile profile) {
        StringBuilder notes = new StringBuilder("Activite generee automatiquement selon l'etat du patient.");
        notes.append(" Niveau recommande: ").append(profile.niveau()).append('.');

        if (!normalize(etat.getTraitementEnCours()).isEmpty()) {
            notes.append(" Traitement pris en compte: ").append(normalize(etat.getTraitementEnCours())).append('.');
        }

        if (!normalize(etat.getRemarquesCliniques()).isEmpty()) {
            notes.append(" Contexte clinique: ").append(normalize(etat.getRemarquesCliniques())).append('.');
        }

        if (!normalize(etat.getTemperatureCorporelle()).isEmpty()) {
            notes.append(" Temperature relevee: ").append(etat.getTemperatureCorporelle()).append(" deg C.");
        }

        if (!normalize(etat.getNiveauHydratation()).isEmpty()) {
            notes.append(" Hydratation declaree: ").append(etat.getNiveauHydratation()).append(" L.");
        }

        notes.append(" Adapter la seance si le coach ou le nutritionniste observe une aggravation.");
        return notes.toString();
    }

    private LocalDateTime defaultActivityDate(Etat etat) {
        return etat.getDateReleve() == null ? LocalDateTime.now() : etat.getDateReleve();
    }

    private boolean containsOneOf(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record RecommendationProfile(
            String type,
            Integer duree,
            Integer calories,
            Double distanceKm,
            Integer heuresRepos,
            String niveau
    ) {
    }
}
