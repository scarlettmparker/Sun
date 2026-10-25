package com.sun.medusa.service;

import com.sun.medusa.model.AuditLogEntity;
import com.sun.medusa.repository.AuditLogRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditService {

  private final AuditLogRepository repository;

  public AuditService(AuditLogRepository repository) {
    this.repository = repository;
  }

  /**
   * Records an audit entry.
   *
   * @param actorId the actor id
   * @param actorEmail the actor email
   * @param action the action name
   * @param target the target
   * @param outcome the outcome
   * @param detail the detail
   * @param ip the client ip
   * @return the saved entry
   */
  public AuditLogEntity record(UUID actorId, String actorEmail, String action, String target,
      String outcome, String detail, String ip) {
    AuditLogEntity entity = new AuditLogEntity();
    entity.setActorId(actorId);
    entity.setActorEmail(actorEmail);
    entity.setAction(action);
    entity.setTarget(target);
    entity.setOutcome(outcome);
    entity.setDetail(detail);
    entity.setIp(ip);
    return repository.save(entity);
  }

  /**
   * Lists audit entries newest first.
   *
   * @param page the page number
   * @param size the page size
   * @return a page of entries
   */
  public Page<AuditLogEntity> list(int page, int size) {
    return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size, Sort.by("createdAt").descending()));
  }
}