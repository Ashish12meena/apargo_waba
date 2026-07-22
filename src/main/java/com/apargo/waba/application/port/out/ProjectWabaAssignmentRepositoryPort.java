package com.apargo.waba.application.port.out;

import com.apargo.waba.domain.entity.ProjectWabaAssignment;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for {@link ProjectWabaAssignment} persistence.
 */
public interface ProjectWabaAssignmentRepositoryPort {

    ProjectWabaAssignment save(ProjectWabaAssignment assignment);

    Optional<ProjectWabaAssignment> findById(Long id);

    List<ProjectWabaAssignment> findByProjectId(Long projectId);

    List<ProjectWabaAssignment> findByWabaAccountId(Long wabaAccountId);

    /** The project's current default WABA, if one is marked. */
    Optional<ProjectWabaAssignment> findByProjectIdAndDefaultAssignmentTrue(Long projectId);

    void deleteById(Long id);
}