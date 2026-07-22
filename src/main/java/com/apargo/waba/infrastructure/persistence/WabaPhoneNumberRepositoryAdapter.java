package com.apargo.waba.infrastructure.persistence;

import com.apargo.waba.application.port.out.WabaPhoneNumberRepositoryPort;
import com.apargo.waba.domain.entity.WabaPhoneNumber;
import com.apargo.waba.infrastructure.persistence.jpa.WabaPhoneNumberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implements {@link WabaPhoneNumberRepositoryPort} on top of Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
public class WabaPhoneNumberRepositoryAdapter implements WabaPhoneNumberRepositoryPort {

    private final WabaPhoneNumberJpaRepository jpaRepository;

    @Override
    public WabaPhoneNumber save(WabaPhoneNumber phoneNumber) {
        return jpaRepository.save(phoneNumber);
    }

    @Override
    public Optional<WabaPhoneNumber> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<WabaPhoneNumber> findByWhatsappPhoneNumberId(String whatsappPhoneNumberId) {
        return jpaRepository.findByWhatsappPhoneNumberId(whatsappPhoneNumberId);
    }

    @Override
    public List<WabaPhoneNumber> findByWabaAccountId(Long wabaAccountId) {
        return jpaRepository.findByWabaAccountId(wabaAccountId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}