package com.sun.jocasta.job;

import com.sun.gaia.service.ConfigurationReconciler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically reconciles enabled configurations into live data.
 */
@Component
public class ConfigurationReloadJob {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationReloadJob.class);

  private final ConfigurationReconciler reconciler;

  public ConfigurationReloadJob(ConfigurationReconciler reconciler) {
    this.reconciler = reconciler;
  }

  /**
   * Reconciles every enabled configuration on a fixed delay.
   */
  @Scheduled(fixedDelay = 30 * 60 * 1000L, initialDelay = 30 * 60 * 1000L)
  public void reloadConfigurations() {
    logger.info("Running scheduled configuration reload");
    reconciler.reconcileAll();
  }
}
