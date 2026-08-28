package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.TextViewEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TextViewRepository extends BaseRepository<TextViewEntity> {

  Optional<TextViewEntity> findByAccountIdAndTextId(UUID accountId, UUID textId);

  Page<TextViewEntity> findByAccountId(UUID accountId, Pageable pageable);

  List<TextViewEntity> findByAccountIdAndTextIdIn(UUID accountId, List<UUID> textIds);

  @Modifying
  @Query(
      value =
          "INSERT INTO hades_text_views (id, account_id, text_id, viewed_at, createdat, lastupdatedat) "
              + "VALUES (gen_random_uuid(), :accountId, :textId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
              + "ON CONFLICT (account_id, text_id) DO UPDATE SET viewed_at = CURRENT_TIMESTAMP, lastupdatedat = CURRENT_TIMESTAMP",
      nativeQuery = true)
  void upsert(@Param("accountId") UUID accountId, @Param("textId") UUID textId);
}
