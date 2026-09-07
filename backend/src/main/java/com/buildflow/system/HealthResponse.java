package com.buildflow.system;

import java.time.Instant;

public record HealthResponse(
    String application,
    String status,
    String version,
    Instant timestamp
) {
}
