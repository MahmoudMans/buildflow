package com.buildflow.siteops.service;

import com.buildflow.siteops.dto.SiteOperationDtos;
import com.buildflow.siteops.entity.ProjectProgressUpdate;
import com.buildflow.siteops.repository.ProjectProgressUpdateRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectProgressUpdateService {

    private final ProjectProgressUpdateRepository
        updateRepository;

    private final SiteOperationsAccessService
        accessService;

    private final JdbcTemplate
        jdbcTemplate;

    public ProjectProgressUpdateService(
        ProjectProgressUpdateRepository
            updateRepository,
        SiteOperationsAccessService
            accessService,
        JdbcTemplate jdbcTemplate
    ) {
        this.updateRepository =
            updateRepository;

        this.accessService =
            accessService;

        this.jdbcTemplate =
            jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<
        SiteOperationDtos.ProgressUpdateView
    > list(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        accessService.requireView(
            organizationId,
            projectId,
            userId
        );

        return updateRepository
            .findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
                projectId,
                organizationId
            )
            .stream()
            .map(this::toView)
            .toList();
    }

    @Transactional
    public SiteOperationDtos.ProgressUpdateView
        create(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            SiteOperationDtos
                .CreateProgressUpdateRequest request
        ) {

        accessService.requireManage(
            organizationId,
            projectId,
            userId
        );

        BigDecimal snapshot =
            currentProjectProgress(
                organizationId,
                projectId
            );

        ProjectProgressUpdate update =
            ProjectProgressUpdate.create(
                organizationId,
                projectId,
                snapshot,
                request.summary(),
                request.blockers(),
                request.nextSteps(),
                userId,
                Instant.now()
            );

        update =
            updateRepository.save(
                update
            );

        return toView(update);
    }

    private BigDecimal currentProjectProgress(
        UUID organizationId,
        UUID projectId
    ) {
        BigDecimal value =
            jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(
                    progress_percentage,
                    0
                )
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                """,
                BigDecimal.class,
                projectId,
                organizationId
            );

        if (value == null) {
            return BigDecimal.ZERO
                .setScale(2);
        }

        return value.setScale(
            2,
            RoundingMode.HALF_UP
        );
    }

    private SiteOperationDtos.ProgressUpdateView
        toView(
            ProjectProgressUpdate update
        ) {

        return new SiteOperationDtos
            .ProgressUpdateView(
                update.getId(),
                update.getProjectId(),
                update
                    .getProgressPercentageSnapshot(),
                update.getSummary(),
                update.getBlockers(),
                update.getNextSteps(),
                update.getCreatedBy(),
                update.getCreatedAt()
            );
    }
}
