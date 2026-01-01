package com.sun.cerberus.repository;

import com.sun.cerberus.model.GalleryItemEntity;
import com.sun.base.repository.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GalleryItemRepository extends BaseRepository<GalleryItemEntity> {
  // Domain-specific query methods can be added here

  @Query(value = "SELECT * FROM gallery_items WHERE foreign_object ?| :ids", nativeQuery = true)
  List<GalleryItemEntity> findByForeignObjectIn(@Param("ids") String[] ids);
}