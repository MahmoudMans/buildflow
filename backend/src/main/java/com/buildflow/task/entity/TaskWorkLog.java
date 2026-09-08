package com.buildflow.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "task_work_logs")
public class TaskWorkLog {

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
        name = "task_id",
        nullable = false
    )
    private UUID taskId;

    @Column(
        name = "membership_id",
        nullable = false
    )
    private UUID membershipId;

    @Column(
        name = "work_date",
        nullable = false
    )
    private LocalDate workDate;

    @Column(
        name = "minutes_worked",
        nullable = false
    )
    private int minutesWorked;

    private String description;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    protected TaskWorkLog() {
    }

    public static TaskWorkLog create(
        UUID organizationId,
        UUID projectId,
        UUID taskId,
        UUID membershipId,
        LocalDate workDate,
        int minutesWorked,
        String description,
        Instant now
    ) {
        TaskWorkLog log =
            new TaskWorkLog();

        log.id =
            UUID.randomUUID();

        log.organizationId =
            organizationId;

        log.projectId =
            projectId;

        log.taskId =
            taskId;

        log.membershipId =
            membershipId;

        log.workDate =
            workDate;

        log.minutesWorked =
            minutesWorked;

        log.description =
            description;

        log.createdAt =
            now;

        return log;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public int getMinutesWorked() {
        return minutesWorked;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
