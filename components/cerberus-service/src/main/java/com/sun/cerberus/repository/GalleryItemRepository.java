package com.sun.cerberus.repository;

import com.sun.cerberus.model.GalleryItemEntity;
import com.sun.base.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GalleryItemRepository extends BaseRepository<GalleryItemEntity> {
  // Domain-specific query methods can be added here

  @Query(value = "SELECT * FROM cerberus_gallery_items WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = ANY(?1))", nativeQuery = true)
  List<GalleryItemEntity> findByRemoteObjectsIn(String[] ids);
}