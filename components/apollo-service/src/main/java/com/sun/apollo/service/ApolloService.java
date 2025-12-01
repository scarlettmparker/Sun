package com.sun.apollo.service;

import com.sun.apollo.model.SongEntity;
import com.sun.base.service.BaseService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApolloService extends BaseService<SongEntity> {

  /**
   * Retrieves all songs.
   *
   * @return a list of SongEntity objects
   */
  public List<SongEntity> listSongs() {
    return findAll();
  }

  /**
   * Retrieves a specific song by ID.
   *
   * @param id the song ID
   * @return an Optional containing the SongEntity if found
   */
  public Optional<SongEntity> locateSong(UUID id) {
    return findById(id);
  }

  // Domain-specific logic for Stem Player can be added here
}