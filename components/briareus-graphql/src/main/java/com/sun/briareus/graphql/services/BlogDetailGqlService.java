package com.sun.briareus.graphql.services;

import com.sun.briareus.codegen.types.AttachedText;
import com.sun.briareus.codegen.types.BlogDetail;
import com.sun.briareus.codegen.types.BlogGalleryItem;
import com.sun.briareus.codegen.types.BlogPost;
import com.sun.briareus.codegen.types.BlogPostInput;
import com.sun.briareus.codegen.types.BlogPropertySet;
import com.sun.briareus.codegen.types.BlogWithPropertiesInput;
import com.sun.briareus.codegen.types.QueryResult;
import com.sun.briareus.codegen.types.QuerySuccess;
import com.sun.briareus.codegen.types.StandardError;
import com.sun.briareus.graphql.mappers.AttachedTextMapper;
import com.sun.briareus.graphql.mappers.BlogDetailMapper;
import com.sun.briareus.graphql.mappers.BlogGalleryItemMapper;
import com.sun.briareus.graphql.mappers.BlogPostMapper;
import com.sun.briareus.graphql.mappers.BlogPropertySetMapper;
import com.sun.briareus.model.BlogPostTypeEntity;
import com.sun.briareus.model.PostEntity;
import com.sun.briareus.repository.PostRepository;
import com.sun.briareus.service.BlogPostTypeService;
import com.sun.briareus.service.BriareusService;
import com.sun.base.permify.PermifyClient;
import com.sun.base.permify.PermifyUtil;
import com.sun.cerberus.model.GalleryItemEntity;
import com.sun.cerberus.repository.GalleryItemRepository;
import com.sun.cerberus.service.CerberusService;
import com.sun.gaia.model.PropertySetEntryEntity;
import com.sun.gaia.model.PropertySetSchemaEntity;
import com.sun.gaia.repository.ObjectShareRepository;
import com.sun.gaia.service.PropertySetService;
import com.sun.gaia.service.UserContextHolder;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.repository.ReaderTextRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates a blog post with property set and gallery.
 */
@Service
public class BlogDetailGqlService {

  private static final Logger logger = LoggerFactory.getLogger(BlogDetailGqlService.class);

  private static final String OWNER_BLOG = "Blog";

  @Autowired
  private BriareusService briareusService;

  @Autowired
  private BlogPostTypeService blogPostTypeService;

  @Autowired
  private BlogPostMapper blogPostMapper;

  @Autowired
  private BlogDetailMapper blogDetailMapper;

  @Autowired
  private BlogGalleryItemMapper blogGalleryItemMapper;

  @Autowired
  private BlogPropertySetMapper blogPropertySetMapper;

  @Autowired
  private AttachedTextMapper attachedTextMapper;

  @Autowired
  private PropertySetService propertySetService;

  @Autowired
  private GalleryItemRepository galleryItemRepository;

  @Autowired
  private CerberusService cerberusService;

  @Autowired
  private ReaderTextRepository readerTextRepository;

  @Autowired
  private PostRepository postRepository;

  @Autowired
  private PermifyClient permifyClient;

  @Autowired
  private ObjectShareRepository objectShareRepository;

  /**
   * Returns aggregated detail for a post.
   *
   * @param id the post id
   * @return the detail
   */
  @Transactional(readOnly = true)
  public BlogDetail blogDetail(String id) {
    UUID postId = UUID.fromString(id);
    PostEntity post = briareusService.locatePost(postId)
        .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
    if (!canView(post)) {
      throw new RuntimeException("Not authorized to view blog post: " + id);
    }
    BlogPost mappedPost = blogPostMapper.map(post);
    BlogPropertySet propertySet = resolvePropertySet(post);
    List<BlogGalleryItem> galleryItems = resolveGallery(post);
    List<AttachedText> attachedTexts = resolveAttachedTexts(post);
    List<BlogPost> linkedPosts = resolveLinkedPosts(post, propertySet);
    return blogDetailMapper.map(mappedPost, propertySet, galleryItems, attachedTexts, linkedPosts);
  }

