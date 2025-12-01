package com.sun.briareus.graphql.services;

import com.sun.briareus.service.BriareusService;
import com.sun.briareus.graphql.mappers.BlogPostMapper;
import com.sun.briareus.codegen.types.BlogPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import com.sun.briareus.model.PostEntity;

/**
 * Service for handling GraphQL-specific business logic for the Blogsite.
 * This service acts as an intermediary between the GraphQL layer and the domain
 * services.
 */
@Service
public class BlogGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(BlogGraphQLService.class);

  @Autowired
  private BriareusService briareusService;

  @Autowired
  private BlogPostMapper blogPostMapper;

  /**
   * Retrieves all blog posts.
   * 
   * @return a list of GraphQL BlgoPost objects
   */
  @Transactional("briareusTransactionManager")
  public List<BlogPost> listBlogPosts() {
    logger.info("Retrieving blog posts");

    List<PostEntity> postEntities = briareusService.listPosts();
    List<BlogPost> blogPosts = postEntities.stream()
        .map(blogPostMapper::map)
        .collect(Collectors.toList());

    logger.info("Retrieved {} blog posts", blogPosts.size());
    return blogPosts;
  }

  /**
   * Retrieves a specific blog post with its information by ID.
   * 
   * @param id the blog post ID as string
   * @return the GraphQL BlogPost object
   */
  @Transactional("briareusTransactionManager")
  public BlogPost locateBlogPost(String id) {
    logger.info("Retrieving blog post by ID: {}", id);

    PostEntity postEntity = briareusService.locatePost(java.util.UUID.fromString(id))
        .orElseThrow(() -> new RuntimeException("Blog post not found with id: " + id));

    BlogPost blogPost = blogPostMapper.map(postEntity);

    logger.info("Retrieved blog post {} with id {}", blogPost.getTitle(), blogPost.getId());
    return blogPost;
  }
}