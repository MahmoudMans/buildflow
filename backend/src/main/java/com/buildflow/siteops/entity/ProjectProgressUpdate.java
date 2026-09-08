package com.buildflow.siteops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_progress_updates")
public class ProjectProgressUpdate {

    @Id
    private UUID id;

    @Column(
        name = "organization_id",
        nullable = false
    )
    private UUID organizationId;

    @Column(
        name = "project_id",
        nullable = false
    )
    private UUID projectId;

    @Column(
        name = "progress_percentage_snapshot",
        nullable = false,
        precision = 5,
        scale = 2
    )
    private BigDecimal
        progressPercentageSnapshot;

    @Column(nullable = false)
    private String summary;

    private String blockers;

    @Column(name = "next_steps")
    private String nextSteps;

    @Column(
        name = "created_by",
        nullable = false
    )
    private UUID createdBy;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    protected ProjectProgressUpdate() {
    }

    public static ProjectProgressUpdate create(
        UUID organizationId,
        UUID projectId,
        BigDecimal progressPercentageSnapshot,
        String summary,
        String blockers,
        String nextSteps,
        UUID createdBy,
        Instant now
    ) {
        ProjectProgressUpdate update =
            new ProjectProgressUpdate();

        update.id =
            UUID.randomUUID();

        update.organizationId =
            organizationId;

        update.projectId =
            projectId;

        update.progressPercentageSnapshot =
            progressPercentageSnapshot;

        update.summary =
            summary.trim();

        update.blockers =
            clean(blockers);

        update.nextSteps =
            clean(nextSteps);

        update.createdBy =
            createdBy;

        update.createdAt =
            now;

        return update;
    }

    private static String clean(
        String value
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public BigDecimal
        getProgressPercentageSnapshot() {

        return progressPercentageSnapshot;
    }

    public String getSummary() {
        return summary;
    }

    public String getBlockers() {
        return blockers;
    }

    public String getNextSteps() {
        return nextSteps;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
