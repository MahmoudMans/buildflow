package com.buildflow.task.service;

import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.project.entity.ProjectMember;
import com.buildflow.project.entity.ProjectRole;
import com.buildflow.project.repository.ProjectMemberRepository;
import com.buildflow.shared.error.BusinessException;
import com.buildflow.task.entity.Task;
import com.buildflow.task.repository.TaskAssignmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TaskAccessService {

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectMemberRepository
        projectMemberRepository;

    private final TaskAssignmentRepository
        assignmentRepository;

    public TaskAccessService(
        OrganizationAccessService
            organizationAccessService,
        ProjectMemberRepository
            projectMemberRepository,
        TaskAssignmentRepository
            assignmentRepository
    ) {
        this.organizationAccessService =
            organizationAccessService;

        this.projectMemberRepository =
            projectMemberRepository;

        this.assignmentRepository =
            assignmentRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requirePlanner(
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
                == OrganizationRole.OWNER
            || membership.getRole()
                == OrganizationRole.ADMIN
        ) {
            return membership;
        }

        ProjectMember projectMember =
            requireActiveProjectMember(
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
            throw taskManagementDenied();
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireOperator(
        Task task,
        UUID userId
    ) {
        OrganizationMembership membership =
            organizationAccessService
                .requireMembership(
                    task.getOrganizationId(),
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

        if (
            membership.getRole()
                == OrganizationRole.ACCOUNTANT
            || membership.getRole()
                == OrganizationRole.CLIENT
        ) {
            throw taskOperationDenied();
        }

        ProjectMember projectMember =
            requireActiveProjectMember(
                task.getOrganizationId(),
                task.getProjectId(),
                membership
            );

        if (
            projectMember.getProjectRole()
                == ProjectRole.PROJECT_MANAGER
            || projectMember.getProjectRole()
                == ProjectRole.SITE_MANAGER
        ) {
            return membership;
        }

        if (
            !assignmentRepository
                .existsByTaskIdAndMembershipId(
                    task.getId(),
                    membership.getId()
                )
        ) {
            throw taskOperationDenied();
        }

        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireCommenter(
        Task task,
        UUID userId
    ) {
        OrganizationMembership membership =
            organizationAccessService
                .requireMembership(
                    task.getOrganizationId(),
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

        if (
            membership.getRole()
                == OrganizationRole.ACCOUNTANT
            || membership.getRole()
                == OrganizationRole.CLIENT
        ) {
            throw taskOperationDenied();
        }

        requireActiveProjectMember(
            task.getOrganizationId(),
            task.getProjectId(),
            membership
        );

        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireWorkLogger(
        Task task,
        UUID userId
    ) {
        OrganizationMembership membership =
            requireCommenter(
                task,
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

        if (
            !assignmentRepository
                .existsByTaskIdAndMembershipId(
                    task.getId(),
                    membership.getId()
                )
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "TASK_WORK_LOG_FORBIDDEN",
                "You must be assigned to the task before recording work."
            );
        }

        return membership;
    }

    private ProjectMember requireActiveProjectMember(
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
                this::taskOperationDenied
            );
    }

    private BusinessException
        taskManagementDenied() {

        return new BusinessException(
            HttpStatus.FORBIDDEN,
            "TASK_MANAGEMENT_FORBIDDEN",
            "You do not have permission to manage project tasks."
        );
    }

    private BusinessException
        taskOperationDenied() {

        return new BusinessException(
            HttpStatus.FORBIDDEN,
            "TASK_OPERATION_FORBIDDEN",
            "You do not have permission to modify this task."
        );
    }
}
