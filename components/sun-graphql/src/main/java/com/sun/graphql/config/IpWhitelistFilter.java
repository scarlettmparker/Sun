package com.sun.graphql.config;

import com.sun.gaia.service.IpWhitelistService;
import com.sun.gaia.service.UserContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Blocks authenticated requests from IPs not on the whitelist.
 *
 * <p>Runs after {@link JwtAuthFilter} (order +50) so the caller's identity is
 * known. Unauthenticated requests pass through; authenticated requests are
 * checked against the enabled whitelist patterns.
 *
 * <p>Disabled entirely when {@code app.bypass-permissions=true}.
 */
@Component
@Profile("!test")
@ConditionalOnProperty(name = "app.bypass-permissions", havingValue = "false", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE + 55)
public class IpWhitelistFilter extends OncePerRequestFilter {

    private final IpWhitelistService ipWhitelistService;

    /**
     * Request path prefixes to exclude from IP checks (e.g. login, logout,
     * register, password-reset). Configured via
     * {@code ip-whitelist.excluded-paths} as a comma-separated list.
     */
    private final List<String> excludedPaths;

    public IpWhitelistFilter(
            IpWhitelistService ipWhitelistService,
            @Value("${ip-whitelist.excluded-paths:}") String excludedPaths) {
        this.ipWhitelistService = ipWhitelistService;
        this.excludedPaths = excludedPaths == null || excludedPaths.isBlank()
                ? List.of()
                : List.of(excludedPaths.split("\\s*,\\s*"));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        for (String prefix : excludedPaths) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only block authenticated requests.
        if (UserContextHolder.getUserId() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        if (!ipWhitelistService.isAllowed(ip)) {
            throw new AccessDeniedException("IP not whitelisted: " + ip);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the client IP, preferring the first hop of X-Forwarded-For
     * over the raw remote address.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
