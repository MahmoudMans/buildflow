package com.buildflow.shared.error;

import java.time.Instant;
import java.util.List;

public record ApiError(
    String code,
    String message,
    List<FieldViolation> fieldErrors,
    Instant timestamp,
    String traceId
) {

    public static ApiError create(
        String code,
        String message,
        List<FieldViolation> fieldErrors,
        String traceId
    ) {
        return new ApiError(
            code,
            message,
            List.copyOf(fieldErrors),
            Instant.now(),
            traceId
        );
    }
}
