package com.buildflow.budget.service;

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
public class BudgetAccessService {

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectAccessService
        projectAccessService;

    private final ProjectMemberRepository
        projectMemberRepository;

    public BudgetAccessService(
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
            projectMemberRepository
                .findByProjectIdAndOrganizationIdAndMembershipIdAndRemovedAtIsNull(
                    projectId,
                    organizationId,
                    membership.getId()
                )
                .orElseThrow(
                    this::budgetForbidden
                );

        if (
            projectMember.getProjectRole()
                != ProjectRole.PROJECT_MANAGER
        ) {
            throw budgetForbidden();
        }

        return project;
    }

    @Transactional(readOnly = true)
    public Project requireEdit(
        UUID organizationId,
        UUID projectId,
        UUID userId
    ) {
        Project project =
            projectAccessService
                .requireManage(
                    organizationId,
                    projectId,
                    userId
                );

        if (
            project.isBudgetBaselineLocked()
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "BUDGET_BASELINE_LOCKED",
                "The original project budget is locked. Future budget changes must use change orders."
            );
        }

        return project;
    }

    private BusinessException
        budgetForbidden() {

        return new BusinessException(
            HttpStatus.FORBIDDEN,
            "BUDGET_ACCESS_FORBIDDEN",
            "You do not have permission to access this project budget."
        );
    }
}
