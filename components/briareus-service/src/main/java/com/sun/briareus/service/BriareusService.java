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
   * Retrieves a page of posts matching the filters.
   *
   * @param filters the filter criteria
   * @param pageable the pagination and sort
   * @return the matching page
   */
  public Page<PostEntity> listPostsPaged(List<FilterSpec> filters, Pageable pageable) {
    Specification<PostEntity> spec = FilterBuilder.buildFilters(filters);
    return spec == null ? findAllPaged(pageable) : postRepository.findAll(spec, pageable);
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
    return postRepository.findByRemoteObjectsIn(ids.toArray(new String[0]));
  }

  /**
   * Lists direct children of a parent post.
   *
   * @param parentId the parent post id
   * @param pageable the pagination and sort
   * @return the matching page
   */
  public Page<PostEntity> children(UUID parentId, Pageable pageable) {
    if (parentId == null) {
      return Page.empty(pageable);
    }
    return postRepository.findByParentId(parentId, pageable);
  }

  /**
   * Lists direct children of a parent post.
   *
   * @param parentId the parent post id
   * @return children
   */
  public List<PostEntity> children(UUID parentId) {
    if (parentId == null) {
      return List.of();
    }
    return postRepository.findByParentId(parentId);
  }
}