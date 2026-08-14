package com.sun.hades.graphql.inference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.hades.model.enums.CefrLevel;
import com.sun.hades.codegen.types.TextLevelAssessment;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InferenceClientTest {

  @Test
  void classify_returnsEmptyWhenServiceUnhealthy() {
    InferenceProcessManager manager = mock(InferenceProcessManager.class);
    when(manager.isHealthy()).thenReturn(false);
    InferenceClient client = new InferenceClient(manager, 8084);

    assertThat(client.classify("some text")).isEmpty();
  }

  @Test
  void map_buildsAssessmentFromResponse() {
    Map<?, ?> body = Map.of(
        "level", "B2",
        "confidence", 0.62,
        "probabilities", List.of(
            Map.of("level", "B1", "probability", 0.2),
            Map.of("level", "B2", "probability", 0.62)),
        "factors", List.of(
            Map.of("name", "avgSentenceLength", "value", 18.5, "direction", "up", "weight", 0.8)));

    TextLevelAssessment result = InferenceClient.map(body);

    assertThat(result.getLevel()).isEqualTo(CefrLevel.B2);
    assertThat(result.getConfidence()).isEqualTo(0.62f);
    assertThat(result.getProbabilities()).hasSize(2);
    assertThat(result.getProbabilities().get(1).getLevel()).isEqualTo(CefrLevel.B2);
    assertThat(result.getFactors()).hasSize(1);
    assertThat(result.getFactors().get(0).getDirection()).isEqualTo("up");
  }

  @Test
  void map_handlesNullBody() {
    assertThat(InferenceClient.map(null)).isNull();
  }
}
