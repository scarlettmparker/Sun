package com.sun.graphql.rest;

import com.sun.gaia.model.IpWhitelistEntryEntity;
import com.sun.gaia.service.IpWhitelistService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns the active IP whitelist for the Fastify-level gate.
 *
 * REST (not GraphQL) because the Fastify server fetches this during boot,
 * before any GraphQL resolver stack exists.
 */
@RestController
@RequestMapping("/api/public")
public class IpWhitelistController {

    private final IpWhitelistService ipWhitelistService;

    public IpWhitelistController(IpWhitelistService ipWhitelistService) {
        this.ipWhitelistService = ipWhitelistService;
    }

    @GetMapping("/ip-whitelist")
    public Map<String, Object> getWhitelist() {
        List<IpWhitelistEntryEntity> entries = ipWhitelistService.findEnabled();
        List<String> patterns = entries.stream()
                .map(IpWhitelistEntryEntity::getPattern)
                .toList();
        boolean bypass = patterns.size() == 1 && patterns.get(0).equals("*");
        return Map.of("patterns", patterns, "bypass", bypass);
    }
}
