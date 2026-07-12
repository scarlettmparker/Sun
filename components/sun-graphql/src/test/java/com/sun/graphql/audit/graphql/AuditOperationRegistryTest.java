package com.sun.graphql.audit.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.base.audit.enums.OperationType;
import org.junit.jupiter.api.Test;

class AuditOperationRegistryTest {

  private final AuditOperationRegistry registry = new AuditOperationRegistry();

  @Test
  void forParentType_deriviesNamespaceAndTypeForMutations() {
    var meta = registry.forParentType("HadesMutations", "createAnnotation");

    assertThat(meta.namespace()).isEqualTo("HADES");
    assertThat(meta.operationType()).isEqualTo(OperationType.MUTATION);
    assertThat(meta.eventType()).isEqualTo("HADES_CREATEANNOTATION");
  }

  @Test
  void forParentType_deriviesNamespaceAndTypeForQueries() {
    var meta = registry.forParentType("GaiaQueries", "me");

    assertThat(meta.namespace()).isEqualTo("GAIA");
    assertThat(meta.operationType()).isEqualTo(OperationType.QUERY);
    assertThat(meta.eventType()).isEqualTo("GAIA_ME");
  }

  @Test
  void forParentType_handlesCompoundNamesLikeStemPlayer() {
    var meta = registry.forParentType("StemPlayerQueries", "tracks");

    assertThat(meta.namespace()).isEqualTo("STEMPLAYER");
    assertThat(meta.operationType()).isEqualTo(OperationType.QUERY);
  }

  @Test
  void forParentType_bareMutationRootHasNoNamespace() {
    var meta = registry.forParentType("Mutation", "create");

    assertThat(meta.namespace()).isNull();
    assertThat(meta.operationType()).isEqualTo(OperationType.MUTATION);
    assertThat(meta.eventType()).isEqualTo("CREATE");
  }

  @Test
  void forParentType_bareQueryRootHasNoNamespace() {
    var meta = registry.forParentType("Query", "things");

    assertThat(meta.namespace()).isNull();
    assertThat(meta.operationType()).isEqualTo(OperationType.QUERY);
  }

  @Test
  void forEndpoint_alwaysReturnsAnonymousRestMetadata() {
    var meta = registry.forEndpoint("POST", "/graphql");

    assertThat(meta.operationType()).isEqualTo(OperationType.REST);
    assertThat(meta.eventType()).isEqualTo("UNKNOWN");
    assertThat(meta.namespace()).isNull();
  }
}
