package com.buildflow.document.service;

import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.portal.service.ClientPortalAccessService;
import com.buildflow.project.entity.ProjectMember;
import com.buildflow.project.entity.ProjectRole;
import com.buildflow.project.repository.ProjectMemberRepository;
import com.buildflow.project.service.ProjectAccessService;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DocumentAccessService {

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectAccessService
        projectAccessService;

    private final ProjectMemberRepository
        projectMemberRepository;

    private final ClientPortalAccessService
        clientPortalAccessService;

    public DocumentAccessService(
        OrganizationAccessService
            organizationAccessService,
        ProjectAccessService projectAccessService,
        ProjectMemberRepository
            projectMemberRepository,
        ClientPortalAccessService
            clientPortalAccessService
    ) {
        this.organizationAccessService =
            organizationAccessService;

        this.projectAccessService =
            projectAccessService;

        this.projectMemberRepository =
            projectMemberRepository;

        this.clientPortalAccessService =
            clientPortalAccessService;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireView(
        UUID organizationId,
        UUID projectId,
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
                == OrganizationRole.CLIENT
        ) {
            clientPortalAccessService
                .requireProject(
                    organizationId,
                    userId,
                    projectId
                );

            return membership;
        }

        projectAccessService.requireVisible(
            organizationId,
            projectId,
            userId
        );

        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership
        requireDocumentManage(
            UUID organizationId,
            UUID projectId,
            UUID userId
        ) {

        return requireManage(
            organizationId,
            projectId,
            userId,
            false
        );
    }

    @Transactional(readOnly = true)
    public OrganizationMembership
        requirePhotoManage(
            UUID organizationId,
            UUID projectId,
            UUID userId
        ) {

        return requireManage(
            organizationId,
            projectId,
            userId,
            true
        );
    }

    private OrganizationMembership
        requireManage(
            UUID organizationId,
            UUID projectId,
            UUID userId,
            boolean allowSiteManager
        ) {

        projectAccessService.requireVisible(
            organizationId,
            projectId,
            userId
        );

        OrganizationMembership membership =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        OrganizationRole role =
            membership.getRole();

        if (
            role == OrganizationRole.OWNER
            || role == OrganizationRole.ADMIN
        ) {
            return membership;
        }

        if (
            role == OrganizationRole.PROJECT_MANAGER
        ) {
            requireProjectRole(
                organizationId,
                projectId,
                membership,
                ProjectRole.PROJECT_MANAGER
            );

            return membership;
        }

        if (
            allowSiteManager
            && role
                == OrganizationRole.SITE_MANAGER
        ) {
            requireProjectRole(
                organizationId,
                projectId,
                membership,
                ProjectRole.SITE_MANAGER
            );

            return membership;
        }

        throw new BusinessException(
            HttpStatus.FORBIDDEN,
            "DOCUMENT_MANAGEMENT_FORBIDDEN",
            "You do not have permission to manage project files."
        );
    }

    private void requireProjectRole(
        UUID organizationId,
        UUID projectId,
        OrganizationMembership membership,
        ProjectRole role
    ) {
        ProjectMember member =
            projectMemberRepository
                .findByProjectIdAndOrganizationIdAndMembershipIdAndRemovedAtIsNull(
                    projectId,
                    organizationId,
                    membership.getId()
                )
                .orElseThrow(
                    this::forbidden
                );

        if (
            member.getProjectRole()
                != role
        ) {
            throw forbidden();
        }
    }

    private BusinessException forbidden() {
        return new BusinessException(
            HttpStatus.FORBIDDEN,
            "DOCUMENT_PROJECT_ACCESS_FORBIDDEN",
            "You do not have permission to manage project files."
        );
    }
}
