package com.sun.dionysus.repository;

import com.sun.dionysus.model.KeyDetailEntity;
import com.sun.dionysus.model.enums.Status;
import com.sun.base.repository.BaseRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for KeyDetail entities.
 */
@Repository
public interface KeyDetailEntityRepository extends BaseRepository<KeyDetailEntity> {

  @Query("select k from KeyDetailEntity k where k.bucket = :bucket "
      + "and k.keyPath like :keyPathPrefix% and k.status = :status")
  List<KeyDetailEntity> findByPrefixAndStatus(
      @Param("bucket") String bucket,
      @Param("keyPathPrefix") String keyPathPrefix,
      @Param("status") Status status);

  @Query("select k from KeyDetailEntity k where k.bucket = :bucket "
      + "and k.keyPath = :keyPath and k.status = :status")
  Optional<KeyDetailEntity> findByKey(
      @Param("bucket") String bucket,
      @Param("keyPath") String keyPath,
      @Param("status") Status status);

  @Query("select k from KeyDetailEntity k where k.bucket = :bucket "
      + "and k.keyPath like :keyPathPrefix%")
  List<KeyDetailEntity> findByPrefix(
      @Param("bucket") String bucket,
      @Param("keyPathPrefix") String keyPathPrefix);

  @Query("select k from KeyDetailEntity k where k.bucket = :bucket and k.keyPath = :keyPath")
  List<KeyDetailEntity> findByBucketAndKeyPath(String bucket, String keyPath);

  @Query("select k from KeyDetailEntity k where k.bucket = :bucket "
      + "and k.contentType like :contentTypePrefix% and k.status = :status")
  List<KeyDetailEntity> findByContentTypePrefixAndStatus(
      @Param("bucket") String bucket,
      @Param("contentTypePrefix") String contentTypePrefix,
      @Param("status") Status status);

  @Query("select k from KeyDetailEntity k where k.bucket = :bucket "
      + "and k.keyPath = :keyPath and k.contentType like :contentTypePrefix% "
      + "and k.status = :status")
  Optional<KeyDetailEntity> findByKeyAndContentType(
      @Param("bucket") String bucket,
      @Param("keyPath") String keyPath,
      @Param("contentTypePrefix") String contentTypePrefix,
      @Param("status") Status status);

  @Query("select k from KeyDetailEntity k where k.bucket = :bucket "
      + "and (:keyPath is null or k.keyPath = :keyPath) "
      + "and (:contentTypePrefix is null or k.contentType like :contentTypePrefix%) "
      + "and k.status = :status")
  Page<KeyDetailEntity> search(
      @Param("bucket") String bucket,
      @Param("keyPath") String keyPath,
      @Param("contentTypePrefix") String contentTypePrefix,
      @Param("status") Status status,
      Pageable pageable);
}
