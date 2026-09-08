package com.buildflow.commercial.service;

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
public class CommercialAccessService {

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectAccessService
        projectAccessService;

    private final ProjectMemberRepository
        projectMemberRepository;

    public CommercialAccessService(
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
            ProjectMember member =
                requireProjectMember(
                    organizationId,
                    projectId,
                    membership
                );

            if (
                member.getProjectRole()
                    == ProjectRole.PROJECT_MANAGER
            ) {
                return membership;
            }
        }

        throw financialForbidden();
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

        requireCommercialProjectOpen(
            project
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
            role != OrganizationRole.OWNER
            && role != OrganizationRole.ADMIN
            && role != OrganizationRole.ACCOUNTANT
        ) {
            throw financialForbidden();
        }

        return membership;
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
            role != OrganizationRole.OWNER
            && role != OrganizationRole.ADMIN
            && role != OrganizationRole.CLIENT
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "QUOTE_DECISION_FORBIDDEN",
                "You do not have permission to accept or reject this quotation."
            );
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requirePayment(
        UUID organizationId,
        UUID projectId,
        UUID userId
    ) {
        return requireManage(
            organizationId,
            projectId,
            userId
        );
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
                this::financialForbidden
            );
    }

    private void requireCommercialProjectOpen(
        Project project
    ) {
        switch (project.getStatus()) {
            case CANCELLED,
                 ARCHIVED ->
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "PROJECT_CLOSED_FOR_COMMERCIAL_WORK",
                    "Commercial records cannot be modified for this project."
                );

            default -> {
            }
        }
    }

    private BusinessException
        financialForbidden() {

        return new BusinessException(
            HttpStatus.FORBIDDEN,
            "COMMERCIAL_ACCESS_FORBIDDEN",
            "You do not have permission to access this project commercial information."
        );
    }
}
