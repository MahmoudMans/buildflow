package com.buildflow.project.service;

import com.buildflow.client.entity.Client;
import com.buildflow.client.service.ClientService;
import com.buildflow.organization.entity.MembershipStatus;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.repository.OrganizationMembershipRepository;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.Project;
import com.buildflow.project.entity.ProjectMember;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectRole;
import com.buildflow.project.entity.ProjectStatus;
import com.buildflow.project.repository.ProjectMemberRepository;
import com.buildflow.project.repository.ProjectRepository;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ProjectService {

    private static final Set<
        OrganizationRole
    > PROJECT_CREATORS =
        Set.of(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.PROJECT_MANAGER
        );

    private final ProjectRepository
        projectRepository;

    private final ProjectMemberRepository
        projectMemberRepository;

    private final
        OrganizationMembershipRepository
            membershipRepository;

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectAccessService
        projectAccessService;

    private final ClientService
        clientService;

    private final ProjectNumberService
        projectNumberService;

    public ProjectService(
        ProjectRepository projectRepository,
        ProjectMemberRepository
            projectMemberRepository,
        OrganizationMembershipRepository
            membershipRepository,
        OrganizationAccessService
            organizationAccessService,
        ProjectAccessService
            projectAccessService,
        ClientService clientService,
        ProjectNumberService
            projectNumberService
    ) {
        this.projectRepository =
            projectRepository;

        this.projectMemberRepository =
            projectMemberRepository;

        this.membershipRepository =
            membershipRepository;

        this.organizationAccessService =
            organizationAccessService;

        this.projectAccessService =
            projectAccessService;

        this.clientService =
            clientService;

        this.projectNumberService =
            projectNumberService;
    }

    @Transactional
    public ProjectDtos.ProjectView create(
        UUID organizationId,
        UUID userId,
        ProjectDtos.CreateProjectRequest
            request
    ) {
        OrganizationMembership actor =
            organizationAccessService
                .requireAnyRole(
                    organizationId,
                    userId,
                    PROJECT_CREATORS
                );

        Client client =
            clientService.requireClient(
                organizationId,
                request.clientId()
            );

        validateDates(
            request.startDate(),
            request.plannedEndDate()
        );

        String currency =
            normalizeCurrency(
                request.currency() == null
                    ? actor
                        .getOrganization()
                        .getCurrency()
                    : request.currency()
            );

        UUID managerMembershipId =
            request
                .projectManagerMembershipId();

        if (
            managerMembershipId
                == null
            && actor.getRole()
                == OrganizationRole.PROJECT_MANAGER
        ) {
            managerMembershipId =
                actor.getId();
        }

        if (
            managerMembershipId
                != null
        ) {
            requireManagerMembership(
                organizationId,
                managerMembershipId
            );
        }

        ZoneId zoneId =
            ZoneId.of(
                actor
                    .getOrganization()
                    .getTimezone()
            );

        int year =
            LocalDate.now(
                zoneId
            )
            .getYear();

        String projectNumber =
            projectNumberService
                .nextProjectNumber(
                    organizationId,
                    year
                );

        Instant now =
            Instant.now();

        Project project =
            Project.create(
                organizationId,
                request.clientId(),
                projectNumber,
                cleanRequired(
                    request.name()
                ),
                clean(
                    request.description()
                ),
                request.projectType(),
                request.priority() == null
                    ? ProjectPriority.MEDIUM
                    : request.priority(),
                clean(
                    request.siteAddress()
                ),
                clean(
                    request.city()
                ),
                clean(
                    request.postalCode()
                ),
                clean(
                    request.country()
                ),
                request.startDate(),
                request.plannedEndDate(),
                currency,
                managerMembershipId,
                now
            );

        projectRepository.save(
            project
        );

        if (
            managerMembershipId
                != null
        ) {
            ensureProjectMember(
                project,
                managerMembershipId,
                ProjectRole.PROJECT_MANAGER,
                now
            );
        }

        return ProjectDtos
            .ProjectView
            .from(
                project,
                client
            );
    }

    @Transactional(readOnly = true)
    public List<
        ProjectDtos.ProjectSummary
    > list(
        UUID organizationId,
        UUID userId
    ) {
        OrganizationMembership membership =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        List<Project> projects;

        if (
            membership.getRole()
                == OrganizationRole.OWNER
            || membership.getRole()
                == OrganizationRole.ADMIN
            || membership.getRole()
                == OrganizationRole.ACCOUNTANT
        ) {
            projects =
                projectRepository
                    .findByOrganizationIdOrderByCreatedAtDesc(
                        organizationId
                    );
        } else if (
            membership.getRole()
                == OrganizationRole.CLIENT
        ) {
            projects =
                List.of();
        } else {
            List<UUID> projectIds =
                projectMemberRepository
                    .findByOrganizationIdAndMembershipIdAndRemovedAtIsNull(
                        organizationId,
                        membership.getId()
                    )
                    .stream()
                    .map(
                        ProjectMember::getProjectId
                    )
                    .distinct()
                    .toList();

            if (
                projectIds.isEmpty()
            ) {
                projects =
                    List.of();
            } else {
                projects =
                    projectRepository
                        .findByOrganizationIdAndIdInOrderByCreatedAtDesc(
                            organizationId,
                            projectIds
                        );
            }
        }

        return projects
            .stream()
            .map(
                project -> {
                    Client client =
                        clientService
                            .requireClient(
                                organizationId,
                                project.getClientId()
                            );

                    return ProjectDtos
                        .ProjectSummary
                        .from(
                            project,
                            client
                        );
                }
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDtos.ProjectView get(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        Project project =
            projectAccessService
                .requireVisible(
                    organizationId,
                    projectId,
                    userId
                );

        Client client =
            clientService
                .requireClient(
                    organizationId,
                    project.getClientId()
                );

        return ProjectDtos
            .ProjectView
            .from(
                project,
                client
            );
    }

    @Transactional
    public ProjectDtos.ProjectView update(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        ProjectDtos.UpdateProjectRequest
            request
    ) {
        Project project =
            projectAccessService
                .requireManage(
                    organizationId,
                    projectId,
                    userId
                );

        UUID clientId =
            request.clientId();

        if (clientId != null) {
            clientService.requireClient(
                organizationId,
                clientId
            );
        }

        validateDatesForUpdate(
            project,
            request
        );

        String currency =
            request.currency() == null
                ? null
                : normalizeCurrency(
                    request.currency()
                );

        boolean managerSupplied =
            request
                .projectManagerMembershipId()
                != null
            || Boolean.TRUE.equals(
                request.clearProjectManager()
            );

        UUID managerMembershipId =
            null;

        if (
            request
                .projectManagerMembershipId()
                != null
        ) {
            managerMembershipId =
                request
                    .projectManagerMembershipId();

            requireManagerMembership(
                organizationId,
                managerMembershipId
            );
        }

        project.update(
            clientId,
            cleanPreserveNull(
                request.name()
            ),
            cleanPreserveNull(
                request.description()
            ),
            request.projectType(),
            request.priority(),
            cleanPreserveNull(
                request.siteAddress()
            ),
            cleanPreserveNull(
                request.city()
            ),
            cleanPreserveNull(
                request.postalCode()
            ),
            cleanPreserveNull(
                request.country()
            ),
            request.startDate(),
            request.plannedEndDate(),
            currency,
            managerMembershipId,
            managerSupplied,
            Instant.now()
        );

        if (
            managerMembershipId
                != null
        ) {
            ensureProjectMember(
                project,
                managerMembershipId,
                ProjectRole.PROJECT_MANAGER,
                Instant.now()
            );
        }

        Client client =
            clientService.requireClient(
                organizationId,
                project.getClientId()
            );

        return ProjectDtos
            .ProjectView
            .from(
                project,
                client
            );
    }

    @Transactional
    public ProjectDtos.ProjectView transition(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        ProjectStatus target
    ) {
        Project project =
            projectAccessService
                .requireManage(
                    organizationId,
                    projectId,
                    userId
                );

        OrganizationMembership actor =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        ZoneId zone =
            ZoneId.of(
                actor
                    .getOrganization()
                    .getTimezone()
            );

        project.transition(
            target,
            LocalDate.now(zone),
            Instant.now()
        );

        Client client =
            clientService.requireClient(
                organizationId,
                project.getClientId()
            );

        return ProjectDtos
            .ProjectView
            .from(
                project,
                client
            );
    }

    @Transactional(readOnly = true)
    public List<
        ProjectDtos.ProjectMemberView
    > listMembers(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        projectAccessService
            .requireVisible(
                organizationId,
                projectId,
                userId
            );

        return projectMemberRepository
            .findByProjectIdAndOrganizationIdAndRemovedAtIsNullOrderByJoinedAtAsc(
                projectId,
                organizationId
            )
            .stream()
            .map(
                projectMember -> {
                    OrganizationMembership
                        membership =
                            membershipRepository
                                .findByIdAndOrganizationId(
                                    projectMember
                                        .getMembershipId(),
                                    organizationId
                                )
                                .orElseThrow(
                                    () ->
                                        new IllegalStateException(
                                            "Project membership references an unavailable organization membership."
                                        )
                                );

                    return ProjectDtos
                        .ProjectMemberView
                        .from(
                            projectMember,
                            membership
                        );
                }
            )
            .toList();
    }

    @Transactional
    public ProjectDtos.ProjectMemberView
        addMember(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            ProjectDtos.AddProjectMemberRequest
                request
        ) {

        Project project =
            projectAccessService
                .requireManage(
                    organizationId,
                    projectId,
                    userId
                );

        OrganizationMembership membership =
            requireAssignableMembership(
                organizationId,
                request.membershipId()
            );

        if (
            projectMemberRepository
                .existsByProjectIdAndOrganizationIdAndMembershipIdAndRemovedAtIsNull(
                    projectId,
                    organizationId,
                    request.membershipId()
                )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PROJECT_MEMBER_ALREADY_EXISTS",
                "This organization member is already assigned to the project."
            );
        }

        validateProjectRole(
            membership,
            request.projectRole()
        );

        ProjectMember member =
            ProjectMember.create(
                organizationId,
                projectId,
                request.membershipId(),
                request.projectRole(),
                Instant.now()
            );

        projectMemberRepository.save(
            member
        );

        if (
            request.projectRole()
                == ProjectRole.PROJECT_MANAGER
            && project
                .getProjectManagerMembershipId()
                == null
        ) {
            project
                .setProjectManagerMembershipId(
                    membership.getId(),
                    Instant.now()
                );
        }

        return ProjectDtos
            .ProjectMemberView
            .from(
                member,
                membership
            );
    }

    @Transactional
    public ProjectDtos.ProjectMemberView
        changeMemberRole(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            UUID projectMemberId,
            ProjectDtos.ChangeProjectRoleRequest
                request
        ) {

        Project project =
            projectAccessService
                .requireManage(
                    organizationId,
                    projectId,
                    userId
                );

        ProjectMember projectMember =
            projectMemberRepository
                .findByIdAndProjectIdAndOrganizationId(
                    projectMemberId,
                    projectId,
                    organizationId
                )
                .filter(
                    member ->
                        member.getRemovedAt()
                            == null
                )
                .orElseThrow(
                    this::projectMemberNotFound
                );

        OrganizationMembership membership =
            requireAssignableMembership(
                organizationId,
                projectMember
                    .getMembershipId()
            );

        validateProjectRole(
            membership,
            request.projectRole()
        );

        if (
            project
                .getProjectManagerMembershipId()
                != null
            && project
                .getProjectManagerMembershipId()
                .equals(
                    projectMember
                        .getMembershipId()
                )
            && request.projectRole()
                != ProjectRole.PROJECT_MANAGER
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PRIMARY_MANAGER_ROLE_REQUIRED",
                "The primary project manager must keep the PROJECT_MANAGER project role."
            );
        }

        projectMember.changeRole(
            request.projectRole()
        );

        return ProjectDtos
            .ProjectMemberView
            .from(
                projectMember,
                membership
            );
    }

    @Transactional
    public void removeMember(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        UUID projectMemberId
    ) {
        Project project =
            projectAccessService
                .requireManage(
                    organizationId,
                    projectId,
                    userId
                );

        ProjectMember projectMember =
            projectMemberRepository
                .findByIdAndProjectIdAndOrganizationId(
                    projectMemberId,
                    projectId,
                    organizationId
                )
                .filter(
                    member ->
                        member.getRemovedAt()
                            == null
                )
                .orElseThrow(
                    this::projectMemberNotFound
                );

        if (
            project
                .getProjectManagerMembershipId()
                != null
            && project
                .getProjectManagerMembershipId()
                .equals(
                    projectMember
                        .getMembershipId()
                )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PRIMARY_MANAGER_CANNOT_BE_REMOVED",
                "Assign or clear the primary project manager before removing this member."
            );
        }

        projectMember.remove(
            Instant.now()
        );
    }

    private void ensureProjectMember(
        Project project,
        UUID membershipId,
        ProjectRole role,
        Instant now
    ) {
        projectMemberRepository
            .findByProjectIdAndOrganizationIdAndMembershipIdAndRemovedAtIsNull(
                project.getId(),
                project.getOrganizationId(),
                membershipId
            )
            .ifPresentOrElse(
                existing -> {
                    if (
                        existing.getProjectRole()
                            != role
                    ) {
                        existing.changeRole(
                            role
                        );
                    }
                },
                () ->
                    projectMemberRepository
                        .save(
                            ProjectMember.create(
                                project
                                    .getOrganizationId(),
                                project.getId(),
                                membershipId,
                                role,
                                now
                            )
                        )
            );
    }

    private OrganizationMembership
        requireManagerMembership(
            UUID organizationId,
            UUID membershipId
        ) {

        OrganizationMembership membership =
            requireAssignableMembership(
                organizationId,
                membershipId
            );

        if (
            membership.getRole()
                != OrganizationRole.OWNER
            && membership.getRole()
                != OrganizationRole.ADMIN
            && membership.getRole()
                != OrganizationRole.PROJECT_MANAGER
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PROJECT_MANAGER",
                "The selected member cannot be assigned as project manager."
            );
        }

        return membership;
    }

    private OrganizationMembership
        requireAssignableMembership(
            UUID organizationId,
            UUID membershipId
        ) {

        OrganizationMembership membership =
            membershipRepository
                .findByIdAndOrganizationId(
                    membershipId,
                    organizationId
                )
                .orElseThrow(
                    () ->
                        new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "ORGANIZATION_MEMBER_NOT_FOUND",
                            "The selected organization member does not exist."
                        )
                );

        if (
            membership.getStatus()
                != MembershipStatus.ACTIVE
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "ORGANIZATION_MEMBER_INACTIVE",
                "An inactive organization member cannot be assigned to a project."
            );
        }

        if (
            membership.getRole()
                == OrganizationRole.CLIENT
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "CLIENT_CANNOT_JOIN_INTERNAL_TEAM",
                "Client accounts cannot be assigned to the internal project team."
            );
        }

        return membership;
    }

    private void validateProjectRole(
        OrganizationMembership membership,
        ProjectRole projectRole
    ) {
        if (
            projectRole
                == ProjectRole.PROJECT_MANAGER
        ) {
            requireManagerMembership(
                membership
                    .getOrganization()
                    .getId(),
                membership.getId()
            );
        }
    }

    private void validateDates(
        LocalDate startDate,
        LocalDate plannedEndDate
    ) {
        if (
            startDate != null
            && plannedEndDate != null
            && plannedEndDate
                .isBefore(startDate)
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PROJECT_DATES",
                "The planned end date cannot be before the project start date."
            );
        }
    }

    private void validateDatesForUpdate(
        Project project,
        ProjectDtos.UpdateProjectRequest
            request
    ) {
        LocalDate start =
            request.startDate()
                == null
                ? project.getStartDate()
                : request.startDate();

        LocalDate end =
            request.plannedEndDate()
                == null
                ? project.getPlannedEndDate()
                : request.plannedEndDate();

        validateDates(
            start,
            end
        );
    }

    private String normalizeCurrency(
        String value
    ) {
        String currency =
            value
                .trim()
                .toUpperCase(
                    Locale.ROOT
                );

        try {
            Currency.getInstance(
                currency
            );
        } catch (
            IllegalArgumentException exception
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_CURRENCY",
                "The project currency is invalid."
            );
        }

        return currency;
    }

    private String cleanRequired(
        String value
    ) {
        return value.trim();
    }

    private String clean(
        String value
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private String cleanPreserveNull(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private BusinessException
        projectMemberNotFound() {

        return new BusinessException(
            HttpStatus.NOT_FOUND,
            "PROJECT_MEMBER_NOT_FOUND",
            "The project member does not exist."
        );
    }
}
