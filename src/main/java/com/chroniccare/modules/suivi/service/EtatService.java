package com.chroniccare.modules.suivi.service;

import com.chroniccare.modules.suivi.model.Etat;
import com.chroniccare.modules.suivi.repository.ActiviteRepository;
import com.chroniccare.modules.suivi.repository.EtatRepository;

import java.time.LocalDateTime;
import java.util.List;

public class EtatService {
    private final EtatRepository etatRepository;
    private final ActiviteRepository activiteRepository;
    private final ActiviteRecommendationService activiteRecommendationService;

    public EtatService(EtatRepository etatRepository, ActiviteRepository activiteRepository) {
        this.etatRepository = etatRepository;
        this.activiteRepository = activiteRepository;
        this.activiteRecommendationService = new ActiviteRecommendationService();
    }

    public List<Etat> listAll() {
        return etatRepository.findAll();
    }

    public Etat create(Long utilisateurId, String traitementEnCours, String remarquesCliniques,
                       String temperatureCorporelle, String niveauHydratation, LocalDateTime dateReleve) {
        validate(utilisateurId, temperatureCorporelle, niveauHydratation, dateReleve);
        Etat savedEtat = etatRepository.save(new Etat(
                null, utilisateurId, normalize(traitementEnCours), normalize(remarquesCliniques),
                normalizeNullableChoice(temperatureCorporelle), normalizeNullableChoice(niveauHydratation), dateReleve
        ));
        activiteRepository.save(activiteRecommendationService.buildRecommendedActivite(savedEtat));
        return savedEtat;
    }

    public Etat update(Long id, Long utilisateurId, String traitementEnCours, String remarquesCliniques,
                       String temperatureCorporelle, String niveauHydratation, LocalDateTime dateReleve) {
        if (id == null) throw new IllegalArgumentException("ID etat obligatoire.");
        validate(utilisateurId, temperatureCorporelle, niveauHydratation, dateReleve);
        return etatRepository.update(id, new Etat(
                id, utilisateurId, normalize(traitementEnCours), normalize(remarquesCliniques),
                normalizeNullableChoice(temperatureCorporelle), normalizeNullableChoice(niveauHydratation), dateReleve
        ));
    }

    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("ID etat obligatoire.");
        if (!activiteRepository.findByEtatId(id).isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer: des activites utilisent cet etat.");
        }
        etatRepository.deleteById(id);
    }

    private void validate(Long utilisateurId, String temperatureCorporelle, String niveauHydratation,
                          LocalDateTime dateReleve) {
        if (utilisateurId == null || utilisateurId <= 0) {
            throw new IllegalArgumentException("Utilisateur obligatoire.");
        }
        String normalizedTemperature = normalizeNullableChoice(temperatureCorporelle);
        if (!normalizedTemperature.isEmpty() && !Etat.TEMPERATURE_CORPORELLE_CHOICES.contains(normalizedTemperature)) {
            throw new IllegalArgumentException("Temperature corporelle invalide.");
        }
        String normalizedHydratation = normalizeNullableChoice(niveauHydratation);
        if (!normalizedHydratation.isEmpty() && !Etat.NIVEAU_HYDRATATION_CHOICES.contains(normalizedHydratation)) {
            throw new IllegalArgumentException("Niveau d hydratation invalide.");
        }
        if (dateReleve == null) throw new IllegalArgumentException("Date releve obligatoire.");
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim();
    }

    private String normalizeNullableChoice(String input) {
        return input == null ? "" : input.trim();
    }
}
