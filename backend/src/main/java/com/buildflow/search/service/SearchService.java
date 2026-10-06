package com.buildflow.search.service;

import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.service.ProjectService;
import com.buildflow.search.dto.SearchDtos;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Global search across the records a user is allowed to see.
 *
 * Visibility mirrors the existing modules: projects and everything inside
 * them follow project access, clients and suppliers follow the same role
 * lists as their own pages, invoices are limited to owners, administrators
 * and accountants, and purchase orders follow the procurement rule.
 */
@Service
public class SearchService {

    private static final int MIN_QUERY_LENGTH = 2;

    private static final int MAX_QUERY_LENGTH = 80;

    private static final int PER_KIND = 5;

    private static final Set<String> CLIENT_ROLES =
        Set.of("OWNER", "ADMIN", "PROJECT_MANAGER", "ACCOUNTANT");

    private static final Set<String> SUPPLIER_ROLES =
        Set.of(
            "OWNER",
            "ADMIN",
            "PROJECT_MANAGER",
            "SITE_MANAGER",
            "ACCOUNTANT"
        );

    private static final Set<String> FINANCE_ROLES =
        Set.of("OWNER", "ADMIN", "ACCOUNTANT");

    private final NamedParameterJdbcTemplate jdbc;

    private final ProjectService projectService;

    private final OrganizationAccessService organizationAccessService;

