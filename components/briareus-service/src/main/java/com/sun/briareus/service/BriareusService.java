package com.sun.briareus.service;

import com.sun.briareus.model.PostEntity;
import com.sun.briareus.repository.PostRepository;
import com.sun.base.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

@Service
@Transactional("briareusTransactionManager")
public class BriareusService extends BaseService<PostEntity> {

  public BriareusService(PostRepository repository) {
    super(repository);
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
   * Retrieves a specific post by ID.
   * 
   * @param id the post ID
   * @return an Optional containing the PostEntity if found
   */
  public Optional<PostEntity> locatePost(UUID id) {
    return findById(id);
  }
}