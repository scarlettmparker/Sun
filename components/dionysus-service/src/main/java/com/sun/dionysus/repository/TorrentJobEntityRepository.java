package com.sun.dionysus.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.dionysus.model.TorrentJobEntity;
import com.sun.dionysus.model.enums.TorrentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for TorrentJob entities.
 */
@Repository
public interface TorrentJobEntityRepository extends BaseRepository<TorrentJobEntity> {

  List<TorrentJobEntity> findByStatusIn(Collection<TorrentStatus> statuses);

  List<TorrentJobEntity> findByBucketAndStatusIn(String bucket, Collection<TorrentStatus> statuses);

  Optional<TorrentJobEntity> findByInfoHash(String infoHash);

  Optional<TorrentJobEntity> findByScratchPath(String scratchPath);

  List<TorrentJobEntity> findByBucketAndTargetKeyPath(String bucket, String targetKeyPath);

  @Modifying
  @Query("update TorrentJobEntity set status = :status where id = :id")
  void updateStatus(UUID id, TorrentStatus status);
}