  /**
   * Creates a blog post with property set values in one transaction.
   *
   * @param input the input
   * @return the result
   */
  @Transactional
  public QueryResult createBlogWithProperties(BlogWithPropertiesInput input) {
    try {
      if (input.getTitle() == null || input.getTitle().isBlank()) {
        throw new IllegalArgumentException("Title is required");
      }
      if (input.getTypeId() == null || input.getTypeId().isBlank()) {
        throw new IllegalArgumentException("Type is required");
      }
      BlogPostTypeEntity type = blogPostTypeService.findById(UUID.fromString(input.getTypeId()))
          .orElseThrow(() -> new IllegalArgumentException("Type not found: " + input.getTypeId()));
      if (input.getParentId() != null && !input.getParentId().isBlank()) {
        validateParent(input.getParentId());
      }
      BlogPostInput postInput = blogPostMapper.toPostInput(input);
      PostEntity postEntity = blogPostMapper.mapInput(input.getTitle(), postInput);
      postEntity.setType(type);
      if (input.getParentId() != null && !input.getParentId().isBlank()) {
        postEntity.setParentId(UUID.fromString(input.getParentId()));
      }
      PostEntity saved = briareusService.save(postEntity);
      writeOwnerTuple(saved.getId());
      if (input.getPropertySet() != null) {
        Map<String, Object> values = toMap(input.getPropertySet());
        if (values != null && !values.isEmpty()) {
          String schemaName = schemaNameFor(type);
          propertySetService.upsertEntry(OWNER_BLOG, schemaName, "briareus:post:" + saved.getId(), values, false);
          handleGalleryForPropertySet(saved.getId(), values);
        }
      }
      handleRemoteObjectGalleryLinks(saved);
      logger.info("createBlogWithProperties succeeded {}", saved.getId());
      return QuerySuccess.newBuilder().message("Created").id(saved.getId().toString()).build();
    } catch (Exception e) {
      logger.error("createBlogWithProperties failed", e);
      return StandardError.newBuilder().message(e.getMessage()).build();
    }
  }

  /**
   * Updates a blog post and its property set.
   *
   * @param id the post id
   * @param input the input
   * @return the result
   */
  @Transactional
  public QueryResult updateBlogWithProperties(String id, BlogWithPropertiesInput input) {
    try {
      UUID postId = UUID.fromString(id);
      PostEntity post = briareusService.locatePost(postId)
          .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
      if (!canEdit(post)) {
        throw new IllegalArgumentException("Not authorized to edit post: " + id);
      }
      if (input.getTypeId() != null && !input.getTypeId().isBlank()) {
        UUID newTypeId = UUID.fromString(input.getTypeId());
        if (post.getType() != null && !post.getType().getId().equals(newTypeId)) {
          throw new IllegalArgumentException("type immutable");
        }
      }
      if (input.getTitle() != null && !input.getTitle().isBlank()) {
        post.setTitle(input.getTitle());
      }
      BlogPostInput postInput = blogPostMapper.toPostInput(input);
      blogPostMapper.update(post, postInput);
      if (input.getParentId() != null && !input.getParentId().isBlank()) {
        validateParent(input.getParentId());
        post.setParentId(UUID.fromString(input.getParentId()));
      }
      PostEntity saved = briareusService.save(post);
      if (input.getPropertySet() != null) {
        Map<String, Object> values = toMap(input.getPropertySet());
        if (values != null) {
          String schemaName = schemaNameFor(saved.getType());
          propertySetService.upsertEntry(OWNER_BLOG, schemaName, "briareus:post:" + saved.getId(), values, false);
          handleGalleryForPropertySet(saved.getId(), values);
        }
      }
      logger.info("updateBlogWithProperties succeeded {}", saved.getId());
      return QuerySuccess.newBuilder().message("Updated").id(saved.getId().toString()).build();
    } catch (Exception e) {
      logger.error("updateBlogWithProperties failed", e);
      return StandardError.newBuilder().message(e.getMessage()).build();
    }
  }

