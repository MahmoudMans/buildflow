package com.buildflow.shared.error;

public record FieldViolation(
    String field,
    String message
) {
}
