package com.sun.medusa.graphql.mappers;

import com.sun.medusa.codegen.types.AuditLog;
import com.sun.medusa.model.AuditLogEntity;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

  public AuditLog map(AuditLogEntity entity) {
    return AuditLog.newBuilder()
        .id(entity.getId().toString())
        .createdAt(entity.getCreatedAt() == null ? null : entity.getCreatedAt().atOffset(ZoneOffset.UTC))
        .actorId(entity.getActorId() == null ? null : entity.getActorId().toString())
        .actorEmail(entity.getActorEmail())
        .action(entity.getAction())
        .target(entity.getTarget())
        .outcome(entity.getOutcome())
        .detail(entity.getDetail())
        .ip(entity.getIp())
        .build();
  }
}