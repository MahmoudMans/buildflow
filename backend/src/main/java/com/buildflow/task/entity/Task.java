package com.buildflow.task.entity;

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
@Table(name = "tasks")
public class Task {

    private static final Map<
        TaskStatus,
        Set<TaskStatus>
    > ALLOWED_TRANSITIONS =
        Map.of(
            TaskStatus.TODO,
            Set.of(
                TaskStatus.IN_PROGRESS,
                TaskStatus.CANCELLED
            ),

            TaskStatus.IN_PROGRESS,
            Set.of(
                TaskStatus.BLOCKED,
                TaskStatus.REVIEW,
                TaskStatus.DONE,
                TaskStatus.CANCELLED
            ),

            TaskStatus.BLOCKED,
            Set.of(
                TaskStatus.IN_PROGRESS,
                TaskStatus.CANCELLED
            ),

            TaskStatus.REVIEW,
            Set.of(
                TaskStatus.IN_PROGRESS,
                TaskStatus.DONE,
                TaskStatus.CANCELLED
            ),

            TaskStatus.DONE,
            Set.of(
                TaskStatus.IN_PROGRESS
            ),

            TaskStatus.CANCELLED,
            Set.of(
                TaskStatus.TODO
            )
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

    @Column(name = "milestone_id")
    private UUID milestoneId;

    @Column(name = "parent_task_id")
    private UUID parentTaskId;

    @Column(
        nullable = false,
        length = 300
    )
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(
        name = "estimated_hours",
        precision = 10,
        scale = 2
    )
    private BigDecimal estimatedHours;

    @Column(
        name = "progress_percentage",
        nullable = false,
        precision = 5,
        scale = 2
    )
    private BigDecimal progressPercentage;

    @Column(nullable = false)
    private int weight;

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

    @Column(
        name = "updated_at",
        nullable = false
    )
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Task() {
    }

    public static Task create(
        UUID organizationId,
        UUID projectId,
        UUID milestoneId,
        UUID parentTaskId,
        String title,
        String description,
        TaskPriority priority,
        LocalDate startDate,
        LocalDate dueDate,
        BigDecimal estimatedHours,
        int weight,
        UUID createdBy,
        Instant now
    ) {
        validateDates(
            startDate,
            dueDate
        );

        validateWeight(weight);

        if (
            estimatedHours != null
            && estimatedHours.signum() < 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_ESTIMATED_HOURS",
                "Estimated hours cannot be negative."
            );
        }

        Task task =
            new Task();

        task.id =
            UUID.randomUUID();

        task.organizationId =
            organizationId;

        task.projectId =
            projectId;

        task.milestoneId =
            milestoneId;

        task.parentTaskId =
            parentTaskId;

        task.title =
            title;

        task.description =
            description;

        task.status =
            TaskStatus.TODO;

        task.priority =
            priority;

        task.startDate =
            startDate;

        task.dueDate =
            dueDate;

        task.estimatedHours =
            estimatedHours;

        task.progressPercentage =
            BigDecimal.ZERO;

        task.weight =
            weight;

        task.createdBy =
            createdBy;

        task.createdAt =
            now;

        task.updatedAt =
            now;

        return task;
    }

    public void update(
        UUID milestoneId,
        boolean milestoneSupplied,
        String title,
        String description,
        TaskPriority priority,
        LocalDate startDate,
        LocalDate dueDate,
        BigDecimal estimatedHours,
        Integer weight,
        Instant now
    ) {
        if (
            status == TaskStatus.DONE
            || status == TaskStatus.CANCELLED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "TASK_NOT_EDITABLE",
                "Completed or cancelled tasks cannot be edited."
            );
        }

        LocalDate resultingStart =
            startDate == null
                ? this.startDate
                : startDate;

        LocalDate resultingDue =
            dueDate == null
                ? this.dueDate
                : dueDate;

        validateDates(
            resultingStart,
            resultingDue
        );

        if (milestoneSupplied) {
            this.milestoneId =
                milestoneId;
        }

        if (title != null) {
            this.title =
                title;
        }

        if (description != null) {
            this.description =
                description;
        }

        if (priority != null) {
            this.priority =
                priority;
        }

        if (startDate != null) {
            this.startDate =
                startDate;
        }

        if (dueDate != null) {
            this.dueDate =
                dueDate;
        }

        if (estimatedHours != null) {
            if (
                estimatedHours.signum()
                    < 0
            ) {
                throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ESTIMATED_HOURS",
                    "Estimated hours cannot be negative."
                );
            }

