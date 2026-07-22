package com.apargo.waba.application.usecase;

import com.apargo.waba.api.request.StartOnboardingRequest;
import com.apargo.waba.api.response.OnboardingTaskResponse;
import com.apargo.waba.application.mapper.OnboardingTaskMapper;
import com.apargo.waba.application.port.in.OnboardingUsecase;
import com.apargo.waba.application.port.out.OnboardingTaskRepositoryPort;
import com.apargo.waba.common.exception.InvalidOnboardingStateException;
import com.apargo.waba.common.exception.ResourceNotFoundException;
import com.apargo.waba.domain.entity.OnboardingTask;
import com.apargo.waba.domain.enums.OnboardingStatus;
import com.apargo.waba.domain.enums.OnboardingStep;
import com.apargo.waba.infrastructure.config.OnboardingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link OnboardingUsecase}.
 *
 * <p>Owns task creation, state transitions (retry/cancel/delete) and
 * idempotency handling. Actual Meta Graph API workflow execution is
 * delegated to {@link OnboardingWorkflowExecutor} — kept as a separate bean
 * specifically so its {@code @Async} entry point is invoked through a real
 * injected dependency rather than via self-invocation on this class (see
 * that class's Javadoc for why that distinction matters).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingUsecase {

    private final OnboardingTaskRepositoryPort onboardingTaskRepositoryPort;
    private final OnboardingWorkflowExecutor workflowExecutor;
    private final OnboardingProperties onboardingProperties;
    private final OnboardingTaskMapper mapper;

    @Override
    @Transactional
    public OnboardingTaskResponse startOnboarding(StartOnboardingRequest request) {

        return onboardingTaskRepositoryPort.findByIdempotencyKey(request.getIdempotencyKey())
                .map(existing -> {
                    log.info("Idempotency key {} already used — returning existing task id={}",
                            request.getIdempotencyKey(), existing.getId());
                    return mapper.toResponse(existing);
                })
                .orElseGet(() -> createAndStart(request));
    }

    private OnboardingTaskResponse createAndStart(StartOnboardingRequest request) {

        OnboardingTask task = OnboardingTask.builder()
                .organizationId(request.getOrganizationId())
                .projectId(request.getProjectId())
                .oauthCode(request.getOauthCode())
                .idempotencyKey(request.getIdempotencyKey())
                .status(OnboardingStatus.PENDING)
                .build();

        task = onboardingTaskRepositoryPort.save(task);
        log.info("Created onboarding task id={} organizationId={}", task.getId(), task.getOrganizationId());

        workflowExecutor.run(task.getId());

        return mapper.toResponse(task);
    }

    @Override
    public OnboardingTaskResponse getTask(Long taskId) {
        return mapper.toResponse(findOrThrow(taskId));
    }

    @Override
    public List<OnboardingTaskResponse> listTasks(Long organizationId, OnboardingStatus status) {
        List<OnboardingTask> tasks = (status != null)
                ? onboardingTaskRepositoryPort.findByOrganizationIdAndStatus(organizationId, status)
                : onboardingTaskRepositoryPort.findByOrganizationId(organizationId);

        return tasks.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public OnboardingTaskResponse retryTask(Long taskId) {

        OnboardingTask task = findOrThrow(taskId);

        if (!task.canRetry(onboardingProperties.getMaxRetries())) {
            throw new InvalidOnboardingStateException(
                    "Task " + taskId + " cannot be retried (status=" + task.getStatus()
                            + ", retryCount=" + task.getRetryCount()
                            + ", maxRetries=" + onboardingProperties.getMaxRetries() + ")");
        }

        task.incrementRetry();
        task.start(task.getCurrentStep() != null ? task.getCurrentStep() : OnboardingStep.TOKEN_EXCHANGE);
        task = onboardingTaskRepositoryPort.save(task);

        log.info("Retrying onboarding task id={} from step={} (attempt {}/{})",
                task.getId(), task.getCurrentStep(), task.getRetryCount(), onboardingProperties.getMaxRetries());

        workflowExecutor.run(task.getId());

        return mapper.toResponse(task);
    }

    @Override
    @Transactional
    public OnboardingTaskResponse cancelTask(Long taskId) {

        OnboardingTask task = findOrThrow(taskId);

        if (task.isCompleted()) {
            throw new InvalidOnboardingStateException(
                    "Task " + taskId + " is already COMPLETED and cannot be cancelled");
        }

        task.cancel();
        task = onboardingTaskRepositoryPort.save(task);
        log.info("Cancelled onboarding task id={}", task.getId());

        return mapper.toResponse(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        OnboardingTask task = findOrThrow(taskId);
        task.markDeleted();
        onboardingTaskRepositoryPort.save(task);
        log.info("Soft-deleted onboarding task id={}", taskId);
    }

    private OnboardingTask findOrThrow(Long taskId) {
        return onboardingTaskRepositoryPort.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding task not found: " + taskId));
    }
}