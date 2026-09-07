package com.buildflow.organization.entity;

import com.buildflow.auth.entity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_memberships")
public class OrganizationMembership {

    @Id
    private UUID id;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "organization_id",
        nullable = false
    )
    private Organization organization;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrganizationRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MembershipStatus status;

    @Column(
        name = "joined_at",
        nullable = false
    )
    private Instant joinedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected OrganizationMembership() {
    }

    public static OrganizationMembership create(
        Organization organization,
        AppUser user,
        OrganizationRole role,
        Instant now
    ) {
        OrganizationMembership membership =
            new OrganizationMembership();

        membership.id = UUID.randomUUID();
        membership.organization =
            organization;
        membership.user = user;
        membership.role = role;
        membership.status =
            MembershipStatus.ACTIVE;
        membership.joinedAt = now;

        return membership;
    }

    public void changeRole(
        OrganizationRole role
    ) {
        this.role = role;
    }

    public void deactivate(
        Instant now
    ) {
        status = MembershipStatus.INACTIVE;
        deactivatedAt = now;
    }

    public void activate() {
        status = MembershipStatus.ACTIVE;
        deactivatedAt = null;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public AppUser getUser() {
        return user;
    }

    public OrganizationRole getRole() {
        return role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }
}
