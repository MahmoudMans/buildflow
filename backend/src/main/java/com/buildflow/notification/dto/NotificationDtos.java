package com.buildflow.notification.dto;

import java.time.Instant;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record View(
        UUID id,
        String type,
        String title,
        String message,
        String entityType,
        UUID entityId,
        Instant readAt,
        Instant createdAt
    ) {
    }

    public record UnreadCount(
        long count
    ) {
    }
}
