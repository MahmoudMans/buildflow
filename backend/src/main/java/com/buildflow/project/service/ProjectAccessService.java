package com.buildflow.project.service;

import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.project.entity.Project;
import com.buildflow.project.entity.ProjectMember;
import com.buildflow.project.entity.ProjectRole;
import com.buildflow.project.repository.ProjectMemberRepository;
import com.buildflow.project.repository.ProjectRepository;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class ProjectAccessService {

    private static final Set<
        OrganizationRole
    > GLOBAL_PROJECT_VIEWERS =
        Set.of(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.ACCOUNTANT
        );

    private final ProjectRepository
        projectRepository;

    private final ProjectMemberRepository
        projectMemberRepository;

    private final OrganizationAccessService
        organizationAccessService;

    public ProjectAccessService(
        ProjectRepository projectRepository,
        ProjectMemberRepository
            projectMemberRepository,
        OrganizationAccessService
            organizationAccessService
    ) {
        this.projectRepository =
            projectRepository;

        this.projectMemberRepository =
            projectMemberRepository;

        this.organizationAccessService =
            organizationAccessService;
    }

    @Transactional(readOnly = true)
    public Project requireVisible(
        UUID organizationId,
        UUID projectId,
        UUID userId
    ) {
        Project project =
            requireProject(
                organizationId,
                projectId
            );

        OrganizationMembership membership =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        if (
            GLOBAL_PROJECT_VIEWERS
                .contains(
                    membership.getRole()
                )
        ) {
            return project;
        }

        if (
            membership.getRole()
                == OrganizationRole.CLIENT
        ) {
            throw projectNotFound();
        }

        boolean member =
            projectMemberRepository
                .existsByProjectIdAndOrganizationIdAndMembershipIdAndRemovedAtIsNull(
                    projectId,
                    organizationId,
                    membership.getId()
                );

        if (!member) {
            throw projectNotFound();
        }

        return project;
    }

    @Transactional(readOnly = true)
    public Project requireManage(
        UUID organizationId,
        UUID projectId,
        UUID userId
    ) {
        Project project =
            requireProject(
                organizationId,
                projectId
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
        ) {
            return project;
        }

        if (
            membership.getRole()
                != OrganizationRole.PROJECT_MANAGER
        ) {
            throw permissionDenied();
        }

        ProjectMember projectMember =
            projectMemberRepository
                .findByProjectIdAndOrganizationIdAndMembershipIdAndRemovedAtIsNull(
                    projectId,
                    organizationId,
                    membership.getId()
                )
                .orElseThrow(
                    this::permissionDenied
                );

        if (
            projectMember.getProjectRole()
                != ProjectRole.PROJECT_MANAGER
        ) {
            throw permissionDenied();
        }

        return project;
    }

    @Transactional(readOnly = true)
    public Project requireProject(
        UUID organizationId,
        UUID projectId
    ) {
        return projectRepository
            .findByIdAndOrganizationId(
                projectId,
                organizationId
            )
            .orElseThrow(
                this::projectNotFound
            );
    }

    private BusinessException
        projectNotFound() {

        return new BusinessException(
            HttpStatus.NOT_FOUND,
            "PROJECT_NOT_FOUND",
            "The project does not exist or is not accessible."
        );
    }

    private BusinessException
        permissionDenied() {

        return new BusinessException(
            HttpStatus.FORBIDDEN,
            "PROJECT_PERMISSION_DENIED",
            "You do not have permission to manage this project."
        );
    }
}
