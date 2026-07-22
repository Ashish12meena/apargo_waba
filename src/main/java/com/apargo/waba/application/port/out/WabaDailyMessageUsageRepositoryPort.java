package com.apargo.waba.application.port.out;

import com.apargo.waba.domain.entity.WabaDailyMessageUsage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port for {@link WabaDailyMessageUsage} persistence.
 */
public interface WabaDailyMessageUsageRepositoryPort {

    WabaDailyMessageUsage save(WabaDailyMessageUsage usage);

    /** The single row for a WABA on a given date, if it exists yet. */
    Optional<WabaDailyMessageUsage> findByWabaAccountIdAndUsageDate(Long wabaAccountId, LocalDate usageDate);

    List<WabaDailyMessageUsage> findByWabaAccountIdAndUsageDateBetween(
            Long wabaAccountId, LocalDate from, LocalDate to);

    /**
     * Usage rows for every WABA in a set, on one date — used to sum usage
     * across all WABAs under the same Business Manager (portfolio-level
     * limit enforcement).
     */
    List<WabaDailyMessageUsage> findByWabaAccountIdInAndUsageDate(List<Long> wabaAccountIds, LocalDate usageDate);

    /** Rows older than the retention window — candidates for scheduled cleanup. */
    List<WabaDailyMessageUsage> findByUsageDateBefore(LocalDate cutoffDate);

    void deleteAll(List<WabaDailyMessageUsage> usages);
}