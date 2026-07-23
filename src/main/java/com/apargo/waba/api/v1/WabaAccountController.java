package com.apargo.waba.api.v1;

import com.apargo.waba.api.response.WabaAccountResponse;
import com.apargo.waba.application.port.in.WabaAccountUsecase;
import com.apargo.waba.common.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read access to {@link com.apargo.waba.domain.entity.WabaAccount}.
 * <p>
 * Contains no business logic — delegates immediately to
 * {@link WabaAccountUsecase}, per {@code docs/rules.md}.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/waba-accounts")
@RequiredArgsConstructor
@Tag(name = "WABA Accounts", description = "Read access to onboarded WhatsApp Business Accounts")
public class WabaAccountController {

    private final WabaAccountUsecase wabaAccountUsecase;

    @GetMapping
    @Operation(
            summary = "List WABA accounts",
            description = "Provide exactly one of organizationId or projectId. "
                    + "organizationId returns every WABA owned by the organization; "
                    + "projectId returns every WABA assigned to that project "
                    + "(via ProjectWabaAssignment).")
    public ResponseEntity<List<WabaAccountResponse>> listWabaAccounts(
            @Parameter(description = "Return all WABAs owned by this organization")
            @RequestParam(required = false) @Positive Long organizationId,

            @Parameter(description = "Return all WABAs assigned to this project")
            @RequestParam(required = false) @Positive Long projectId) {

        boolean hasOrg = organizationId != null;
        boolean hasProject = projectId != null;

        if (hasOrg == hasProject) {
            // Both provided, or neither provided — reject either way rather
            // than silently picking one and surprising the caller.
            throw new InvalidRequestException(
                    "Provide exactly one of 'organizationId' or 'projectId', not both or neither.");
        }

        if (hasOrg) {
            log.info("GET /api/v1/waba-accounts organizationId={}", organizationId);
            return ResponseEntity.ok(wabaAccountUsecase.listByOrganization(organizationId));
        }

        log.info("GET /api/v1/waba-accounts projectId={}", projectId);
        return ResponseEntity.ok(wabaAccountUsecase.listByProject(projectId));
    }
}