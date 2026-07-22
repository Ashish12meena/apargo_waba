package com.apargo.waba.common.exception;

import com.apargo.waba.api.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

/**
 * Single source of truth for turning exceptions into HTTP responses across
 * every {@code api/v1/*} controller.
 * <p>
 * Keeps controllers free of try/catch noise — a controller method either
 * returns a success response or lets an exception propagate here.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiErrorResponse.FieldViolation> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldViolation)
                .toList();

        log.warn("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);

        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    /**
     * A required {@code @RequestParam} was missing entirely — e.g. Meta (or
     * any caller) omitting {@code hub.mode}/{@code hub.verify_token}/
     * {@code hub.challenge} on the webhook GET handshake. Without this
     * handler such requests fell through to the generic 500 handler below,
     * which is both semantically wrong (this is a client error, not a
     * server error) and unhelpful for debugging a misconfigured caller.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Missing required parameter on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    /** A {@code @RequestParam}/{@code @PathVariable} value couldn't be converted to its target type. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("Type mismatch on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'", request, null);
    }

    /** Request body could not be parsed (malformed JSON, wrong type, etc.). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Unreadable request body on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.info("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidOnboardingStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidState(
            InvalidOnboardingStateException ex, HttpServletRequest request) {

        log.warn("Invalid state transition on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MetaGraphApiException.class)
    public ResponseEntity<ApiErrorResponse> handleGraphApiFailure(
            MetaGraphApiException ex, HttpServletRequest request) {

        // 4xx from Meta (bad token, invalid request) surfaces as 502 to our
        // own caller — it's still "the upstream we depend on failed", not a
        // bad request against *this* API. isRetryable() is available to
        // callers/monitoring via the log line if a distinction matters later.
        log.error("Meta Graph API call failed on {} (retryable={}): {}",
                request.getRequestURI(), ex.isRetryable(), ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, "Upstream WhatsApp Cloud API call failed", request, null);
    }

    @ExceptionHandler(WebhookConfigurationException.class)
    public ResponseEntity<ApiErrorResponse> handleWebhookConfig(
            WebhookConfigurationException ex, HttpServletRequest request) {

        log.error("Webhook configuration error on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Service misconfiguration", request, null);
    }

    @ExceptionHandler(TokenCipherException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenCipher(
            TokenCipherException ex, HttpServletRequest request) {

        log.error("Token cipher error on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Service misconfiguration", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status, String message, HttpServletRequest request,
            List<ApiErrorResponse.FieldViolation> fieldErrors) {

        ApiErrorResponse body = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private ApiErrorResponse.FieldViolation toFieldViolation(FieldError fieldError) {
        return ApiErrorResponse.FieldViolation.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .build();
    }
}