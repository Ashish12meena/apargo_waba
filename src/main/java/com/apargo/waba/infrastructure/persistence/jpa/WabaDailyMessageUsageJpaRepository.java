package com.apargo.waba.infrastructure.persistence.jpa;

import com.apargo.waba.domain.entity.WabaDailyMessageUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link WabaDailyMessageUsage}.
 * <p>
 * Infrastructure-only — see
 * {@link com.apargo.waba.application.port.out.WabaDailyMessageUsageRepositoryPort}.
 */
public interface WabaDailyMessageUsageJpaRepository extends JpaRepository<WabaDailyMessageUsage, Long> {

    Optional<WabaDailyMessageUsage> findByWabaAccountIdAndUsageDate(Long wabaAccountId, LocalDate usageDate);

    List<WabaDailyMessageUsage> findByWabaAccountIdAndUsageDateBetween(
            Long wabaAccountId, LocalDate from, LocalDate to);

    List<WabaDailyMessageUsage> findByWabaAccountIdInAndUsageDate(List<Long> wabaAccountIds, LocalDate usageDate);

    List<WabaDailyMessageUsage> findByUsageDateBefore(LocalDate cutoffDate);
}