  /**
   * Resolves the property set for a post.
   *
   * @param post the post
   * @return the property set, or null when no schema exists
   */
  private BlogPropertySet resolvePropertySet(PostEntity post) {
    if (post.getType() == null) {
      return null;
    }
    String schemaName = schemaNameFor(post.getType());
    String entryName = "briareus:post:" + post.getId();
    Optional<PropertySetSchemaEntity> schemaOpt = propertySetService.getSchemaEntity(OWNER_BLOG, schemaName);
    if (schemaOpt.isEmpty()) {
      return null;
    }
    PropertySetSchemaEntity schema = schemaOpt.get();
    Optional<PropertySetEntryEntity> entryOpt = propertySetService.getEntry(OWNER_BLOG, schemaName, entryName);
    PropertySetEntryEntity entry = entryOpt.orElse(null);
    return blogPropertySetMapper.map(schema, entry, entryName);
  }

  /**
   * Resolves gallery items for a post, merging both directions.
   *
   * @param post the post
   * @return the gallery items
   */
  private List<BlogGalleryItem> resolveGallery(PostEntity post) {
    List<GalleryItemEntity> byPost = galleryItemRepository.findByRemoteObjectsIn(new String[]{"briareus:post:" + post.getId()});
    List<String> inverseIds = post.getRemoteObject() == null ? List.of()
        : post.getRemoteObject().stream().filter(v -> v.startsWith("cerberus:gallery:")).toList();
    List<GalleryItemEntity> inverse = inverseIds.isEmpty() ? List.of()
        : galleryItemRepository.findByRemoteObjectsIn(inverseIds.toArray(new String[0]));
    List<GalleryItemEntity> merged = new ArrayList<>();
    merged.addAll(byPost);
    for (GalleryItemEntity e : inverse) {
      if (merged.stream().noneMatch(m -> m.getId().equals(e.getId()))) {
        merged.add(e);
      }
    }
    return merged.stream().map(blogGalleryItemMapper::map).toList();
  }

  /**
   * Returns attached texts for a post, ordered as in remoteObject.
   *
   * @param post the post
   * @return the texts
   */
  private List<AttachedText> resolveAttachedTexts(PostEntity post) {
    if (post.getRemoteObject() == null) {
      return List.of();
    }
    List<String> rawIds = post.getRemoteObject().stream()
        .filter(v -> v.startsWith("hades:text:"))
        .map(v -> v.replace("hades:text:", "").toLowerCase().trim())
        .toList();
    List<UUID> uuids = rawIds.stream().map(v -> {
      try {
        return UUID.fromString(v);
      } catch (Exception e) {
        return null;
      }
    }).filter(Objects::nonNull).toList();
    if (uuids.isEmpty()) {
      return List.of();
    }
    List<ReaderTextEntity> entities = readerTextRepository.findAllById(uuids);
    Map<UUID, ReaderTextEntity> byId = entities.stream()
        .collect(Collectors.toMap(ReaderTextEntity::getId, Function.identity()));
    return uuids.stream().map(byId::get).filter(Objects::nonNull)
        .filter(e -> e.getStatus().name().equals("ACTIVE"))
        .map(attachedTextMapper::map)
        .toList();
  }

  /**
   * Resolves linked posts from property set and remoteObject.
   *
   * @param post the post
   * @param propertySet the property set
   * @return the linked posts
   */
  private List<BlogPost> resolveLinkedPosts(PostEntity post, BlogPropertySet propertySet) {
    List<String> ids = new ArrayList<>();
    if (propertySet != null && propertySet.getValues() instanceof Map<?, ?> map) {
      Object linked = map.get("linkedPostIds");
      if (linked instanceof List<?> list) {
        for (Object o : list) {
          if (o instanceof String s) ids.add(s);
        }
      }
    }
    if (post.getRemoteObject() != null) {
      for (String v : post.getRemoteObject()) {
        if (v.startsWith("briareus:post:")) {
          ids.add(v.replace("briareus:post:", "").trim());
        }
      }
    }
    List<UUID> uuids = ids.stream().map(s -> {
      try {
        return UUID.fromString(s);
      } catch (Exception e) {
        return null;
      }
    }).filter(Objects::nonNull).distinct().toList();
    if (uuids.isEmpty()) {
      return List.of();
    }
    List<String> remoteIds = uuids.stream().map(id -> "briareus:post:" + id).toList();
    List<PostEntity> visible = briareusService.listByRemoteObjects(remoteIds, currentUserId());
    Map<UUID, PostEntity> byId = visible.stream()
        .collect(Collectors.toMap(PostEntity::getId, Function.identity()));
    return uuids.stream().map(byId::get).filter(Objects::nonNull)
        .map(blogPostMapper::map)
        .toList();
  }

