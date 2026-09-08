package com.buildflow.portal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_portal_accesses")
public class ClientPortalAccess {

    @Id
    private UUID id;

    @Column(
        name = "organization_id",
        nullable = false
    )
    private UUID organizationId;

    @Column(
        name = "client_id",
        nullable = false
    )
    private UUID clientId;

    @Column(
        name = "membership_id",
        nullable = false
    )
    private UUID membershipId;

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

    protected ClientPortalAccess() {
    }

    public static ClientPortalAccess create(
        UUID organizationId,
        UUID clientId,
        UUID membershipId,
        UUID createdBy,
        Instant now
    ) {
        ClientPortalAccess access =
            new ClientPortalAccess();

        access.id =
            UUID.randomUUID();

        access.organizationId =
            organizationId;

        access.clientId =
            clientId;

        access.membershipId =
            membershipId;

        access.createdBy =
            createdBy;

        access.createdAt =
            now;

        return access;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
