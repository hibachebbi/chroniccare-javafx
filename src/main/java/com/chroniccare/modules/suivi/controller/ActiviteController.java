package com.chroniccare.modules.suivi.controller;

import com.chroniccare.modules.suivi.model.Activite;
import com.chroniccare.modules.suivi.model.Etat;
import com.chroniccare.modules.suivi.service.ActiviteService;
import com.chroniccare.modules.suivi.service.EtatService;

import java.time.LocalDateTime;
import java.util.List;

public class ActiviteController {
    private final ActiviteService activiteService;
    private final EtatService etatService;

    public ActiviteController(ActiviteService activiteService, EtatService etatService) {
        this.activiteService = activiteService;
        this.etatService = etatService;
    }

    public List<Activite> listActivites() { return activiteService.listAll(); }
    public List<Etat> listEtats() { return etatService.listAll(); }

    public Activite createActivite(Long utilisateurId, Long etatId, String type, Integer duree, Integer calories,
                                   Double distanceKm, Integer heuresRepos, LocalDateTime dateActivite, String notes) {
        return activiteService.create(utilisateurId, etatId, type, duree, calories,
                distanceKm, heuresRepos, dateActivite, notes);
    }

    public Activite updateActivite(Long id, Long utilisateurId, Long etatId, String type, Integer duree,
                                   Integer calories, Double distanceKm, Integer heuresRepos,
                                   LocalDateTime dateActivite, String notes) {
        return activiteService.update(id, utilisateurId, etatId, type, duree, calories,
                distanceKm, heuresRepos, dateActivite, notes);
    }

    public void deleteActivite(Long id) { activiteService.delete(id); }
}
