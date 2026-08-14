package com.sun.briareus.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.briareus.model.BlogPostTypeEntity;
import java.util.Optional;

/**
 * Data access for blog post types.
 */
public interface BlogPostTypeRepository extends BaseRepository<BlogPostTypeEntity> {

  /**
   * Finds a type by its unique name.
   *
   * @param name the type name
   * @return the matching type, if any
   */
  Optional<BlogPostTypeEntity> findByName(String name);
}
