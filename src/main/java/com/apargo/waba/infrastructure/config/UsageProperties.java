package com.apargo.waba.infrastructure.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Tunables for {@code WabaDailyMessageUsage} tracking and retention.
 *
 * <p>Bound from the {@code usage:} block in {@code application.yaml}.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "usage")
public class UsageProperties {

    /**
     * Number of days a daily usage record is kept before a scheduled
     * cleanup job may archive/delete it.
     *
     * <p>Per {@code WabaDailyMessageUsage} Javadoc: records are only
     * needed for limit enforcement, not long-term analytics, so anything
     * older than this window is safe to prune.
     */
    @Min(1)
    private int dailyUsageRetentionDays = 90;
}