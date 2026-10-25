package com.sun.medusa.service;

import com.sun.medusa.model.PieceSetEntity;
import com.sun.medusa.model.PieceSetMemberEntity;
import com.sun.medusa.model.PieceSetMemberId;
import com.sun.medusa.repository.PieceSetMemberRepository;
import com.sun.medusa.repository.PieceSetRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PieceSetService {

  private final PieceSetRepository setRepository;
  private final PieceSetMemberRepository memberRepository;

  public PieceSetService(PieceSetRepository setRepository,
      PieceSetMemberRepository memberRepository) {
    this.setRepository = setRepository;
    this.memberRepository = memberRepository;
  }

  /**
   * Lists all piece sets.
   *
   * @return the piece sets
   */
  /**
   * Lists sets visible to the caller (system or owned).
   *
   * @param ownerId the caller id, or null for all
   * @return the sets
   */
  public List<PieceSetEntity> list(UUID ownerId) {
    if (ownerId == null) {
      return setRepository.findAll();
    }
    return setRepository.findBySystemTrueOrCreatedByIsNullOrCreatedBy(ownerId);
  }

  /**
   * Finds a piece set by id.
   *
   * @param id the set id
   * @return the set, if found
   */
  public Optional<PieceSetEntity> findById(UUID id) {
    return setRepository.findById(id);
  }

  /**
   * Saves a piece set and its members.
   *
   * @param set the piece set
   * @param members the set members
   * @return the saved set
   */
  public PieceSetEntity save(PieceSetEntity set, List<PieceSetMemberEntity> members) {
    if (set.getId() != null) {
      PieceSetEntity existing = setRepository.findById(set.getId()).orElse(null);
      if (existing != null && existing.isSystem()) {
        throw new IllegalStateException("System sets cannot be edited");
      }
      if (existing != null) {
        set.setCreatedBy(existing.getCreatedBy());
      }
    }
    PieceSetEntity saved = setRepository.save(set);
    memberRepository.deleteAll(memberRepository.findBySetId(saved.getId()));
    for (PieceSetMemberEntity member : members) {
      String slot = member.getId() == null ? null : member.getId().getSlot();
      UUID defId = member.getPieceDefinitionId();
      PieceSetMemberEntity toSave = new PieceSetMemberEntity(saved.getId(), slot, defId);
      memberRepository.save(toSave);
    }
    return saved;
  }

  /**
   * Deletes a non-system piece set.
   *
   * @param id the set id
   * @return true when deleted
   */
  public boolean delete(UUID id) {
    PieceSetEntity existing = setRepository.findById(id).orElse(null);
    if (existing == null || existing.isSystem()) {
      return false;
    }
    memberRepository.deleteAll(memberRepository.findBySetId(id));
    setRepository.deleteById(id);
    return true;
  }

  /**
   * Finds the members of a set.
   *
   * @param setId the set id
   * @return the members
   */
  public List<PieceSetMemberEntity> findMembers(UUID setId) {
    return memberRepository.findBySetId(setId);
  }
}