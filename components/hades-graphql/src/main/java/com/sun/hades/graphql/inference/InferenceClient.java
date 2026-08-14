package com.sun.hades.graphql.inference;

import com.sun.hades.codegen.types.ComplexityFactor;
import com.sun.hades.codegen.types.LevelProbability;
import com.sun.hades.codegen.types.TextLevelAssessment;
import com.sun.hades.model.enums.CefrLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the Python CEFR inference service.
 */
@Component
public class InferenceClient {

  private final InferenceProcessManager manager;
  private final RestClient restClient;

  public InferenceClient(
      InferenceProcessManager manager,
      @Value("${cefr.inference.port:8084}") int port) {
    this.manager = manager;
    this.restClient = RestClient.builder()
        .baseUrl("http://127.0.0.1:" + port)
        .build();
  }

  /**
   * Classifies a text, or empty when the service is unavailable.
   *
   * @param text the text to classify
   * @return the assessment, if the service answered
   */
  public Optional<TextLevelAssessment> classify(String text) {
    if (!manager.isHealthy()) {
      return Optional.empty();
    }
    try {
      Map<?, ?> body = restClient.post()
          .uri("/classify")
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("text", text))
          .retrieve()
          .body(Map.class);
      return Optional.of(map(body));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Maps the inference response into the GraphQL assessment type.
   *
   * @param body the parsed JSON response
   * @return the assessment
   */
  @SuppressWarnings("unchecked")
  static TextLevelAssessment map(Map<?, ?> body) {
    if (body == null) {
      return null;
    }
    List<LevelProbability> probabilities = new ArrayList<>();
    Object probabilitiesValue = body.get("probabilities");
    if (probabilitiesValue instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof Map<?, ?> entry) {
          probabilities.add(LevelProbability.newBuilder()
              .level(CefrLevel.valueOf(String.valueOf(entry.get("level"))))
              .probability(((Number) entry.get("probability")).floatValue())
              .build());
        }
      }
    }
    List<ComplexityFactor> factors = new ArrayList<>();
    Object factorsValue = body.get("factors");
    if (factorsValue instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof Map<?, ?> entry) {
          factors.add(ComplexityFactor.newBuilder()
              .name(String.valueOf(entry.get("name")))
              .value(((Number) entry.get("value")).floatValue())
              .direction(String.valueOf(entry.get("direction")))
              .weight(((Number) entry.get("weight")).floatValue())
              .build());
        }
      }
    }
    return TextLevelAssessment.newBuilder()
        .level(CefrLevel.valueOf(String.valueOf(body.get("level"))))
        .confidence(((Number) body.get("confidence")).floatValue())
        .probabilities(probabilities)
        .factors(factors)
        .build();
  }
}
