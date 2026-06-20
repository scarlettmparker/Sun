package com.sun.dionysus.service.repository;

import com.sun.dionysus.graphql.models.KeyDetailEntity;
import com.sun.dionysus.graphql.models.Status;
import com.sun.base.repository.BaseRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for KeyDetail entities.
 */
@Repository
public interface KeyDetailEntityRepository extends BaseRepository<KeyDetailEntity> {
  List<KeyDetailEntity> findByBucketAndKeyPathStartingWithAndStatus(String bucket, String keyPath, Status status);
  Optional<KeyDetailEntity> findByBucketAndKeyPathAndStatus(String bucket, String keyPath, Status status);
  List<KeyDetailEntity> findByBucketAndKeyPathStartingWith(String bucket, String keyPath);
  List<KeyDetailEntity> findByBucketAndKeyPath(String bucket, String keyPath);
}
