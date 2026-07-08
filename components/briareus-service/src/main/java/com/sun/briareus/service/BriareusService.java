package com.sun.briareus.service;

import com.sun.briareus.model.PostEntity;
import com.sun.briareus.repository.PostRepository;
import com.sun.base.service.BaseService;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

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
   * Retrieves all posts with a Pageable input.
   * 
   * @param pageable Pageable.
   * @return a list of Paged PostEntity objects.
   */
  public Page<PostEntity> listPostsPaged(Pageable pageable) {
    return findAllPaged(pageable);
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
}