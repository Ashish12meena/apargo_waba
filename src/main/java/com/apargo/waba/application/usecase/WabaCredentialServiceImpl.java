package com.apargo.waba.application.usecase;

import com.apargo.waba.api.response.WabaCredentialResponse;
import com.apargo.waba.application.port.in.WabaCredentialUsecase;
import com.apargo.waba.application.port.out.MetaOAuthTokenRepositoryPort;
import com.apargo.waba.application.port.out.ProjectWabaAssignmentRepositoryPort;
import com.apargo.waba.application.port.out.TokenCipherPort;
import com.apargo.waba.application.port.out.WabaAccountRepositoryPort;
import com.apargo.waba.application.port.out.WabaPhoneNumberRepositoryPort;
import com.apargo.waba.common.exception.CredentialUnavailableException;
import com.apargo.waba.common.exception.ResourceNotFoundException;
import com.apargo.waba.domain.entity.MetaOAuthToken;
import com.apargo.waba.domain.entity.ProjectWabaAssignment;
import com.apargo.waba.domain.entity.WabaAccount;
import com.apargo.waba.domain.entity.WabaPhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link WabaCredentialUsecase}.
 *
 * <h2>Logging discipline</h2>
 *
 * Nothing in this class logs the token, any prefix or suffix of it, or its
 * length. Identifiers only. A token fragment in a log is still a token
 * fragment in whatever aggregator ships those logs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WabaCredentialServiceImpl implements WabaCredentialUsecase {

    private final WabaAccountRepositoryPort wabaAccountRepositoryPort;
    private final WabaPhoneNumberRepositoryPort wabaPhoneNumberRepositoryPort;
    private final ProjectWabaAssignmentRepositoryPort projectWabaAssignmentRepositoryPort;
    private final MetaOAuthTokenRepositoryPort metaOAuthTokenRepositoryPort;
    private final TokenCipherPort tokenCipherPort;

    @Override
    @Transactional(readOnly = true)
    public WabaCredentialResponse resolveByWabaId(Long organizationId, Long projectId, String wabaId) {
        log.info("Resolving credential by wabaId={} organizationId={} projectId={}",
                wabaId, organizationId, projectId);

        WabaAccount account = wabaAccountRepositoryPort.findByWabaId(wabaId)
                .orElseThrow(() -> notFound("WABA not found for wabaId=" + wabaId));

        verifyScope(account, organizationId, projectId);

        MetaOAuthToken token = resolveToken(account);

        return WabaCredentialResponse.builder()
                .organizationId(account.getOrganizationId())
                .wabaAccountId(account.getId())
                .wabaId(account.getWabaId())
                .accessToken(tokenCipherPort.decrypt(token.getAccessToken()))
                .tokenType(token.getTokenType())
                .expiresAt(token.getExpiresAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WabaCredentialResponse resolveByPhoneNumberId(Long organizationId, Long projectId, String phoneNumberId) {
        log.info("Resolving credential by phoneNumberId={} organizationId={} projectId={}",
                phoneNumberId, organizationId, projectId);

        WabaPhoneNumber phoneNumber = wabaPhoneNumberRepositoryPort
                .findByWhatsappPhoneNumberId(phoneNumberId)
                .orElseThrow(() -> notFound("Phone number not found for phoneNumberId=" + phoneNumberId));

        // The phone carries only its parent's internal id, so the account
        // is loaded to reach organizationId — the scope check and the
        // token lookup both hang off it.
        WabaAccount account = wabaAccountRepositoryPort.findById(phoneNumber.getWabaAccountId())
                .orElseThrow(() -> notFound(
                        "Parent WABA not found for phoneNumberId=" + phoneNumberId));

        verifyScope(account, organizationId, projectId);
        verifySendable(phoneNumber);

        MetaOAuthToken token = resolveToken(account);

        return WabaCredentialResponse.builder()
                .organizationId(account.getOrganizationId())
                .wabaAccountId(account.getId())
                .wabaId(account.getWabaId())
                .phoneNumberId(phoneNumber.getWhatsappPhoneNumberId())
                .displayPhoneNumber(phoneNumber.getDisplayPhoneNumber())
                .accessToken(tokenCipherPort.decrypt(token.getAccessToken()))
                .tokenType(token.getTokenType())
                .expiresAt(token.getExpiresAt())
                .build();
    }

    // ----------------------------------------------------
    // Scope verification
    // ----------------------------------------------------

    /**
     * Confirms the caller's asserted tenant scope actually matches the
     * resource. Every failure is reported as "not found" — see
     * {@link WabaCredentialUsecase} for why a 403 would leak more than it
     * is worth.
     */
    private void verifyScope(WabaAccount account, Long organizationId, Long projectId) {
        if (!account.getOrganizationId().equals(organizationId)) {
            log.warn("Scope violation: wabaAccountId={} belongs to organizationId={} but caller asserted {}",
                    account.getId(), account.getOrganizationId(), organizationId);
            throw notFound("WABA not found within organizationId=" + organizationId);
        }

        if (projectId == null) {
            // Not every internal caller works inside a project — a
            // template sync or a webhook reconciler is org-scoped. Absent
            // means "no project narrowing", not "skip the check I should
            // have done": the organization check above always runs.
            return;
        }

        boolean assigned = projectWabaAssignmentRepositoryPort.findByProjectId(projectId)
                .stream()
                .map(ProjectWabaAssignment::getWabaAccountId)
                .anyMatch(id -> id.equals(account.getId()));

        if (!assigned) {
            log.warn("Scope violation: wabaAccountId={} is not assigned to projectId={}",
                    account.getId(), projectId);
            throw notFound("WABA not found within projectId=" + projectId);
        }
    }

    /**
     * Fails a send-path request early when Meta would reject it anyway —
     * a disabled or unverified number produces a Graph API error that is
     * far harder to interpret at the call site than this one.
     */
    private void verifySendable(WabaPhoneNumber phoneNumber) {
        if (!phoneNumber.canSendMessages()) {
            throw new CredentialUnavailableException(
                    "Phone number " + phoneNumber.getWhatsappPhoneNumberId()
                            + " cannot send messages (status=" + phoneNumber.getStatus()
                            + ", verification=" + phoneNumber.getVerificationStatus() + ")");
        }
    }

    // ----------------------------------------------------
    // Token resolution
    // ----------------------------------------------------

    /**
     * Finds the Meta connection backing a WABA.
     * <p>
     * {@code meta_oauth_token_id} is the real FK and is preferred. It is
     * nullable, though — a BSP-managed WABA may never have had one, and
     * older rows predate it — so this falls back to the organization's
     * single token ({@code uq_meta_oauth_tokens_org} guarantees at most
     * one). Both paths respect {@code @SQLRestriction}, so a soft-deleted
     * token reads as absent, which is the correct outcome: a revoked
     * connection should not keep serving credentials.
     */
    private MetaOAuthToken resolveToken(WabaAccount account) {
        if (account.getMetaOAuthTokenId() != null) {
            MetaOAuthToken token = metaOAuthTokenRepositoryPort.findById(account.getMetaOAuthTokenId())
                    .orElseThrow(() -> new CredentialUnavailableException(
                            "Meta connection for wabaId=" + account.getWabaId()
                                    + " has been revoked or removed — re-authorization required"));
            return verifyUsable(token, account);
        }

        List<MetaOAuthToken> tokens =
                metaOAuthTokenRepositoryPort.findByOrganizationId(account.getOrganizationId());

        if (tokens.isEmpty()) {
            throw new CredentialUnavailableException(
                    "No Meta connection exists for organizationId=" + account.getOrganizationId()
                            + " — complete onboarding before sending");
        }

        return verifyUsable(tokens.get(0), account);
    }

    /**
     * An expired token is rejected here rather than handed over to fail
     * at Meta. The caller gets an actionable reason, and the failure is
     * attributed to this service's credential state instead of surfacing
     * as an unexplained upstream 401 in the messaging service.
     */
    private MetaOAuthToken verifyUsable(MetaOAuthToken token, WabaAccount account) {
        if (token.isExpired()) {
            log.warn("Token expired for organizationId={} wabaId={} expiredAt={}",
                    account.getOrganizationId(), account.getWabaId(), token.getExpiresAt());
            throw new CredentialUnavailableException(
                    "Meta access token for organizationId=" + account.getOrganizationId()
                            + " expired at " + token.getExpiresAt() + " — re-authorization required");
        }
        return token;
    }

    private ResourceNotFoundException notFound(String message) {
        return new ResourceNotFoundException(message);
    }
}