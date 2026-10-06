package com.buildflow.audit.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What the audit log screen reads: one page of entries with the names of the
 * people and projects already filled in, and the values to offer in its
 * filters. The older {@link AuditDtos} stays as it is.
 */
public final class AuditSearchDtos {

    private AuditSearchDtos() {
    }

    /**
     * One recorded action. actorName and actorEmail are null for actions that
     * had no signed-in user. oldValues and newValues are shown as stored,
     * except that password-like and token-like fields are masked.
     */
    public record Entry(
        UUID id,
        UUID projectId,
        String projectName,
        String projectNumber,
        UUID actorUserId,
        String actorName,
        String actorEmail,
        String action,
        String entityType,
        UUID entityId,
        String oldValues,
        String newValues,
        String ipAddress,
        Instant createdAt
    ) {
    }

    /** Newest first. page starts at 0. */
    public record SearchPage(
        List<Entry> items,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    public record ActorOption(
        UUID userId,
        String name,
        String email
    ) {
    }

    public record ProjectOption(
        UUID projectId,
        String name,
        String projectNumber
    ) {
    }

    /** Values that exist in this organization's log. */
    public record Facets(
        List<String> actions,
        List<String> entityTypes,
        List<ActorOption> actors,
        List<ProjectOption> projects
    ) {
    }
}
