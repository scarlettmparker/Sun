package com.sun.briareus.service;

import com.sun.briareus.model.PostEntity;
import com.sun.base.service.BaseService;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

@Service
public class BriareusService extends BaseService<PostEntity> {

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