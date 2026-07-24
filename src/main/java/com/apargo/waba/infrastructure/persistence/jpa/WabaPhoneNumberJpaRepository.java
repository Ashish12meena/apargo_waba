package com.apargo.waba.infrastructure.persistence.jpa;

import com.apargo.waba.domain.entity.WabaPhoneNumber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link WabaPhoneNumber}.
 * <p>
 * Infrastructure-only — see {@link com.apargo.waba.application.port.out.WabaPhoneNumberRepositoryPort}.
 */
public interface WabaPhoneNumberJpaRepository extends JpaRepository<WabaPhoneNumber, Long> {

    Optional<WabaPhoneNumber> findByWhatsappPhoneNumberId(String whatsappPhoneNumberId);

    List<WabaPhoneNumber> findByWabaAccountId(Long wabaAccountId);

    List<WabaPhoneNumber> findByWabaAccountIdIn(List<Long> wabaAccountIds);
}