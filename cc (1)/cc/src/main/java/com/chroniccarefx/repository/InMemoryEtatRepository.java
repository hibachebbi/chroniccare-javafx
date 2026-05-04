package com.chroniccarefx.repository;

import com.chroniccarefx.model.Etat;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public class InMemoryEtatRepository implements EtatRepository {
    private final Map<Long, Etat> store = new HashMap<>();
    private long sequence = 0L;

    @Override
    public List<Etat> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Etat::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public Optional<Etat> findById(Long id) {
        Etat etat = store.get(id);
        return Optional.ofNullable(etat == null ? null : copy(etat));
    }

    @Override
    public Etat save(Etat entity) {
        sequence++;
        Etat saved = copy(entity);
        saved.setId(sequence);
        store.put(sequence, saved);
        return copy(saved);
    }

    @Override
    public Etat update(Long id, Etat entity) {
        if (!store.containsKey(id)) {
            throw new NoSuchElementException("Etat introuvable: " + id);
        }

        Etat updated = copy(entity);
        updated.setId(id);
        store.put(id, updated);
        return copy(updated);
    }

    @Override
    public void deleteById(Long id) {
        if (store.remove(id) == null) {
            throw new NoSuchElementException("Etat introuvable: " + id);
        }
    }

    private Etat copy(Etat source) {
        return new Etat(
                source.getId(),
                source.getUtilisateurId(),
                source.getTraitementEnCours(),
                source.getRemarquesCliniques(),
                source.getTemperatureCorporelle(),
                source.getNiveauHydratation(),
                source.getDateReleve()
        );
    }
}
