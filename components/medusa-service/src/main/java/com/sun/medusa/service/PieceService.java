package com.sun.medusa.service;

import com.sun.medusa.model.PieceDefinitionEntity;
import com.sun.medusa.model.PieceSpriteEntity;
import com.sun.medusa.repository.PieceDefinitionRepository;
import com.sun.medusa.repository.PieceSpriteRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PieceService {

  private final PieceDefinitionRepository definitionRepository;
  private final PieceSpriteRepository spriteRepository;

  public PieceService(PieceDefinitionRepository definitionRepository,
      PieceSpriteRepository spriteRepository) {
    this.definitionRepository = definitionRepository;
    this.spriteRepository = spriteRepository;
  }

  /**
   * Lists all piece definitions.
   *
   * @return the piece definitions
   */
  /**
   * Lists pieces visible to the caller (system, unowned, or owned).
   *
   * @param ownerId the caller id, or null for all
   * @return the pieces
   */
  public List<PieceDefinitionEntity> list(UUID ownerId) {
    if (ownerId == null) {
      return definitionRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }
    List<PieceDefinitionEntity> pieces =
        definitionRepository.findBySystemTrueOrCreatedByIsNullOrCreatedBy(ownerId);
    pieces.sort(Comparator.comparing(PieceDefinitionEntity::getName,
        Comparator.nullsLast(String::compareToIgnoreCase)));
    return pieces;
  }

  /**
   * Finds a piece definition by id.
   *
   * @param id the piece id
   * @return the piece, if found
   */
  public Optional<PieceDefinitionEntity> findById(UUID id) {
    return definitionRepository.findById(id);
  }

  /**
   * Saves a piece definition and its sprite.
   *
   * @param definition the piece definition
   * @param headKey the head sprite key
   * @param bodyKey the body sprite key
   * @param footKey the foot sprite key
   * @return the saved piece
   */
  public PieceDefinitionEntity save(PieceDefinitionEntity definition, String headKey,
      String bodyKey, String footKey) {
    if (definition.getId() != null) {
      PieceDefinitionEntity existing = definitionRepository.findById(definition.getId())
          .orElseThrow(() -> new EntityNotFoundException("Piece not found"));
      if (existing.isSystem()) {
        throw new IllegalStateException("System pieces cannot be edited");
      }
      definition.setCreatedBy(existing.getCreatedBy());
    }
    PieceDefinitionEntity saved = definitionRepository.save(definition);

    PieceSpriteEntity sprite = spriteRepository.findByPieceDefinitionId(saved.getId())
        .orElseGet(PieceSpriteEntity::new);
    sprite.setPieceDefinitionId(saved.getId());
    sprite.setHeadKey(headKey);
    sprite.setBodyKey(bodyKey);
    sprite.setFootKey(footKey);
    spriteRepository.save(sprite);

    return saved;
  }

  /**
   * Deletes a non-system piece.
   *
   * @param id the piece id
   * @return true when deleted
   */
  public boolean delete(UUID id) {
    PieceDefinitionEntity existing = definitionRepository.findById(id).orElse(null);
    if (existing == null || existing.isSystem()) {
      return false;
    }
    definitionRepository.deleteById(id);
    return true;
  }

  /**
   * Finds the sprite for a piece definition.
   *
   * @param definitionId the piece definition id
   * @return the sprite, or null
   */
  public PieceSpriteEntity findSprite(UUID definitionId) {
    return spriteRepository.findByPieceDefinitionId(definitionId).orElse(null);
  }
}