package com.buildflow.audit.service;

import com.buildflow.audit.dto.AuditSearchDtos;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Searches the audit log of one organization, a page at a time. Only owners
 * and administrators may use it, the same rule as the older audit list.
 */
@Service
public class AuditLogSearchService {

    private final NamedParameterJdbcTemplate jdbc;

    private final OrganizationAccessService organizationAccessService;

    public AuditLogSearchService(
        JdbcTemplate jdbcTemplate,
        OrganizationAccessService organizationAccessService
    ) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.organizationAccessService = organizationAccessService;
    }

    @Transactional(readOnly = true)
    public AuditSearchDtos.SearchPage search(
        UUID organizationId,
        UUID userId,
        AuditSearchCriteria criteria
    ) {
        ZoneId zone = requireAdmin(organizationId, userId);

        AuditSearchSql.Filter filter =
            AuditSearchSql.filter(
                organizationId,
                criteria,
                criteria.fromInstant(zone),
                criteria.toExclusiveInstant(zone)
            );

        Long counted =
            jdbc.queryForObject(
                AuditSearchSql.countSql(filter.where()),
                new MapSqlParameterSource(filter.parameters()),
                Long.class
            );

        long total = counted == null ? 0L : counted;

        List<AuditSearchDtos.Entry> items =
            total == 0L
                ? List.of()
                : jdbc.query(
                    AuditSearchSql.selectSql(filter.where()),
                    new MapSqlParameterSource(filter.parameters())
                        .addValue("limit", criteria.size())
                        .addValue("offset", criteria.offset()),
                    this::mapEntry
                );

        return new AuditSearchDtos.SearchPage(
            items,
            criteria.page(),
            criteria.size(),
            total,
            (int) ((total + criteria.size() - 1) / criteria.size())
        );
    }

    @Transactional(readOnly = true)
    public AuditSearchDtos.Facets facets(
        UUID organizationId,
        UUID userId
    ) {
        requireAdmin(organizationId, userId);

        MapSqlParameterSource params =
            new MapSqlParameterSource()
                .addValue("organizationId", organizationId);

        return new AuditSearchDtos.Facets(
            jdbc.query(
                AuditSearchSql.ACTIONS,
                params,
                (rs, row) -> rs.getString("action")
            ),
            jdbc.query(
                AuditSearchSql.ENTITY_TYPES,
                params,
                (rs, row) -> rs.getString("entity_type")
            ),
            jdbc.query(
                AuditSearchSql.ACTORS,
                params,
                (rs, row) ->
                    new AuditSearchDtos.ActorOption(
                        rs.getObject("id", UUID.class),
                        fullName(
                            rs.getString("first_name"),
                            rs.getString("last_name")
                        ),
                        rs.getString("email")
                    )
            ),
            jdbc.query(
                AuditSearchSql.PROJECTS,
                params,
                (rs, row) ->
                    new AuditSearchDtos.ProjectOption(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("project_number")
                    )
            )
        );
    }

    /** Checks the caller may read the log and returns the org's time zone. */
    private ZoneId requireAdmin(
        UUID organizationId,
        UUID userId
    ) {
        OrganizationMembership membership =
            organizationAccessService.requireMembership(
                organizationId,
                userId
            );

        if (
            membership.getRole() != OrganizationRole.OWNER
            && membership.getRole() != OrganizationRole.ADMIN
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "AUDIT_LOG_FORBIDDEN",
                "Only owners and administrators can access audit logs."
            );
        }

        String timezone = membership.getOrganization().getTimezone();

        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("UTC");
        }

        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return ZoneId.of("UTC");
        }
    }

    private AuditSearchDtos.Entry mapEntry(
        ResultSet rs,
        int rowNumber
    ) throws SQLException {
        return new AuditSearchDtos.Entry(
            rs.getObject("id", UUID.class),
            rs.getObject("project_id", UUID.class),
            rs.getString("project_name"),
            rs.getString("project_number"),
            rs.getObject("actor_user_id", UUID.class),
            fullName(
                rs.getString("first_name"),
                rs.getString("last_name")
            ),
            rs.getString("email"),
            rs.getString("action"),
            rs.getString("entity_type"),
            rs.getObject("entity_id", UUID.class),
            AuditValueRedactor.redact(rs.getString("old_values")),
            AuditValueRedactor.redact(rs.getString("new_values")),
            rs.getString("ip_address"),
            instant(rs.getTimestamp("created_at"))
        );
    }

    static String fullName(String firstName, String lastName) {

        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();

        String joined = (first + " " + last).trim();

        return joined.isEmpty() ? null : joined;
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