            this.estimatedHours =
                estimatedHours;
        }

        if (weight != null) {
            validateWeight(weight);

            this.weight =
                weight;
        }

        this.updatedAt =
            now;
    }

    public void setProgress(
        BigDecimal progress,
        Instant now
    ) {
        if (
            progress == null
            || progress.compareTo(
                BigDecimal.ZERO
            ) < 0
            || progress.compareTo(
                BigDecimal.valueOf(100)
            ) > 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_TASK_PROGRESS",
                "Task progress must be between 0 and 100."
            );
        }

        switch (status) {
            case TODO -> {
                if (
                    progress.compareTo(
                        BigDecimal.ZERO
                    ) != 0
                ) {
                    throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "TODO_PROGRESS_MUST_BE_ZERO",
                        "A TODO task must remain at 0% progress."
                    );
                }
            }

            case IN_PROGRESS -> {
                if (
                    progress.compareTo(
                        BigDecimal.ONE
                    ) < 0
                    || progress.compareTo(
                        BigDecimal.valueOf(99)
                    ) > 0
                ) {
                    throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "INVALID_IN_PROGRESS_VALUE",
                        "An in-progress task must be between 1% and 99%."
                    );
                }
            }

            case BLOCKED ->
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BLOCKED_PROGRESS_LOCKED",
                    "Progress cannot change while a task is blocked."
                );

            case REVIEW -> {
                if (
                    progress.compareTo(
                        BigDecimal.valueOf(90)
                    ) < 0
                    || progress.compareTo(
                        BigDecimal.valueOf(99)
                    ) > 0
                ) {
                    throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "INVALID_REVIEW_PROGRESS",
                        "A task in review must remain between 90% and 99%."
                    );
                }
            }

            case DONE ->
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "DONE_PROGRESS_LOCKED",
                    "A completed task is fixed at 100%."
                );

            case CANCELLED ->
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CANCELLED_TASK_LOCKED",
                    "A cancelled task cannot change progress."
                );
        }

        this.progressPercentage =
            progress;

        this.updatedAt =
            now;
    }

    public void transition(
        TaskStatus target,
        Instant now
    ) {
        Set<TaskStatus> allowed =
            ALLOWED_TRANSITIONS.get(
                status
            );

        if (
            allowed == null
            || !allowed.contains(target)
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "INVALID_TASK_TRANSITION",
                "Task status cannot transition from "
                    + status
                    + " to "
                    + target
                    + "."
            );
        }

        status =
            target;

        switch (target) {
            case TODO -> {
                progressPercentage =
                    BigDecimal.ZERO;

                completedAt =
                    null;
            }

            case IN_PROGRESS -> {
                if (
                    progressPercentage.compareTo(
                        BigDecimal.ZERO
                    ) == 0
                ) {
                    progressPercentage =
                        BigDecimal.ONE;
                }

                if (
                    progressPercentage.compareTo(
                        BigDecimal.valueOf(100)
                    ) == 0
                ) {
                    progressPercentage =
                        BigDecimal.valueOf(99);
                }

                completedAt =
                    null;
            }

            case BLOCKED -> {
            }

            case REVIEW -> {
                if (
                    progressPercentage.compareTo(
                        BigDecimal.valueOf(90)
                    ) < 0
                ) {
                    progressPercentage =
                        BigDecimal.valueOf(90);
                }

                completedAt =
                    null;
            }

            case DONE -> {
                progressPercentage =
                    BigDecimal.valueOf(100);

                completedAt =
                    now;
            }

            case CANCELLED -> {
                completedAt =
                    null;
            }
        }

        updatedAt =
            now;
    }

    private static void validateDates(
        LocalDate startDate,
        LocalDate dueDate
    ) {
        if (
            startDate != null
            && dueDate != null
            && dueDate.isBefore(
                startDate
            )
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_TASK_DATES",
                "The task due date cannot be before the start date."
            );
        }
    }

    private static void validateWeight(
        int weight
    ) {
        if (
            weight < 1
            || weight > 10
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_TASK_WEIGHT",
                "Task weight must be between 1 and 10."
            );
        }
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

    public UUID getMilestoneId() {
        return milestoneId;
    }

    public UUID getParentTaskId() {
        return parentTaskId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getEstimatedHours() {
        return estimatedHours;
    }

    public BigDecimal getProgressPercentage() {
        return progressPercentage;
    }

    public int getWeight() {
        return weight;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
