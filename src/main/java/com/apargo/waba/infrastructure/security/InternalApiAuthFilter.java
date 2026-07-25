package com.apargo.waba.infrastructure.security;

import com.apargo.waba.api.internal.InternalHeaders;
import com.apargo.waba.api.response.ApiErrorResponse;
import com.apargo.waba.infrastructure.config.InternalApiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * Guards every request under {@code internal.api.path-prefix}.
 *
 * <h2>Why a plain servlet filter</h2>
 *
 * This service has no Spring Security on the classpath, and pulling it in
 * for one shared-secret check would bring an entire filter chain and a
 * default-deny posture that the existing public controllers are not
 * written for. A {@link OncePerRequestFilter} scoped to a single path
 * prefix does exactly one job and changes nothing else. If Spring
 * Security is adopted later, this logic moves into an
 * {@code AuthenticationFilter} and this class goes away.
 *
 * <h2>This is one layer, not the whole defence</h2>
 *
 * The endpoints behind this filter return decrypted Meta access tokens.
 * A leaked token grants control of the customer's WhatsApp Business
 * Account — messaging, template management, and (given the scopes this
 * service requests) business management. Treat the filter as the last
 * line, not the only one:
 *
 * <ol>
 *   <li>API gateway refuses to route {@code /internal/**} from outside.</li>
 *   <li>Network policy allows the port only from sibling service subnets.
 *       Note this service registers with Eureka, so anything already in
 *       the mesh can reach it — network placement alone proves nothing
 *       about <em>which</em> service is calling.</li>
 *   <li>This filter, on a rotatable shared secret.</li>
 *   <li>Per-request tenant checks in the use case (does this org actually
 *       own this WABA?).</li>
 * </ol>
 *
 * <h2>Upgrade path</h2>
 *
 * A single shared key cannot tell callers apart: any holder can assert
 * any {@code X-Organization-Id}. Move to per-caller keys when you need to
 * revoke one service without rotating all of them, and to mTLS or a
 * gateway-signed JWT when the organization needs to be a
 * cryptographically verified claim rather than a self-asserted header.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalApiAuthFilter extends OncePerRequestFilter {

    private final InternalApiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(properties.getPathPrefix());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String caller = request.getHeader(InternalHeaders.CALLER_SERVICE);
        String requestId = request.getHeader(InternalHeaders.REQUEST_ID);
        String organizationId = request.getHeader(InternalHeaders.ORGANIZATION_ID);

        if (properties.isAuthEnabled() && !hasValidKey(request)) {
            // No detail in the response and no echo of the presented key —
            // a caller that failed auth learns only that it failed.
            log.warn("Rejected internal request to {} from caller={} requestId={}",
                    request.getRequestURI(), caller, requestId);
            writeUnauthorized(request, response);
            return;
        }

        // The service's console log pattern reserves %X{traceId} and
        // %X{orgId}; populating them here means every downstream log line
        // for this request is attributable without extra plumbing.
        try {
            if (organizationId != null) {
                MDC.put("orgId", organizationId);
            }
            if (requestId != null) {
                MDC.put("traceId", requestId);
            }
            filterChain.doFilter(request, response);
        } finally {
            // Threads are pooled — leaving MDC populated would mislabel
            // an unrelated later request.
            MDC.remove("orgId");
            MDC.remove("traceId");
        }
    }

    /**
     * Compared with {@link MessageDigest#isEqual} rather than
     * {@link String#equals} so that comparison time does not vary with how
     * many leading characters matched. Over enough requests, that
     * difference is measurable and lets an attacker recover the key one
     * character at a time.
     */
    private boolean hasValidKey(HttpServletRequest request) {
        String presented = request.getHeader(InternalHeaders.API_KEY);
        if (presented == null || presented.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                properties.getApiKey().getBytes(StandardCharsets.UTF_8));
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Invalid or missing internal API credentials")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}