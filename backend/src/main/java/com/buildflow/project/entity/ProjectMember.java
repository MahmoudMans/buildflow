package com.buildflow.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_members")
public class ProjectMember {

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
        name = "membership_id",
        nullable = false
    )
    private UUID membershipId;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "project_role",
        nullable = false
    )
    private ProjectRole projectRole;

    @Column(
        name = "joined_at",
        nullable = false
    )
    private Instant joinedAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected ProjectMember() {
    }

    public static ProjectMember create(
        UUID organizationId,
        UUID projectId,
        UUID membershipId,
        ProjectRole projectRole,
        Instant now
    ) {
        ProjectMember member =
            new ProjectMember();

        member.id =
            UUID.randomUUID();

        member.organizationId =
            organizationId;

        member.projectId =
            projectId;

        member.membershipId =
            membershipId;

        member.projectRole =
            projectRole;

        member.joinedAt =
            now;

        return member;
    }

    public void changeRole(
        ProjectRole projectRole
    ) {
        this.projectRole =
            projectRole;
    }

    public void remove(
        Instant now
    ) {
        this.removedAt =
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

    public UUID getMembershipId() {
        return membershipId;
    }

    public ProjectRole getProjectRole() {
        return projectRole;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }
}
