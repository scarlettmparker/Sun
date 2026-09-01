package com.sun.jocasta.repository;

import com.sun.jocasta.model.AnswerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<AnswerEntity, UUID>, JpaSpecificationExecutor<AnswerEntity> {

  Page<AnswerEntity> findByQuestionId(UUID questionId, Pageable pageable);

  List<AnswerEntity> findByQuestionIdIn(Collection<UUID> questionIds);
}
