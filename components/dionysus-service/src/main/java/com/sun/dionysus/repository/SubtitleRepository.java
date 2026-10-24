package com.sun.dionysus.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.dionysus.model.SubtitleEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for subtitle objects.
 */
@Repository
public interface SubtitleRepository extends BaseRepository<SubtitleEntity> {

  List<SubtitleEntity> findByBucketAndKeyPath(String bucket, String keyPath);

  Optional<SubtitleEntity> findByBucketAndSubtitleKey(String bucket, String subtitleKey);

  @Query("select s from SubtitleEntity s where s.bucket = :bucket "
      + "and s.keyPath like :keyPathPrefix%")
  List<SubtitleEntity> findByPrefix(
      @Param("bucket") String bucket,
      @Param("keyPathPrefix") String keyPathPrefix);

  @Modifying
  @Query("delete from SubtitleEntity s where s.bucket = :bucket and s.subtitleKey = :subtitleKey")
  int deleteBySubtitleKey(
      @Param("bucket") String bucket,
      @Param("subtitleKey") String subtitleKey);

  @Modifying
  @Query("delete from SubtitleEntity s where s.bucket = :bucket and s.keyPath = :keyPath")
  int deleteForVideo(
      @Param("bucket") String bucket,
      @Param("keyPath") String keyPath);

  @Modifying
  @Query("delete from SubtitleEntity s where s.bucket = :bucket "
      + "and s.keyPath like :keyPathPrefix%")
  int deleteUnderPrefix(
      @Param("bucket") String bucket,
      @Param("keyPathPrefix") String keyPathPrefix);
}