    public SearchService(
        JdbcTemplate jdbcTemplate,
        ProjectService projectService,
        OrganizationAccessService organizationAccessService
    ) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.projectService = projectService;
        this.organizationAccessService = organizationAccessService;
    }

    @Transactional(readOnly = true)
    public SearchDtos.SearchResponse search(
        UUID organizationId,
        UUID userId,
        String rawQuery
    ) {
        var membership =
            organizationAccessService.requireMembership(
                organizationId,
                userId
            );

        String role = membership.getRole().name();

        if ("CLIENT".equals(role)) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "INTERNAL_SEARCH_FORBIDDEN",
                "Search is not available in the client portal."
            );
        }

        String query = clean(rawQuery);

        if (query.length() < MIN_QUERY_LENGTH) {
            return new SearchDtos.SearchResponse(query, List.of());
        }

        String needle = query.toLowerCase(Locale.ROOT);
        String escaped = escapeLike(needle);

        MapSqlParameterSource params =
            new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("userId", userId)
                .addValue("pattern", "%" + escaped + "%")
                .addValue("prefix", escaped + "%")
                .addValue("limit", PER_KIND);

        Map<UUID, ProjectDtos.ProjectSummary> projects =
            new LinkedHashMap<>();

        for (
            ProjectDtos.ProjectSummary project
            : projectService.list(organizationId, userId)
        ) {
            projects.put(project.id(), project);
        }

        List<SearchDtos.SearchHit> hits = new ArrayList<>();

        // ---------- Projects ----------

        projects.values()
            .stream()
            .filter(
                project ->
                    contains(project.name(), needle)
                        || contains(project.projectNumber(), needle)
                        || contains(project.clientName(), needle)
            )
            .sorted(
                Comparator
                    .comparingInt(
                        (ProjectDtos.ProjectSummary project) ->
                            startsWith(project.name(), needle)
                                || startsWith(project.projectNumber(), needle)
                                ? 0
                                : 1
                    )
                    .thenComparing(
                        ProjectDtos.ProjectSummary::name,
                        String.CASE_INSENSITIVE_ORDER
                    )
            )
            .limit(PER_KIND)
            .forEach(
                project ->
                    hits.add(
                        new SearchDtos.SearchHit(
                            "PROJECT",
                            project.id(),
                            project.id(),
                            project.name(),
                            project.projectNumber()
                                + " · "
                                + project.clientName(),
                            project.status() == null
                                ? null
                                : project.status().name()
                        )
                    )
            );

        boolean hasProjects = !projects.isEmpty();

        if (hasProjects) {
            params.addValue("projectIds", projects.keySet());
        }

        // ---------- Tasks ----------

        if (hasProjects) {
            hits.addAll(
                jdbc.query(
                    """
                    SELECT t.id, t.project_id, t.title, t.status
                    FROM tasks t
                    WHERE t.organization_id = :organizationId
                      AND t.project_id IN (:projectIds)
                      AND t.title ILIKE :pattern ESCAPE '!'
                    ORDER BY CASE
                                 WHEN lower(t.title) LIKE :prefix ESCAPE '!'
                                     THEN 0
                                 ELSE 1
                             END,
                             t.created_at DESC
                    LIMIT :limit
                    """,
                    params,
                    (rs, rowNumber) -> {
                        UUID projectId =
                            rs.getObject("project_id", UUID.class);
                        String status = rs.getString("status");

                        return new SearchDtos.SearchHit(
                            "TASK",
                            rs.getObject("id", UUID.class),
                            projectId,
                            rs.getString("title"),
                            projects.get(projectId).name()
                                + " · "
                                + label(status),
                            status
                        );
                    }
                )
            );
        }

        // ---------- Clients ----------

        if (CLIENT_ROLES.contains(role)) {
            hits.addAll(
                jdbc.query(
                    """
                    SELECT id, company_name, first_name, last_name, email,
                           type, status
                    FROM clients
                    WHERE organization_id = :organizationId
                      AND (
                          company_name ILIKE :pattern ESCAPE '!'
                          OR first_name ILIKE :pattern ESCAPE '!'
                          OR last_name ILIKE :pattern ESCAPE '!'
                          OR email ILIKE :pattern ESCAPE '!'
                          OR (
                              COALESCE(first_name, '')
                              || ' '
                              || COALESCE(last_name, '')
                          ) ILIKE :pattern ESCAPE '!'
                      )
                    ORDER BY CASE status WHEN 'ACTIVE' THEN 0 ELSE 1 END,
                             COALESCE(company_name, last_name) ASC
                    LIMIT :limit
                    """,
                    params,
                    (rs, rowNumber) -> {
                        String company = rs.getString("company_name");
                        String email = rs.getString("email");

                        String title =
                            company != null && !company.isBlank()
                                ? company
                                : (
                                    nullToEmpty(rs.getString("first_name"))
                                        + " "
                                        + nullToEmpty(rs.getString("last_name"))
                                ).strip();

                        return new SearchDtos.SearchHit(
                            "CLIENT",
                            rs.getObject("id", UUID.class),
                            null,
                            title,
                            email != null && !email.isBlank()
                                ? email
                                : label(rs.getString("type")),
                            rs.getString("status")
                        );
                    }
                )
            );
        }

        // ---------- Suppliers ----------

        if (SUPPLIER_ROLES.contains(role)) {
            hits.addAll(
                jdbc.query(
                    """
                    SELECT id, name, contact_name, email, status
                    FROM suppliers
                    WHERE organization_id = :organizationId
                      AND (
                          name ILIKE :pattern ESCAPE '!'
                          OR contact_name ILIKE :pattern ESCAPE '!'
                          OR email ILIKE :pattern ESCAPE '!'
                      )
                    ORDER BY CASE status WHEN 'ACTIVE' THEN 0 ELSE 1 END,
                             name ASC
                    LIMIT :limit
                    """,
                    params,
                    (rs, rowNumber) -> {
                        String contact = rs.getString("contact_name");
                        String email = rs.getString("email");

                        List<String> parts = new ArrayList<>();

                        if (contact != null && !contact.isBlank()) {
                            parts.add(contact);
                        }

                        if (email != null && !email.isBlank()) {
                            parts.add(email);
                        }

                        return new SearchDtos.SearchHit(
                            "SUPPLIER",
                            rs.getObject("id", UUID.class),
                            null,
                            rs.getString("name"),
                            String.join(" · ", parts),
                            rs.getString("status")
                        );
                    }
                )
            );
        }

        // ---------- Invoices (finance roles) ----------

        if (hasProjects && FINANCE_ROLES.contains(role)) {
            hits.addAll(
                jdbc.query(
                    """
                    SELECT i.id, i.project_id, i.invoice_number, i.status,
                           i.due_date
                    FROM invoices i
                    WHERE i.organization_id = :organizationId
                      AND i.project_id IN (:projectIds)
                      AND i.invoice_number ILIKE :pattern ESCAPE '!'
                    ORDER BY CASE
                                 WHEN lower(i.invoice_number)
                                     LIKE :prefix ESCAPE '!'
                                     THEN 0
                                 ELSE 1
                             END,
                             i.due_date DESC
                    LIMIT :limit
                    """,
                    params,
                    (rs, rowNumber) -> {
                        UUID projectId =
                            rs.getObject("project_id", UUID.class);
                        String status = rs.getString("status");
                        LocalDate due =
                            rs.getObject("due_date", LocalDate.class);

                        return new SearchDtos.SearchHit(
                            "INVOICE",
                            rs.getObject("id", UUID.class),
                            projectId,
                            rs.getString("invoice_number"),
                            projects.get(projectId).name()
                                + " · "
                                + label(status)
                                + (due == null ? "" : " · due " + due),
                            status
                        );
                    }
                )
            );
        }

        // ---------- Purchase orders ----------
        //
        // Same rule as the procurement pages: owners, administrators and
        // accountants see every purchase order of the projects they can
        // see; everybody else only on projects where they are a project
        // manager or site manager.

        if (hasProjects) {
            String memberScope =
                FINANCE_ROLES.contains(role)
                    ? ""
                    : """
                      AND po.project_id IN (
                          SELECT pm.project_id
                          FROM project_members pm
                          JOIN organization_memberships om
                            ON om.id = pm.membership_id
                           AND om.organization_id = pm.organization_id
                          WHERE pm.organization_id = :organizationId
                            AND om.user_id = :userId
                            AND pm.removed_at IS NULL
                            AND pm.project_role
                                IN ('PROJECT_MANAGER', 'SITE_MANAGER')
                      )
                      """;

            String purchaseOrderSql =
                """
                SELECT po.id, po.project_id, po.purchase_order_number,
                       po.status, s.name AS supplier_name
                FROM purchase_orders po
                JOIN suppliers s
                  ON s.id = po.supplier_id
                 AND s.organization_id = po.organization_id
                WHERE po.organization_id = :organizationId
                  AND po.project_id IN (:projectIds)
                  AND (
                      po.purchase_order_number ILIKE :pattern ESCAPE '!'
                      OR s.name ILIKE :pattern ESCAPE '!'
                  )
                """
                    + memberScope
                    + """
                ORDER BY CASE
                             WHEN lower(po.purchase_order_number)
                                 LIKE :prefix ESCAPE '!'
                                 THEN 0
                             ELSE 1
                         END,
                         po.created_at DESC
                LIMIT :limit
                """;

            hits.addAll(
                jdbc.query(
                    purchaseOrderSql,
                    params,
                    (rs, rowNumber) -> {
                        UUID projectId =
                            rs.getObject("project_id", UUID.class);
                        String status = rs.getString("status");

                        return new SearchDtos.SearchHit(
                            "PURCHASE_ORDER",
                            rs.getObject("id", UUID.class),
                            projectId,
                            rs.getString("purchase_order_number"),
                            rs.getString("supplier_name")
                                + " · "
                                + projects.get(projectId).name()
                                + " · "
                                + label(status),
                            status
                        );
                    }
                )
            );
        }

        return new SearchDtos.SearchResponse(query, hits);
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.strip();

        return trimmed.length() > MAX_QUERY_LENGTH
            ? trimmed.substring(0, MAX_QUERY_LENGTH).strip()
            : trimmed;
    }

    /** Makes %, _ and the escape character itself match literally. */
    private static String escapeLike(String value) {
        return value
            .replace("!", "!!")
            .replace("%", "!%")
            .replace("_", "!_");
    }

    private static boolean contains(String value, String needle) {
        return value != null
            && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean startsWith(String value, String needle) {
        return value != null
            && value.toLowerCase(Locale.ROOT).startsWith(needle);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String label(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String text = value.toLowerCase(Locale.ROOT).replace('_', ' ');

        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
