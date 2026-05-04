package com.chroniccarefx.repository;

import com.chroniccarefx.model.Activite;

import java.util.List;

public interface ActiviteRepository extends CrudRepository<Activite> {
    List<Activite> findByEtatId(Long etatId);
}
