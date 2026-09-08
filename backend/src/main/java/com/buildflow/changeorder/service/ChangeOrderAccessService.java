package com.buildflow.changeorder.service;

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
public class ChangeOrderAccessService {

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectAccessService
        projectAccessService;

    private final ProjectMemberRepository
        projectMemberRepository;

    public ChangeOrderAccessService(
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

        OrganizationRole role =
            membership.getRole();

        if (
            role == OrganizationRole.OWNER
            || role == OrganizationRole.ADMIN
            || role == OrganizationRole.ACCOUNTANT
            || role == OrganizationRole.CLIENT
        ) {
            return membership;
        }

        if (
            role == OrganizationRole.PROJECT_MANAGER
        ) {
            requireProjectManager(
                organizationId,
                projectId,
                membership
            );

            return membership;
        }

        throw forbidden();
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
            case CANCELLED,
                 ARCHIVED ->
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "PROJECT_CLOSED_FOR_CHANGE_ORDERS",
                    "Change orders cannot be modified for this project."
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
            requireProjectManager(
                organizationId,
                projectId,
                membership
            );

            return membership;
        }

        throw forbidden();
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireDecision(
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

        OrganizationRole role =
            membership.getRole();

        if (
            role == OrganizationRole.OWNER
            || role == OrganizationRole.ADMIN
            || role == OrganizationRole.CLIENT
        ) {
            return membership;
        }

        throw new BusinessException(
            HttpStatus.FORBIDDEN,
            "CHANGE_ORDER_DECISION_FORBIDDEN",
            "You do not have permission to approve or reject this change order."
        );
    }

    @Transactional(readOnly = true)
    public OrganizationMembership
        requireFinancialView(
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

        OrganizationRole role =
            membership.getRole();

        if (
            role == OrganizationRole.OWNER
            || role == OrganizationRole.ADMIN
            || role == OrganizationRole.ACCOUNTANT
        ) {
            return membership;
        }

        if (
            role == OrganizationRole.PROJECT_MANAGER
        ) {
            requireProjectManager(
                organizationId,
                projectId,
                membership
            );

            return membership;
        }

        throw new BusinessException(
            HttpStatus.FORBIDDEN,
            "CHANGE_ORDER_FINANCIAL_ACCESS_FORBIDDEN",
            "You do not have permission to view internal change-order financial calculations."
        );
    }

    private void requireProjectManager(
        UUID organizationId,
        UUID projectId,
        OrganizationMembership membership
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
                != ProjectRole.PROJECT_MANAGER
        ) {
            throw forbidden();
        }
    }

    private BusinessException forbidden() {
        return new BusinessException(
            HttpStatus.FORBIDDEN,
            "CHANGE_ORDER_ACCESS_FORBIDDEN",
            "You do not have permission to manage change orders for this project."
        );
    }
}
