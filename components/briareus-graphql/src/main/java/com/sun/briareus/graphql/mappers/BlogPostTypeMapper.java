package com.sun.briareus.graphql.mappers;

import com.sun.briareus.codegen.types.BlogPostType;
import com.sun.briareus.model.BlogPostTypeEntity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Converts blog post type entities to their GraphQL representation.
 */
@Component
public class BlogPostTypeMapper {

  /**
   * Maps a single entity to a GraphQL type.
   *
   * @param entity the persisted entity
   * @return the GraphQL BlogPostType
   */
  public BlogPostType map(BlogPostTypeEntity entity) {
    BlogPostType.Builder builder = BlogPostType.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName());
    if (entity.getDescription() != null) {
      builder.description(entity.getDescription());
    }
    return builder.build();
  }

  /**
   * Maps a list of entities.
   *
   * @param entities the persisted entities
   * @return the list of GraphQL types
   */
  public List<BlogPostType> map(List<BlogPostTypeEntity> entities) {
    return entities.stream().map(this::map).collect(Collectors.toList());
  }
}
