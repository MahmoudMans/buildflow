package com.buildflow.activity.service;

import com.buildflow.activity.dto.ActivityDtos;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.project.service.ProjectAccessService;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectActivityService {

    private final JdbcTemplate jdbcTemplate;

    private final ProjectAccessService
        projectAccessService;

    private final OrganizationAccessService
        organizationAccessService;

    public ProjectActivityService(
        JdbcTemplate jdbcTemplate,
        ProjectAccessService
            projectAccessService,
        OrganizationAccessService
            organizationAccessService
    ) {
        this.jdbcTemplate =
            jdbcTemplate;

        this.projectAccessService =
            projectAccessService;

        this.organizationAccessService =
            organizationAccessService;
    }

    @Transactional(readOnly = true)
    public List<ActivityDtos.View> list(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        projectAccessService.requireVisible(
            organizationId,
            projectId,
            userId
        );

        var membership =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        if (
            membership.getRole()
                == OrganizationRole.CLIENT
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "INTERNAL_ACTIVITY_FEED_FORBIDDEN",
                "The internal project activity feed is not available in the client portal."
            );
        }

        return jdbcTemplate.query(
            """
            SELECT
                id,
                project_id,
                actor_user_id,
                action,
                message,
                entity_type,
                entity_id,
                created_at
            FROM project_activity
            WHERE organization_id = ?
              AND project_id = ?
            ORDER BY created_at DESC
            LIMIT 200
            """,
            (resultSet, rowNumber) ->
                new ActivityDtos.View(
                    resultSet.getObject(
                        "id",
                        UUID.class
                    ),
                    resultSet.getObject(
                        "project_id",
                        UUID.class
                    ),
                    resultSet.getObject(
                        "actor_user_id",
                        UUID.class
                    ),
                    resultSet.getString(
                        "action"
                    ),
                    resultSet.getString(
                        "message"
                    ),
                    resultSet.getString(
                        "entity_type"
                    ),
                    resultSet.getObject(
                        "entity_id",
                        UUID.class
                    ),
                    instant(
                        resultSet.getTimestamp(
                            "created_at"
                        )
                    )
                ),
            organizationId,
            projectId
        );
    }

    private static Instant instant(
        Timestamp value
    ) {
        return value == null
            ? null
            : value.toInstant();
    }
}
