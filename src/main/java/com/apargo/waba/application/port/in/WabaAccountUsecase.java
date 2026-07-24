package com.apargo.waba.application.port.in;

import com.apargo.waba.api.response.WabaAccountDetailResponse;
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

    /**
     * All WABAs belonging to an organization, each including its
     * registered phone numbers.
     */
    List<WabaAccountDetailResponse> listDetailByOrganization(Long organizationId);

    /**
     * All WABAs assigned to a project, resolved through
     * {@link com.apargo.waba.domain.entity.ProjectWabaAssignment}, each
     * including its registered phone numbers.
     */
    List<WabaAccountDetailResponse> listDetailByProject(Long projectId);

    /**
     * A single WABA resolved by Meta's WABA id, including its registered
     * phone numbers.
     *
     * @throws com.apargo.waba.common.exception.ResourceNotFoundException
     *         if no WABA account exists for the given id
     */
    WabaAccountDetailResponse getByWabaId(String wabaId);
}