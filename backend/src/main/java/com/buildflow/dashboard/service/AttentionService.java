package com.buildflow.dashboard.service;

import com.buildflow.dashboard.dto.AttentionDtos;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.service.ProjectService;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AttentionService {

    private static final int PER_KIND_LIMIT = 6;

    private static final Set<String> FINANCE_ROLES =
        Set.of("OWNER", "ADMIN", "ACCOUNTANT");

    private static final BigDecimal ZERO =
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private record InvoiceTotals(long count, BigDecimal amount) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    private final DashboardService dashboardService;

    private final ProjectService projectService;

    private final OrganizationAccessService organizationAccessService;

    public AttentionService(
        JdbcTemplate jdbcTemplate,
        DashboardService dashboardService,
        ProjectService projectService,
        OrganizationAccessService organizationAccessService
    ) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.dashboardService = dashboardService;
        this.projectService = projectService;
        this.organizationAccessService = organizationAccessService;
    }

    @Transactional(readOnly = true)
    public AttentionDtos.AttentionSummary summary(
        UUID organizationId,
        UUID userId
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
                "INTERNAL_DASHBOARD_FORBIDDEN",
                "The internal analytics dashboard is not available in the client portal."
            );
        }

        boolean financeVisible = FINANCE_ROLES.contains(role);

        Map<UUID, ProjectDtos.ProjectSummary> projects =
            new LinkedHashMap<>();

        for (
            ProjectDtos.ProjectSummary project
            : projectService.list(organizationId, userId)
        ) {
            projects.put(project.id(), project);
        }

        if (projects.isEmpty()) {
            return new AttentionDtos.AttentionSummary(
                financeVisible,
                0L,
                0L,
                0L,
                ZERO,
                0L,
                0L,
                List.of()
            );
        }

        Set<UUID> projectIds = projects.keySet();

        LocalDate today = LocalDate.now();

        MapSqlParameterSource params =
            new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("projectIds", projectIds)
                .addValue("limit", PER_KIND_LIMIT);

        List<AttentionDtos.AttentionItem> items = new ArrayList<>();

        // ---------- Overdue tasks ----------

        String taskScope = """
            FROM tasks
            WHERE organization_id = :organizationId
              AND project_id IN (:projectIds)
              AND due_date < CURRENT_DATE
              AND status NOT IN ('DONE', 'CANCELLED')
            """;

        String taskOrder = """
            ORDER BY due_date ASC,
                     CASE priority
                         WHEN 'CRITICAL' THEN 0
                         WHEN 'HIGH' THEN 1
                         WHEN 'MEDIUM' THEN 2
                         ELSE 3
                     END
            LIMIT :limit
            """;

        long overdueTasks =
            count("SELECT COUNT(*) " + taskScope, params);

        items.addAll(
            jdbc.query(
                "SELECT id, project_id, title, due_date, priority "
                    + taskScope
                    + taskOrder,
                params,
                (rs, rowNumber) -> {
                    UUID taskId = rs.getObject("id", UUID.class);
                    UUID projectId =
                        rs.getObject("project_id", UUID.class);
                    LocalDate due =
                        rs.getObject("due_date", LocalDate.class);
                    String priority = rs.getString("priority");
                    long days = ChronoUnit.DAYS.between(due, today);

                    return item(
                        "OVERDUE_TASK",
                        taskSeverity(priority, days),
                        taskId,
                        projects.get(projectId),
                        rs.getString("title"),
                        days
                            + (days == 1 ? " day" : " days")
                            + " overdue · "
                            + label(priority)
                            + " priority",
                        due,
                        null
                    );
                }
            )
        );

        // ---------- Open incidents ----------

        String incidentScope = """
            FROM site_incidents
            WHERE organization_id = :organizationId
              AND project_id IN (:projectIds)
              AND status NOT IN ('RESOLVED', 'CLOSED')
            """;

        String incidentOrder = """
            ORDER BY CASE severity
                         WHEN 'CRITICAL' THEN 0
                         WHEN 'HIGH' THEN 1
                         WHEN 'MEDIUM' THEN 2
                         ELSE 3
                     END,
                     reported_at DESC
            LIMIT :limit
            """;

        long openIncidents =
            count("SELECT COUNT(*) " + incidentScope, params);

        items.addAll(
            jdbc.query(
                "SELECT id, project_id, incident_number, title, severity, status, reported_at "
                    + incidentScope
                    + incidentOrder,
                params,
                (rs, rowNumber) -> {
                    UUID incidentId = rs.getObject("id", UUID.class);
                    UUID projectId =
                        rs.getObject("project_id", UUID.class);
                    OffsetDateTime reportedAt =
                        rs.getObject("reported_at", OffsetDateTime.class);
                    String severity = rs.getString("severity");

                    return item(
                        "OPEN_INCIDENT",
                        incidentSeverity(severity),
                        incidentId,
                        projects.get(projectId),
                        rs.getString("title"),
                        rs.getString("incident_number")
                            + " · "
                            + label(severity)
                            + " severity · "
                            + label(rs.getString("status")),
                        reportedAt == null
                            ? null
                            : reportedAt.toLocalDate(),
                        null
                    );
                }
            )
        );

        // ---------- Overdue invoices (finance roles only) ----------

        long overdueInvoices = 0L;
        BigDecimal overdueInvoiceAmount = ZERO;

        if (financeVisible) {
            String invoiceScope = """
                FROM invoices
                WHERE organization_id = :organizationId
                  AND project_id IN (:projectIds)
                  AND status IN ('ISSUED', 'PARTIALLY_PAID', 'OVERDUE')
                  AND amount_outstanding > 0
                  AND due_date < CURRENT_DATE
                """;

            InvoiceTotals totals =
                jdbc.queryForObject(
                    "SELECT COUNT(*) AS invoice_count, "
                        + "COALESCE(SUM(amount_outstanding), 0) AS outstanding "
                        + invoiceScope,
                    params,
                    (rs, rowNumber) ->
                        new InvoiceTotals(
                            rs.getLong("invoice_count"),
                            rs.getBigDecimal("outstanding")
                        )
                );

            if (totals != null) {
                overdueInvoices = totals.count();
                overdueInvoiceAmount =
                    totals.amount().setScale(2, RoundingMode.HALF_UP);
            }

            items.addAll(
                jdbc.query(
                    "SELECT id, project_id, invoice_number, due_date, amount_outstanding "
                        + invoiceScope
                        + " ORDER BY due_date ASC LIMIT :limit",
                    params,
                    (rs, rowNumber) -> {
                        UUID invoiceId = rs.getObject("id", UUID.class);
                        UUID projectId =
                            rs.getObject("project_id", UUID.class);
                        LocalDate due =
                            rs.getObject("due_date", LocalDate.class);
                        long days = ChronoUnit.DAYS.between(due, today);

                        return item(
                            "OVERDUE_INVOICE",
                            days >= 30 ? "CRITICAL" : "WARNING",
                            invoiceId,
                            projects.get(projectId),
                            "Invoice " + rs.getString("invoice_number"),
                            days
                                + (days == 1 ? " day" : " days")
                                + " overdue",
                            due,
                            rs.getBigDecimal("amount_outstanding")
                                .setScale(2, RoundingMode.HALF_UP)
                        );
                    }
                )
            );
        }

        // ---------- Change orders awaiting client review ----------

        String changeScope = """
            FROM change_orders
            WHERE organization_id = :organizationId
              AND project_id IN (:projectIds)
              AND status = 'CLIENT_REVIEW'
            """;

        long changeOrdersAwaitingClient =
            count("SELECT COUNT(*) " + changeScope, params);

        items.addAll(
            jdbc.query(
                "SELECT id, project_id, change_order_number, title, client_review_at, submitted_at "
                    + changeScope
                    + " ORDER BY COALESCE(client_review_at, submitted_at) ASC NULLS LAST LIMIT :limit",
                params,
                (rs, rowNumber) -> {
                    UUID changeOrderId = rs.getObject("id", UUID.class);
                    UUID projectId =
                        rs.getObject("project_id", UUID.class);
                    OffsetDateTime since =
                        rs.getObject("client_review_at", OffsetDateTime.class);

                    if (since == null) {
                        since =
                            rs.getObject("submitted_at", OffsetDateTime.class);
                    }

                    LocalDate sinceDate =
                        since == null ? null : since.toLocalDate();

                    long days =
                        sinceDate == null
                            ? 0L
                            : ChronoUnit.DAYS.between(sinceDate, today);

                    return item(
                        "CHANGE_ORDER_REVIEW",
                        days >= 7 ? "WARNING" : "INFO",
                        changeOrderId,
                        projects.get(projectId),
                        rs.getString("change_order_number")
                            + " · "
                            + rs.getString("title"),
                        sinceDate == null
                            ? "Awaiting client decision"
                            : "Waiting "
                                + days
                                + (days == 1 ? " day" : " days")
                                + " for the client decision",
                        sinceDate,
                        null
                    );
                }
            )
        );

        // ---------- Projects at risk ----------

        List<DashboardService.AtRiskProject> atRisk =
            new ArrayList<>(
                dashboardService.atRiskProjects(
                    organizationId,
                    projectIds
                )
            );

        atRisk.sort(
            (left, right) ->
                Integer.compare(
                    "CRITICAL".equals(left.healthStatus()) ? 0 : 1,
                    "CRITICAL".equals(right.healthStatus()) ? 0 : 1
                )
        );

        for (
            DashboardService.AtRiskProject project
            : atRisk.subList(0, Math.min(PER_KIND_LIMIT, atRisk.size()))
        ) {
            items.add(
                item(
                    "PROJECT_AT_RISK",
                    "CRITICAL".equals(project.healthStatus())
                        ? "CRITICAL"
                        : "WARNING",
                    project.id(),
                    projects.get(project.id()),
                    project.name(),
                    String.join(" · ", project.reasons()),
                    null,
                    null
                )
            );
        }

        items.sort(
            (left, right) -> {
                int bySeverity =
                    Integer.compare(
                        severityRank(left.severity()),
                        severityRank(right.severity())
                    );

                if (bySeverity != 0) {
                    return bySeverity;
                }

                if (left.date() == null && right.date() == null) {
                    return left.kind().compareTo(right.kind());
                }

                if (left.date() == null) {
                    return 1;
                }

                if (right.date() == null) {
                    return -1;
                }

                return left.date().compareTo(right.date());
            }
        );

        return new AttentionDtos.AttentionSummary(
            financeVisible,
            overdueTasks,
            openIncidents,
            overdueInvoices,
            overdueInvoiceAmount,
            changeOrdersAwaitingClient,
            atRisk.size(),
            items
        );
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long value = jdbc.queryForObject(sql, params, Long.class);

        return value == null ? 0L : value;
    }

    private AttentionDtos.AttentionItem item(
        String kind,
        String severity,
        UUID entityId,
        ProjectDtos.ProjectSummary project,
        String title,
        String detail,
        LocalDate date,
        BigDecimal amount
    ) {
        return new AttentionDtos.AttentionItem(
            kind,
            severity,
            entityId,
            project.id(),
            project.projectNumber(),
            project.name(),
            title,
            detail,
            date,
            amount
        );
    }

    private static String taskSeverity(String priority, long daysOverdue) {
        if ("CRITICAL".equals(priority)) {
            return "CRITICAL";
        }

        if ("HIGH".equals(priority) || daysOverdue >= 7) {
            return "WARNING";
        }

        return "INFO";
    }

    private static String incidentSeverity(String severity) {
        if ("CRITICAL".equals(severity)) {
            return "CRITICAL";
        }

        if ("HIGH".equals(severity)) {
            return "WARNING";
        }

        return "INFO";
    }

    private static int severityRank(String severity) {
        if ("CRITICAL".equals(severity)) {
            return 0;
        }

        if ("WARNING".equals(severity)) {
            return 1;
        }

        return 2;
    }

    private static String label(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String text = value.toLowerCase(Locale.ROOT).replace('_', ' ');

        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
