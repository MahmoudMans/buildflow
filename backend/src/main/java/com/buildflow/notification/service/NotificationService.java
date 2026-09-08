package com.buildflow.notification.service;

import com.buildflow.notification.dto.NotificationDtos;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private final JdbcTemplate jdbcTemplate;

    private final OrganizationAccessService
        organizationAccessService;

    public NotificationService(
        JdbcTemplate jdbcTemplate,
        OrganizationAccessService
            organizationAccessService
    ) {
        this.jdbcTemplate =
            jdbcTemplate;

        this.organizationAccessService =
            organizationAccessService;
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.View> list(
        UUID organizationId,
        UUID userId
    ) {
        organizationAccessService
            .requireMembership(
                organizationId,
                userId
            );

        return jdbcTemplate.query(
            """
            SELECT
                id,
                type,
                title,
                message,
                entity_type,
                entity_id,
                read_at,
                created_at
            FROM notifications
            WHERE organization_id = ?
              AND user_id = ?
            ORDER BY created_at DESC
            LIMIT 100
            """,
            (resultSet, rowNumber) ->
                new NotificationDtos.View(
                    resultSet.getObject(
                        "id",
                        UUID.class
                    ),
                    resultSet.getString(
                        "type"
                    ),
                    resultSet.getString(
                        "title"
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
                            "read_at"
                        )
                    ),
                    instant(
                        resultSet.getTimestamp(
                            "created_at"
                        )
                    )
                ),
            organizationId,
            userId
        );
    }

    @Transactional(readOnly = true)
    public NotificationDtos.UnreadCount
        unreadCount(
            UUID organizationId,
            UUID userId
        ) {

        organizationAccessService
            .requireMembership(
                organizationId,
                userId
            );

        Long count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM notifications
                WHERE organization_id = ?
                  AND user_id = ?
                  AND read_at IS NULL
                """,
                Long.class,
                organizationId,
                userId
            );

        return new NotificationDtos
            .UnreadCount(
                count == null
                    ? 0L
                    : count
            );
    }

    @Transactional
    public void markRead(
        UUID organizationId,
        UUID userId,
        UUID notificationId
    ) {
        organizationAccessService
            .requireMembership(
                organizationId,
                userId
            );

        int updated =
            jdbcTemplate.update(
                """
                UPDATE notifications
                SET read_at =
                    COALESCE(
                        read_at,
                        ?
                    )
                WHERE id = ?
                  AND organization_id = ?
                  AND user_id = ?
                """,
                Timestamp.from(Instant.now()),
                notificationId,
                organizationId,
                userId
            );

        if (updated != 1) {
            throw new BusinessException(
                HttpStatus.NOT_FOUND,
                "NOTIFICATION_NOT_FOUND",
                "The notification does not exist."
            );
        }
    }

    @Transactional
    public int markAllRead(
        UUID organizationId,
        UUID userId
    ) {
        organizationAccessService
            .requireMembership(
                organizationId,
                userId
            );

        return jdbcTemplate.update(
            """
            UPDATE notifications
            SET read_at = ?
            WHERE organization_id = ?
              AND user_id = ?
              AND read_at IS NULL
            """,
            Timestamp.from(Instant.now()),
            organizationId,
            userId
        );
    }

    @Transactional
    public void notifyUser(
        UUID organizationId,
        UUID userId,
        String type,
        String title,
        String message,
        String entityType,
        UUID entityId,
        String sourceKey
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO notifications (
                id,
                user_id,
                organization_id,
                type,
                title,
                message,
                entity_type,
                entity_id,
                source_key,
                read_at,
                created_at
            )
            VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?
            )
            ON CONFLICT (
                user_id,
                source_key
            )
            WHERE source_key IS NOT NULL
            DO NOTHING
            """,
            UUID.randomUUID(),
            userId,
            organizationId,
            type,
            title,
            message,
            entityType,
            entityId,
            sourceKey,
            Timestamp.from(Instant.now())
        );
    }

    @Transactional
    public int notifyProjectStakeholders(
        UUID organizationId,
        UUID projectId,
        String type,
        String title,
        String message,
        String entityType,
        UUID entityId,
        String sourceKey
    ) {
        Set<UUID> recipients =
            new LinkedHashSet<>(
                jdbcTemplate.query(
                    """
                    SELECT DISTINCT user_id
                    FROM (
                        SELECT
                            om.user_id
                        FROM organization_memberships om
                        WHERE om.organization_id = ?
                          AND om.role IN (
                                'OWNER',
                                'ADMIN'
                          )

                        UNION

                        SELECT
                            om.user_id
                        FROM project_members pm
                        JOIN organization_memberships om
                          ON om.id =
                                pm.membership_id
                         AND om.organization_id =
                                pm.organization_id
                        WHERE pm.organization_id = ?
                          AND pm.project_id = ?
                          AND pm.removed_at IS NULL
                    ) recipients
                    """,
                    (resultSet, rowNumber) ->
                        resultSet.getObject(
                            "user_id",
                            UUID.class
                        ),
                    organizationId,
                    organizationId,
                    projectId
                )
            );

        for (UUID recipient : recipients) {
            notifyUser(
                organizationId,
                recipient,
                type,
                title,
                message,
                entityType,
                entityId,
                sourceKey == null
                    ? null
                    : sourceKey
                        + ":"
                        + recipient
            );
        }

        return recipients.size();
    }

    private static Instant instant(
        Timestamp value
    ) {
        return value == null
            ? null
            : value.toInstant();
    }
}
