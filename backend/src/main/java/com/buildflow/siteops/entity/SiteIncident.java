package com.buildflow.siteops.entity;

import com.buildflow.shared.error.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "site_incidents")
public class SiteIncident {

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
        name = "incident_number",
        nullable = false,
        length = 50
    )
    private String incidentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSeverity severity;

    @Column(
        nullable = false,
        length = 250
    )
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(
        name = "occurred_at",
        nullable = false
    )
    private Instant occurredAt;

    @Column(
        name = "reported_at",
        nullable = false
    )
    private Instant reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Column(
        name = "reported_by",
        nullable = false
    )
    private UUID reportedBy;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    private String resolution;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(
        name = "client_visible",
        nullable = false
    )
    private boolean clientVisible;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected SiteIncident() {
    }

    public static SiteIncident create(
        UUID organizationId,
        UUID projectId,
        String incidentNumber,
        IncidentType type,
        IncidentSeverity severity,
        String title,
        String description,
        Instant occurredAt,
        UUID reportedBy,
        UUID assignedTo,
        boolean clientVisible,
        Instant now
    ) {
        SiteIncident incident =
            new SiteIncident();

        incident.id =
            UUID.randomUUID();

        incident.organizationId =
            organizationId;

        incident.projectId =
            projectId;

        incident.incidentNumber =
            incidentNumber;

        incident.type =
            type;

        incident.severity =
            severity;

        incident.title =
            title.trim();

        incident.description =
            description.trim();

        incident.occurredAt =
            occurredAt == null
                ? now
                : occurredAt;

        incident.reportedAt =
            now;

        incident.status =
            IncidentStatus.OPEN;

        incident.reportedBy =
            reportedBy;

        incident.assignedTo =
            assignedTo;

        incident.clientVisible =
            clientVisible;

        incident.createdAt =
            now;

        incident.updatedAt =
            now;

        return incident;
    }

    public void update(
        IncidentType type,
        IncidentSeverity severity,
        String title,
        String description,
        UUID assignedTo,
        Boolean clientVisible,
        Instant now
    ) {
        requireOpenWorkflow();

        if (type != null) {
            this.type = type;
        }

        if (severity != null) {
            this.severity = severity;
        }

        if (title != null) {
            this.title =
                title.trim();
        }

        if (description != null) {
            this.description =
                description.trim();
        }

        if (assignedTo != null) {
            this.assignedTo =
                assignedTo;
        }

        if (clientVisible != null) {
            this.clientVisible =
                clientVisible;
        }

        updatedAt = now;
    }

    public void investigate(
        Instant now
    ) {
        if (
            status != IncidentStatus.OPEN
            && status
                != IncidentStatus.ACTION_REQUIRED
        ) {
            throw invalidTransition(
                "Only an open or action-required incident can enter investigation."
            );
        }

        status =
            IncidentStatus.INVESTIGATING;

        updatedAt = now;
    }

    public void requireAction(
        Instant now
    ) {
        if (
            status != IncidentStatus.OPEN
            && status
                != IncidentStatus.INVESTIGATING
        ) {
            throw invalidTransition(
                "Only an open or investigating incident can require action."
            );
        }

        status =
            IncidentStatus.ACTION_REQUIRED;

        updatedAt = now;
    }

    public void resolve(
        String resolution,
        Instant now
    ) {
        if (
            status == IncidentStatus.RESOLVED
            || status == IncidentStatus.CLOSED
        ) {
            throw invalidTransition(
                "The incident is already resolved or closed."
            );
        }

        if (
            resolution == null
            || resolution.isBlank()
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INCIDENT_RESOLUTION_REQUIRED",
                "A resolution is required before an incident can be resolved."
            );
        }

        this.resolution =
            resolution.trim();

        resolvedAt = now;

        status =
            IncidentStatus.RESOLVED;

        updatedAt = now;
    }

    public void close(
        Instant now
    ) {
        if (
            status != IncidentStatus.RESOLVED
        ) {
            throw invalidTransition(
                "Only a resolved incident can be closed."
            );
        }

        status =
            IncidentStatus.CLOSED;

        updatedAt = now;
    }

    private void requireOpenWorkflow() {
        if (
            status == IncidentStatus.RESOLVED
            || status == IncidentStatus.CLOSED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "INCIDENT_IMMUTABLE",
                "A resolved or closed incident cannot be modified."
            );
        }
    }

    private BusinessException invalidTransition(
        String message
    ) {
        return new BusinessException(
            HttpStatus.CONFLICT,
            "INVALID_INCIDENT_TRANSITION",
            message
        );
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

    public String getIncidentNumber() {
        return incidentNumber;
    }

    public IncidentType getType() {
        return type;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public UUID getReportedBy() {
        return reportedBy;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public String getResolution() {
        return resolution;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public boolean isClientVisible() {
        return clientVisible;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
