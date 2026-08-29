package com.sun.briareus.graphql.services;

import com.sun.base.nlp.WikipediaService;
import com.sun.base.nlp.WikipediaSummary;
import com.sun.base.nlp.WiktionaryService;
import com.sun.base.nlp.WiktionaryEntry;
import com.sun.base.permify.PermifyClient;
import com.sun.base.permify.PermifyUtil;
import com.sun.base.util.FilterSpec;
import com.sun.base.util.GraphQLSupport;
import com.sun.base.util.PageRequests;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogPostInput;
import com.sun.briareus.codegen.types.BlogPostType;
import com.sun.briareus.codegen.types.IngestBlogInput;
import com.sun.briareus.codegen.types.PagedBlogPosts;
import com.sun.briareus.codegen.types.PageInfo;
import com.sun.briareus.codegen.types.PaginationInput;
import com.sun.briareus.codegen.types.QueryResult;
import com.sun.briareus.codegen.types.QuerySuccess;
import com.sun.briareus.codegen.types.SourceKind;
import com.sun.briareus.codegen.types.StandardError;
import com.sun.briareus.graphql.mappers.BlogPostMapper;
import com.sun.briareus.graphql.mappers.BlogPostTypeMapper;
import com.sun.briareus.model.BlogPostTypeEntity;
import com.sun.briareus.model.PostEntity;
import com.sun.briareus.service.BlogPostTypeService;
import com.sun.briareus.service.BriareusService;
import com.sun.gaia.repository.ObjectShareRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.sun.gaia.service.UserContextHolder;
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

  @Autowired
  private PermifyClient permifyClient;

  @Autowired
  private ObjectShareRepository objectShareRepository;

  @Autowired
  private WikipediaService wikipediaService;

  @Autowired
  private WiktionaryService wiktionaryService;

  /**
   * Retrieves a page of blog posts matching the filters.
   *
   * @param pagination the pagination and filter input
   * @return the matching page
   */
  @Transactional(readOnly = true)
  public PagedBlogPosts listBlogPosts(PaginationInput pagination) {
    logger.info("Retrieving blog posts");

    Pageable pageable = toPageable(pagination, "lastUpdatedAt", Sort.Direction.DESC);
    List<FilterSpec> filters = GraphQLSupport.toFilterSpecs(
        pagination == null ? null : pagination.getFilters(),
        f -> new FilterSpec(f.getField(), f.getOperator().name(), f.getValue()));
    UUID viewer = currentUserId();
    Page<PostEntity> result = briareusService.listPostsPaged(filters, pageable, viewer);
    List<BlogPost> items = result.getContent().stream()
        .map(blogPostMapper::map)
        .toList();

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
    if (!canView(postEntity)) {
      throw new RuntimeException("Not authorized to view blog post: " + id);
    }

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

    UUID viewer = currentUserId();
    List<PostEntity> postEntities = briareusService.listByRemoteObjects(ids, viewer);
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
      if (input.getParentId() != null) {
        UUID parentId = UUID.fromString(input.getParentId());
        PostEntity parent = briareusService.locatePost(parentId).orElse(null);
        if (parent == null) {
          return StandardError.newBuilder()
              .message("Parent post not found: " + input.getParentId())
              .build();
        }
        if (!canView(parent)) {
          return StandardError.newBuilder()
              .message("Not authorized to create child under parent: " + input.getParentId())
              .build();
        }
      }
      PostEntity postEntity = blogPostMapper.mapInput(title, input);
      PostEntity savedEntity = briareusService.save(postEntity);
      writeOwnerTuple(savedEntity.getId());

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
   * Lists children of a parent post with pagination.
   *
   * @param parentId the parent post id
   * @param pagination the pagination input
   * @return the matching page
   */
  @Transactional(readOnly = true)
  public PagedBlogPosts children(String parentId, PaginationInput pagination) {
    logger.info("Retrieving children for parentId: {}", parentId);

    PostEntity parent = briareusService.locatePost(UUID.fromString(parentId)).orElse(null);
    if (parent != null && !canView(parent)) {
      throw new RuntimeException("Not authorized to view parent post: " + parentId);
    }
    Pageable pageable = toPageable(pagination, "lastUpdatedAt", Sort.Direction.DESC);
    UUID viewer = currentUserId();
    Page<PostEntity> result = briareusService.children(UUID.fromString(parentId), pageable, viewer);
    List<BlogPost> items = result.getContent().stream()
        .map(blogPostMapper::map)
        .toList();

    logger.info("Retrieved {} children for parentId: {}", items.size(), parentId);
    return PagedBlogPosts.newBuilder().items(items).pageInfo(pageInfo(result)).build();
  }

  /**
   * Appends a remote-object edge to a post.
   *
   * @param postId the post id
   * @param target the remote-object string to add
   * @return the outcome
   */
  @Transactional
  public QueryResult addRemoteObject(String postId, String target) {
    logger.info("Adding remote object {} to post {}", target, postId);

    if (target == null || target.isBlank()) {
      return StandardError.newBuilder().message("Target is required").build();
    }
    PostEntity post = briareusService.locatePost(UUID.fromString(postId))
        .orElse(null);
    if (post == null) {
      return StandardError.newBuilder().message("Blog post not found: " + postId).build();
    }
    if (!canEdit(post)) {
      return StandardError.newBuilder().message("Not authorized to edit blog post: " + postId).build();
    }
    List<String> remoteObjects = post.getRemoteObject() == null
        ? new ArrayList<>()
        : new ArrayList<>(post.getRemoteObject());
    if (!remoteObjects.contains(target)) {
      remoteObjects.add(target);
      post.setRemoteObject(remoteObjects);
      briareusService.save(post);
    }
    logger.info("Added remote object {} to post {}", target, postId);
    return QuerySuccess.newBuilder().message("Remote object added").id(postId).build();
  }

  /**
   * Removes a remote-object edge from a post.
   *
   * @param postId the post id
   * @param target the remote-object string to remove
   * @return the outcome
   */
  @Transactional
  public QueryResult removeRemoteObject(String postId, String target) {
    logger.info("Removing remote object {} from post {}", target, postId);

    if (target == null || target.isBlank()) {
      return StandardError.newBuilder().message("Target is required").build();
    }
    PostEntity post = briareusService.locatePost(UUID.fromString(postId))
        .orElse(null);
    if (post == null) {
      return StandardError.newBuilder().message("Blog post not found: " + postId).build();
    }
    if (!canEdit(post)) {
      return StandardError.newBuilder().message("Not authorized to edit blog post: " + postId).build();
    }
    List<String> remoteObjects = post.getRemoteObject() == null
        ? new ArrayList<>()
        : new ArrayList<>(post.getRemoteObject());
    if (remoteObjects.remove(target)) {
      post.setRemoteObject(remoteObjects);
      briareusService.save(post);
    }
    logger.info("Removed remote object {} from post {}", target, postId);
    return QuerySuccess.newBuilder().message("Remote object removed").id(postId).build();
  }

  /**
   * Creates a blog from a Wikipedia or Wiktionary source.
   *
   * @param input the ingest input
   * @return the outcome
   */
  @Transactional
  public QueryResult ingestBlogFromSource(IngestBlogInput input) {
    if (input == null) {
      return StandardError.newBuilder().message("Input is required").build();
    }
    String title = input.getTitle();
    String typeName = input.getTypeName();
    SourceKind sourceKind = input.getSourceKind();
    String sourceId = input.getSourceId();
    if (title == null || title.isBlank()) {
      return StandardError.newBuilder().message("Title is required").build();
    }
    if (typeName == null || typeName.isBlank()) {
      return StandardError.newBuilder().message("Type name is required").build();
    }
    if (sourceKind == null) {
      return StandardError.newBuilder().message("Source kind is required").build();
    }
    if (sourceId == null || sourceId.isBlank()) {
      return StandardError.newBuilder().message("Source id is required").build();
    }
    UUID viewer = currentUserId();
    if (viewer == null) {
      return StandardError.newBuilder().message("Authentication required").build();
    }
    BlogPostTypeEntity type = blogPostTypeService.findByName(typeName).orElse(null);
    if (type == null) {
      return StandardError.newBuilder().message("Unknown type: " + typeName).build();
    }
    String norm = sourceId.toLowerCase().trim();
    String summary;
    String sourceUrl;
    List<String> edges = new ArrayList<>();
    if (sourceKind == SourceKind.WIKIPEDIA) {
      WikipediaSummary s = wikipediaService.summary(norm);
      if (s == null || s.extract() == null || s.extract().isBlank()) {
        return StandardError.newBuilder().message("not_found").build();
      }
      summary = s.extract();
      sourceUrl = s.pageUrl();
      if (sourceUrl == null || sourceUrl.isBlank()) {
        sourceUrl = "https://en.wikipedia.org/wiki/" + norm;
      }
      edges.add("source:wikipedia:" + norm);
      edges.add("wikipedia:page:" + norm);
    } else if (sourceKind == SourceKind.WIKTIONARY) {
      WiktionaryEntry e = wiktionaryService.define(norm);
      if (e == null || e.definitions().isEmpty()) {
        return StandardError.newBuilder().message("not_found").build();
      }
      summary = String.join("\n\n", e.definitions());
      sourceUrl = e.sourceUrl();
      if (sourceUrl == null || sourceUrl.isBlank()) {
        sourceUrl = "https://en.wiktionary.org/wiki/" + norm;
      }
      edges.add("source:wiktionary:" + norm);
    } else {
      return StandardError.newBuilder().message("Unsupported sourceKind").build();
    }
    PostEntity post = new PostEntity();
    post.setTitle(title.trim());
    post.setContent(summary + "\n\nSource: " + sourceUrl);
    post.setType(type);
    post.setLanguage("en");
    post.setTags(List.of(sourceKind.name().toLowerCase(), "ingested"));
    post.setRemoteObject(edges);
    PostEntity saved = briareusService.save(post);
    writeOwnerTuple(saved.getId());
    logger.info("Ingested blog {} from {}:{}", saved.getId(), sourceKind, norm);
    return QuerySuccess.newBuilder()
        .message("Blog ingested")
        .id(saved.getId().toString())
        .build();
  }

  /**
   * Returns the current authenticated user id, or null when unauthenticated.
   *
   * @return the viewer id
   */
  private UUID currentUserId() {
    return UserContextHolder.getUserId();
  }

  /**
   * Checks whether the current viewer may view the post.
   *
   * @param post the post
   * @return true when visible
   */
  private boolean canView(PostEntity post) {
    if (post.getType() != null) {
      String typeName = post.getType().getName();
      if ("BOT_FAQ".equals(typeName) || "BOT_HELP".equals(typeName)) {
        return true;
      }
    }
    UUID viewer = currentUserId();
    if (viewer != null && post.getCreatedBy() != null && post.getCreatedBy().equals(viewer)) {
      return true;
    }
    if (viewer == null) {
      return false;
    }
    return objectShareRepository.isVisibleToViewer("briareus_post", post.getId(), viewer);
  }

  /**
   * Checks whether the current viewer may edit the post.
   *
   * @param post the post
   * @return true when editable
   */
  private boolean canEdit(PostEntity post) {
    UUID viewer = currentUserId();
    return viewer != null && post.getCreatedBy() != null && post.getCreatedBy().equals(viewer);
  }

  /**
   * Writes owner tuple for a newly created post.
   *
   * @param postId the post id
   */
  private void writeOwnerTuple(UUID postId) {
    UUID viewer = currentUserId();
    if (viewer == null) {
      return;
    }
    try {
      permifyClient.writeTuple(
          PermifyUtil.object("briareus_post", postId), "owner", PermifyUtil.userSubject(viewer));
    } catch (Exception e) {
      logger.warn("Permify write owner failed for {}", postId, e);
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
    if (pagination.getSorts() != null && !pagination.getSorts().isEmpty()) {
      List<Sort.Order> orders = pagination.getSorts().stream()
          .map(s -> new Sort.Order(Sort.Direction.valueOf(s.getDir().name()), s.getField()))
          .toList();
      return PageRequests.of(pagination.getPage(), pagination.getSize(), orders, defaultSortBy,
          defaultDir);
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
