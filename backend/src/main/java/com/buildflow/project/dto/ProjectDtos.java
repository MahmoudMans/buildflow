package com.buildflow.project.dto;

import com.buildflow.client.entity.Client;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.project.entity.Project;
import com.buildflow.project.entity.ProjectMember;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectRole;
import com.buildflow.project.entity.ProjectStatus;
import com.buildflow.project.entity.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(

        @NotNull
        UUID clientId,

        @NotBlank
        @Size(max = 250)
        String name,

        @Size(max = 5000)
        String description,

        @NotNull
        ProjectType projectType,

        ProjectPriority priority,

        @Size(max = 300)
        String siteAddress,

        @Size(max = 150)
        String city,

        @Size(max = 50)
        String postalCode,

        @Size(max = 150)
        String country,

        LocalDate startDate,

        LocalDate plannedEndDate,

        @Size(max = 3)
        String currency,

        UUID projectManagerMembershipId

    ) {
    }

    public record UpdateProjectRequest(

        UUID clientId,

        @Size(
            min = 1,
            max = 250
        )
        String name,

        @Size(max = 5000)
        String description,

        ProjectType projectType,

        ProjectPriority priority,

        @Size(max = 300)
        String siteAddress,

        @Size(max = 150)
        String city,

        @Size(max = 50)
        String postalCode,

        @Size(max = 150)
        String country,

        LocalDate startDate,

        LocalDate plannedEndDate,

        @Size(max = 3)
        String currency,

        UUID projectManagerMembershipId,

        Boolean clearProjectManager

    ) {
    }

    public record AddProjectMemberRequest(

        @NotNull
        UUID membershipId,

        @NotNull
        ProjectRole projectRole

    ) {
    }

    public record ChangeProjectRoleRequest(

        @NotNull
        ProjectRole projectRole

    ) {
    }

    public record ProjectSummary(
        UUID id,
        String projectNumber,
        String name,
        String clientName,
        ProjectType projectType,
        ProjectStatus status,
        ProjectPriority priority,
        LocalDate startDate,
        LocalDate plannedEndDate,
        BigDecimal progressPercentage,
        String currency,
        Instant createdAt
    ) {
        public static ProjectSummary from(
            Project project,
            Client client
        ) {
            return new ProjectSummary(
                project.getId(),
                project.getProjectNumber(),
                project.getName(),
                client.getDisplayName(),
                project.getProjectType(),
                project.getStatus(),
                project.getPriority(),
                project.getStartDate(),
                project.getPlannedEndDate(),
                project.getProgressPercentage(),
                project.getCurrency(),
                project.getCreatedAt()
            );
        }
    }

    public record ProjectView(
        UUID id,
        UUID clientId,
        String clientName,
        String projectNumber,
        String name,
        String description,
        ProjectType projectType,
        ProjectStatus status,
        ProjectPriority priority,
        String siteAddress,
        String city,
        String postalCode,
        String country,
        LocalDate startDate,
        LocalDate plannedEndDate,
        LocalDate actualEndDate,
        String currency,
        BigDecimal contractValue,
        BigDecimal originalBudget,
        UUID projectManagerMembershipId,
        BigDecimal progressPercentage,
        Instant createdAt,
        Instant archivedAt
    ) {
        public static ProjectView from(
            Project project,
            Client client
        ) {
            return new ProjectView(
                project.getId(),
                project.getClientId(),
                client.getDisplayName(),
                project.getProjectNumber(),
                project.getName(),
                project.getDescription(),
                project.getProjectType(),
                project.getStatus(),
                project.getPriority(),
                project.getSiteAddress(),
                project.getCity(),
                project.getPostalCode(),
                project.getCountry(),
                project.getStartDate(),
                project.getPlannedEndDate(),
                project.getActualEndDate(),
                project.getCurrency(),
                project.getContractValue(),
                project.getOriginalBudget(),
                project
                    .getProjectManagerMembershipId(),
                project.getProgressPercentage(),
                project.getCreatedAt(),
                project.getArchivedAt()
            );
        }
    }

    public record ProjectMemberView(
        UUID id,
        UUID membershipId,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        ProjectRole projectRole,
        Instant joinedAt
    ) {
        public static ProjectMemberView from(
            ProjectMember projectMember,
            OrganizationMembership membership
        ) {
            return new ProjectMemberView(
                projectMember.getId(),
                projectMember.getMembershipId(),
                membership
                    .getUser()
                    .getId(),
                membership
                    .getUser()
                    .getFirstName(),
                membership
                    .getUser()
                    .getLastName(),
                membership
                    .getUser()
                    .getEmail(),
                projectMember.getProjectRole(),
                projectMember.getJoinedAt()
            );
        }
    }
}
