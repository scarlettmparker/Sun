package com.sun.base.audit.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.base.audit.enums.AuditOutcome;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuditContextTest {

  private final AuditContext context = new AuditContext();

  @AfterEach
  void tearDown() {
    context.clear();
  }

  @Test
  void addOperation_collectsOperationsForCurrentThread() {
    context.begin();
    AuditContext.AuditOperation op = new AuditContext.AuditOperation(
        "createAnnotation", OperationMetadata.unknown(null, null), null, null, AuditOutcome.SUCCESS, null);

    context.addOperation(op);

    assertThat(context.operations()).containsExactly(op);
  }

  @Test
  void clear_dropsStateForCurrentThread() {
    context.begin();
    UUID userId = UUID.randomUUID();
    context.setUserId(userId);

    context.clear();

    // A fresh state is created lazily on read, so the cleared id is gone.
    assertThat(context.getUserId()).isNull();
  }

  @Test
  void setUserId_roundTripsTheValue() {
    context.begin();
    UUID userId = UUID.randomUUID();

    context.setUserId(userId);

    assertThat(context.getUserId()).isEqualTo(userId);
  }

  @Test
  void operations_createsStateDefensivelyWhenBeginSkipped() {
    assertThat(context.operations()).isEmpty();
  }
}
