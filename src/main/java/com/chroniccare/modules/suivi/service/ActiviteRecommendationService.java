package com.chroniccare.modules.suivi.service;

import com.chroniccare.modules.suivi.model.Activite;
import com.chroniccare.modules.suivi.model.Etat;

import java.time.LocalDateTime;
import java.util.Locale;

public class ActiviteRecommendationService {

    public Activite buildRecommendedActivite(Etat etat) {
        if (etat == null) {
            throw new IllegalArgumentException("Etat obligatoire pour generer une activite.");
        }

        RecommendationProfile profile = buildProfile(etat);
        return new Activite(
                null,
                etat.getUtilisateurId(),
                etat.getId(),
                profile.type(),
                profile.duree(),
                profile.calories(),
                profile.distanceKm(),
                profile.heuresRepos(),
                etat.getDateReleve() == null ? LocalDateTime.now() : etat.getDateReleve(),
                buildNotes(etat, profile),
                LocalDateTime.now()
        );
    }

    private RecommendationProfile buildProfile(Etat etat) {
        double temperature = parseDouble(etat.getTemperatureCorporelle());
        double hydratation = parseDouble(etat.getNiveauHydratation());
        String contexte = (normalize(etat.getTraitementEnCours()) + " " + normalize(etat.getRemarquesCliniques()))
                .toLowerCase(Locale.ROOT);

        if (temperature >= 39.0 || containsOneOf(contexte, "fievre", "temperature trop elevee", "forte fievre")) {
            return new RecommendationProfile("repos", 0, 0, null, 12, "repos complet");
        }

        if (containsOneOf(contexte, "repos", "arret", "bless", "fract", "crise", "urgence")) {
            return new RecommendationProfile("repos", 0, 0, null, 10, "repos medical");
        }

        if (temperature >= 38.0 || hydratation > 0 && hydratation < 1.5
                || containsOneOf(contexte, "fatigue", "douleur", "vertige", "faible", "naus", "fiev")) {
            return new RecommendationProfile("marche", 10, 35, 0.4, 9, "intensite douce");
        }

        if (temperature >= 37.5 || hydratation > 0 && hydratation < 2.0
                || containsOneOf(contexte, "stress", "suivi", "controle", "anx", "tension", "respir")) {
            return new RecommendationProfile("marche", 25, 110, 1.5, 8, "intensite moderee");
        }

        if (containsOneOf(contexte, "cardio", "diab", "surpoids", "reeducation")) {
            return new RecommendationProfile("marche", 30, 140, 2.0, 7, "endurance reguliere");
        }

        return new RecommendationProfile("marche", 30, 140, 2.2, 7, "entretien general");
    }

    private String buildNotes(Etat etat, RecommendationProfile profile) {
        StringBuilder notes = new StringBuilder("Activite generee automatiquement selon l'etat du patient.");
        notes.append(" Niveau recommande: ").append(profile.niveau()).append('.');

        if ("repos".equals(profile.type())) {
            notes.append(" Effort physique non recommande pour le moment.");
        }

        if (!normalize(etat.getTraitementEnCours()).isEmpty()) {
            notes.append(" Traitement: ").append(normalize(etat.getTraitementEnCours())).append('.');
        }
        if (!normalize(etat.getRemarquesCliniques()).isEmpty()) {
            notes.append(" Remarques cliniques: ").append(normalize(etat.getRemarquesCliniques())).append('.');
        }
        if (!normalize(etat.getTemperatureCorporelle()).isEmpty()) {
            notes.append(" Temperature: ").append(etat.getTemperatureCorporelle()).append(" deg C.");
        }
        if (!normalize(etat.getNiveauHydratation()).isEmpty()) {
            notes.append(" Hydratation: ").append(etat.getNiveauHydratation()).append(" L.");
        }

        if (parseDouble(etat.getTemperatureCorporelle()) >= 39.0) {
            notes.append(" Temperature tres elevee: repos, hydratation et avis medical conseilles.");
        }

        return notes.toString();
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
