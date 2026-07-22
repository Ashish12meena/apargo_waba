package com.apargo.waba.infrastructure.security;

import com.apargo.waba.application.port.out.TokenCipherPort;
import com.apargo.waba.common.exception.TokenCipherException;
import com.apargo.waba.infrastructure.config.MetaApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implements {@link TokenCipherPort} using AES-256-GCM.
 *
 * <h2>Key</h2>
 * {@code meta.token.encryption-key} (see {@link MetaApiProperties.Token})
 * must be a Base64-encoded 32-byte (256-bit) key, e.g. generated with
 * {@code openssl rand -base64 32}. Supplied via environment variable in
 * every real environment - never committed to source control.
 *
 * <h2>Format</h2>
 * Output is Base64({@code iv (12 bytes)} + {@code ciphertext+tag}) — the IV
 * is random per encryption call and safe to store alongside the
 * ciphertext (GCM does not require IV secrecy, only IV uniqueness per key).
 */
@Slf4j
@Component
public class AesGcmTokenCipherAdapter implements TokenCipherPort {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final MetaApiProperties metaApiProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmTokenCipherAdapter(MetaApiProperties metaApiProperties) {
        this.metaApiProperties = metaApiProperties;
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            SecretKeySpec key = resolveKey();

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (GeneralSecurityException e) {
            log.error("Failed to encrypt token", e);
            throw new TokenCipherException("Token encryption failed", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            SecretKeySpec key = resolveKey();

            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length < GCM_IV_LENGTH_BYTES) {
                throw new TokenCipherException("Stored ciphertext is too short to contain a valid IV");
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] actualCiphertext = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, actualCiphertext, 0, actualCiphertext.length);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plaintext = cipher.doFinal(actualCiphertext);
            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException e) {
            log.error("Failed to decrypt token", e);
            throw new TokenCipherException("Token decryption failed", e);
        }
    }

    private SecretKeySpec resolveKey() {
        String encodedKey = metaApiProperties.getToken().getEncryptionKey();
        if (!StringUtils.hasText(encodedKey)) {
            throw new TokenCipherException("meta.token.encryption-key is not configured");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException e) {
            throw new TokenCipherException("meta.token.encryption-key is not valid Base64", e);
        }
        if (keyBytes.length != 32) {
            throw new TokenCipherException(
                    "meta.token.encryption-key must decode to 32 bytes (256-bit AES key), got " + keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
    }
}