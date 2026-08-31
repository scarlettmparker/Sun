package com.sun.briareus.graphql.mappers;

import java.time.ZoneOffset;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogPostInput;
import com.sun.briareus.model.BlogPostTypeEntity;
import com.sun.briareus.model.PostEntity;
import com.sun.briareus.service.BlogPostTypeService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Converts between blog post entities and their GraphQL representation.
 */
@Component
public class BlogPostMapper {

  private static final Logger logger = LoggerFactory.getLogger(BlogPostMapper.class);

  @Autowired
  private BlogPostTypeMapper blogPostTypeMapper;

  @Autowired
  private BlogPostTypeService blogPostTypeService;

  /**
   * Maps a domain PostEntity to a GraphQL BlogPost type.
   *
   * @param postEntity the domain PostEntity to map
   * @return the mapped GraphQL BlogPost type
   */
  public BlogPost map(PostEntity postEntity) {
    logger.debug("Mapping post {}", postEntity.getTitle());

    BlogPost.Builder builder = BlogPost.newBuilder()
        .id(postEntity.getId().toString())
        .title(postEntity.getTitle())
        .content(postEntity.getContent())
        .tags(postEntity.getTags())
        .remoteObject(postEntity.getRemoteObject())
        .language(postEntity.getLanguage())
        .parentId(postEntity.getParentId() == null ? null : postEntity.getParentId().toString())
        .createdAt(postEntity.getCreatedAt() == null ? null : postEntity.getCreatedAt().atOffset(ZoneOffset.UTC))
        .updatedAt(postEntity.getLastUpdatedAt() == null ? null : postEntity.getLastUpdatedAt().atOffset(ZoneOffset.UTC));
    if (postEntity.getType() != null) {
      builder.type(blogPostTypeMapper.map(postEntity.getType()));
    }

    BlogPost blogPost = builder.build();
    logger.debug("Mapped blog post {} with id {}", postEntity.getTitle(), postEntity.getId());
    return blogPost;
  }

  /**
   * Maps a GraphQL BlogPostInput and title to a domain PostEntity.
   *
   * @param title the title of the blog post
   * @param input the GraphQL BlogPostInput to map
   * @return the mapped domain PostEntity
   */
  public PostEntity mapInput(String title, BlogPostInput input) {
    logger.debug("Mapping input for blog post with title: {}", title);

    PostEntity postEntity = new PostEntity();
    postEntity.setTitle(title);
    postEntity.setContent(input.getContent());
    postEntity.setTags(input.getTags());
    postEntity.setRemoteObject(input.getRemoteObject());
    postEntity.setLanguage(input.getLanguage());
    if (input.getTypeId() != null) {
      postEntity.setType(resolveType(UUID.fromString(input.getTypeId())));
    }
    if (input.getParentId() != null) {
      UUID parentId = UUID.fromString(input.getParentId());
      postEntity.setParentId(parentId);
    }

    logger.debug("Mapped input to post entity with title: {}", title);
    return postEntity;
  }

  /**
   * Updates an existing post from input.
   *
   * @param entity the existing entity
   * @param input the update input
   */
  public void update(PostEntity entity, BlogPostInput input) {
    if (input.getContent() != null) {
      entity.setContent(input.getContent());
    }
    if (input.getTags() != null) {
      entity.setTags(input.getTags());
    }
    if (input.getRemoteObject() != null) {
      entity.setRemoteObject(input.getRemoteObject());
    }
    if (input.getLanguage() != null) {
      entity.setLanguage(input.getLanguage());
    }
    if (input.getParentId() != null) {
      entity.setParentId(UUID.fromString(input.getParentId()));
    }
    if (input.getTypeId() != null) {
      entity.setType(resolveType(UUID.fromString(input.getTypeId())));
    }
  }

  /**
   * Loads a post type by id.
   *
   * @param id the post type id
   * @return the post type
   */
  private BlogPostTypeEntity resolveType(UUID id) {
    return blogPostTypeService.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Blog post type not found: " + id));
  }
}
