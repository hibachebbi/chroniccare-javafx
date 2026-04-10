package com.chroniccarefx.service;

import com.chroniccarefx.model.Etat;
import com.chroniccarefx.repository.InMemoryActiviteRepository;
import com.chroniccarefx.repository.InMemoryEtatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActiviteServiceTest {
    private ActiviteService activiteService;
    private EtatService etatService;

    @BeforeEach
    void setUp() {
        InMemoryActiviteRepository activiteRepository = new InMemoryActiviteRepository();
        InMemoryEtatRepository etatRepository = new InMemoryEtatRepository();

        etatService = new EtatService(etatRepository, activiteRepository);
        activiteService = new ActiviteService(activiteRepository, etatRepository);
    }

    @Test
    void shouldCreateActiviteWhenDataIsValid() {
        Etat etat = etatService.create(1L, "Suivi", "actif", "37.0", "2.0", LocalDateTime.now());

        var activite = activiteService.create(1L, etat.getId(), "course", 45, 300, 5.0, 7, LocalDateTime.now(), "Controle");

        assertEquals(1L, activite.getId());
        assertEquals("course", activite.getType());
    }

    @Test
    void shouldRejectCreateWhenEtatDoesNotExist() {
        assertThrows(NoSuchElementException.class,
                () -> activiteService.create(1L, 99L, "marche", 20, 80, null, null, LocalDateTime.now(), ""));
    }
}
