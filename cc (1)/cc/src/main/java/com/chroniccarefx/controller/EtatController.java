package com.chroniccarefx.controller;

import com.chroniccarefx.model.Etat;
import com.chroniccarefx.service.EtatService;

import java.time.LocalDateTime;
import java.util.List;

public class EtatController {
    private final EtatService etatService;

    public EtatController(EtatService etatService) {
        this.etatService = etatService;
    }

    public List<Etat> listEtats() {
        return etatService.listAll();
    }

    public Etat createEtat(
            Long utilisateurId,
            String traitementEnCours,
            String remarquesCliniques,
            String temperatureCorporelle,
            String niveauHydratation,
            LocalDateTime dateReleve
    ) {
        return etatService.create(
                utilisateurId,
                traitementEnCours,
                remarquesCliniques,
                temperatureCorporelle,
                niveauHydratation,
                dateReleve
        );
    }

    public Etat updateEtat(
            Long id,
            Long utilisateurId,
            String traitementEnCours,
            String remarquesCliniques,
            String temperatureCorporelle,
            String niveauHydratation,
            LocalDateTime dateReleve
    ) {
        return etatService.update(
                id,
                utilisateurId,
                traitementEnCours,
                remarquesCliniques,
                temperatureCorporelle,
                niveauHydratation,
                dateReleve
        );
    }

    public void deleteEtat(Long id) {
        etatService.delete(id);
    }
}
