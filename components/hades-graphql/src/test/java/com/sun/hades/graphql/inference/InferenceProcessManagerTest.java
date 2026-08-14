package com.sun.hades.graphql.inference;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InferenceProcessManagerTest {

  @Test
  void disabledManagerStartsWithoutSpawning() {
    InferenceProcessManager manager = new InferenceProcessManager(false, "python3", "app", 8084, 1000);

    manager.start();

    assertThat(manager.isRunning()).isFalse();
    assertThat(manager.isHealthy()).isFalse();
    assertThat(manager.isEnabled()).isFalse();
    manager.stop();
  }

  @Test
  void notHealthyWhenNoProcessRunning() {
    InferenceProcessManager manager = new InferenceProcessManager(true, "python3", "app", 8084, 1000);

    assertThat(manager.isHealthy()).isFalse();
    manager.stop();
  }
}
