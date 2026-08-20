package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderAccountEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReaderAccountRepository extends BaseRepository<ReaderAccountEntity> {

  Optional<ReaderAccountEntity> findByDiscordId(String discordId);

  List<ReaderAccountEntity> findByDiscordIdIn(Collection<String> discordIds);

  Optional<ReaderAccountEntity> findByGaiaAccountId(UUID gaiaAccountId);

  List<ReaderAccountEntity> findByGaiaAccountIdIn(Collection<UUID> gaiaAccountIds);

  /**
   * Searches by username or display name.
   */
  @Query(
      "SELECT r FROM ReaderAccountEntity r WHERE "
          + "LOWER(r.discordUsername) LIKE LOWER(CONCAT('%', :query, '%')) OR "
          + "LOWER(r.globalName) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<ReaderAccountEntity> searchByUsername(@Param("query") String query, Pageable pageable);
}
