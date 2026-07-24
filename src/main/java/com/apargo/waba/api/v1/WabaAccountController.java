package com.apargo.waba.api.v1;

import com.apargo.waba.api.response.WabaAccountDetailResponse;
import com.apargo.waba.api.response.WabaAccountResponse;
import com.apargo.waba.application.port.in.WabaAccountUsecase;
import com.apargo.waba.common.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/organization/{organizationId}")
    @Operation(
            summary = "List WABA accounts for an organization, with phone numbers",
            description = "Every WABA owned by the organization, each including its registered phone numbers.")
    public ResponseEntity<List<WabaAccountDetailResponse>> listByOrganizationDetail(
            @Parameter(description = "Organization to list WABAs for")
            @PathVariable @NotNull @Positive Long organizationId) {

        log.info("GET /api/v1/waba-accounts/organization/{}", organizationId);
        return ResponseEntity.ok(wabaAccountUsecase.listDetailByOrganization(organizationId));
    }

    @GetMapping("/project/{projectId}")
    @Operation(
            summary = "List WABA accounts for a project, with phone numbers",
            description = "Every WABA assigned to the project (via ProjectWabaAssignment), "
                    + "each including its registered phone numbers.")
    public ResponseEntity<List<WabaAccountDetailResponse>> listByProjectDetail(
            @Parameter(description = "Project to list WABAs for")
            @PathVariable @NotNull @Positive Long projectId) {

        log.info("GET /api/v1/waba-accounts/project/{}", projectId);
        return ResponseEntity.ok(wabaAccountUsecase.listDetailByProject(projectId));
    }

    @GetMapping("/waba/{wabaId}")
    @Operation(
            summary = "Get a WABA account by Meta's WABA id",
            description = "Resolves a WABA using Meta's globally unique WABA id and returns it "
                    + "together with its registered phone numbers.")
    @ApiResponse(responseCode = "200", description = "WABA found")
    @ApiResponse(responseCode = "404", description = "No WABA account exists for the given wabaId")
    public ResponseEntity<WabaAccountDetailResponse> getByWabaId(
            @Parameter(description = "Meta's WABA id", example = "123456789012345")
            @PathVariable @NotBlank String wabaId) {

        log.info("GET /api/v1/waba-accounts/waba/{}", wabaId);
        return ResponseEntity.ok(wabaAccountUsecase.getByWabaId(wabaId));
    }
}