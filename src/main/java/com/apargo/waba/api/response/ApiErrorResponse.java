package com.apargo.waba.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error envelope returned for every failed API call.
 * <p>
 * Every {@code api/v1/*} controller relies on {@code GlobalExceptionHandler}
 * to produce this shape — no controller should hand-build its own error
 * body.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response envelope")
public class ApiErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    /** Populated only for {@code 400} bean-validation failures — field name to violation message. */
    private List<FieldViolation> fieldErrors;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldViolation {
        private String field;
        private String message;
    }
}