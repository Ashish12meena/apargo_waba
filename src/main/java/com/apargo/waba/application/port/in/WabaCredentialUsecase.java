package com.apargo.waba.application.port.in;

import com.apargo.waba.api.response.WabaCredentialResponse;

/**
 * Inbound port for resolving Meta access tokens on the internal
 * service-to-service surface.
 *
 * <h2>Tenant scoping</h2>
 *
 * Both methods take {@code organizationId} and an optional
 * {@code projectId}. Neither is used to <em>find</em> the credential —
 * {@code wabaId} and {@code phoneNumberId} are globally unique, so the
 * lookup needs nothing else. They are used to <em>verify</em> the caller
 * is entitled to it:
 *
 * <ul>
 *   <li>the WABA must belong to {@code organizationId};</li>
 *   <li>if {@code projectId} is supplied, that project must have a
 *       {@link com.apargo.waba.domain.entity.ProjectWabaAssignment} to
 *       the WABA.</li>
 * </ul>
 *
 * <p>A failed check raises
 * {@link com.apargo.waba.common.exception.ResourceNotFoundException}, not
 * a 403. Returning "exists, but not yours" tells a caller that a given
 * WABA id is real and belongs to somebody — with globally unique Meta ids
 * that is enough to enumerate. Both cases look identical from outside.
 *
 * <h2>Why scoping is checked at all</h2>
 *
 * Without it, a bug in one calling service — a stale {@code wabaId} in a
 * queue message, a mixed-up loop variable — would silently return one
 * tenant's Meta credential to work being done for another. The check is
 * one query and turns that class of bug into a 404 at the boundary.
 */
public interface WabaCredentialUsecase {

    /**
     * The credential for a WABA, addressed by Meta's WABA id. Suitable
     * for account-level Graph API work (templates, WABA settings).
     *
     * @param organizationId tenant the caller asserts it is acting for; required
     * @param projectId      optional project scope; verified when non-null
     * @throws com.apargo.waba.common.exception.ResourceNotFoundException
     *         if no such WABA exists, or it is outside the asserted scope
     * @throws com.apargo.waba.common.exception.CredentialUnavailableException
     *         if the organization has no Meta connection, or its token has expired
     */
    WabaCredentialResponse resolveByWabaId(Long organizationId, Long projectId, String wabaId);

    /**
     * The credential for a specific phone number, addressed by Meta's
     * Phone Number ID. This is the send path: the response carries both
     * the token and the {@code phoneNumberId} that
     * {@code POST /{phone-number-id}/messages} needs, so the caller needs
     * one round trip rather than two.
     *
     * <p>Also asserts the number is actually able to send (active
     * internally and verified by Meta), so a number that would be
     * rejected by Meta fails here with a clear reason instead of as an
     * opaque Graph API error.
     *
     * @param organizationId tenant the caller asserts it is acting for; required
     * @param projectId      optional project scope; verified when non-null
     * @throws com.apargo.waba.common.exception.ResourceNotFoundException
     *         if no such phone number exists, or it is outside the asserted scope
     * @throws com.apargo.waba.common.exception.CredentialUnavailableException
     *         if the number cannot send, the organization has no Meta connection,
     *         or its token has expired
     */
    WabaCredentialResponse resolveByPhoneNumberId(Long organizationId, Long projectId, String phoneNumberId);
}