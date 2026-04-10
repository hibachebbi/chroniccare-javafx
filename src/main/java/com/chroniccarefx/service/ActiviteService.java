package com.chroniccarefx.service;

import com.chroniccarefx.model.Activite;
import com.chroniccarefx.repository.ActiviteRepository;
import com.chroniccarefx.repository.EtatRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

public class ActiviteService {
    private final ActiviteRepository activiteRepository;
    private final EtatRepository etatRepository;

    public ActiviteService(ActiviteRepository activiteRepository, EtatRepository etatRepository) {
        this.activiteRepository = activiteRepository;
        this.etatRepository = etatRepository;
    }

    public List<Activite> listAll() {
        return activiteRepository.findAll();
    }

    public Activite create(
            Long utilisateurId,
            Long etatId,
            String type,
            Integer duree,
            Integer calories,
            Double distanceKm,
            Integer heuresRepos,
            LocalDateTime dateActivite,
            String notes
    ) {
        validate(utilisateurId, etatId, type, duree, calories, distanceKm, heuresRepos, dateActivite);
        Activite activite = new Activite(
                null,
                utilisateurId,
                etatId,
                type.trim(),
                duree,
                calories,
                distanceKm,
                heuresRepos,
                dateActivite,
                normalize(notes),
                LocalDateTime.now()
        );
        return activiteRepository.save(activite);
    }

    public Activite update(
            Long id,
            Long utilisateurId,
            Long etatId,
            String type,
            Integer duree,
            Integer calories,
            Double distanceKm,
            Integer heuresRepos,
            LocalDateTime dateActivite,
            String notes
    ) {
        if (id == null) {
            throw new IllegalArgumentException("ID activite obligatoire.");
        }

        validate(utilisateurId, etatId, type, duree, calories, distanceKm, heuresRepos, dateActivite);
        LocalDateTime createdAt = activiteRepository.findById(id)
                .map(Activite::getCreatedAt)
                .orElse(LocalDateTime.now());

        Activite activite = new Activite(
                id,
                utilisateurId,
                etatId,
                type.trim(),
                duree,
                calories,
                distanceKm,
                heuresRepos,
                dateActivite,
                normalize(notes),
                createdAt
        );
        return activiteRepository.update(id, activite);
    }

    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID activite obligatoire.");
        }
        activiteRepository.deleteById(id);
    }

    private void validate(
            Long utilisateurId,
            Long etatId,
            String type,
            Integer duree,
            Integer calories,
            Double distanceKm,
            Integer heuresRepos,
            LocalDateTime dateActivite
    ) {
        if (utilisateurId == null || utilisateurId <= 0) {
            throw new IllegalArgumentException("Utilisateur obligatoire.");
        }

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Type activite obligatoire.");
        }

        if (!Activite.TYPE_CHOICES.contains(type.trim())) {
            throw new IllegalArgumentException("Type invalide: marche, course ou velo.");
        }

        if (duree == null || duree <= 0) {
            throw new IllegalArgumentException("Duree obligatoire et > 0.");
        }

        if (calories == null || calories < 0) {
            throw new IllegalArgumentException("Calories obligatoires et >= 0.");
        }

        if (distanceKm != null && distanceKm < 0) {
            throw new IllegalArgumentException("Distance doit etre >= 0.");
        }

        if (heuresRepos != null && heuresRepos < 0) {
            throw new IllegalArgumentException("Heures repos doivent etre >= 0.");
        }

        if (dateActivite == null) {
            throw new IllegalArgumentException("Date activite obligatoire.");
        }

        if (etatId != null && etatRepository.findById(etatId).isEmpty()) {
            throw new NoSuchElementException("Etat introuvable: " + etatId);
        }
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim();
    }
}
