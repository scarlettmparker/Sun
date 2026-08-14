package com.sun.briareus.service;

import com.sun.base.service.BaseService;
import com.sun.briareus.model.BlogPostTypeEntity;
import com.sun.briareus.repository.BlogPostTypeRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for blog post types.
 */
@Service
@Transactional
public class BlogPostTypeService extends BaseService<BlogPostTypeEntity> {

  private final BlogPostTypeRepository typeRepository;

  public BlogPostTypeService(BlogPostTypeRepository repository) {
    super(repository);
    this.typeRepository = repository;
  }

  /**
   * Finds a type by its unique name.
   *
   * @param name the type name
   * @return the matching type, if any
   */
  public Optional<BlogPostTypeEntity> findByName(String name) {
    return typeRepository.findByName(name);
  }
}
