package com.buildflow.siteops.service;

import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.project.entity.Project;
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
public class SiteOperationsAccessService {

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectAccessService
        projectAccessService;

    private final ProjectMemberRepository
        projectMemberRepository;

    public SiteOperationsAccessService(
        OrganizationAccessService
            organizationAccessService,
        ProjectAccessService
            projectAccessService,
        ProjectMemberRepository
            projectMemberRepository
    ) {
        this.organizationAccessService =
            organizationAccessService;

        this.projectAccessService =
            projectAccessService;

        this.projectMemberRepository =
            projectMemberRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireView(
        UUID organizationId,
        UUID projectId,
        UUID userId
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

        if (
            membership.getRole()
                == OrganizationRole.CLIENT
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "SITE_OPERATIONS_CLIENT_ACCESS_FORBIDDEN",
                "Client access to site operations is provided through the client portal."
            );
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireManage(
        UUID organizationId,
        UUID projectId,
        UUID userId
    ) {
        Project project =
            projectAccessService
                .requireVisible(
                    organizationId,
                    projectId,
                    userId
                );

        switch (project.getStatus()) {
            case COMPLETED,
                 CANCELLED,
                 ARCHIVED ->
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "PROJECT_CLOSED_FOR_SITE_OPERATIONS",
                    "Site operations cannot be modified for this project."
                );

            default -> {
            }
        }

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
            role == OrganizationRole.SITE_MANAGER
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
            "SITE_OPERATIONS_MANAGE_FORBIDDEN",
            "You do not have permission to manage site operations for this project."
        );
    }

    private void requireProjectRole(
        UUID organizationId,
        UUID projectId,
        OrganizationMembership membership,
        ProjectRole expectedRole
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
                != expectedRole
        ) {
            throw forbidden();
        }
    }

    private BusinessException forbidden() {
        return new BusinessException(
            HttpStatus.FORBIDDEN,
            "SITE_OPERATIONS_PROJECT_ACCESS_FORBIDDEN",
            "You do not have permission to manage site operations for this project."
        );
    }
}
