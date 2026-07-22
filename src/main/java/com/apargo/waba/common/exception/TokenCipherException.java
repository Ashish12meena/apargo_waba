package com.apargo.waba.common.exception;

/**
 * Thrown when encrypting or decrypting a stored credential fails - a
 * missing/invalid {@code meta.token.encryption-key}, a corrupted stored
 * value, or a JCE provider failure.
 * <p>
 * Always an operational/configuration problem, never a client input
 * problem - mapped to {@code 500} by {@code GlobalExceptionHandler}.
 */
public class TokenCipherException extends RuntimeException {

    public TokenCipherException(String message) {
        super(message);
    }

    public TokenCipherException(String message, Throwable cause) {
        super(message, cause);
    }
}