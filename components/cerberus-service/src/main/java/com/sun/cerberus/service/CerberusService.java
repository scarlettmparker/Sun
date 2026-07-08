package com.sun.cerberus.service;

import com.sun.cerberus.model.GalleryItemEntity;
import com.sun.cerberus.repository.GalleryItemRepository;
import com.sun.base.service.BaseService;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

@Service
@Transactional("cerberusTransactionManager")
public class CerberusService extends BaseService<GalleryItemEntity> {

  private final GalleryItemRepository galleryItemRepository;

  public CerberusService(GalleryItemRepository repository) {
    super(repository);
    this.galleryItemRepository = repository;
  }

  /**
   * Retrieves all gallery items.
   *
   * @return a list of GalleryItemEntity objects
   */
  public List<GalleryItemEntity> list() {
    return findAll();
  }

  /**
   * Retrieves all gallery items with a Pageable input.
   * 
   * @param pageable Pageable.
   * @return a list of Paged GalleryItemEntity objects.
   */
  public Page<GalleryItemEntity> listPaged(Pageable pageable) {
    return findAllPaged(pageable);
  }

  /**
   * Retrieves a specific gallery item by ID.
   *
   * @param id the gallery item ID
   * @return an Optional containing the GalleryItemEntity if found
   */
  public Optional<GalleryItemEntity> locate(UUID id) {
    return findById(id);
  }

  /**
   * Retrieves gallery items that have any of the specified foreign object IDs.
   *
   * @param ids the list of foreign object IDs to search for
   * @return a list of GalleryItemEntity objects
   */
  public List<GalleryItemEntity> listByRemoteObjects(List<String> ids) {
    return galleryItemRepository.findByRemoteObjectsIn(ids.toArray(new String[0]));
  }

  /**
   * Retrieves gallery items by their IDs (batch lookup).
   *
   * @param ids the gallery item IDs
   * @return a list of GalleryItemEntity objects
   */
  public List<GalleryItemEntity> locateByIds(List<UUID> ids) {
    return galleryItemRepository.findAllById(ids);
  }
}