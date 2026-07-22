package com.apargo.waba.application.port.out;

/**
 * Outbound port for encrypting/decrypting secrets before persistence -
 * currently used for {@code MetaOAuthToken.accessToken} and
 * {@code OnboardingTask.encryptedAccessToken}.
 * <p>
 * Both entities' Javadoc are explicit that plaintext tokens must never be
 * persisted. This port exists so that requirement is enforced at a single
 * choke point rather than left to each call site to remember.
 */
public interface TokenCipherPort {

    /** Encrypts {@code plaintext}, returning a value safe to persist. */
    String encrypt(String plaintext);

    /** Reverses {@link #encrypt(String)}. */
    String decrypt(String ciphertext);
}