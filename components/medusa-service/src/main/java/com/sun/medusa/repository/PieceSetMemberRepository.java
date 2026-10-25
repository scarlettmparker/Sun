package com.sun.medusa.repository;

import com.sun.medusa.model.PieceSetMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PieceSetMemberRepository extends JpaRepository<PieceSetMemberEntity, com.sun.medusa.model.PieceSetMemberId> {

  @Query("SELECT m FROM PieceSetMemberEntity m WHERE m.id.pieceSetId = :setId")
  List<PieceSetMemberEntity> findBySetId(@Param("setId") UUID setId);
}