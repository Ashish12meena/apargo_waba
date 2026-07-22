package com.apargo.waba.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Typed value for a {@code WebhookChange} whose {@code field} is
 * {@code "account_update"}.
 *
 * <h2>Confidence level</h2>
 * Only {@code event} is modeled — this is the one field confirmed across
 * multiple sources (Meta's own Embedded Signup implementation guide and
 * changelog) to always be present and to drive behavior (e.g.
 * {@code PARTNER_ADDED}, {@code PARTNER_REMOVED}, {@code AD_ACCOUNT_LINKED}).
 * Event-specific payload fields beyond that (e.g. whatever
 * {@code disconnection_info} looks like on {@code PARTNER_REMOVED}) have
 * <b>not</b> been cross-checked against a live payload in this environment
 * and are deliberately not modeled yet — add them only after confirming the
 * exact shape against {@code docs/waba-meta-docs-reference.md} / a captured
 * real payload, rather than guessing field names.
 *
 * <p>Kept as a raw {@link String} for {@code event} (not an enum) for the
 * same forward-compatibility reason as {@link WebhookChange#getField()} -
 * Meta adds new event values without notice (e.g. {@code AD_ACCOUNT_LINKED}
 * was added after this webhook field already existed).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookAccountUpdateValue {

    /**
     * The specific account-level event that occurred. Known values include
     * {@code PARTNER_ADDED} (fired on successful Embedded Signup
     * completion - see {@code OnboardingWorkflowExecutor} for the
     * API-driven onboarding path this webhook runs alongside) and
     * {@code PARTNER_REMOVED} (the business disconnected this app from
     * their WABA).
     */
    private String event;
}