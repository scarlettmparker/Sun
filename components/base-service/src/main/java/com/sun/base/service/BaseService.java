package com.sun.base.service;

import com.sun.base.model.BaseEntity;
import com.sun.base.repository.BaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
public abstract class BaseService<T extends BaseEntity> {

    @Autowired
    protected BaseRepository<T> repository;

    public List<T> findAll() {
        return repository.findAll();
    }

    public Optional<T> findById(UUID id) {
        return repository.findById(id);
    }

    public T save(T entity) {
        return repository.save(entity);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    // Extension points for domain-specific logic
}