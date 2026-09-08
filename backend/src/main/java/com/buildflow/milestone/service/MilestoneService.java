package com.buildflow.milestone.service;

import com.buildflow.milestone.dto.MilestoneDtos;
import com.buildflow.milestone.entity.Milestone;
import com.buildflow.milestone.entity.MilestoneStatus;
import com.buildflow.milestone.repository.MilestoneRepository;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.project.entity.Project;
import com.buildflow.project.service.ProjectAccessService;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class MilestoneService {

    private final MilestoneRepository
        milestoneRepository;

    private final ProjectAccessService
        projectAccessService;

    private final OrganizationAccessService
        organizationAccessService;

    public MilestoneService(
        MilestoneRepository
            milestoneRepository,
        ProjectAccessService
            projectAccessService,
        OrganizationAccessService
            organizationAccessService
    ) {
        this.milestoneRepository =
            milestoneRepository;

        this.projectAccessService =
            projectAccessService;

        this.organizationAccessService =
            organizationAccessService;
    }

    @Transactional(readOnly = true)
    public List<
        MilestoneDtos.MilestoneView
    > list(
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

        return milestoneRepository
            .findByProjectIdAndOrganizationIdOrderByPlannedDateAscCreatedAtAsc(
                projectId,
                organizationId
            )
            .stream()
            .map(
                MilestoneDtos
                    .MilestoneView
                    ::from
            )
            .toList();
    }

    @Transactional
    public MilestoneDtos.MilestoneView
        create(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            MilestoneDtos
                .CreateMilestoneRequest
                request
        ) {

        projectAccessService
            .requireManage(
                organizationId,
                projectId,
                userId
            );

        Project project =
            projectAccessService
                .requireProject(
                    organizationId,
                    projectId
                );

        if (
            project.getStatus().name()
                .equals("ARCHIVED")
            || project.getStatus().name()
                .equals("CANCELLED")
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PROJECT_NOT_PLANNABLE",
                "Milestones cannot be added to this project."
            );
        }

        Milestone milestone =
            Milestone.create(
                organizationId,
                projectId,
                request.name().trim(),
                clean(
                    request.description()
                ),
                request.plannedDate(),
                request.clientVisible(),
                Instant.now()
            );

        milestoneRepository.save(
            milestone
        );

        return MilestoneDtos
            .MilestoneView
            .from(milestone);
    }

    @Transactional
    public MilestoneDtos.MilestoneView
        update(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            UUID milestoneId,
            MilestoneDtos
                .UpdateMilestoneRequest
                request
        ) {

        projectAccessService
            .requireManage(
                organizationId,
                projectId,
                userId
            );

        Milestone milestone =
            requireMilestone(
                organizationId,
                projectId,
                milestoneId
            );

        milestone.update(
            cleanPreserveNull(
                request.name()
            ),
            cleanPreserveNull(
                request.description()
            ),
            request.plannedDate(),
            request.progressPercentage(),
            request.clientVisible(),
            Instant.now()
        );

        return MilestoneDtos
            .MilestoneView
            .from(milestone);
    }

    @Transactional
    public MilestoneDtos.MilestoneView
        transition(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            UUID milestoneId,
            MilestoneStatus target
        ) {

        projectAccessService
            .requireManage(
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

        Milestone milestone =
            requireMilestone(
                organizationId,
                projectId,
                milestoneId
            );

        ZoneId zone =
            ZoneId.of(
                membership
                    .getOrganization()
                    .getTimezone()
            );

        milestone.transition(
            target,
            LocalDate.now(zone),
            Instant.now()
        );

        return MilestoneDtos
            .MilestoneView
            .from(milestone);
    }

    private Milestone requireMilestone(
        UUID organizationId,
        UUID projectId,
        UUID milestoneId
    ) {
        return milestoneRepository
            .findByIdAndProjectIdAndOrganizationId(
                milestoneId,
                projectId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "MILESTONE_NOT_FOUND",
                        "The milestone does not exist or is not accessible."
                    )
            );
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
}
