package com.buildflow.audit.aspect;

import java.util.UUID;

public record TrackedActionEvent(
    UUID organizationId,
    UUID projectId,
    UUID actorUserId,
    String action,
    String entityType,
    UUID entityId,
    String newValues
) {
}
