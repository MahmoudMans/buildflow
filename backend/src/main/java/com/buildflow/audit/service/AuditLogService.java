package com.buildflow.audit.service;

import com.buildflow.audit.dto.AuditDtos;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
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
public class AuditLogService {

    private final JdbcTemplate jdbcTemplate;

    private final OrganizationAccessService
        organizationAccessService;

    public AuditLogService(
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
    public List<AuditDtos.View> list(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        var membership =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                );

        if (
            membership.getRole()
                != OrganizationRole.OWNER
            && membership.getRole()
                != OrganizationRole.ADMIN
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "AUDIT_LOG_FORBIDDEN",
                "Only owners and administrators can access audit logs."
            );
        }

        if (projectId == null) {
            return jdbcTemplate.query(
                """
                SELECT
                    id,
                    organization_id,
                    project_id,
                    actor_user_id,
                    action,
                    entity_type,
                    entity_id,
                    old_values,
                    new_values,
                    ip_address,
                    created_at
                FROM audit_logs
                WHERE organization_id = ?
                ORDER BY created_at DESC
                LIMIT 500
                """,
                this::map,
                organizationId
            );
        }

        return jdbcTemplate.query(
            """
            SELECT
                id,
                organization_id,
                project_id,
                actor_user_id,
                action,
                entity_type,
                entity_id,
                old_values,
                new_values,
                ip_address,
                created_at
            FROM audit_logs
            WHERE organization_id = ?
              AND project_id = ?
            ORDER BY created_at DESC
            LIMIT 500
            """,
            this::map,
            organizationId,
            projectId
        );
    }

    private AuditDtos.View map(
        java.sql.ResultSet resultSet,
        int rowNumber
    ) throws java.sql.SQLException {
        return new AuditDtos.View(
            resultSet.getObject(
                "id",
                UUID.class
            ),
            resultSet.getObject(
                "organization_id",
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
                "entity_type"
            ),
            resultSet.getObject(
                "entity_id",
                UUID.class
            ),
            resultSet.getString(
                "old_values"
            ),
            resultSet.getString(
                "new_values"
            ),
            resultSet.getString(
                "ip_address"
            ),
            instant(
                resultSet.getTimestamp(
                    "created_at"
                )
            )
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
