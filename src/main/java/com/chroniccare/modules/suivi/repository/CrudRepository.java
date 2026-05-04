package com.chroniccare.modules.suivi.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T> {
    List<T> findAll();
    Optional<T> findById(Long id);
    T save(T entity);
    T update(Long id, T entity);
    void deleteById(Long id);
}
