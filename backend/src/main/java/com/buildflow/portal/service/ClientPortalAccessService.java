package com.buildflow.portal.service;

import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.portal.dto.ClientPortalDtos;
import com.buildflow.portal.entity.ClientPortalAccess;
import com.buildflow.portal.repository.ClientPortalAccessRepository;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ClientPortalAccessService {

    private final OrganizationAccessService
        organizationAccessService;

    private final ClientPortalAccessRepository
        repository;

    private final JdbcTemplate
        jdbcTemplate;

    public ClientPortalAccessService(
        OrganizationAccessService
            organizationAccessService,
        ClientPortalAccessRepository repository,
        JdbcTemplate jdbcTemplate
    ) {
        this.organizationAccessService =
            organizationAccessService;

        this.repository =
            repository;

        this.jdbcTemplate =
            jdbcTemplate;
    }

    @Transactional
    public ClientPortalDtos.PortalAccessView link(
        UUID organizationId,
        UUID actorUserId,
        UUID clientId,
        UUID targetUserId
    ) {
        OrganizationMembership actor =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    actorUserId
                );

        if (
            actor.getRole()
                != OrganizationRole.OWNER
            && actor.getRole()
                != OrganizationRole.ADMIN
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "CLIENT_PORTAL_LINK_FORBIDDEN",
                "Only an owner or administrator can authorize client portal access."
            );
        }

        Integer clientExists =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM clients
                WHERE id = ?
                  AND organization_id = ?
                """,
                Integer.class,
                clientId,
                organizationId
            );

        if (
            clientExists == null
            || clientExists == 0
        ) {
            throw new BusinessException(
                HttpStatus.NOT_FOUND,
                "CLIENT_NOT_FOUND",
                "The requested client does not exist."
            );
        }

        OrganizationMembership target =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    targetUserId
                );

        ClientPortalAccess access =
            repository
                .findByOrganizationIdAndMembershipIdAndClientId(
                    organizationId,
                    target.getId(),
                    clientId
                )
                .orElseGet(
                    () ->
                        repository.save(
                            ClientPortalAccess.create(
                                organizationId,
                                clientId,
                                target.getId(),
                                actorUserId,
                                Instant.now()
                            )
                        )
                );

        return new ClientPortalDtos
            .PortalAccessView(
                access.getId(),
                access.getClientId(),
                access.getMembershipId(),
                access.getCreatedAt()
            );
    }

    @Transactional(readOnly = true)
    public OrganizationMembership
        requireClientMembership(
            UUID organizationId,
            UUID userId
        ) {

        OrganizationMembership membership =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        if (
            membership.getRole()
                != OrganizationRole.CLIENT
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "CLIENT_PORTAL_ROLE_REQUIRED",
                "This resource is available only through the client portal."
            );
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public UUID requireProject(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        OrganizationMembership membership =
            requireClientMembership(
                organizationId,
                userId
            );

        UUID clientId =
            jdbcTemplate.query(
                """
                SELECT client_id
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                  AND archived_at IS NULL
                """,
                resultSet ->
                    resultSet.next()
                        ? resultSet.getObject(
                            1,
                            UUID.class
                        )
                        : null,
                projectId,
                organizationId
            );

        if (
            clientId == null
            || !repository
                .existsByOrganizationIdAndMembershipIdAndClientId(
                    organizationId,
                    membership.getId(),
                    clientId
                )
        ) {
            throw new BusinessException(
                HttpStatus.NOT_FOUND,
                "CLIENT_PORTAL_PROJECT_NOT_FOUND",
                "The requested project is not available in your client portal."
            );
        }

        return clientId;
    }
}
