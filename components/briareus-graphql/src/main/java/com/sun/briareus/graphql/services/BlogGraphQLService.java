package com.sun.briareus.graphql.services;

import com.sun.base.util.FilterSpec;
import com.sun.base.util.GraphQLSupport;
import com.sun.base.util.PageRequests;
import com.sun.briareus.graphql.mappers.BlogPostMapper;
import com.sun.briareus.graphql.mappers.BlogPostTypeMapper;
import com.sun.briareus.model.BlogPostTypeEntity;
import com.sun.briareus.model.PostEntity;
import com.sun.briareus.service.BlogPostTypeService;
import com.sun.briareus.service.BriareusService;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogPostInput;
import com.sun.briareus.codegen.types.BlogPostType;
import com.sun.briareus.codegen.types.PagedBlogPosts;
import com.sun.briareus.codegen.types.PageInfo;
import com.sun.briareus.codegen.types.PaginationInput;
import com.sun.briareus.codegen.types.QueryResult;
import com.sun.briareus.codegen.types.QuerySuccess;
import com.sun.briareus.codegen.types.StandardError;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL business logic for the blogsite.
 */
@Service
public class BlogGraphQLService {

  private static final Logger logger = LoggerFactory.getLogger(BlogGraphQLService.class);

  @Autowired
  private BriareusService briareusService;

  @Autowired
  private BlogPostTypeService blogPostTypeService;

  @Autowired
  private BlogPostMapper blogPostMapper;

  @Autowired
  private BlogPostTypeMapper blogPostTypeMapper;

  /**
   * Retrieves a page of blog posts matching the filters.
   *
   * @param pagination the pagination and filter input
   * @return the matching page
   */
  @Transactional(readOnly = true)
  public PagedBlogPosts listBlogPosts(PaginationInput pagination) {
    logger.info("Retrieving blog posts");

    Pageable pageable = toPageable(pagination, "createdAt", Sort.Direction.DESC);
    List<FilterSpec> filters = GraphQLSupport.toFilterSpecs(
        pagination == null ? null : pagination.getFilters(),
        f -> new FilterSpec(f.getField(), f.getOperator().name(), f.getValue()));
    Page<PostEntity> result = briareusService.listPostsPaged(filters, pageable);
    List<BlogPost> items = result.getContent().stream().map(blogPostMapper::map).toList();

    logger.info("Retrieved {} blog posts", items.size());
    return PagedBlogPosts.newBuilder().items(items).pageInfo(pageInfo(result)).build();
  }

  /**
   * Retrieves a specific blog post with its information by ID.
   *
   * @param id the blog post ID as string
   * @return the GraphQL BlogPost object
   */
  @Transactional(readOnly = true)
  public BlogPost locateBlogPost(String id) {
    logger.info("Retrieving blog post by ID: {}", id);

    PostEntity postEntity = briareusService.locatePost(UUID.fromString(id))
        .orElseThrow(() -> new RuntimeException("Blog post not found with id: " + id));

    BlogPost blogPost = blogPostMapper.map(postEntity);

    logger.info("Retrieved blog post {} with id {}", blogPost.getTitle(), blogPost.getId());
    return blogPost;
  }

  /**
   * Retrieves blog posts that reference any of the given remote-object ids.
   *
   * @param ids the remote-object ids to match
   * @return a list of GraphQL BlogPost objects
   */
  @Transactional(readOnly = true)
  public List<BlogPost> listByRemoteObjects(List<String> ids) {
    logger.info("Retrieving blog posts by remote object ids: {}", ids);

    List<PostEntity> postEntities = briareusService.listByRemoteObjects(ids);
    List<BlogPost> blogPosts = postEntities.stream()
        .map(blogPostMapper::map)
        .collect(Collectors.toList());

    logger.info("Retrieved {} blog posts matching remote object ids", blogPosts.size());
    return blogPosts;
  }

  /**
   * Lists every blog post type.
   *
   * @return the post types
   */
  @Transactional(readOnly = true)
  public List<BlogPostType> blogPostTypes() {
    logger.info("Retrieving blog post types");
    return blogPostTypeMapper.map(blogPostTypeService.findAll());
  }

  /**
   * Creates a blog post type with a unique name.
   *
   * @param name the type name
   * @param description an optional description
   * @return the outcome of the creation
   */
  @Transactional
  public QueryResult createBlogPostType(String name, String description) {
    logger.info("Creating blog post type: {}", name);

    if (name == null || name.isBlank()) {
      return StandardError.newBuilder().message("Blog post type name is required").build();
    }
    if (blogPostTypeService.findByName(name).isPresent()) {
      return StandardError.newBuilder().message("Blog post type already exists: " + name).build();
    }

    BlogPostTypeEntity entity = new BlogPostTypeEntity();
    entity.setName(name);
    entity.setDescription(description);
    BlogPostTypeEntity saved = blogPostTypeService.save(entity);

    logger.info("Created blog post type {} with id {}", name, saved.getId());
    return QuerySuccess.newBuilder()
        .message("Blog post type created")
        .id(saved.getId().toString())
        .build();
  }

  /**
   * Creates a new blog post.
   *
   * @param title the title of the blog post
   * @param input the input data for the blog post
   * @return QueryResult indicating success or error
   */
  @Transactional
  public QueryResult createBlogPost(String title, BlogPostInput input) {
    logger.info("Creating blog post with title: {}", title);

    try {
      PostEntity postEntity = blogPostMapper.mapInput(title, input);
      PostEntity savedEntity = briareusService.save(postEntity);

      logger.info("Successfully created blog post with id: {}", savedEntity.getId());
      return QuerySuccess.newBuilder()
          .message("Blog post created successfully")
          .id(savedEntity.getId().toString())
          .build();
    } catch (Exception e) {
      logger.error("Failed to create blog post with title: {}", title, e);
      return StandardError.newBuilder()
          .message("Failed to create blog post: " + e.getMessage())
          .build();
    }
  }

  /**
   * Builds a pageable from the pagination input.
   *
   * @param pagination the pagination input
   * @param defaultSortBy the fallback sort property
   * @param defaultDir the fallback sort direction
   * @return the pageable
   */
  private Pageable toPageable(PaginationInput pagination, String defaultSortBy,
      Sort.Direction defaultDir) {
    if (pagination == null) {
      return PageRequests.of(null, null, null, null, defaultSortBy, defaultDir);
    }
    return PageRequests.of(pagination.getPage(), pagination.getSize(), pagination.getSortBy(),
        pagination.getSortDir() == null ? null : pagination.getSortDir().name(),
        defaultSortBy, defaultDir);
  }

  /**
   * Builds page metadata from a Spring data page.
   *
   * @param result the data page
   * @return the GraphQL PageInfo
   */
  private PageInfo pageInfo(Page<?> result) {
    return PageInfo.newBuilder()
        .page(result.getNumber())
        .size(result.getSize())
        .totalPages(result.getTotalPages())
        .totalCount((int) result.getTotalElements())
        .hasNextPage(result.hasNext())
        .hasPreviousPage(result.hasPrevious())
        .build();
  }
}
