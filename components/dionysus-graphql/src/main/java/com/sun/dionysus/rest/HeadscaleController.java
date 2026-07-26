package com.sun.dionysus.rest;

import com.sun.dionysus.headscale.HeadscaleService;
import com.sun.dionysus.headscale.HeadscaleService.HeadscaleNode;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for managing Headscale nodes and generating pre-auth key QR codes.
 */
@RestController
@RequestMapping("/api/headscale")
public class HeadscaleController {

  private static final Logger log = LoggerFactory.getLogger(HeadscaleController.class);

  @Autowired
  private HeadscaleService headscaleService;

  private static final Pattern EXPIRY_PATTERN = Pattern.compile("^(\\d+)([smh])$");

  /**
   * Lists all Tailscale nodes on the tailnet.
   */
  @GetMapping("/nodes")
  public ResponseEntity<List<HeadscaleNode>> listNodes() {
    return ResponseEntity.ok(headscaleService.listNodes());
  }

  /**
   * Returns a single Tailscale node by id.
   */
  @GetMapping("/nodes/{id}")
  public ResponseEntity<HeadscaleNode> getNode(@PathVariable long id) {
    return headscaleService.listNodes().stream()
        .filter(n -> n.id() == id)
        .findFirst()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Expires a node, removing it from the tailnet.
   */
  @PostMapping("/nodes/{id}/expire")
  public ResponseEntity<Void> expireNode(@PathVariable long id) {
    headscaleService.expireNode(id);
    return ResponseEntity.ok().build();
  }

  /**
   * Generates a pre-auth key and returns it as a QR code PNG image.
   *
   * @param expiry duration string like "30m", "1h", "6h", "24h".
   */
  @GetMapping(value = "/preauth-key", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> preauthKey(@RequestParam(defaultValue = "1h") String expiry) {
    Duration d = parseExpiry(expiry);
    String key = headscaleService.createPreAuthKey(d);
    log.info("Generated pre-auth key with expiry {}", expiry);

    try {
      byte[] png = generateQRCode(key);
      return ResponseEntity.ok()
          .header("X-Preauth-Key", key)
          .contentType(MediaType.IMAGE_PNG)
          .body(png);
    } catch (Exception e) {
      log.error("Failed to generate QR code", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Parses expiry strings like "30m", "1h", "6h", "24h".
   */
  private Duration parseExpiry(String value) {
    var m = EXPIRY_PATTERN.matcher(value.trim());
    if (!m.matches()) return Duration.ofHours(1);
    long amount = Long.parseLong(m.group(1));
    return switch (m.group(2)) {
      case "s" -> Duration.ofSeconds(amount);
      case "m" -> Duration.ofMinutes(amount);
      case "h" -> Duration.ofHours(amount);
      default -> Duration.ofHours(1);
    };
  }

  /**
   * Generates a QR code PNG using the qrencode CLI.
   */
  private byte[] generateQRCode(String content) throws Exception {
    ProcessBuilder pb = new ProcessBuilder("qrencode", "-t", "PNG", "-o", "-", content);
    pb.redirectErrorStream(true);
    Process p = pb.start();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    p.getInputStream().transferTo(out);
    int exit = p.waitFor();
    if (exit != 0) {
      throw new RuntimeException("qrencode exited with code " + exit);
    }
    return out.toByteArray();
  }
}
