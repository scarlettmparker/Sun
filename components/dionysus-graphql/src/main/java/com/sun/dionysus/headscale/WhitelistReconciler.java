package com.sun.dionysus.headscale;

import com.sun.gaia.service.TailscaleDeviceService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically upserts Tailscale device records from Headscale into Gaia.
 */
@Component
public class WhitelistReconciler {

  private static final Logger log = LoggerFactory.getLogger(WhitelistReconciler.class);

  @Autowired
  private HeadscaleService headscaleService;

  @Autowired
  private TailscaleDeviceService tailscaleDeviceService;

  @Scheduled(fixedDelay = 300_000, initialDelay = 15_000)
  public void reconcile() {
    try {
      List<HeadscaleService.HeadscaleNode> nodes = headscaleService.listNodes();
      for (HeadscaleService.HeadscaleNode node : nodes) {
        tailscaleDeviceService.upsertFromHeadscale(
            node.id(), node.name(), node.ipv4(), node.lastSeen(), node.online());
      }
    } catch (Exception e) {
      log.warn("Device reconciliation failed", e);
    }
  }
}
