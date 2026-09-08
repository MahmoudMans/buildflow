package com.buildflow.audit.dto;

import java.time.Instant;
import java.util.UUID;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record View(
        UUID id,
        UUID organizationId,
        UUID projectId,
        UUID actorUserId,
        String action,
        String entityType,
        UUID entityId,
        String oldValues,
        String newValues,
        String ipAddress,
        Instant createdAt
    ) {
    }
}
