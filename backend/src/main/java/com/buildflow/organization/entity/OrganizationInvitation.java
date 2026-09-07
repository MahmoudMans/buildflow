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
@Table(name = "organization_invitations")
public class OrganizationInvitation {

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

    @Column(
        nullable = false,
        length = 320
    )
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrganizationRole role;

    @Column(
        name = "token_hash",
        nullable = false,
        length = 64
    )
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvitationStatus status;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "invited_by",
        nullable = false
    )
    private AppUser invitedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    private AppUser acceptedBy;

    @Column(
        name = "expires_at",
        nullable = false
    )
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

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

    protected OrganizationInvitation() {
    }

    public static OrganizationInvitation create(
        Organization organization,
        String email,
        OrganizationRole role,
        String tokenHash,
        AppUser invitedBy,
        Instant expiresAt,
        Instant now
    ) {
        OrganizationInvitation invitation =
            new OrganizationInvitation();

        invitation.id = UUID.randomUUID();
        invitation.organization =
            organization;
        invitation.email = email;
        invitation.role = role;
        invitation.tokenHash = tokenHash;
        invitation.status =
            InvitationStatus.PENDING;
        invitation.invitedBy = invitedBy;
        invitation.expiresAt = expiresAt;
        invitation.createdAt = now;
        invitation.updatedAt = now;

        return invitation;
    }

    public void expire(
        Instant now
    ) {
        status = InvitationStatus.EXPIRED;
        updatedAt = now;
    }

    public void revoke(
        Instant now
    ) {
        status = InvitationStatus.REVOKED;
        updatedAt = now;
    }

    public void accept(
        AppUser user,
        Instant now
    ) {
        status = InvitationStatus.ACCEPTED;
        acceptedBy = user;
        acceptedAt = now;
        updatedAt = now;
    }

    public void resend(
        String tokenHash,
        Instant expiresAt,
        Instant now
    ) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.status = InvitationStatus.PENDING;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getEmail() {
        return email;
    }

    public OrganizationRole getRole() {
        return role;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
