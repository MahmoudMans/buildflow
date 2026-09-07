package com.buildflow.security;

import java.util.UUID;

public record BuildFlowPrincipal(
    UUID userId,
    UUID sessionId,
    String email
) {
}
