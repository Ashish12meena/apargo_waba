package com.apargo.waba.application.usecase;

import com.apargo.waba.api.response.WabaAccountResponse;
import com.apargo.waba.application.mapper.WabaAccountMapper;
import com.apargo.waba.application.port.in.WabaAccountUsecase;
import com.apargo.waba.application.port.out.ProjectWabaAssignmentRepositoryPort;
import com.apargo.waba.application.port.out.WabaAccountRepositoryPort;
import com.apargo.waba.domain.entity.ProjectWabaAssignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of {@link WabaAccountUsecase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WabaAccountServiceImpl implements WabaAccountUsecase {

    private final WabaAccountRepositoryPort wabaAccountRepositoryPort;
    private final ProjectWabaAssignmentRepositoryPort projectWabaAssignmentRepositoryPort;
    private final WabaAccountMapper mapper;

    @Override
    public List<WabaAccountResponse> listByOrganization(Long organizationId) {
        log.info("Listing WABA accounts for organizationId={}", organizationId);
        return wabaAccountRepositoryPort.findByOrganizationId(organizationId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<WabaAccountResponse> listByProject(Long projectId) {
        log.info("Listing WABA accounts for projectId={}", projectId);

        List<Long> wabaAccountIds = projectWabaAssignmentRepositoryPort.findByProjectId(projectId)
                .stream()
                .map(ProjectWabaAssignment::getWabaAccountId)
                .toList();

        if (wabaAccountIds.isEmpty()) {
            return List.of();
        }

        return wabaAccountRepositoryPort.findByIds(wabaAccountIds)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}