package com.apargo.waba.api.v1;

import com.apargo.waba.api.request.StartOnboardingRequest;
import com.apargo.waba.api.response.OnboardingTaskResponse;
import com.apargo.waba.application.port.in.OnboardingUsecase;
import com.apargo.waba.domain.enums.OnboardingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Drives the Embedded Signup onboarding workflow ({@link
 * com.apargo.waba.domain.entity.OnboardingTask}).
 * <p>
 * Contains no business logic — every method delegates immediately to
 * {@link OnboardingUsecase}, per {@code docs/rules.md} ("api never touches
 * entities or repositories directly").
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "Embedded Signup onboarding workflow")
public class OnboardingController {

    private final OnboardingUsecase onboardingUsecase;

    @PostMapping
    @Operation(
            summary = "Start a new onboarding attempt",
            description = "Creates an OnboardingTask and asynchronously begins the Embedded Signup "
                    + "workflow. Replaying the same idempotencyKey returns the existing task instead "
                    + "of starting a duplicate.")
    @ApiResponse(responseCode = "201", description = "Task created (or existing task returned for a repeated idempotency key)")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    public ResponseEntity<OnboardingTaskResponse> startOnboarding(
            @Valid @RequestBody StartOnboardingRequest request) {

        log.info("POST /api/v1/onboarding organizationId={} idempotencyKey={}",
                request.getOrganizationId(), request.getIdempotencyKey());

        OnboardingTaskResponse response = onboardingUsecase.startOnboarding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get onboarding task status", description = "Poll the current status/step/error of an onboarding task.")
    @ApiResponse(responseCode = "200", description = "Task found")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<OnboardingTaskResponse> getTask(
            @Parameter(description = "Onboarding task id") @PathVariable @NotNull @Positive Long taskId) {

        return ResponseEntity.ok(onboardingUsecase.getTask(taskId));
    }

    @GetMapping
    @Operation(summary = "List onboarding tasks for an organization", description = "Optionally filter by status, for support/debugging.")
    public ResponseEntity<List<OnboardingTaskResponse>> listTasks(
            @Parameter(description = "Organization to list tasks for", required = true)
            @RequestParam @NotNull @Positive Long organizationId,

            @Parameter(description = "Optional status filter")
            @RequestParam(required = false) OnboardingStatus status) {

        return ResponseEntity.ok(onboardingUsecase.listTasks(organizationId, status));
    }

    @PostMapping("/{taskId}/retry")
    @Operation(
            summary = "Retry a failed onboarding task",
            description = "Resumes a FAILED task from its last checkpoint, subject to the configured max retry count.")
    @ApiResponse(responseCode = "200", description = "Retry started")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @ApiResponse(responseCode = "409", description = "Task is not in a retryable state")
    public ResponseEntity<OnboardingTaskResponse> retryTask(
            @PathVariable @NotNull @Positive Long taskId) {

        log.info("POST /api/v1/onboarding/{}/retry", taskId);
        return ResponseEntity.ok(onboardingUsecase.retryTask(taskId));
    }

    @PostMapping("/{taskId}/cancel")
    @Operation(summary = "Cancel an onboarding task", description = "Marks an in-progress/pending task as CANCELLED.")
    @ApiResponse(responseCode = "200", description = "Task cancelled")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @ApiResponse(responseCode = "409", description = "Task already COMPLETED")
    public ResponseEntity<OnboardingTaskResponse> cancelTask(
            @PathVariable @NotNull @Positive Long taskId) {

        log.info("POST /api/v1/onboarding/{}/cancel", taskId);
        return ResponseEntity.ok(onboardingUsecase.cancelTask(taskId));
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Soft-delete an onboarding task record")
    @ApiResponse(responseCode = "204", description = "Task deleted")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<Void> deleteTask(
            @PathVariable @NotNull @Positive Long taskId) {

        log.info("DELETE /api/v1/onboarding/{}", taskId);
        onboardingUsecase.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }
}