package com.sun.dionysus.headscale;

import com.sun.gaia.model.IpWhitelistEntryEntity;
import com.sun.gaia.service.IpWhitelistService;
import com.sun.gaia.service.TailscaleDeviceService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically reconciles Tailscale node IPs with the Gaia IP whitelist.
 * New nodes are automatically added. Suspended entries are left untouched.
 */
@Component
public class WhitelistReconciler {

  private static final Logger log = LoggerFactory.getLogger(WhitelistReconciler.class);

  private static final String TAILSCALE_PREFIX = "Tailscale: ";

  @Autowired
  private HeadscaleService headscaleService;

  @Autowired
  private IpWhitelistService ipWhitelistService;

  @Autowired
  private TailscaleDeviceService tailscaleDeviceService;

  /**
   * Runs 15s after boot and every 5 minutes thereafter. Adds any Tailscale
   * node IPs that are not yet in the whitelist and upserts Gaia device records.
   */
  @Scheduled(fixedDelay = 300_000, initialDelay = 15_000)
  public void reconcile() {
    try {
      List<HeadscaleService.HeadscaleNode> nodes = headscaleService.listNodes();
      List<IpWhitelistEntryEntity> existing = ipWhitelistService.listAll();

      for (HeadscaleService.HeadscaleNode node : nodes) {
        if (node.ipv4() == null || node.ipv4().isBlank()) continue;
        String ip = node.ipv4();

        boolean found = false;
        boolean suspended = false;
        for (IpWhitelistEntryEntity entry : existing) {
          if (entry.getPattern().equals(ip)) {
            found = true;
            if (!entry.isEnabled()) suspended = true;
            break;
          }
        }

        if (found && !suspended) {
          log.debug("Skipping existing IP {}", ip);
        } else if (suspended) {
          log.debug("Skipping suspended IP {}", ip);
        } else {
          ipWhitelistService.addEntry(ip, TAILSCALE_PREFIX + node.name(), false);
          log.info("Auto-whitelisted Tailscale node {} ({})", node.name(), ip);
        }

        tailscaleDeviceService.upsertFromHeadscale(node.id(), node.name(), node.ipv4(), node.lastSeen(), node.online());
      }
    } catch (Exception e) {
      log.warn("Whitelist reconciliation failed", e);
    }
  }
}
