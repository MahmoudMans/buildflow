package com.buildflow.activity.dto;

import java.time.Instant;
import java.util.UUID;

public final class ActivityDtos {

    private ActivityDtos() {
    }

    public record View(
        UUID id,
        UUID projectId,
        UUID actorUserId,
        String action,
        String message,
        String entityType,
        UUID entityId,
        Instant createdAt
    ) {
    }
}
