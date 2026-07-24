package com.apargo.waba.application.usecase;

import com.apargo.waba.api.response.WabaAccountDetailResponse;
import com.apargo.waba.api.response.WabaAccountResponse;
import com.apargo.waba.application.mapper.WabaAccountMapper;
import com.apargo.waba.application.port.in.WabaAccountUsecase;
import com.apargo.waba.application.port.out.ProjectWabaAssignmentRepositoryPort;
import com.apargo.waba.application.port.out.WabaAccountRepositoryPort;
import com.apargo.waba.application.port.out.WabaPhoneNumberRepositoryPort;
import com.apargo.waba.common.exception.ResourceNotFoundException;
import com.apargo.waba.domain.entity.ProjectWabaAssignment;
import com.apargo.waba.domain.entity.WabaAccount;
import com.apargo.waba.domain.entity.WabaPhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link WabaAccountUsecase}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WabaAccountServiceImpl implements WabaAccountUsecase {

    private final WabaAccountRepositoryPort wabaAccountRepositoryPort;
    private final WabaPhoneNumberRepositoryPort wabaPhoneNumberRepositoryPort;
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

    @Override
    public List<WabaAccountDetailResponse> listDetailByOrganization(Long organizationId) {
        log.info("Listing WABA account details for organizationId={}", organizationId);
        return toDetailResponses(wabaAccountRepositoryPort.findByOrganizationId(organizationId));
    }

    @Override
    public List<WabaAccountDetailResponse> listDetailByProject(Long projectId) {
        log.info("Listing WABA account details for projectId={}", projectId);

        List<Long> wabaAccountIds = projectWabaAssignmentRepositoryPort.findByProjectId(projectId)
                .stream()
                .map(ProjectWabaAssignment::getWabaAccountId)
                .toList();

        if (wabaAccountIds.isEmpty()) {
            return List.of();
        }

        return toDetailResponses(wabaAccountRepositoryPort.findByIds(wabaAccountIds));
    }

    /**
     * Shared by every "detail" listing method — batch-fetches phone
     * numbers for the given accounts in a single query (instead of one
     * {@code findByWabaAccountId} per account) and maps each account
     * together with its own phone numbers.
     */
    private List<WabaAccountDetailResponse> toDetailResponses(List<WabaAccount> accounts) {
        if (accounts.isEmpty()) {
            return List.of();
        }

        List<Long> accountIds = accounts.stream()
                .map(WabaAccount::getId)
                .toList();

        Map<Long, List<WabaPhoneNumber>> phoneNumbersByAccountId =
                wabaPhoneNumberRepositoryPort.findByWabaAccountIdIn(accountIds)
                        .stream()
                        .collect(Collectors.groupingBy(WabaPhoneNumber::getWabaAccountId));

        return accounts.stream()
                .map(account -> mapper.toDetailResponse(
                        account,
                        phoneNumbersByAccountId.getOrDefault(account.getId(), List.of())))
                .toList();
    }

    @Override
    public WabaAccountDetailResponse getByWabaId(String wabaId) {
        log.info("Fetching WABA account detail for wabaId={}", wabaId);

        WabaAccount account = wabaAccountRepositoryPort.findByWabaId(wabaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "WABA account not found for wabaId=" + wabaId));

        List<WabaPhoneNumber> phoneNumbers =
                wabaPhoneNumberRepositoryPort.findByWabaAccountId(account.getId());

        return mapper.toDetailResponse(account, phoneNumbers);
    }
}