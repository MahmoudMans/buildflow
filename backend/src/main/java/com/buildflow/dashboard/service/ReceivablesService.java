package com.buildflow.dashboard.service;

import com.buildflow.dashboard.dto.ReceivablesDtos;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReceivablesService {

    private static final int PROJECT_LIMIT = 6;

    private static final int FORECAST_MONTHS = 6;

    private static final Set<String> FINANCE_ROLES =
        Set.of("OWNER", "ADMIN", "ACCOUNTANT");

    private static final BigDecimal ZERO =
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private static final String SCOPE = """
        FROM invoices
        WHERE organization_id = :organizationId
          AND project_id IN (:projectIds)
          AND status IN ('ISSUED', 'PARTIALLY_PAID', 'OVERDUE')
          AND amount_outstanding > 0
        """;

    private record BucketDefinition(String key, String label) {
    }

    private static final List<BucketDefinition> BUCKETS =
        List.of(
            new BucketDefinition("CURRENT", "Not yet due"),
            new BucketDefinition("DAYS_1_30", "1-30 days overdue"),
            new BucketDefinition("DAYS_31_60", "31-60 days overdue"),
            new BucketDefinition("DAYS_61_90", "61-90 days overdue"),
            new BucketDefinition("DAYS_90_PLUS", "Over 90 days overdue")
        );

    private record BucketRow(
        String key,
        long count,
        BigDecimal amount
    ) {
    }

    private record MonthRow(
        String month,
        long count,
        BigDecimal amount
    ) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    private final ProjectService projectService;

    private final OrganizationAccessService organizationAccessService;

    public ReceivablesService(
        JdbcTemplate jdbcTemplate,
        ProjectService projectService,
        OrganizationAccessService organizationAccessService
    ) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.projectService = projectService;
        this.organizationAccessService = organizationAccessService;
    }

    @Transactional(readOnly = true)
    public ReceivablesDtos.ReceivablesSummary summary(
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

        LocalDate today = LocalDate.now();

        if (!FINANCE_ROLES.contains(role)) {
            return new ReceivablesDtos.ReceivablesSummary(
                false,
                ZERO,
                ZERO,
                0L,
                0L,
                BigDecimal.ZERO.setScale(1),
                List.of(),
                List.of(),
                ZERO,
                List.of()
            );
        }

        Map<UUID, ProjectDtos.ProjectSummary> projects =
            new LinkedHashMap<>();

        for (
            ProjectDtos.ProjectSummary project
            : projectService.list(organizationId, userId)
        ) {
            projects.put(project.id(), project);
        }

        if (projects.isEmpty()) {
            return assemble(
                today,
                List.of(),
                List.of(),
                List.of()
            );
        }

        MapSqlParameterSource params =
            new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("projectIds", projects.keySet())
                .addValue("today", today)
                .addValue("limit", PROJECT_LIMIT);

        List<BucketRow> bucketRows =
            jdbc.query(
                """
                SELECT CASE
                           WHEN due_date >= CAST(:today AS date)
                               THEN 'CURRENT'
                           WHEN CAST(:today AS date) - due_date <= 30
                               THEN 'DAYS_1_30'
                           WHEN CAST(:today AS date) - due_date <= 60
                               THEN 'DAYS_31_60'
                           WHEN CAST(:today AS date) - due_date <= 90
                               THEN 'DAYS_61_90'
                           ELSE 'DAYS_90_PLUS'
                       END AS bucket,
                       COUNT(*) AS invoice_count,
                       COALESCE(SUM(amount_outstanding), 0) AS amount
                """
                    + SCOPE
                    + " GROUP BY 1",
                params,
                (rs, rowNumber) ->
                    new BucketRow(
                        rs.getString("bucket"),
                        rs.getLong("invoice_count"),
                        money(rs.getBigDecimal("amount"))
                    )
            );

        List<MonthRow> monthRows =
            jdbc.query(
                """
                SELECT to_char(due_date, 'YYYY-MM') AS month,
                       COUNT(*) AS invoice_count,
                       COALESCE(SUM(amount_outstanding), 0) AS amount
                """
                    + SCOPE
                    + " AND due_date >= CAST(:today AS date)"
                    + " GROUP BY 1 ORDER BY 1",
                params,
                (rs, rowNumber) ->
                    new MonthRow(
                        rs.getString("month"),
                        rs.getLong("invoice_count"),
                        money(rs.getBigDecimal("amount"))
                    )
            );

        List<ReceivablesDtos.ProjectReceivable> projectRows =
            jdbc.query(
                """
                SELECT project_id,
                       COUNT(*) AS open_invoices,
                       COALESCE(SUM(amount_outstanding), 0) AS outstanding,
                       COALESCE(
                           SUM(
                               CASE
                                   WHEN due_date < CAST(:today AS date)
                                       THEN amount_outstanding
                                   ELSE 0
                               END
                           ),
                           0
                       ) AS overdue,
                       COALESCE(
                           MAX(
                               CASE
                                   WHEN due_date < CAST(:today AS date)
                                       THEN CAST(:today AS date) - due_date
                               END
                           ),
                           0
                       ) AS oldest
                """
                    + SCOPE
                    + " GROUP BY project_id"
                    + " ORDER BY overdue DESC, outstanding DESC"
                    + " LIMIT :limit",
                params,
                (rs, rowNumber) -> {
                    ProjectDtos.ProjectSummary project =
                        projects.get(
                            rs.getObject("project_id", UUID.class)
                        );

                    return new ReceivablesDtos.ProjectReceivable(
                        project.id(),
                        project.projectNumber(),
                        project.name(),
                        rs.getLong("open_invoices"),
                        money(rs.getBigDecimal("outstanding")),
                        money(rs.getBigDecimal("overdue")),
                        rs.getLong("oldest")
                    );
                }
            );

        return assemble(today, bucketRows, monthRows, projectRows);
    }

    private ReceivablesDtos.ReceivablesSummary assemble(
        LocalDate today,
        List<BucketRow> bucketRows,
        List<MonthRow> monthRows,
        List<ReceivablesDtos.ProjectReceivable> projectRows
    ) {
        Map<String, BucketRow> byKey = new HashMap<>();

        for (BucketRow row : bucketRows) {
            byKey.put(row.key(), row);
        }

        List<ReceivablesDtos.AgingBucket> aging = new ArrayList<>();

        long openInvoices = 0L;
        long overdueInvoices = 0L;
        BigDecimal totalOutstanding = ZERO;
        BigDecimal totalOverdue = ZERO;

        for (BucketDefinition bucket : BUCKETS) {
            BucketRow row = byKey.get(bucket.key());

            long count = row == null ? 0L : row.count();
            BigDecimal amount = row == null ? ZERO : row.amount();

            aging.add(
                new ReceivablesDtos.AgingBucket(
                    bucket.key(),
                    bucket.label(),
                    count,
                    amount
                )
            );

            openInvoices += count;
            totalOutstanding = totalOutstanding.add(amount);

            if (!"CURRENT".equals(bucket.key())) {
                overdueInvoices += count;
                totalOverdue = totalOverdue.add(amount);
            }
        }

        BigDecimal overduePercentage =
            totalOutstanding.signum() > 0
                ? totalOverdue
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalOutstanding, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(1);

        Map<String, MonthRow> byMonth = new HashMap<>();

        for (MonthRow row : monthRows) {
            byMonth.put(row.month(), row);
        }

        YearMonth first = YearMonth.from(today);

        List<ReceivablesDtos.ExpectedCollection> expected =
            new ArrayList<>();

        for (int offset = 0; offset < FORECAST_MONTHS; offset++) {
            String key = first.plusMonths(offset).toString();

            MonthRow row = byMonth.remove(key);

            expected.add(
                new ReceivablesDtos.ExpectedCollection(
                    key,
                    row == null ? 0L : row.count(),
                    row == null ? ZERO : row.amount()
                )
            );
        }

        BigDecimal later = ZERO;

        for (MonthRow row : byMonth.values()) {
            later = later.add(row.amount());
        }

        return new ReceivablesDtos.ReceivablesSummary(
            true,
            totalOutstanding,
            totalOverdue,
            openInvoices,
            overdueInvoices,
            overduePercentage,
            aging,
            expected,
            later,
            projectRows
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null
            ? ZERO
            : value.setScale(2, RoundingMode.HALF_UP);
    }
}
