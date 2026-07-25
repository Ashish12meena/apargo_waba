package com.apargo.waba.api.internal;

/**
 * Header names used on the internal service-to-service surface
 * ({@code /internal/**}).
 *
 * <h2>Why these live in headers and not the path</h2>
 *
 * The path carries <em>resource identity</em> — the thing being addressed.
 * {@code wabaId} and {@code phoneNumberId} are globally unique in Meta's
 * namespace, so either one alone fully identifies the credential being
 * requested. Putting {@code organizationId}/{@code projectId} in the path
 * as well would mint several distinct URLs for one resource, which breaks
 * caching and gateway routing for no gain.
 *
 * <p>{@code organizationId} and {@code projectId} are <em>caller
 * context</em>: an assertion of "I am acting on behalf of this tenant".
 * The service verifies that assertion against the data (does this WABA
 * really belong to that org? does that project really have an assignment
 * to it?) and refuses when it doesn't hold. Cross-cutting context of that
 * shape belongs in headers — it propagates through gateways and Feign
 * interceptors without every call site having to thread it through a URL,
 * and it feeds straight into MDC logging (this service's log pattern
 * already reserves {@code %X{orgId}}).
 *
 * <p>They are deliberately not query parameters: query strings are logged
 * verbatim by proxies, load balancers and access logs.
 *
 * <h2>Trust level</h2>
 *
 * {@link #ORGANIZATION_ID} is a claim the caller makes about itself, not
 * proof. It is only as trustworthy as {@link #API_KEY} — any service
 * holding the shared key can assert any org. That is acceptable while
 * every internal caller is equally trusted; it stops being acceptable the
 * moment one is not. See {@code InternalApiAuthFilter} for the upgrade
 * path (per-caller keys, then a gateway-signed JWT carrying the org as a
 * verified claim).
 */
public final class InternalHeaders {

    /** Shared secret proving the caller is inside the trust boundary. */
    public static final String API_KEY = "X-Internal-Api-Key";

    /** Calling service name — for audit trails and per-caller rate limits. */
    public static final String CALLER_SERVICE = "X-Internal-Caller";

    /** Tenant the caller is acting for. Verified against the resource. */
    public static final String ORGANIZATION_ID = "X-Org-Id";

    /** Optional project scope. When present, verified against the resource. */
    public static final String PROJECT_ID = "X-Project-Id";

    /** Correlation id propagated across services. */
    public static final String REQUEST_ID = "X-Request-Id";

    private InternalHeaders() {
    }
}