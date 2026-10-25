package com.sun.medusa.model;

import com.sun.base.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "medusa_audit_log")
public class AuditLogEntity extends BaseEntity {

  @Column(name = "actor_id")
  private UUID actorId;

  @Column(name = "actor_email")
  private String actorEmail;

  @Column(name = "action", nullable = false)
  private String action;

  @Column(name = "target")
  private String target;

  @Column(name = "outcome", nullable = false)
  private String outcome;

  @Column(name = "detail", columnDefinition = "TEXT")
  private String detail;

  @Column(name = "ip")
  private String ip;

  public UUID getActorId() {
    return actorId;
  }

  public void setActorId(UUID actorId) {
    this.actorId = actorId;
  }

  public String getActorEmail() {
    return actorEmail;
  }

  public void setActorEmail(String actorEmail) {
    this.actorEmail = actorEmail;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }
}