package com.sun.hades.repository;

import com.sun.base.repository.BaseRepository;
import com.sun.hades.model.ReaderTextEntity;
import com.sun.hades.model.enums.ReaderTextStatus;
import com.sun.hades.service.ReaderTextSummary;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReaderTextRepository
    extends BaseRepository<ReaderTextEntity>, JpaSpecificationExecutor<ReaderTextEntity> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from ReaderTextEntity t where t.id = :id")
  Optional<ReaderTextEntity> findByIdForUpdate(@Param("id") UUID id);

  /**
   * Lists active text summaries, including the total count.
   *
   * @param status the status to match
   * @param pageable the page request
   * @return a page of summaries
   */
  @Query("select new com.sun.hades.service.ReaderTextSummary("
      + "t.id, t.title, t.language, t.level, t.ownerId, t.sourceId, t.status, t.createdAt, t.lastUpdatedAt) "
      + "from ReaderTextEntity t where t.status = :status")
  Page<ReaderTextSummary> findSummaries(@Param("status") ReaderTextStatus status, Pageable pageable);

  /**
   * Lists active text summaries without a count query.
   *
   * @param status the status to match
   * @param pageable the page request
   * @return a slice of summaries
   */
  @Query("select new com.sun.hades.service.ReaderTextSummary("
      + "t.id, t.title, t.language, t.level, t.ownerId, t.sourceId, t.status, t.createdAt, t.lastUpdatedAt) "
      + "from ReaderTextEntity t where t.status = :status")
  Slice<ReaderTextSummary> findSummariesSlice(
      @Param("status") ReaderTextStatus status, Pageable pageable);

  /**
   * Reads the maintained active-text counter.
   *
   * @param status the status to read
   * @return the count, or empty when no counter row exists
   */
  @Query(value = "select total from hades_reader_text_counts where status = :status",
      nativeQuery = true)
  Optional<Long> findCount(@Param("status") String status);
}
