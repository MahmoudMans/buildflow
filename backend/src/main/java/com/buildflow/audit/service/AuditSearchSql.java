package com.buildflow.audit.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The SQL of the audit search. Only fixed text is ever added to a query:
 * everything a person typed travels as a bound parameter.
 */
public final class AuditSearchSql {

    /** The conditions (without the word WHERE) and the values they use. */
    public record Filter(
        String where,
        Map<String, Object> parameters
    ) {
    }

    public static final String ACTIONS =
        """
        SELECT DISTINCT a.action
        FROM audit_logs a
        WHERE a.organization_id = :organizationId
        ORDER BY a.action
        LIMIT 500
        """;

    public static final String ENTITY_TYPES =
        """
        SELECT DISTINCT a.entity_type
        FROM audit_logs a
        WHERE a.organization_id = :organizationId
        ORDER BY a.entity_type
        LIMIT 500
        """;

    public static final String ACTORS =
        """
        SELECT DISTINCT u.id, u.first_name, u.last_name, u.email
        FROM audit_logs a
        JOIN app_users u ON u.id = a.actor_user_id
        WHERE a.organization_id = :organizationId
        ORDER BY u.first_name, u.last_name, u.email
        LIMIT 500
        """;

    public static final String PROJECTS =
        """
        SELECT p.id, p.name, p.project_number
        FROM projects p
        WHERE p.organization_id = :organizationId
          AND EXISTS (
              SELECT 1
              FROM audit_logs a
              WHERE a.organization_id = p.organization_id
                AND a.project_id = p.id
          )
        ORDER BY p.name, p.project_number
        LIMIT 500
        """;

    private AuditSearchSql() {
    }

    public static Filter filter(
        UUID organizationId,
        AuditSearchCriteria criteria,
        Instant from,
        Instant toExclusive
    ) {

        StringBuilder where =
            new StringBuilder("a.organization_id = :organizationId");

        Map<String, Object> parameters = new LinkedHashMap<>();

        parameters.put("organizationId", organizationId);

        if (criteria.projectId() != null) {
            where.append("\n  AND a.project_id = :projectId");
            parameters.put("projectId", criteria.projectId());
        }

        if (criteria.actorUserId() != null) {
            where.append("\n  AND a.actor_user_id = :actorUserId");
            parameters.put("actorUserId", criteria.actorUserId());
        }

        if (criteria.entityType() != null) {
            where.append("\n  AND a.entity_type = :entityType");
            parameters.put("entityType", criteria.entityType());
        }

        if (criteria.action() != null) {
            where.append("\n  AND a.action = :action");
            parameters.put("action", criteria.action());
        }

        if (from != null) {
            where.append("\n  AND a.created_at >= :fromInstant");
            parameters.put("fromInstant", Timestamp.from(from));
        }

        if (toExclusive != null) {
            where.append("\n  AND a.created_at < :toInstant");
            parameters.put("toInstant", Timestamp.from(toExclusive));
        }

        if (criteria.text() != null) {
            where.append(
                """

                  AND (
                      a.action ILIKE :text ESCAPE '!'
                      OR a.entity_type ILIKE :text ESCAPE '!'
                      OR a.new_values ILIKE :text ESCAPE '!'
                      OR u.email ILIKE :text ESCAPE '!'
                      OR (u.first_name || ' ' || u.last_name) ILIKE :text ESCAPE '!'
                  )"""
            );
            parameters.put("text", criteria.likePattern());
        }

        return new Filter(where.toString(), parameters);
    }

    public static String selectSql(String where) {
        return """
            SELECT
                a.id,
                a.project_id,
                p.name AS project_name,
                p.project_number AS project_number,
                a.actor_user_id,
                u.first_name,
                u.last_name,
                u.email,
                a.action,
                a.entity_type,
                a.entity_id,
                a.old_values,
                a.new_values,
                a.ip_address,
                a.created_at
            FROM audit_logs a
            LEFT JOIN app_users u ON u.id = a.actor_user_id
            LEFT JOIN projects p
                   ON p.id = a.project_id
                  AND p.organization_id = a.organization_id
            WHERE\s""" + where + """

            ORDER BY a.created_at DESC, a.id DESC
            LIMIT :limit OFFSET :offset
            """;
    }

    public static String countSql(String where) {
        return """
            SELECT count(*)
            FROM audit_logs a
            LEFT JOIN app_users u ON u.id = a.actor_user_id
            WHERE\s""" + where + "\n";
    }
}
