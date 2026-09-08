package com.buildflow.expense.service;

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
public class ExpenseAccessService {

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectAccessService
        projectAccessService;

    private final ProjectMemberRepository
        projectMemberRepository;

    public ExpenseAccessService(
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
            throw expenseForbidden();
        }

        return project;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership
        requireCreateOrSubmit(
            UUID organizationId,
            UUID projectId,
            UUID userId
        ) {

        requireView(
            organizationId,
            projectId,
            userId
        );

        return organizationAccessService
            .requireMembership(
                organizationId,
                userId
            );
    }

    @Transactional(readOnly = true)
    public OrganizationMembership
        requireApproval(
            UUID organizationId,
            UUID projectId,
            UUID userId
        ) {

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
                != OrganizationRole.OWNER
            && membership.getRole()
                != OrganizationRole.ADMIN
            && membership.getRole()
                != OrganizationRole.ACCOUNTANT
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "EXPENSE_APPROVAL_FORBIDDEN",
                "You do not have permission to approve or reject expenses."
            );
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
                this::expenseForbidden
            );
    }

    private BusinessException
        expenseForbidden() {

        return new BusinessException(
            HttpStatus.FORBIDDEN,
            "EXPENSE_ACCESS_FORBIDDEN",
            "You do not have permission to access project expenses."
        );
    }
}
