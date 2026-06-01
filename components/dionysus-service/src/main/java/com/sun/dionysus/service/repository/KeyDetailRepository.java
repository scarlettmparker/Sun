package com.sun.dionysus.service.repository;

import com.sun.dionysus.graphql.models.KeyDetail;
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
public interface KeyDetailRepository extends BaseRepository<KeyDetail> {
  List<KeyDetail> findByBucketAndKeyPathStartingWithAndStatus(String bucket, String keyPath, Status status);
  Optional<KeyDetail> findByBucketAndKeyPathAndStatus(String bucket, String keyPath, Status status);
  List<KeyDetail> findByBucketAndKeyPathStartingWith(String bucket, String keyPath);
  List<KeyDetail> findByBucketAndKeyPath(String bucket, String keyPath);
}
