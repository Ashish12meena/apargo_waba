package com.apargo.waba.infrastructure.persistence;

import com.apargo.waba.application.port.out.ProjectWabaAssignmentRepositoryPort;
import com.apargo.waba.domain.entity.ProjectWabaAssignment;
import com.apargo.waba.infrastructure.persistence.jpa.ProjectWabaAssignmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implements {@link ProjectWabaAssignmentRepositoryPort} on top of Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
public class ProjectWabaAssignmentRepositoryAdapter implements ProjectWabaAssignmentRepositoryPort {

    private final ProjectWabaAssignmentJpaRepository jpaRepository;

    @Override
    public ProjectWabaAssignment save(ProjectWabaAssignment assignment) {
        return jpaRepository.save(assignment);
    }

    @Override
    public Optional<ProjectWabaAssignment> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ProjectWabaAssignment> findByProjectId(Long projectId) {
        return jpaRepository.findByProjectId(projectId);
    }

    @Override
    public List<ProjectWabaAssignment> findByWabaAccountId(Long wabaAccountId) {
        return jpaRepository.findByWabaAccountId(wabaAccountId);
    }

    @Override
    public Optional<ProjectWabaAssignment> findByProjectIdAndDefaultAssignmentTrue(Long projectId) {
        return jpaRepository.findByProjectIdAndDefaultAssignmentTrue(projectId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}