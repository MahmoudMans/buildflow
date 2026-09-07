package com.buildflow.organization.repository;

import com.buildflow.organization.entity.InvitationStatus;
import com.buildflow.organization.entity.OrganizationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationInvitationRepository
    extends JpaRepository<
        OrganizationInvitation,
        UUID
    > {

    Optional<OrganizationInvitation>
        findByTokenHash(
            String tokenHash
        );

    Optional<OrganizationInvitation>
        findByOrganizationIdAndEmailIgnoreCaseAndStatus(
            UUID organizationId,
            String email,
            InvitationStatus status
        );

    Optional<OrganizationInvitation>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    List<OrganizationInvitation>
        findByOrganizationIdOrderByCreatedAtDesc(
            UUID organizationId
        );
}
