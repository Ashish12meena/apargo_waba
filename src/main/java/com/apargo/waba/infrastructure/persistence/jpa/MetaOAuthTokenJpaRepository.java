package com.apargo.waba.infrastructure.persistence.jpa;

import com.apargo.waba.domain.entity.MetaOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetaOAuthTokenJpaRepository extends JpaRepository<MetaOAuthToken, Long> {

    List<MetaOAuthToken> findByOrganizationId(Long organizationId);

    boolean existsByOrganizationId(Long organizationId);
}