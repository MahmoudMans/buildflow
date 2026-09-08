package com.buildflow.task.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_comments")
public class TaskComment {

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
        name = "author_membership_id",
        nullable = false
    )
    private UUID authorMembershipId;

    @Column(nullable = false)
    private String content;

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

    @Column(name = "edited_at")
    private Instant editedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TaskComment() {
    }

    public static TaskComment create(
        UUID organizationId,
        UUID projectId,
        UUID taskId,
        UUID authorMembershipId,
        String content,
        Instant now
    ) {
        TaskComment comment =
            new TaskComment();

        comment.id =
            UUID.randomUUID();

        comment.organizationId =
            organizationId;

        comment.projectId =
            projectId;

        comment.taskId =
            taskId;

        comment.authorMembershipId =
            authorMembershipId;

        comment.content =
            content;

        comment.createdAt =
            now;

        comment.updatedAt =
            now;

        return comment;
    }

    public void edit(
        String content,
        Instant now
    ) {
        this.content =
            content;

        this.updatedAt =
            now;

        this.editedAt =
            now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getAuthorMembershipId() {
        return authorMembershipId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEditedAt() {
        return editedAt;
    }
}