  /**
   * Returns the schema name for a type.
   *
   * @param type the type
   * @return the schema name
   */
  private String schemaNameFor(BlogPostTypeEntity type) {
    if (type == null) {
      return "review-attributes";
    }
    if ("REVIEW".equals(type.getName())) {
      return "review-attributes";
    }
    return type.getName().toLowerCase() + "-attributes";
  }

  /**
   * Converts property set object to map.
   *
   * @param propertySet the property set object
   * @return the map, or null
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> toMap(Object propertySet) {
    if (propertySet == null) {
      return null;
    }
    if (propertySet instanceof Map) {
      return (Map<String, Object>) propertySet;
    }
    return null;
  }

  /**
   * Links gallery items for a cover image key.
   *
   * @param postId the post id
   * @param values the property values
   */
  private void handleGalleryForPropertySet(UUID postId, Map<String, Object> values) {
    Object cover = values.get("coverImageKey");
    if (cover instanceof String key && !key.isBlank()) {
      List<GalleryItemEntity> existing = galleryItemRepository.findByRemoteObjectsIn(new String[]{"briareus:post:" + postId});
      boolean alreadyLinked = existing.stream().anyMatch(e -> key.equals(e.getImagePath()));
      if (!alreadyLinked) {
        List<GalleryItemEntity> byKey = galleryItemRepository.findByImagePath(key);
        if (!byKey.isEmpty()) {
          for (GalleryItemEntity g : byKey) {
            List<String> ro = g.getRemoteObject() == null ? new ArrayList<>() : new ArrayList<>(g.getRemoteObject());
            String target = "briareus:post:" + postId;
            if (!ro.contains(target)) {
              ro.add(target);
              g.setRemoteObject(ro);
              cerberusService.save(g);
            }
          }
        } else {
          GalleryItemEntity created = new GalleryItemEntity();
          created.setTitle("cover-" + postId);
          created.setImagePath(key);
          created.setRemoteObject(List.of("briareus:post:" + postId));
          cerberusService.save(created);
        }
      }
    }
  }

  /**
   * Ensures remote-object gallery links are bidirectional.
   *
   * @param post the post
   */
  private void handleRemoteObjectGalleryLinks(PostEntity post) {
    if (post.getRemoteObject() == null) {
      return;
    }
    List<String> galleryTargets = post.getRemoteObject().stream()
        .filter(v -> v.startsWith("cerberus:gallery:")).toList();
    if (galleryTargets.isEmpty()) {
      return;
    }
    List<GalleryItemEntity> items = galleryItemRepository.findByRemoteObjectsIn(galleryTargets.toArray(new String[0]));
    for (GalleryItemEntity g : items) {
      List<String> ro = g.getRemoteObject() == null ? new ArrayList<>() : new ArrayList<>(g.getRemoteObject());
      String target = "briareus:post:" + post.getId();
      if (!ro.contains(target)) {
        ro.add(target);
        g.setRemoteObject(ro);
        cerberusService.save(g);
      }
    }
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
      permifyClient.writeTuple(PermifyUtil.object("briareus_post", postId), "owner", PermifyUtil.userSubject(viewer));
    } catch (Exception e) {
      logger.warn("Permify write owner failed for {}", postId, e);
    }
  }

  /**
   * Validates parent existence and visibility, returning its UUID or null.
   *
   * @param parentIdStr the parent id string
   * @return the parent id, or null when blank
   */
  private void validateParent(String parentIdStr) {
    if (parentIdStr == null || parentIdStr.isBlank()) {
      return;
    }
    UUID parentId = UUID.fromString(parentIdStr);
    PostEntity parent = briareusService.locatePost(parentId).orElse(null);
    if (parent == null) {
      throw new IllegalArgumentException("Parent post not found: " + parentIdStr);
    }
    if (!canView(parent)) {
      throw new IllegalArgumentException("Not authorized to create child under parent: " + parentIdStr);
    }
  }
}
