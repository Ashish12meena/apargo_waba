package com.apargo.waba.infrastructure.persistence;

import com.apargo.waba.application.port.out.WabaDailyMessageUsageRepositoryPort;
import com.apargo.waba.domain.entity.WabaDailyMessageUsage;
import com.apargo.waba.infrastructure.persistence.jpa.WabaDailyMessageUsageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Implements {@link WabaDailyMessageUsageRepositoryPort} on top of Spring Data
 * JPA.
 */
@Repository
@RequiredArgsConstructor
public class WabaDailyMessageUsageRepositoryAdapter implements WabaDailyMessageUsageRepositoryPort {

    private final WabaDailyMessageUsageJpaRepository jpaRepository;

    @Override
    public WabaDailyMessageUsage save(WabaDailyMessageUsage usage) {
        return jpaRepository.save(usage);
    }

    @Override
    public Optional<WabaDailyMessageUsage> findByWabaAccountIdAndUsageDate(Long wabaAccountId, LocalDate usageDate) {
        return jpaRepository.findByWabaAccountIdAndUsageDate(wabaAccountId, usageDate);
    }

    @Override
    public List<WabaDailyMessageUsage> findByWabaAccountIdAndUsageDateBetween(
            Long wabaAccountId, LocalDate from, LocalDate to) {
        return jpaRepository.findByWabaAccountIdAndUsageDateBetween(wabaAccountId, from, to);
    }

    @Override
    public List<WabaDailyMessageUsage> findByWabaAccountIdInAndUsageDate(
            List<Long> wabaAccountIds, LocalDate usageDate) {
        return jpaRepository.findByWabaAccountIdInAndUsageDate(wabaAccountIds, usageDate);
    }

    @Override
    public List<WabaDailyMessageUsage> findByUsageDateBefore(LocalDate cutoffDate) {
        return jpaRepository.findByUsageDateBefore(cutoffDate);
    }

    @Override
    public void deleteAll(List<WabaDailyMessageUsage> usages) {
        jpaRepository.deleteAll(usages);
    }
}