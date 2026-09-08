package com.buildflow.portal.repository;

import com.buildflow.portal.entity.ClientPortalAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientPortalAccessRepository
    extends JpaRepository<
        ClientPortalAccess,
        UUID
    > {

    boolean
        existsByOrganizationIdAndMembershipIdAndClientId(
            UUID organizationId,
            UUID membershipId,
            UUID clientId
        );

    Optional<ClientPortalAccess>
        findByOrganizationIdAndMembershipIdAndClientId(
            UUID organizationId,
            UUID membershipId,
            UUID clientId
        );
}
