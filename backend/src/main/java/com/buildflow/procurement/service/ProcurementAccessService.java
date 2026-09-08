package com.buildflow.procurement.service;

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
public class ProcurementAccessService {

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectAccessService
        projectAccessService;

    private final ProjectMemberRepository
        projectMemberRepository;

    public ProcurementAccessService(
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
    public Project requireView(
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

        OrganizationMembership membership =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        if (
            membership.getRole()
                == OrganizationRole.OWNER
            || membership.getRole()
                == OrganizationRole.ADMIN
            || membership.getRole()
                == OrganizationRole.ACCOUNTANT
        ) {
            return project;
        }

        ProjectMember projectMember =
            requireProjectMember(
                organizationId,
                projectId,
                membership
            );

        if (
            projectMember.getProjectRole()
                != ProjectRole.PROJECT_MANAGER
            && projectMember.getProjectRole()
                != ProjectRole.SITE_MANAGER
        ) {
            throw procurementForbidden();
        }

        return project;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership
        requireManage(
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

        requireProjectOpen(project);

        OrganizationMembership membership =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        if (
            membership.getRole()
                == OrganizationRole.OWNER
            || membership.getRole()
                == OrganizationRole.ADMIN
        ) {
            return membership;
        }

        ProjectMember projectMember =
            requireProjectMember(
                organizationId,
                projectId,
                membership
            );

        if (
            projectMember.getProjectRole()
                != ProjectRole.PROJECT_MANAGER
        ) {
            throw procurementForbidden();
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership
        requireApproval(
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

        requireProjectOpen(project);

        OrganizationMembership membership =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        if (
            membership.getRole()
                != OrganizationRole.OWNER
            && membership.getRole()
                != OrganizationRole.ADMIN
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "PURCHASE_ORDER_APPROVAL_FORBIDDEN",
                "Only organization owners or administrators can approve purchase orders."
            );
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership
        requireReceipt(
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

        requireProjectOpen(project);

        OrganizationMembership membership =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        if (
            membership.getRole()
                == OrganizationRole.OWNER
            || membership.getRole()
                == OrganizationRole.ADMIN
        ) {
            return membership;
        }

        ProjectMember projectMember =
            requireProjectMember(
                organizationId,
                projectId,
                membership
            );

        if (
            projectMember.getProjectRole()
                != ProjectRole.PROJECT_MANAGER
            && projectMember.getProjectRole()
                != ProjectRole.SITE_MANAGER
        ) {
            throw procurementForbidden();
        }

        return membership;
    }

    private ProjectMember requireProjectMember(
        UUID organizationId,
        UUID projectId,
        OrganizationMembership membership
    ) {
        return projectMemberRepository
            .findByProjectIdAndOrganizationIdAndMembershipIdAndRemovedAtIsNull(
                projectId,
                organizationId,
                membership.getId()
            )
            .orElseThrow(
                this::procurementForbidden
            );
    }

    private void requireProjectOpen(
        Project project
    ) {
        switch (
            project.getStatus()
        ) {
            case COMPLETED,
                 CANCELLED,
                 ARCHIVED ->
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "PROJECT_NOT_OPEN_FOR_PROCUREMENT",
                    "Procurement cannot be modified for this project."
                );

            default -> {
            }
        }
    }

    private BusinessException
        procurementForbidden() {

        return new BusinessException(
            HttpStatus.FORBIDDEN,
            "PROCUREMENT_ACCESS_FORBIDDEN",
            "You do not have permission to manage project procurement."
        );
    }
}
