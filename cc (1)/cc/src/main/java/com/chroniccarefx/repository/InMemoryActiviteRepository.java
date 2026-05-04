package com.chroniccarefx.repository;

import com.chroniccarefx.model.Activite;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public class InMemoryActiviteRepository implements ActiviteRepository {
    private final Map<Long, Activite> store = new HashMap<>();
    private long sequence = 0L;

    @Override
    public List<Activite> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Activite::getId))
                .map(this::copy)
                .toList();
    }

    @Override
    public Optional<Activite> findById(Long id) {
        Activite activite = store.get(id);
        return Optional.ofNullable(activite == null ? null : copy(activite));
    }

    @Override
    public Activite save(Activite entity) {
        sequence++;
        Activite saved = copy(entity);
        saved.setId(sequence);
        store.put(sequence, saved);
        return copy(saved);
    }

    @Override
    public Activite update(Long id, Activite entity) {
        if (!store.containsKey(id)) {
            throw new NoSuchElementException("Activite introuvable: " + id);
        }

        Activite updated = copy(entity);
        updated.setId(id);
        store.put(id, updated);
        return copy(updated);
    }

    @Override
    public void deleteById(Long id) {
        if (store.remove(id) == null) {
            throw new NoSuchElementException("Activite introuvable: " + id);
        }
    }

    @Override
    public List<Activite> findByEtatId(Long etatId) {
        return store.values().stream()
                .filter(a -> etatId.equals(a.getEtatId()))
                .sorted(Comparator.comparing(Activite::getId))
                .map(this::copy)
                .toList();
    }

    private Activite copy(Activite source) {
        return new Activite(
                source.getId(),
                source.getUtilisateurId(),
                source.getEtatId(),
                source.getType(),
                source.getDuree(),
                source.getCalories(),
                source.getDistanceKm(),
                source.getHeuresRepos(),
                source.getDateActivite(),
                source.getNotes(),
                source.getCreatedAt()
        );
    }
}
