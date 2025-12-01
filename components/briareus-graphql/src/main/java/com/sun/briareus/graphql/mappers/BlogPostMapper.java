package com.sun.briareus.graphql.mappers;

import com.sun.briareus.codegen.types.BlogPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.sun.briareus.model.PostEntity;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting domain Post entities to GraphQL BlogPost types.
 */
@Component
public class BlogPostMapper {
  
  private static final Logger logger = LoggerFactory.getLogger(BlogPostMapper.class);

  @Autowired
  private BlogPostMapper blogPostMapper;

  /**
   * Maps a domain PostEntity to a GraphQL BlogPost type.
   * 
   * @param postEntity the domain PostEntity to map
   * @return the mapped GraphQL BlogPost type.
   */
  public BlogPost map(PostEntity postEntity) {
    logger.debug("Mapping post {}", postEntity.getTitle());

    BlogPost blogPost = BlogPost.newBuilder()
      .id(postEntity.getId().toString())
      .title(postEntity.getTitle())
      .content(postEntity.getContent())
      .tags(postEntity.getTags())
      .build();

    logger.debug("Mapped blog post {} with id {}", postEntity.getTitle(), postEntity.getId());
    return blogPost;
  }

}