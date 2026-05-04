package com.chroniccare.modules.suivi.repository;

import com.chroniccare.modules.suivi.model.Activite;

import java.util.List;

public interface ActiviteRepository extends CrudRepository<Activite> {
    List<Activite> findByEtatId(Long etatId);
}
