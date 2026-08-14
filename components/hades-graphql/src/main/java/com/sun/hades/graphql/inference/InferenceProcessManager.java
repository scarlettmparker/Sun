package com.sun.hades.graphql.inference;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Spawns and supervises the Python CEFR inference service.
 */
@Component
@EnableScheduling
public class InferenceProcessManager implements SmartLifecycle {

  private static final Logger logger = LoggerFactory.getLogger(InferenceProcessManager.class);

  private final boolean enabled;
  private final String pythonPath;
  private final String appDir;
  private final int port;
  private final long startupTimeoutMs;
  private final String baseUrl;
  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();

  private Process process;

  public InferenceProcessManager(
      @Value("${cefr.inference.enabled:false}") boolean enabled,
      @Value("${cefr.inference.python:}") String pythonPath,
      @Value("${cefr.inference.dir:}") String appDir,
      @Value("${cefr.inference.port:8084}") int port,
      @Value("${cefr.inference.startup-timeout-ms:120000}") long startupTimeoutMs) {
    this.enabled = enabled;
    this.pythonPath = pythonPath;
    this.appDir = appDir;
    this.port = port;
    this.startupTimeoutMs = startupTimeoutMs;
    this.baseUrl = "http://127.0.0.1:" + port;
  }

  /**
   * Whether the Python service should be managed at all.
   */
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }

  @Override
  public boolean isRunning() {
    return process != null && process.isAlive();
  }

  @Override
  public void start() {
    if (!enabled) {
      return;
    }
    startProcess();
  }

  @Override
  public void stop() {
    synchronized (this) {
      if (process != null) {
        logger.info("Stopping CEFR inference service");
        process.destroy();
        try {
          if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          process.destroyForcibly();
        }
        process = null;
      }
    }
  }

  @Scheduled(fixedDelayString = "${cefr.inference.health-check-ms:30000}")
  public void healthCheck() {
    if (!enabled) {
      return;
    }
    if (process == null || !process.isAlive()) {
      logger.warn("CEFR inference service is not running; restarting");
      startProcess();
    }
  }

  /**
   * Whether the Python service answers its health endpoint.
   */
  public boolean isHealthy() {
    if (!enabled || process == null) {
      return false;
    }
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl + "/health"))
          .timeout(Duration.ofSeconds(2))
          .GET()
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200;
    } catch (Exception e) {
      return false;
    }
  }

  private void startProcess() {
    synchronized (this) {
      if (isRunning()) {
        return;
      }
      logger.info("Starting CEFR inference service on port {}", port);
      try {
        ProcessBuilder builder = new ProcessBuilder(
            pythonPath, "-m", "uvicorn", "main:app",
            "--host", "127.0.0.1", "--port", String.valueOf(port));
        builder.directory(new File(appDir));
        builder.redirectErrorStream(true);
        process = builder.start();
        drainOutput(process);
      } catch (IOException e) {
        logger.error("Failed to start CEFR inference service", e);
        return;
      }
    }
    waitUntilHealthy();
  }

  private void waitUntilHealthy() {
    long deadline = System.currentTimeMillis() + startupTimeoutMs;
    while (System.currentTimeMillis() < deadline) {
      if (isHealthy()) {
        logger.info("CEFR inference service is healthy on port {}", port);
        return;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
    logger.error("CEFR inference service did not become healthy within {}ms", startupTimeoutMs);
  }

  private static void drainOutput(Process spawned) {
    CompletableFuture.runAsync(() -> {
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(spawned.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          logger.debug("[cefr-inference] {}", line);
        }
      } catch (IOException e) {
        // process ended
      }
    });
  }
}
