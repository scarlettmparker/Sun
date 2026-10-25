package com.sun.medusa.graphql.mappers;

import com.sun.medusa.codegen.types.PieceSet;
import com.sun.medusa.codegen.types.PieceSetInput;
import com.sun.medusa.codegen.types.PieceSetMember;
import com.sun.medusa.codegen.types.PieceSetMemberInput;
import com.sun.medusa.model.PieceSetEntity;
import com.sun.medusa.model.PieceSetMemberEntity;
import com.sun.medusa.service.PieceSetService;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PieceSetMapper {

  public PieceSet map(PieceSetEntity entity, PieceSetService service) {
    List<PieceSetMember> members = service.findMembers(entity.getId()).stream()
        .map(m -> PieceSetMember.newBuilder()
            .slot(m.getId().getSlot())
            .pieceDefinitionId(m.getPieceDefinitionId().toString())
            .build())
        .collect(Collectors.toList());
    return PieceSet.newBuilder()
        .id(entity.getId().toString())
        .name(entity.getName())
        .description(entity.getDescription())
        .startingFen(entity.getStartingFen())
        .isSystem(entity.isSystem())
        .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC))
        .updatedAt(entity.getLastUpdatedAt() == null ? null : entity.getLastUpdatedAt().atOffset(ZoneOffset.UTC))
        .members(members)
        .build();
  }

  public PieceSetEntity mapInput(PieceSetInput input) {
    PieceSetEntity entity = new PieceSetEntity();
    if (input.getId() != null && !input.getId().isBlank()) {
      entity.setId(UUID.fromString(input.getId()));
    }
    entity.setName(input.getName());
    entity.setDescription(input.getDescription());
    entity.setStartingFen(input.getStartingFen());
    entity.setSystem(false);
    return entity;
  }

  public java.util.List<PieceSetMemberEntity> mapMembers(PieceSetInput input, UUID setId) {
    if (input.getMembers() == null) {
      return java.util.List.of();
    }
    return input.getMembers().stream()
        .map(m -> new PieceSetMemberEntity(setId, m.getSlot(), UUID.fromString(m.getPieceDefinitionId())))
        .collect(Collectors.toList());
  }
}
