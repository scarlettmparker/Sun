package com.sun.cerberus.repository;

import com.sun.cerberus.model.GalleryItemEntity;
import com.sun.base.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GalleryItemRepository extends BaseRepository<GalleryItemEntity> {
  // Domain-specific query methods can be added here

  @Query(value = "SELECT * FROM gallery_items WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(foreignobject) AS elem WHERE elem = ANY(?1))", nativeQuery = true)
  List<GalleryItemEntity> findByForeignObjectsIn(String[] ids);
}