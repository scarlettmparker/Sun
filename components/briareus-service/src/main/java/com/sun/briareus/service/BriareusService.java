package com.sun.briareus.service;

import com.sun.briareus.model.PostEntity;
import com.sun.briareus.repository.PostRepository;
import com.sun.base.service.BaseService;
import com.sun.base.util.FilterBuilder;
import com.sun.base.util.FilterSpec;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BriareusService extends BaseService<PostEntity> {

  private final PostRepository postRepository;

  public BriareusService(PostRepository repository) {
    super(repository);
    this.postRepository = repository;
  }

  /**
   * Retrieves all posts.
   * 
   * @return a list of PostEntity objects
   */
  public List<PostEntity> listPosts() {
    return findAll();
  }

  /**
   * Retrieves a page of posts matching the filters for the viewer.
   *
   * @param filters the filter criteria
   * @param pageable the pagination and sort
   * @param viewer the viewer id
   * @return the matching page
   */
  public Page<PostEntity> listPostsPaged(List<FilterSpec> filters, Pageable pageable, UUID viewer) {
    Specification<PostEntity> vis = BriareusVisibilitySpec.visibleTo(viewer);
    Specification<PostEntity> topLevel = (root, query, cb) -> cb.isNull(root.get("parentId"));
    Specification<PostEntity> visTop = vis.and(topLevel);
    Specification<PostEntity> base = FilterBuilder.buildFilters(filters);
    Specification<PostEntity> spec = base == null ? visTop : base.and(visTop);
    return postRepository.findAll(spec, pageable);
  }

  /**
   * Retrieves a page of posts matching the filters (no viewer, public only).
   *
   * @param filters the filter criteria
   * @param pageable the pagination and sort
   * @return the matching page
   */
  public Page<PostEntity> listPostsPaged(List<FilterSpec> filters, Pageable pageable) {
    return listPostsPaged(filters, pageable, null);
  }

  /**
   * Retrieves a specific post by ID.
   * 
   * @param id the post ID
   * @return an Optional containing the PostEntity if found
   */
  public Optional<PostEntity> locatePost(UUID id) {
    return findById(id);
  }

  /**
   * Retrieves posts that reference any of the given remote-object ids.
   *
   * @param ids the remote-object ids to match
   * @return the matching posts
   */
  public List<PostEntity> listByRemoteObjects(List<String> ids) {
    return listByRemoteObjects(ids, null);
  }

  /**
   * Retrieves visible posts that reference any of the given remote-object ids.
   *
   * @param ids the remote-object ids to match
   * @param viewer the viewer id
   * @return the matching posts
   */
  public List<PostEntity> listByRemoteObjects(List<String> ids, UUID viewer) {
    List<PostEntity> candidates = postRepository.findByRemoteObjectsIn(ids.toArray(new String[0]));
    if (candidates.isEmpty()) {
      return List.of();
    }
    Specification<PostEntity> vis = BriareusVisibilitySpec.visibleTo(viewer);
    Specification<PostEntity> idSpec = (root, query, cb) -> root.get("id").in(candidates.stream().map(PostEntity::getId).toList());
    Specification<PostEntity> spec = idSpec.and(vis);
    return postRepository.findAll(spec);
  }

  /**
   * Lists direct children of a parent post.
   *
   * @param parentId the parent post id
   * @param pageable the pagination and sort
   * @return the matching page
   */
  public Page<PostEntity> children(UUID parentId, Pageable pageable) {
    return children(parentId, pageable, null);
  }

  /**
   * Lists visible direct children of a parent post.
   *
   * @param parentId the parent post id
   * @param pageable the pagination and sort
   * @param viewer the viewer id
   * @return the matching page
   */
  public Page<PostEntity> children(UUID parentId, Pageable pageable, UUID viewer) {
    if (parentId == null) {
      return Page.empty(pageable);
    }
    Specification<PostEntity> parentSpec = (root, query, cb) -> cb.equal(root.get("parentId"), parentId);
    Specification<PostEntity> vis = BriareusVisibilitySpec.visibleTo(viewer);
    return postRepository.findAll(parentSpec.and(vis), pageable);
  }

  /**
   * Lists direct children of a parent post.
   *
   * @param parentId the parent post id
   * @return children
   */
  public List<PostEntity> children(UUID parentId) {
    return children(parentId, (UUID) null);
  }

  /**
   * Lists visible direct children of a parent post.
   *
   * @param parentId the parent post id
   * @param viewer the viewer id
   * @return children
   */
  public List<PostEntity> children(UUID parentId, UUID viewer) {
    if (parentId == null) {
      return List.of();
    }
    Specification<PostEntity> parentSpec = (root, query, cb) -> cb.equal(root.get("parentId"), parentId);
    Specification<PostEntity> vis = BriareusVisibilitySpec.visibleTo(viewer);
    return postRepository.findAll(parentSpec.and(vis));
  }
}