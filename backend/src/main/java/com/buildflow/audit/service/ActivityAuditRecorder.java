package com.buildflow.audit.service;

import jakarta.persistence.EntityManager;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
public class ActivityAuditRecorder {

    private final JdbcTemplate jdbcTemplate;

    private final EntityManager entityManager;

    public ActivityAuditRecorder(
        JdbcTemplate jdbcTemplate,
        EntityManager entityManager
    ) {
        this.jdbcTemplate =
            jdbcTemplate;

        this.entityManager =
            entityManager;
    }

    @Transactional
    public void record(
        UUID organizationId,
        UUID projectId,
        UUID actorUserId,
        String action,
        String entityType,
        UUID entityId,
        String oldValues,
        String newValues,
        String ipAddress,
        String activityMessage
    ) {
        Timestamp now =
            Timestamp.from(
                Instant.now()
            );

        /*
         * Phase 10 uses JdbcTemplate for the append-only
         * audit/activity projection while the business
         * aggregates are primarily persisted through JPA.
         *
         * A generated entity UUID can therefore be available
         * before Hibernate has physically issued its INSERT.
         * Flush here so PostgreSQL foreign-key checks observe
         * all pending JPA writes from the current transaction.
         */
        entityManager.flush();

        jdbcTemplate.update(
            """
            INSERT INTO audit_logs (
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
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            UUID.randomUUID(),
            organizationId,
            projectId,
            actorUserId,
            action,
            entityType,
            entityId,
            oldValues,
            newValues,
            ipAddress,
            now
        );

        if (projectId != null) {
            jdbcTemplate.update(
                """
                INSERT INTO project_activity (
                    id,
                    organization_id,
                    project_id,
                    actor_user_id,
                    action,
                    message,
                    entity_type,
                    entity_id,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                organizationId,
                projectId,
                actorUserId,
                action,
                activityMessage,
                entityType,
                entityId,
                now
            );
        }
    }
}
