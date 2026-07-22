package com.apargo.waba.infrastructure.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Tunables for the {@code OnboardingTask} workflow (Embedded Signup saga).
 *
 * <p>Bound from the {@code onboarding:} block in {@code application.yaml}.
 * Keeping these here means retry policy and idempotency windows can be
 * changed per environment (e.g. more aggressive retries in prod) without a
 * code change or redeploy of logic.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "onboarding")
public class OnboardingProperties {

    /**
     * Maximum number of automatic/manual retries allowed for a
     * {@code FAILED} onboarding task before it must be treated as
     * permanently failed.
     *
     * @see com.apargo.waba.domain.entity.OnboardingTask#canRetry(int)
     */
    @Min(0)
    private int maxRetries = 3;

    /**
     * How long an {@code idempotencyKey} remains valid for deduplicating
     * onboarding start requests, in hours.
     */
    @Min(1)
    private int idempotencyTtlHours = 24;
}