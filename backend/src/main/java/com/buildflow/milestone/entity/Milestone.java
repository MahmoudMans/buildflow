package com.buildflow.milestone.entity;

import com.buildflow.shared.error.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "milestones")
public class Milestone {

    private static final Map<
        MilestoneStatus,
        Set<MilestoneStatus>
    > ALLOWED_TRANSITIONS =
        Map.of(
            MilestoneStatus.UPCOMING,
            Set.of(
                MilestoneStatus.IN_PROGRESS,
                MilestoneStatus.COMPLETED,
                MilestoneStatus.MISSED,
                MilestoneStatus.CANCELLED
            ),

            MilestoneStatus.IN_PROGRESS,
            Set.of(
                MilestoneStatus.COMPLETED,
                MilestoneStatus.MISSED,
                MilestoneStatus.CANCELLED
            ),

            MilestoneStatus.MISSED,
            Set.of(
                MilestoneStatus.IN_PROGRESS,
                MilestoneStatus.COMPLETED,
                MilestoneStatus.CANCELLED
            ),

            MilestoneStatus.COMPLETED,
            Set.of(),

            MilestoneStatus.CANCELLED,
            Set.of()
        );

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
        nullable = false,
        length = 250
    )
    private String name;

    private String description;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MilestoneStatus status;

    @Column(
        name = "progress_percentage",
        nullable = false,
        precision = 5,
        scale = 2
    )
    private BigDecimal progressPercentage;

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

    protected Milestone() {
    }

    public static Milestone create(
        UUID organizationId,
        UUID projectId,
        String name,
        String description,
        LocalDate plannedDate,
        boolean clientVisible,
        Instant now
    ) {
        Milestone milestone =
            new Milestone();

        milestone.id =
            UUID.randomUUID();

        milestone.organizationId =
            organizationId;

        milestone.projectId =
            projectId;

        milestone.name =
            name;

        milestone.description =
            description;

        milestone.plannedDate =
            plannedDate;

        milestone.status =
            MilestoneStatus.UPCOMING;

        milestone.progressPercentage =
            BigDecimal.ZERO;

        milestone.clientVisible =
            clientVisible;

        milestone.createdAt =
            now;

        milestone.updatedAt =
            now;

        return milestone;
    }

    public void update(
        String name,
        String description,
        LocalDate plannedDate,
        BigDecimal progressPercentage,
        Boolean clientVisible,
        Instant now
    ) {
        if (
            status == MilestoneStatus.COMPLETED
            || status == MilestoneStatus.CANCELLED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "MILESTONE_NOT_EDITABLE",
                "Completed or cancelled milestones cannot be edited."
            );
        }

        if (name != null) {
            this.name =
                name;
        }

        if (description != null) {
            this.description =
                description;
        }

        if (plannedDate != null) {
            this.plannedDate =
                plannedDate;
        }

        if (
            progressPercentage != null
        ) {
            if (
                progressPercentage.compareTo(
                    BigDecimal.ZERO
                ) < 0
                || progressPercentage.compareTo(
                    BigDecimal.valueOf(100)
                ) > 0
            ) {
                throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_MILESTONE_PROGRESS",
                    "Milestone progress must be between 0 and 100."
                );
            }

            this.progressPercentage =
                progressPercentage;
        }

        if (clientVisible != null) {
            this.clientVisible =
                clientVisible;
        }

        this.updatedAt =
            now;
    }

    public void transition(
        MilestoneStatus target,
        LocalDate today,
        Instant now
    ) {
        Set<MilestoneStatus> allowed =
            ALLOWED_TRANSITIONS.get(
                status
            );

        if (
            allowed == null
            || !allowed.contains(target)
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "INVALID_MILESTONE_TRANSITION",
                "Milestone status cannot transition from "
                    + status
                    + " to "
                    + target
                    + "."
            );
        }

        if (
            target == MilestoneStatus.MISSED
        ) {
            if (
                plannedDate == null
                || !plannedDate.isBefore(today)
            ) {
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "MILESTONE_NOT_OVERDUE",
                    "Only an overdue milestone can be marked missed."
                );
            }
        }

        status =
            target;

        if (
            target == MilestoneStatus.IN_PROGRESS
            && progressPercentage.compareTo(
                BigDecimal.ZERO
            ) == 0
        ) {
            progressPercentage =
                BigDecimal.ONE;
        }

        if (
            target == MilestoneStatus.COMPLETED
        ) {
            progressPercentage =
                BigDecimal.valueOf(100);

            completedDate =
                today;
        }

        updatedAt =
            now;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public MilestoneStatus getStatus() {
        return status;
    }

    public BigDecimal getProgressPercentage() {
        return progressPercentage;
    }

    public boolean isClientVisible() {
        return clientVisible;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
