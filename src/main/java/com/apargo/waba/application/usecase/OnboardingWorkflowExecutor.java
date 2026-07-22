package com.apargo.waba.application.usecase;

import com.apargo.waba.application.port.out.MetaGraphApiPort;
import com.apargo.waba.application.port.out.MetaOAuthTokenRepositoryPort;
import com.apargo.waba.application.port.out.OnboardingTaskRepositoryPort;
import com.apargo.waba.application.port.out.ProjectWabaAssignmentRepositoryPort;
import com.apargo.waba.application.port.out.TokenCipherPort;
import com.apargo.waba.application.port.out.WabaAccountRepositoryPort;
import com.apargo.waba.application.port.out.WabaPhoneNumberRepositoryPort;
import com.apargo.waba.common.exception.MetaGraphApiException;
import com.apargo.waba.domain.entity.MetaOAuthToken;
import com.apargo.waba.domain.entity.OnboardingTask;
import com.apargo.waba.domain.entity.ProjectWabaAssignment;
import com.apargo.waba.domain.entity.WabaAccount;
import com.apargo.waba.domain.entity.WabaPhoneNumber;
import com.apargo.waba.domain.enums.AccountReviewStatus;
import com.apargo.waba.domain.enums.BusinessVerificationStatus;
import com.apargo.waba.domain.enums.MetaTokenType;
import com.apargo.waba.domain.enums.OnboardingStep;
import com.apargo.waba.domain.enums.WabaStatus;
import com.apargo.waba.infrastructure.config.MetaApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes the Embedded Signup onboarding saga off the request thread,
 * driving an {@link OnboardingTask} through every {@link OnboardingStep}.
 *
 * <h2>Why this is a separate bean from {@code OnboardingServiceImpl}</h2>
 * Spring's {@code @Async} proxy only intercepts calls that arrive from
 * <b>outside</b> the bean - self-invocation silently runs synchronously.
 * {@code OnboardingServiceImpl} calls into this bean through a normal
 * injected dependency instead of calling an {@code @Async} method on
 * itself.
 *
 * <h2>Implementation confidence by step</h2>
 * <ul>
 *     <li><b>Implemented against the real Graph API</b>: {@code TOKEN_EXCHANGE},
 *         {@code TOKEN_EXTENSION}, {@code SCOPE_VERIFICATION},
 *         {@code BUSINESS_MANAGER_RESOLUTION}, {@code WABA_RESOLUTION},
 *         {@code PHONE_NUMBER_RESOLUTION}, {@code CREDENTIAL_PERSISTENCE},
 *         {@code WEBHOOK_SUBSCRIPTION}, {@code PHONE_SYNC}. None of these
 *         have been exercised against a live Meta app in this environment -
 *         cross-check exact param/field names against the links in
 *         {@code docs/waba-meta-docs-reference.md} before relying on this
 *         in production.</li>
 *     <li><b>Deliberately not implemented</b>: {@code PHONE_REGISTRATION}
 *         requires a two-step-verification PIN that has no home in the
 *         current API contract ({@code StartOnboardingRequest} has no PIN
 *         field) - faking a default PIN would risk locking a real phone
 *         number, so this step is skipped with a clear log line pointing
 *         at the planned {@code POST /phone-numbers/{id}/register}
 *         endpoint instead.</li>
 *     <li><b>Deliberately no-op</b>: {@code SMB_SYNC} (coexistence-only,
 *         not applicable to a fresh Cloud API number) and
 *         {@code PHASE2_PROVISIONING} (System User upgrade - optional,
 *         out of scope for a first pass).</li>
 * </ul>
 *
 * <p>The task reaches {@code COMPLETED} once a {@link WabaAccount} and
 * {@link WabaPhoneNumber} exist locally - phone <em>registration</em>
 * readiness is tracked separately via
 * {@link WabaPhoneNumber#canSendMessages()}, which correctly stays
 * {@code false} until registration actually happens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnboardingWorkflowExecutor {

    private static final String OAUTH_TOKEN_PATH = "/oauth/access_token";
    private static final String DEBUG_TOKEN_PATH = "/debug_token";

    private final OnboardingTaskRepositoryPort onboardingTaskRepositoryPort;
    private final MetaOAuthTokenRepositoryPort metaOAuthTokenRepositoryPort;
    private final WabaAccountRepositoryPort wabaAccountRepositoryPort;
    private final WabaPhoneNumberRepositoryPort wabaPhoneNumberRepositoryPort;
    private final ProjectWabaAssignmentRepositoryPort projectWabaAssignmentRepositoryPort;
    private final MetaGraphApiPort metaGraphApiPort;
    private final TokenCipherPort tokenCipherPort;
    private final MetaApiProperties metaApiProperties;

    @Async("webhookTaskExecutor")
    public void run(Long taskId) {

        OnboardingTask task = onboardingTaskRepositoryPort.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("OnboardingWorkflowExecutor: task id={} no longer exists — aborting", taskId);
            return;
        }

        try {
            task.start(task.getCurrentStep() != null ? task.getCurrentStep() : OnboardingStep.TOKEN_EXCHANGE);
            onboardingTaskRepositoryPort.save(task);

            // Drive the saga forward one checkpoint at a time, persisting
            // after every step so a crash mid-flow resumes from the last
            // successful checkpoint (see OnboardingStep Javadoc) instead of
            // restarting the whole workflow.
            while (task.isProcessing() && task.getCurrentStep() != null) {
                OnboardingStep before = task.getCurrentStep();
                executeStep(task, before);
                onboardingTaskRepositoryPort.save(task);

                if (task.isCompleted() || task.isFailed()) {
                    break;
                }
                if (task.getCurrentStep() == before) {
                    // A step that neither advanced nor completed/failed the
                    // task would loop forever — treat as a stop, not a bug,
                    // for steps that are deliberate no-ops/skips.
                    log.info("Task id={} did not advance past step={} — stopping this run", task.getId(), before);
                    break;
                }
            }

        } catch (MetaGraphApiException e) {
            log.error("Onboarding task id={} failed at step={}: {}", task.getId(), task.getCurrentStep(), e.getMessage());
            task.fail(e.getMessage());
            onboardingTaskRepositoryPort.save(task);
        } catch (Exception e) {
            log.error("Onboarding task id={} failed unexpectedly at step={}", task.getId(), task.getCurrentStep(), e);
            task.fail("Unexpected error: " + e.getMessage());
            onboardingTaskRepositoryPort.save(task);
        }
    }

    private void executeStep(OnboardingTask task, OnboardingStep step) {
        switch (step) {
            case TOKEN_EXCHANGE -> executeTokenExchange(task);
            case TOKEN_EXTENSION -> executeTokenExtension(task);
            case SCOPE_VERIFICATION -> executeScopeVerification(task);
            case BUSINESS_MANAGER_RESOLUTION -> executeBusinessManagerResolution(task);
            case WABA_RESOLUTION -> executeWabaResolution(task);
            case PHONE_NUMBER_RESOLUTION -> executePhoneNumberResolution(task);
            case CREDENTIAL_PERSISTENCE -> executeCredentialPersistence(task);
            case WEBHOOK_SUBSCRIPTION -> executeWebhookSubscription(task);
            case PHONE_SYNC -> executePhoneSync(task);
            case PHONE_REGISTRATION -> executePhoneRegistration(task);
            case SMB_SYNC -> executeSmbSync(task);
            case PHASE2_PROVISIONING -> executePhase2Provisioning(task);
        }
    }

    // ---------------------------------------------------------------
    // 1. TOKEN_EXCHANGE
    // ---------------------------------------------------------------
    private void executeTokenExchange(OnboardingTask task) {

        Map<String, String> params = new HashMap<>();
        params.put("client_id", metaApiProperties.getAppId());
        params.put("client_secret", metaApiProperties.getAppSecret());
        params.put("code", task.getOauthCode());
        // Required by classic OAuth token exchange so Meta can verify this
        // code was issued for this exact redirect - must match the URI
        // registered in the App Dashboard. NOTE: the JS-SDK/popup variant of
        // Embedded Signup (as opposed to the "Hosted Embedded Signup" redirect
        // variant - see docs/waba-meta-docs-reference.md) may not require or
        // even accept this param; confirm which variant is in use before
        // relying on this call succeeding either with or without it.
        if (metaApiProperties.getOauth().getRedirectUri() != null
                && !metaApiProperties.getOauth().getRedirectUri().isBlank()) {
            params.put("redirect_uri", metaApiProperties.getOauth().getRedirectUri());
        }
        JsonNode response = metaGraphApiPort.postForm(OAUTH_TOKEN_PATH, null, params);

        String accessToken = requireField(response, "access_token", task, "TOKEN_EXCHANGE");
        long expiresIn = response.path("expires_in").asLong(0);

        task.setEncryptedAccessToken(tokenCipherPort.encrypt(accessToken));
        task.setTokenExpiresIn(expiresIn);
        task.moveToStep(OnboardingStep.TOKEN_EXTENSION);

        log.info("Task id={} completed TOKEN_EXCHANGE, moved to {}", task.getId(), task.getCurrentStep());
    }

    // ---------------------------------------------------------------
    // 2. TOKEN_EXTENSION — short-lived user token -> long-lived user token
    // ---------------------------------------------------------------
    private void executeTokenExtension(OnboardingTask task) {

        String shortLivedToken = tokenCipherPort.decrypt(task.getEncryptedAccessToken());

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "fb_exchange_token");
        params.put("client_id", metaApiProperties.getAppId());
        params.put("client_secret", metaApiProperties.getAppSecret());
        params.put("fb_exchange_token", shortLivedToken);

        JsonNode response = metaGraphApiPort.postForm(OAUTH_TOKEN_PATH, null, params);

        String longLivedToken = requireField(response, "access_token", task, "TOKEN_EXTENSION");
        long expiresIn = response.path("expires_in").asLong(0);

        task.setEncryptedAccessToken(tokenCipherPort.encrypt(longLivedToken));
        task.setTokenExpiresIn(expiresIn);
        task.moveToStep(OnboardingStep.SCOPE_VERIFICATION);

        log.info("Task id={} completed TOKEN_EXTENSION, moved to {}", task.getId(), task.getCurrentStep());
    }

    // ---------------------------------------------------------------
    // 3. SCOPE_VERIFICATION — confirm the token actually carries every
    //    permission this service depends on before doing anything else.
    // ---------------------------------------------------------------
    private void executeScopeVerification(OnboardingTask task) {

        String accessToken = tokenCipherPort.decrypt(task.getEncryptedAccessToken());
        String appAccessToken = metaApiProperties.getAppId() + "|" + metaApiProperties.getAppSecret();

        Map<String, String> params = new HashMap<>();
        params.put("input_token", accessToken);

        JsonNode response = metaGraphApiPort.get(DEBUG_TOKEN_PATH, appAccessToken, params);
        JsonNode data = response.path("data");

        List<String> grantedScopes = data.path("scopes").isArray()
                ? data.path("scopes").findValuesAsText("")
                : List.of();

        List<String> requiredScopes = metaApiProperties.getOauth().getScopes();
        List<String> missing = requiredScopes.stream().filter(s -> !grantedScopes.contains(s)).toList();

        if (!missing.isEmpty()) {
            throw new MetaGraphApiException(
                    "Token for task " + task.getId() + " is missing required scopes: " + missing,
                    200, response.toString());
        }

        task.moveToStep(OnboardingStep.BUSINESS_MANAGER_RESOLUTION);
        log.info("Task id={} completed SCOPE_VERIFICATION, moved to {}", task.getId(), task.getCurrentStep());
    }

    // ---------------------------------------------------------------
    // 4. BUSINESS_MANAGER_RESOLUTION
    // ---------------------------------------------------------------
    private void executeBusinessManagerResolution(OnboardingTask task) {

        String accessToken = tokenCipherPort.decrypt(task.getEncryptedAccessToken());

        JsonNode response = metaGraphApiPort.get("/me/businesses", accessToken, Map.of());
        JsonNode businessManager = firstDataElementOrThrow(response, task, "BUSINESS_MANAGER_RESOLUTION");

        String businessManagerId = requireField(businessManager, "id", task, "BUSINESS_MANAGER_RESOLUTION");

        task.setResolvedBusinessManagerId(businessManagerId);
        task.moveToStep(OnboardingStep.WABA_RESOLUTION);

        log.info("Task id={} resolved businessManagerId={}, moved to {}",
                task.getId(), businessManagerId, task.getCurrentStep());
    }

    // ---------------------------------------------------------------
    // 5. WABA_RESOLUTION
    // ---------------------------------------------------------------
    private void executeWabaResolution(OnboardingTask task) {

        String accessToken = tokenCipherPort.decrypt(task.getEncryptedAccessToken());
        String path = "/" + task.getResolvedBusinessManagerId() + "/owned_whatsapp_business_accounts";

        JsonNode response = metaGraphApiPort.get(path, accessToken, Map.of());
        JsonNode waba = firstDataElementOrThrow(response, task, "WABA_RESOLUTION");

        String wabaId = requireField(waba, "id", task, "WABA_RESOLUTION");

        task.setResolvedWabaId(wabaId);
        task.moveToStep(OnboardingStep.PHONE_NUMBER_RESOLUTION);

        log.info("Task id={} resolved wabaId={}, moved to {}", task.getId(), wabaId, task.getCurrentStep());
    }

    // ---------------------------------------------------------------
    // 6. PHONE_NUMBER_RESOLUTION
    // ---------------------------------------------------------------
    private void executePhoneNumberResolution(OnboardingTask task) {

        String accessToken = tokenCipherPort.decrypt(task.getEncryptedAccessToken());
        String path = "/" + task.getResolvedWabaId() + "/phone_numbers";

        JsonNode response = metaGraphApiPort.get(path, accessToken, Map.of());
        JsonNode phone = firstDataElementOrThrow(response, task, "PHONE_NUMBER_RESOLUTION");

        String phoneNumberId = requireField(phone, "id", task, "PHONE_NUMBER_RESOLUTION");

        task.setResolvedPhoneNumberId(phoneNumberId);
        task.moveToStep(OnboardingStep.CREDENTIAL_PERSISTENCE);

        log.info("Task id={} resolved phoneNumberId={}, moved to {}",
                task.getId(), phoneNumberId, task.getCurrentStep());
    }

    // ---------------------------------------------------------------
    // 7. CREDENTIAL_PERSISTENCE — write MetaOAuthToken, WabaAccount,
    //    WabaPhoneNumber locally, and (if the task carries a projectId)
    //    a ProjectWabaAssignment so the initiating project can use the
    //    WABA immediately. Idempotency note: per OnboardingStep
    //    Javadoc this specific step is flagged non-retryable, so every
    //    write here is guarded by a lookup-then-upsert instead of a blind
    //    insert, making it safe to re-run anyway if a crash happens
    //    partway through.
    // ---------------------------------------------------------------
    private void executeCredentialPersistence(OnboardingTask task) {

        String accessToken = tokenCipherPort.decrypt(task.getEncryptedAccessToken());
        String encryptedForStorage = tokenCipherPort.encrypt(accessToken);

        // MetaOAuthToken: the entity enforces one row per organization_id
        // (see uq_meta_oauth_tokens_org) — upsert rather than blind-insert.
        MetaOAuthToken token = metaOAuthTokenRepositoryPort.findByOrganizationId(task.getOrganizationId())
                .stream().findFirst()
                .map(existing -> {
                    existing.setAccessToken(encryptedForStorage);
                    existing.setExpiresAt(task.getTokenExpiresIn() != null
                            ? Instant.now().plusSeconds(task.getTokenExpiresIn()) : null);
                    existing.setBusinessManagerId(task.getResolvedBusinessManagerId());
                    existing.setGrantedAt(Instant.now());
                    return existing;
                })
                .orElseGet(() -> MetaOAuthToken.builder()
                        .organizationId(task.getOrganizationId())
                        .accessToken(encryptedForStorage)
                        .expiresAt(task.getTokenExpiresIn() != null
                                ? Instant.now().plusSeconds(task.getTokenExpiresIn()) : null)
                        .businessManagerId(task.getResolvedBusinessManagerId())
                        .tokenType(MetaTokenType.USER_TOKEN)
                        .grantedAt(Instant.now())
                        .build());
        token = metaOAuthTokenRepositoryPort.save(token);

        // WabaAccount: reuse if this WABA was already persisted by a prior
        // (crashed) attempt at this step.
        Long metaOAuthTokenId = token.getId();
        WabaAccount wabaAccount = wabaAccountRepositoryPort.findByWabaId(task.getResolvedWabaId())
                .orElseGet(() -> WabaAccount.builder()
                        .organizationId(task.getOrganizationId())
                        .metaOAuthTokenId(metaOAuthTokenId)
                        .wabaId(task.getResolvedWabaId())
                        .businessManagerId(task.getResolvedBusinessManagerId())
                        .status(WabaStatus.ACTIVE)
                        .accountReviewStatus(AccountReviewStatus.UNVERIFIED)
                        .businessVerificationStatus(BusinessVerificationStatus.NOT_VERIFIED)
                        .build());
        wabaAccount = wabaAccountRepositoryPort.save(wabaAccount);

        // WabaPhoneNumber: same reuse-if-exists guard.
        Long wabaAccountIdFinal = wabaAccount.getId();
        wabaPhoneNumberRepositoryPort.findByWhatsappPhoneNumberId(task.getResolvedPhoneNumberId())
                .orElseGet(() -> wabaPhoneNumberRepositoryPort.save(
                        WabaPhoneNumber.builder()
                                .wabaAccountId(wabaAccountIdFinal)
                                .whatsappPhoneNumberId(task.getResolvedPhoneNumberId())
                                .build()));

        task.setResultWabaAccountId(wabaAccount.getId());

        assignToProjectIfRequested(task, wabaAccount.getId());

        task.moveToStep(OnboardingStep.WEBHOOK_SUBSCRIPTION);

        log.info("Task id={} persisted wabaAccountId={}, moved to {}",
                task.getId(), wabaAccount.getId(), task.getCurrentStep());
    }

    /**
     * Links the WABA back to the Project that initiated onboarding, per
     * {@link OnboardingTask#getProjectId()}'s Javadoc: "Used for automatic
     * WABA assignment after completion."
     * <p>
     * The project that triggered Embedded Signup should be able to use the
     * WABA it just onboarded without a separate manual API call — this
     * closes that gap. Marked as the project's default assignment since,
     * in this flow, it's the first (and typically only) WABA the project
     * has been onboarded with so far.
     * <p>
     * {@code projectId} is optional on {@link OnboardingTask} — onboarding
     * can be organization-initiated with no specific project in mind, in
     * which case this is a no-op and {@code ProjectWabaAssignment} is left
     * for a separate, explicit assignment API call later.
     * <p>
     * Idempotency: guarded by a lookup before insert, since
     * {@code CREDENTIAL_PERSISTENCE} may be re-entered on a crash/retry —
     * see the class-level Javadoc note on this step's persistence guards.
     */
    private void assignToProjectIfRequested(OnboardingTask task, Long wabaAccountId) {

        Long projectId = task.getProjectId();
        if (projectId == null) {
            log.info("Task id={} has no projectId — skipping automatic ProjectWabaAssignment", task.getId());
            return;
        }

        boolean alreadyAssigned = projectWabaAssignmentRepositoryPort.findByProjectId(projectId).stream()
                .anyMatch(assignment -> assignment.getWabaAccountId().equals(wabaAccountId));

        if (alreadyAssigned) {
            log.info("Task id={}: projectId={} already assigned to wabaAccountId={} — no action needed",
                    task.getId(), projectId, wabaAccountId);
            return;
        }

        ProjectWabaAssignment assignment = ProjectWabaAssignment.builder()
                .projectId(projectId)
                .wabaAccountId(wabaAccountId)
                .defaultAssignment(true)
                .build();
        projectWabaAssignmentRepositoryPort.save(assignment);

        log.info("Task id={}: assigned projectId={} to wabaAccountId={} (default)",
                task.getId(), projectId, wabaAccountId);
    }

    // ---------------------------------------------------------------
    // 8. WEBHOOK_SUBSCRIPTION
    // ---------------------------------------------------------------
    private void executeWebhookSubscription(OnboardingTask task) {

        String accessToken = tokenCipherPort.decrypt(task.getEncryptedAccessToken());
        String path = "/" + task.getResolvedWabaId() + "/subscribed_apps";

        metaGraphApiPort.post(path, accessToken, Map.of());

        task.moveToStep(OnboardingStep.PHONE_SYNC);
        log.info("Task id={} completed WEBHOOK_SUBSCRIPTION, moved to {}", task.getId(), task.getCurrentStep());
    }

    // ---------------------------------------------------------------
    // 9. PHONE_SYNC — re-pull phone numbers and upsert local records with
    //    current Meta-reported operational fields.
    // ---------------------------------------------------------------
    private void executePhoneSync(OnboardingTask task) {

        String accessToken = tokenCipherPort.decrypt(task.getEncryptedAccessToken());
        String path = "/" + task.getResolvedWabaId() + "/phone_numbers";

        JsonNode response = metaGraphApiPort.get(path, accessToken, Map.of());
        JsonNode data = response.path("data");

        int synced = 0;
        if (data.isArray()) {
            for (JsonNode phone : data) {
                String phoneNumberId = phone.path("id").asText(null);
                if (phoneNumberId == null) {
                    continue;
                }
                WabaPhoneNumber entity = wabaPhoneNumberRepositoryPort.findByWhatsappPhoneNumberId(phoneNumberId)
                        .orElseGet(() -> WabaPhoneNumber.builder()
                                .wabaAccountId(task.getResultWabaAccountId())
                                .whatsappPhoneNumberId(phoneNumberId)
                                .build());

                entity.setDisplayPhoneNumber(phone.path("display_phone_number").asText(entity.getDisplayPhoneNumber()));
                entity.setVerifiedName(phone.path("verified_name").asText(entity.getVerifiedName()));
                // NOTE: quality_rating / throughput / name_status field
                // names and enum value mappings need cross-checking against
                // current Graph API response shape before trusting this in prod.

                wabaPhoneNumberRepositoryPort.save(entity);
                synced++;
            }
        }

        task.moveToStep(OnboardingStep.PHONE_REGISTRATION);
        log.info("Task id={} synced {} phone number(s), moved to {}", task.getId(), synced, task.getCurrentStep());
    }

    // ---------------------------------------------------------------
    // 10. PHONE_REGISTRATION — intentionally not executed here.
    // ---------------------------------------------------------------
    private void executePhoneRegistration(OnboardingTask task) {

        // Registration requires a two-step-verification PIN
        // (POST /{phone-number-id}/register {"pin": "..."}) that has no
        // field on StartOnboardingRequest today. Guessing/defaulting a PIN
        // risks locking a real customer phone number, so this step is
        // deliberately skipped rather than faked. Wire this up once the
        // planned POST /api/v1/phone-numbers/{id}/register endpoint
        // (see the architecture doc, §3.3) exists and can collect a PIN
        // from the caller.
        log.info("Task id={}: PHONE_REGISTRATION requires a PIN not yet collected by this API — "
                + "skipping. Register the number explicitly via the phone-numbers API once available.",
                task.getId());

        task.moveToStep(OnboardingStep.SMB_SYNC);
    }

    // ---------------------------------------------------------------
    // 11. SMB_SYNC — optional, coexistence-only step.
    // ---------------------------------------------------------------
    private void executeSmbSync(OnboardingTask task) {

        log.info("Task id={}: SMB_SYNC skipped — only applicable when migrating a number that was "
                + "already active on the WhatsApp Business app (coexistence), not for a fresh Cloud API number.",
                task.getId());

        task.moveToStep(OnboardingStep.PHASE2_PROVISIONING);
    }

    // ---------------------------------------------------------------
    // 12. PHASE2_PROVISIONING — optional System User upgrade, out of scope
    //     for the first pass. Completes the task.
    // ---------------------------------------------------------------
    private void executePhase2Provisioning(OnboardingTask task) {

        log.info("Task id={}: PHASE2_PROVISIONING (System User upgrade) not yet implemented — "
                + "onboarding will complete on the initial USER_TOKEN. See "
                + "MetaOAuthTokenRepositoryPort/#upgrade-to-system-user (planned) to add this later.",
                task.getId());

        task.complete(task.getResultWabaAccountId());
        log.info("Task id={} COMPLETED — wabaAccountId={}", task.getId(), task.getResultWabaAccountId());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private String requireField(JsonNode node, String field, OnboardingTask task, String stepName) {
        String value = node.path(field).asText(null);
        if (value == null) {
            throw new MetaGraphApiException(
                    stepName + " response missing '" + field + "' for task " + task.getId(),
                    200, node.toString());
        }
        return value;
    }

    private JsonNode firstDataElementOrThrow(JsonNode response, OnboardingTask task, String stepName) {
        JsonNode data = response.path("data");
        if (!data.isArray() || data.isEmpty()) {
            throw new MetaGraphApiException(
                    stepName + " returned no results for task " + task.getId(),
                    200, response.toString());
        }
        return data.get(0);
    }
}