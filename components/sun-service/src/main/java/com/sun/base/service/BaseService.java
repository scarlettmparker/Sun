package com.sun.base.service;

import com.sun.base.model.BaseEntity;
import com.sun.base.repository.BaseRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

public abstract class BaseService<T extends BaseEntity> {

    protected final BaseRepository<T> repository;

    protected BaseService(BaseRepository<T> repository) {
        this.repository = repository;
    }

    public List<T> findAll() {
        return repository.findAll();
    }

    public Page<T> findAllPaged(Pageable pageable) {
        return repository.findAll(pageable);
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