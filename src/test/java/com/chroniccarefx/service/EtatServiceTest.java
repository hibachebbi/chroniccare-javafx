package com.chroniccarefx.service;

import com.chroniccarefx.model.Etat;
import com.chroniccarefx.repository.InMemoryActiviteRepository;
import com.chroniccarefx.repository.InMemoryEtatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EtatServiceTest {
    private EtatService etatService;
    private ActiviteService activiteService;

    @BeforeEach
    void setUp() {
        InMemoryActiviteRepository activiteRepository = new InMemoryActiviteRepository();
        InMemoryEtatRepository etatRepository = new InMemoryEtatRepository();

        etatService = new EtatService(etatRepository, activiteRepository);
        activiteService = new ActiviteService(activiteRepository, etatRepository);
    }

    @Test
    void shouldCreateEtatWhenDataIsValid() {
        Etat etat = etatService.create(1L, "Traitement A", "RAS", "37.0", "2.0", LocalDateTime.now());

        assertEquals(1L, etat.getId());
        assertEquals("Traitement A", etat.getTraitementEnCours());
    }

    @Test
    void shouldRejectDeleteEtatWhenUsedByActivite() {
        Etat etat = etatService.create(1L, "En attente", "Validation", "36.5", "2.5", LocalDateTime.now());
        activiteService.create(1L, etat.getId(), "marche", 30, 120, 2.4, 8, LocalDateTime.now(), "ok");

        assertThrows(IllegalStateException.class, () -> etatService.delete(etat.getId()));
    }
}
