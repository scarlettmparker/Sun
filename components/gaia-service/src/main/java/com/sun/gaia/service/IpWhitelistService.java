package com.sun.gaia.service;

import com.sun.gaia.model.IpWhitelistEntryEntity;
import com.sun.gaia.repository.IpWhitelistEntryRepository;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates whether a client IP is permitted by the whitelist.
 *
 * <p>Pattern formats supported:
 * <ul>
 *   <li><b>CIDR</b> - e.g. {@code 192.168.0.0/24}. Proper subnet bitwise match.</li>
 *   <li><b>Glob</b> - e.g. {@code 192.168.0.*} or {@code 5.*}. {@code *} matches
 *       any sequence of characters.</li>
 *   <li><b>Exact</b> - a bare IP like {@code 10.0.0.1}. String equality.</li>
 * </ul>
 *
 * <p>When {@code app.bypass-permissions=true} the check always passes (returns
 * a synthetic wildcard entry so the filter never blocks).
 */
@Service
public class IpWhitelistService {

    private static final Logger log = LoggerFactory.getLogger(IpWhitelistService.class);

    private final IpWhitelistEntryRepository repository;
    private final boolean bypassPermissions;

    private final ConcurrentMap<String, Pattern> globPatternCache = new ConcurrentHashMap<>();

    public IpWhitelistService(
            IpWhitelistEntryRepository repository,
            @Value("${app.bypass-permissions:false}") boolean bypassPermissions) {
        this.repository = repository;
        this.bypassPermissions = bypassPermissions;
    }

    /**
     * Returns enabled entries, or a single wildcard entry when bypass is active.
     */
    public List<IpWhitelistEntryEntity> findEnabled() {
        if (bypassPermissions) {
            return wildcardEntry();
        }
        return repository.findByEnabledTrue();
    }

    /**
     * Returns every whitelist entry regardless of enabled state.
     */
    @Transactional(readOnly = true)
    public List<IpWhitelistEntryEntity> listAll() {
        return repository.findAll();
    }

    /**
     * Creates a new whitelist entry.
     *
     * @param pattern     the IP pattern (CIDR, glob, or exact).
     * @param description optional human-readable label.
     * @param immutable   whether the entry is immutable after creation.
     * @return the saved entity.
     */
    @Transactional
    public IpWhitelistEntryEntity addEntry(String pattern, String description, boolean immutable) {
        // Check for existing entry with the same pattern
        Optional<IpWhitelistEntryEntity> existing = repository.findByPattern(pattern.trim());
        if (existing.isPresent()) {
            IpWhitelistEntryEntity entry = existing.get();
            if (entry.isEnabled()) {
                throw new IllegalArgumentException("Entry already exists for pattern: " + pattern);
            }
            // Re-enable a suspended entry
            entry.setEnabled(true);
            if (description != null) {
                entry.setDescription(description.trim());
            }
            entry.setImmutable(immutable);
            return repository.save(entry);
        }
        IpWhitelistEntryEntity entity = new IpWhitelistEntryEntity();
        entity.setPattern(pattern.trim());
        entity.setDescription(description == null ? null : description.trim());
        entity.setImmutable(immutable);
        return repository.save(entity);
    }

    /**
     * Updates fields on an existing entry. Immutable entries are rejected.
     */
    @Transactional
    public IpWhitelistEntryEntity updateEntry(UUID id, String pattern, String description, Boolean enabled) {
        IpWhitelistEntryEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Whitelist entry not found: " + id));
        if (entity.isImmutable()) {
            throw new IllegalArgumentException("Cannot edit an immutable entry");
        }
        if (pattern != null) {
            entity.setPattern(pattern.trim());
        }
        if (description != null) {
            entity.setDescription(description.trim());
        }
        if (enabled != null) {
            entity.setEnabled(enabled);
        }
        return repository.save(entity);
    }

    /**
     * Deletes a whitelist entry. Immutable entries are rejected.
     */
    @Transactional
    public void deleteEntry(UUID id) {
        IpWhitelistEntryEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Whitelist entry not found: " + id));
        if (entity.isImmutable()) {
            throw new IllegalArgumentException("Cannot delete an immutable entry");
        }
        repository.deleteById(id);
    }

    /**
     * Checks whether the given IP is permitted by any enabled entry.
     *
     * @param ip the client IP string (e.g. {@code "192.168.1.5"}).
     * @return true when the IP matches at least one enabled pattern.
     */
    public boolean isAllowed(String ip) {
        List<IpWhitelistEntryEntity> entries = findEnabled();
        for (IpWhitelistEntryEntity entry : entries) {
            if (matches(entry.getPattern(), ip)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether a single pattern covers the given IP.
     */
    boolean matches(String pattern, String ip) {
        if (pattern.contains("/")) {
            return cidrMatches(pattern, ip);
        }
        if (pattern.contains("*")) {
            return globMatches(pattern, ip);
        }
        return pattern.equals(ip);
    }

    // --- CIDR matching ---------------------------------------------------

    private boolean cidrMatches(String cidr, String ip) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            int prefixLen = Integer.parseInt(parts[1]);
            byte[] addrBytes = InetAddress.getByName(ip).getAddress();
            byte[] cidrBytes = InetAddress.getByName(parts[0]).getAddress();

            if (addrBytes.length != cidrBytes.length) {
                return false;
            }

            int fullBytes = prefixLen / 8;
            int remainingBits = prefixLen % 8;

            for (int i = 0; i < fullBytes && i < addrBytes.length; i++) {
                if (addrBytes[i] != cidrBytes[i]) {
                    return false;
                }
            }

            if (remainingBits > 0 && fullBytes < addrBytes.length) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                if ((addrBytes[fullBytes] & mask) != (cidrBytes[fullBytes] & mask)) {
                    return false;
                }
            }

            return true;
        } catch (UnknownHostException | NumberFormatException e) {
            log.warn("Invalid CIDR pattern or IP: pattern={}, ip={}", cidr, ip, e);
            return false;
        }
    }

    // --- Glob matching ----------------------------------------------------

    private boolean globMatches(String glob, String ip) {
        Pattern p = globPatternCache.computeIfAbsent(glob, g -> {
            String regex = "^" + g.replace(".", "\\.").replace("*", ".*") + "$";
            return Pattern.compile(regex);
        });
        return p.matcher(ip).matches();
    }

    // --- Bypass -----------------------------------------------------------

    /**
     * Returns a synthetic wildcard entry that matches everything, used when
     * {@code app.bypass-permissions=true}.
     */
    private List<IpWhitelistEntryEntity> wildcardEntry() {
        IpWhitelistEntryEntity wildcard = new IpWhitelistEntryEntity();
        wildcard.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        wildcard.setPattern("*");
        wildcard.setEnabled(true);
        return List.of(wildcard);
    }
}
