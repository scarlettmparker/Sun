package com.sun.graphql.audit.chain;

import com.sun.base.audit.entity.AuditEvent;
import com.sun.base.audit.repository.AuditEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Recomputes a namespace's audit hash chain and reports the first row that does
 * not match. Rows are walked in windows, oldest first.
 */
@Service
public class AuditChainVerifier {

  /**
   * Rows read per window while walking the chain.
   */
  public static final int WINDOW_SIZE = 500;

  private final AuditEventRepository repository;
  private final AuditHashing hashing;

  public AuditChainVerifier(AuditEventRepository repository, AuditHashing hashing) {
    this.repository = repository;
    this.hashing = hashing;
  }

  /**
   * Walks a namespace's rows oldest first in windows and recomputes each hash
   * over the previous one.
   *
   * @param namespace the namespace to verify, or null for the unnamed chain
   * @return the verification result
   */
  public AuditChainVerification verify(String namespace) {
    Sort order = Sort.by(Sort.Direction.ASC, "createdAt", "id");
    String prevHash = null;
    int checked = 0;
    int pageNumber = 0;

    while (true) {
      Page<AuditEvent> window = loadWindow(namespace, PageRequest.of(pageNumber, WINDOW_SIZE, order));
      List<AuditEvent> rows = window.getContent();

      for (AuditEvent row : rows) {
        String expected = hashing.hash(row, prevHash);
        if (!expected.equals(row.getRowHash())) {
          return AuditChainVerification.broken(
              checked + 1, row.getId(), row.getCreatedAt(), hashing.isKeyed());
        }
        prevHash = row.getRowHash();
        checked++;
      }

      if (!window.hasNext()) {
        return AuditChainVerification.intact(checked, hashing.isKeyed());
      }
      pageNumber++;
    }
  }

  /**
   * Loads one window of rows to verify.
   *
   * @param namespace the namespace to scope to, or null for the unnamed chain
   * @param pageable the window request, in ascending chain order
   * @return the window
   */
  private Page<AuditEvent> loadWindow(String namespace, PageRequest pageable) {
    return namespace == null
        ? repository.findByNamespaceIsNull(pageable)
        : repository.findByNamespace(namespace, pageable);
  }

  /**
   * Outcome of verifying a chain.
   *
   * @param intact whether every recomputed hash matched the stored one
   * @param checked number of rows walked
   * @param brokenId id of the first mismatching row, or null when intact
   * @param brokenAt timestamp of the first mismatching row, or null when intact
   * @param keyed whether the hash is HMAC-keyed rather than an unkeyed digest
   */
  public record AuditChainVerification(
      boolean intact,
      int checked,
      UUID brokenId,
      Instant brokenAt,
      boolean keyed) {

    /**
     * Builds an intact result.
     *
     * @param checked number of rows walked
     * @param keyed whether the hash is keyed
     * @return the result
     */
    public static AuditChainVerification intact(int checked, boolean keyed) {
      return new AuditChainVerification(true, checked, null, null, keyed);
    }

    /**
     * Builds a broken result.
     *
     * @param checked number of rows walked up to and including the mismatch
     * @param id id of the first mismatching row
     * @param at timestamp of the first mismatching row
     * @param keyed whether the hash is keyed
     * @return the result
     */
    public static AuditChainVerification broken(int checked, UUID id, Instant at, boolean keyed) {
      return new AuditChainVerification(false, checked, id, at, keyed);
    }
  }
}
