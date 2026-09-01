package com.sun.jocasta.repository;

import com.sun.jocasta.model.QuestionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<QuestionEntity, UUID>, JpaSpecificationExecutor<QuestionEntity> {

  @Query(value = "SELECT * FROM jocasta_questions WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = :target)", nativeQuery = true)
  List<QuestionEntity> findByRemoteObject(@Param("target") String target);

  @Query(value = "SELECT * FROM jocasta_questions WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = :target)", countQuery = "SELECT count(*) FROM jocasta_questions WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(remote_object) AS elem WHERE elem = :target)", nativeQuery = true)
  Page<QuestionEntity> findByRemoteObject(@Param("target") String target, Pageable pageable);
}
