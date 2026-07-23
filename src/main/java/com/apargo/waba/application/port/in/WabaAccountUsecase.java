package com.apargo.waba.application.port.in;

import com.apargo.waba.api.response.WabaAccountResponse;

import java.util.List;

/**
 * Inbound port for read access to {@link com.apargo.waba.domain.entity.WabaAccount}.
 */
public interface WabaAccountUsecase {

    /** All WABAs belonging to an organization. */
    List<WabaAccountResponse> listByOrganization(Long organizationId);

    /**
     * All WABAs assigned to a project, resolved through
     * {@link com.apargo.waba.domain.entity.ProjectWabaAssignment}.
     */
    List<WabaAccountResponse> listByProject(Long projectId);
}