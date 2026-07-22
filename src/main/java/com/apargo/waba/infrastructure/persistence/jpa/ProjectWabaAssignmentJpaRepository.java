package com.apargo.waba.infrastructure.persistence.jpa;

import com.apargo.waba.domain.entity.ProjectWabaAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProjectWabaAssignment}.
 * <p>
 * Infrastructure-only — see
 * {@link com.apargo.waba.application.port.out.ProjectWabaAssignmentRepositoryPort}.
 */
public interface ProjectWabaAssignmentJpaRepository extends JpaRepository<ProjectWabaAssignment, Long> {

    List<ProjectWabaAssignment> findByProjectId(Long projectId);

    List<ProjectWabaAssignment> findByWabaAccountId(Long wabaAccountId);

    Optional<ProjectWabaAssignment> findByProjectIdAndDefaultAssignmentTrue(Long projectId);
}