package com.apargo.waba.application.usecase;

import com.apargo.waba.api.request.StartOnboardingRequest;
import com.apargo.waba.api.response.OnboardingTaskResponse;
import com.apargo.waba.application.mapper.OnboardingTaskMapper;
import com.apargo.waba.application.port.in.OnboardingUsecase;
import com.apargo.waba.application.port.out.OnboardingTaskRepositoryPort;
import com.apargo.waba.common.exception.IdempotencyKeyConflictException;
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
import java.util.Objects;

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
                .map(existing -> handleReplay(existing, request))
                .orElseGet(() -> createAndStart(request));
    }

    /**
     * A key match was found. Only treat this as a legitimate idempotent
     * replay if the {@code oauthCode} also matches — a reused key with a
     * different oauthCode means the key was reused incorrectly (client bug
     * or collision), and silently returning the old task's result would
     * silently drop the new request instead of surfacing the problem.
     */
    private OnboardingTaskResponse handleReplay(OnboardingTask existing, StartOnboardingRequest request) {

        String idempotencyKey = request.getIdempotencyKey();

        if (!Objects.equals(existing.getOauthCode(), request.getOauthCode())) {
            throw new IdempotencyKeyConflictException(
                    "idempotencyKey '" + idempotencyKey + "' was already used to start onboarding task id="
                            + existing.getId() + " with a different oauthCode. Reusing an idempotency key "
                            + "must represent a retry of the exact same request — generate a new key for "
                            + "a genuinely new onboarding attempt.");
        }

        log.info("Idempotency key {} already used with matching oauthCode — returning existing task id={}",
                idempotencyKey, existing.getId());
        return mapper.toResponse(existing);
    }

    private OnboardingTaskResponse createAndStart(StartOnboardingRequest request) {

        OnboardingTask task = OnboardingTask.builder()
                .organizationId(request.getOrganizationId())
                .projectId(request.getProjectId())
                .oauthCode(request.getOauthCode())
                .idempotencyKey(request.getIdempotencyKey())
                .status(OnboardingStatus.PENDING)
                // Pre-populate resolution results if the client already has
                // them (e.g. waba_id/phone_number_id from the Embedded
                // Signup JS SDK's postMessage event) — the corresponding
                // *_RESOLUTION step in OnboardingWorkflowExecutor checks
                // these and skips its API call when already set.
                // businessManagerId is deliberately NOT accepted from the
                // client — it's always resolved by the backend (see
                // BUSINESS_MANAGER_RESOLUTION), not client-supplied.
                .resolvedWabaId(request.getWabaId())
                .resolvedPhoneNumberId(request.getPhoneNumberId())
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

        // Step-level check, separate from the task-level status/retryCount
        // check above: canRetry() only asks "is a retry attempt allowed at
        // all", isRetryable() asks "is it SAFE to re-run the specific step
        // this task failed at". Currently every step answers true (see
        // OnboardingStep#isRetryable), but this guard is what would
        // actually stop a retry if a future step were added that can't be
        // made idempotent — without this check, that flag would be
        // documentation only and never enforced.
        if (task.getCurrentStep() != null && !task.getCurrentStep().isRetryable()) {
            throw new InvalidOnboardingStateException(
                    "Task " + taskId + " failed at step " + task.getCurrentStep()
                            + ", which does not support automatic retry. Manual intervention is required.");
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