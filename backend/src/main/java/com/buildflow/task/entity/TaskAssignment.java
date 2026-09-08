package com.buildflow.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_assignments")
@IdClass(TaskAssignmentId.class)
public class TaskAssignment {

    @Id
    @Column(name = "task_id")
    private UUID taskId;

    @Id
    @Column(name = "membership_id")
    private UUID membershipId;

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
        name = "assigned_at",
        nullable = false
    )
    private Instant assignedAt;

    protected TaskAssignment() {
    }

    public static TaskAssignment create(
        UUID taskId,
        UUID organizationId,
        UUID projectId,
        UUID membershipId,
        Instant now
    ) {
        TaskAssignment assignment =
            new TaskAssignment();

        assignment.taskId =
            taskId;

        assignment.organizationId =
            organizationId;

        assignment.projectId =
            projectId;

        assignment.membershipId =
            membershipId;

        assignment.assignedAt =
            now;

        return assignment;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
