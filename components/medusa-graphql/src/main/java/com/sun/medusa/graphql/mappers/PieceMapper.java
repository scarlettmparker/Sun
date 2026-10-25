package com.sun.medusa.graphql.mappers;

import com.sun.medusa.codegen.types.Piece;
import com.sun.medusa.codegen.types.PieceInput;
import com.sun.medusa.codegen.types.PieceSprite;
import com.sun.medusa.codegen.types.PieceSpriteInput;
import com.sun.medusa.model.PieceDefinitionEntity;
import com.sun.medusa.model.PieceSpriteEntity;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PieceMapper {

  public Piece map(PieceDefinitionEntity entity, PieceSpriteEntity sprite) {
    Piece.Builder builder = Piece.newBuilder()
        .id(entity.getId().toString())
        .slug(entity.getSlug())
        .name(entity.getName())
        .description(entity.getDescription())
        .isKing(entity.isKing())
        .promotionTargets(entity.getPromotionTargets())
        .promotionRanks(entity.getPromotionRanks())
        .moveStates(entity.getMoveStates())
        .stateTransition(entity.getStateTransition())
        .edgeReset(entity.isEdgeReset())
        .isSystem(entity.isSystem())
        .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC))
        .updatedAt(entity.getLastUpdatedAt() == null ? null : entity.getLastUpdatedAt().atOffset(ZoneOffset.UTC));
    if (sprite != null) {
      builder.sprite(PieceSprite.newBuilder()
          .headKey(sprite.getHeadKey())
          .bodyKey(sprite.getBodyKey())
          .footKey(sprite.getFootKey())
          .build());
    }
    return builder.build();
  }

  public PieceDefinitionEntity mapInput(PieceInput input) {
    PieceDefinitionEntity entity = new PieceDefinitionEntity();
    if (input.getId() != null && !input.getId().isBlank()) {
      entity.setId(UUID.fromString(input.getId()));
    }
    entity.setSlug(input.getSlug());
    entity.setName(input.getName());
    entity.setDescription(input.getDescription());
    entity.setKing(input.getIsKing());
    entity.setPromotionTargets(input.getPromotionTargets());
    entity.setPromotionRanks(input.getPromotionRanks());
    entity.setMoveStates((List<Object>) input.getMoveStates());
    entity.setStateTransition(input.getStateTransition());
    entity.setEdgeReset(input.getEdgeReset());
    entity.setSystem(false);
    return entity;
  }

  public PieceSpriteInput spriteInput(PieceInput input) {
    return input.getSprite();
  }
}
