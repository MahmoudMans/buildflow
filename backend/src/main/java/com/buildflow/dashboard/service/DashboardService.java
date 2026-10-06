package com.buildflow.dashboard.service;

import com.buildflow.dashboard.dto.DashboardDtos;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.project.service.ProjectAccessService;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DashboardService {

    private static final BigDecimal ZERO =
        BigDecimal.ZERO.setScale(
            2,
            RoundingMode.HALF_UP
        );

    private static final int
        SIGNIFICANT_DELAY_DAYS = 30;

    private static final Pattern
        IDENTIFIER_PATTERN =
            Pattern.compile(
                "[a-z0-9_]+"
            );

    private final JdbcTemplate jdbcTemplate;

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectAccessService
        projectAccessService;

    public DashboardService(
        JdbcTemplate jdbcTemplate,
        OrganizationAccessService
            organizationAccessService,
        ProjectAccessService
            projectAccessService
    ) {
        this.jdbcTemplate =
            jdbcTemplate;

        this.organizationAccessService =
            organizationAccessService;

        this.projectAccessService =
            projectAccessService;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.OrganizationDashboard
        organization(
            UUID organizationId,
            UUID userId
        ) {

        requireInternalMembership(
            organizationId,
            userId
        );

        List<ProjectRow> projects =
            projects(
                organizationId
            );

        long activeProjects =
            projects.stream()
                .filter(
                    project ->
                        "ACTIVE".equals(
                            project.status()
                        )
                )
                .count();

        BigDecimal totalContractValue =
            ZERO;

        BigDecimal forecastRevenue =
            ZERO;

        BigDecimal actualCosts =
            ZERO;

        BigDecimal outstandingInvoices =
            ZERO;

        long projectsAtRisk = 0L;
        long overdueTasks = 0L;
        long openIncidents = 0L;

        List<DashboardDtos.BudgetPerformance>
            budgetPerformance =
                new ArrayList<>();

        for (
            ProjectRow project
            : projects
        ) {
            DashboardDtos.FinancialSnapshot
                financial =
                    financial(
                        organizationId,
                        project.id()
                    );

            if (
                !Set.of(
                    "CANCELLED",
                    "ARCHIVED"
                ).contains(
                    project.status()
                )
            ) {
                totalContractValue =
                    totalContractValue.add(
                        financial
                            .revisedContractValue()
                    );
            }

            if (
                Set.of(
                    "DRAFT",
                    "PLANNING",
                    "ACTIVE",
                    "ON_HOLD"
                ).contains(
                    project.status()
                )
            ) {
                forecastRevenue =
                    forecastRevenue.add(
                        financial
                            .revisedContractValue()
                    );
            }

            actualCosts =
                actualCosts.add(
                    financial.actualCost()
                );

            outstandingInvoices =
                outstandingInvoices.add(
                    financial
                        .outstandingReceivables()
                );

            overdueTasks +=
                overdueTaskCount(
                    organizationId,
                    project.id(),
                    false
                );

            openIncidents +=
                openIncidentCount(
                    organizationId,
                    project.id()
                );

            if (
                "ACTIVE".equals(
                    project.status()
                )
            ) {
                DashboardDtos.ProjectHealth
                    health =
                        health(
                            organizationId,
                            project,
                            financial
                        );

                if (
                    !"ON_TRACK".equals(
                        health.status()
                    )
                ) {
                    projectsAtRisk++;
                }
            }

            BigDecimal variancePercentage =
                percentage(
                    financial
                        .forecastBudgetVariance(),
                    financial.revisedBudget()
                );

            budgetPerformance.add(
                new DashboardDtos
                    .BudgetPerformance(
                        project.id(),
                        project.projectNumber(),
                        project.name(),
                        financial.revisedBudget(),
                        financial.forecastCost(),
                        financial
                            .forecastBudgetVariance(),
                        variancePercentage
                    )
            );
        }

        budgetPerformance.sort(
            (
                DashboardDtos.BudgetPerformance left,
                DashboardDtos.BudgetPerformance right
            ) ->
                right.variance()
                    .abs()
                    .compareTo(
                        left.variance()
                            .abs()
                    )
        );

        if (
            budgetPerformance.size()
                > 8
        ) {
            budgetPerformance =
                new ArrayList<>(
                    budgetPerformance
                        .subList(
                            0,
                            8
                        )
                );
        }

        return new DashboardDtos
            .OrganizationDashboard(
                activeProjects,
                money(
                    totalContractValue
                ),
                money(
                    forecastRevenue
                ),
                money(
                    actualCosts
                ),
                money(
                    outstandingInvoices
                ),
                projectsAtRisk,
                overdueTasks,
                openIncidents,
                organizationCurrency(
                    organizationId
                ),
                projectStatus(
                    projects
                ),
                monthlyRevenue(
                    organizationId,
                    null
                ),
                monthlyExpenses(
                    organizationId,
                    null
                ),
                budgetPerformance,
                upcomingDeadlines(
                    organizationId,
                    null,
                    projects,
                    10
                )
            );
    }

    @Transactional(readOnly = true)
    public DashboardDtos.ProjectDashboard
        project(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        requireInternalMembership(
            organizationId,
            userId
        );

        projectAccessService
            .requireVisible(
                organizationId,
                projectId,
                userId
            );

        ProjectRow project =
            project(
                organizationId,
                projectId
            );

        DashboardDtos.FinancialSnapshot
            financial =
                financial(
                    organizationId,
                    projectId
                );

        List<DashboardDtos.MonthlyValue>
            monthlyExpenses =
                monthlyExpenses(
                    organizationId,
                    projectId
                );

        return new DashboardDtos
            .ProjectDashboard(
                project.id(),
                project.projectNumber(),
                project.name(),
                project.status(),
                project.progressPercentage(),
                project.startDate(),
                project.expectedCompletion(),
                projectManager(
                    organizationId,
                    projectId
                ),
                organizationCurrency(
                    organizationId
                ),
                health(
                    organizationId,
                    project,
                    financial
                ),
                financial,
                openTaskCount(
                    organizationId,
                    projectId
                ),
                overdueTaskCount(
                    organizationId,
                    projectId,
                    false
                ),
                upcomingMilestoneCount(
                    organizationId,
                    projectId
                ),
                openIncidentCount(
                    organizationId,
                    projectId
                ),
                financial
                    .outstandingReceivables(),
                taskStatus(
                    organizationId,
                    projectId
                ),
                monthlyExpenses,
                cumulative(
                    monthlyExpenses
                ),
                new DashboardDtos
                    .BudgetUtilization(
                        financial.originalBudget(),
                        financial.revisedBudget(),
                        financial.actualCost(),
                        financial.forecastCost()
                    ),
                upcomingDeadlines(
                    organizationId,
                    projectId,
                    List.of(
                        project
                    ),
                    12
                )
            );
    }

    public record AtRiskProject(
        UUID id,
        String projectNumber,
        String name,
        String healthStatus,
        List<String> reasons
    ) {
    }

    @Transactional(readOnly = true)
    public List<AtRiskProject> atRiskProjects(
        UUID organizationId,
        Set<UUID> visibleProjectIds
    ) {
        List<AtRiskProject> result = new ArrayList<>();

        for (ProjectRow project : projects(organizationId)) {
            if (
                !visibleProjectIds.contains(project.id())
                || !"ACTIVE".equals(project.status())
            ) {
                continue;
            }

            DashboardDtos.ProjectHealth health =
                health(
                    organizationId,
                    project,
                    financial(organizationId, project.id())
                );

            if (!"ON_TRACK".equals(health.status())) {
                result.add(
                    new AtRiskProject(
                        project.id(),
                        project.projectNumber(),
                        project.name(),
                        health.status(),
                        health.reasons()
                    )
                );
            }
        }

        return result;
    }

    private void requireInternalMembership(
        UUID organizationId,
        UUID userId
    ) {
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
                "INTERNAL_DASHBOARD_FORBIDDEN",
                "The internal analytics dashboard is not available in the client portal."
            );
        }
    }

    private DashboardDtos.FinancialSnapshot
        financial(
            UUID organizationId,
            UUID projectId
        ) {

        BigDecimal originalBudget =
            originalBudget(
                organizationId,
                projectId
            );

        BigDecimal approvedBudgetChanges =
            approvedChangeOrderImpact(
                organizationId,
                projectId,
                "cost_impact"
            );

        BigDecimal revisedBudget =
            originalBudget.add(
                approvedBudgetChanges
            );

        BigDecimal actualCost =
            approvedExpenses(
                organizationId,
                projectId
            );

        BigDecimal committedCost =
            outstandingCommitments(
                organizationId,
                projectId
            );

        BigDecimal forecastCost =
            actualCost.add(
                committedCost
            );

        BigDecimal budgetRemaining =
            revisedBudget.subtract(
                actualCost
            );

        BigDecimal forecastBudgetVariance =
            revisedBudget.subtract(
                forecastCost
            );

        BigDecimal originalContractValue =
            originalContractValue(
                organizationId,
                projectId
            );

        BigDecimal clientChanges =
            approvedChangeOrderImpact(
                organizationId,
                projectId,
                "client_price_impact"
            );

        BigDecimal revisedContractValue =
            originalContractValue.add(
                clientChanges
            );

        BigDecimal totalInvoiced =
            totalInvoiced(
                organizationId,
                projectId
            );

        BigDecimal totalPaid =
            totalPaid(
                organizationId,
                projectId
            );

        BigDecimal outstandingReceivables =
            totalInvoiced.subtract(
                totalPaid
            );

        if (
            outstandingReceivables
                .signum()
                < 0
        ) {
            outstandingReceivables =
                ZERO;
        }

        BigDecimal forecastProfit =
            revisedContractValue.subtract(
                forecastCost
            );

        BigDecimal margin =
            revisedContractValue
                    .signum()
                == 0
                ? ZERO
                : forecastProfit
                    .multiply(
                        BigDecimal.valueOf(
                            100
                        )
                    )
                    .divide(
                        revisedContractValue,
                        2,
                        RoundingMode.HALF_UP
                    );

        return new DashboardDtos
            .FinancialSnapshot(
                money(originalBudget),
                money(revisedBudget),
                money(actualCost),
                money(committedCost),
                money(forecastCost),
                money(budgetRemaining),
                money(
                    forecastBudgetVariance
                ),
                money(
                    originalContractValue
                ),
                money(clientChanges),
                money(
                    revisedContractValue
                ),
                money(totalInvoiced),
                money(totalPaid),
                money(
                    outstandingReceivables
                ),
                money(forecastProfit),
                money(margin)
            );
    }

    private BigDecimal originalBudget(
        UUID organizationId,
        UUID projectId
    ) {
        Set<String> projectColumns =
            columns(
                "projects"
            );

        String projectBudgetColumn =
            firstColumn(
                projectColumns,
                "original_budget",
                "budget_baseline",
                "baseline_budget"
            );

        if (
            projectBudgetColumn
                != null
        ) {
            return scalarMoney(
                """
                SELECT COALESCE(
                    %s,
                    0
                )
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                """.formatted(
                    identifier(
                        projectBudgetColumn
                    )
                ),
                projectId,
                organizationId
            );
        }

        String baselineTable =
            firstExistingTable(
                "project_budget_baselines",
                "budget_baselines"
            );

        if (
            baselineTable
                != null
        ) {
            Set<String> baselineColumns =
                columns(
                    baselineTable
                );

            String amount =
                firstColumn(
                    baselineColumns,
                    "original_budget",
                    "baseline_amount",
                    "amount",
                    "total"
                );

            if (
                amount != null
                && baselineColumns
                    .contains(
                        "project_id"
                    )
            ) {
                return scalarMoney(
                    """
                    SELECT COALESCE(
                        SUM(%s),
                        0
                    )
                    FROM %s
                    WHERE project_id = ?
                    %s
                    """.formatted(
                        identifier(amount),
                        identifier(
                            baselineTable
                        ),
                        baselineColumns
                                .contains(
                                    "organization_id"
                                )
                            ? "AND organization_id = ?"
                            : ""
                    ),
                    baselineColumns
                            .contains(
                                "organization_id"
                            )
                        ? new Object[] {
                            projectId,
                            organizationId
                        }
                        : new Object[] {
                            projectId
                        }
                );
            }
        }

        String itemTable =
            firstExistingTable(
                "budget_items",
                "project_budget_items"
            );

        if (itemTable == null) {
            return ZERO;
        }

        Set<String> itemColumns =
            columns(
                itemTable
            );

        String amount =
            firstColumn(
                itemColumns,
                "planned_amount",
                "budgeted_amount",
                "amount",
                "total_amount"
            );

        if (amount == null) {
            return ZERO;
        }

        if (
            itemColumns.contains(
                "project_id"
            )
        ) {
            return scalarMoney(
                """
                SELECT COALESCE(
                    SUM(%s),
                    0
                )
                FROM %s
                WHERE project_id = ?
                %s
                """.formatted(
                    identifier(amount),
                    identifier(
                        itemTable
                    ),
                    itemColumns.contains(
                        "organization_id"
                    )
                        ? "AND organization_id = ?"
                        : ""
                ),
                itemColumns.contains(
                    "organization_id"
                )
                    ? new Object[] {
                        projectId,
                        organizationId
                    }
                    : new Object[] {
                        projectId
                    }
            );
        }

        String categoryId =
            firstColumn(
                itemColumns,
                "budget_category_id",
                "category_id"
            );

        String categoryTable =
            firstExistingTable(
                "budget_categories",
                "project_budget_categories"
            );

        if (
            categoryId == null
            || categoryTable == null
        ) {
            return ZERO;
        }

        Set<String> categoryColumns =
            columns(
                categoryTable
            );

        if (
            !categoryColumns
                .contains(
                    "project_id"
                )
        ) {
            return ZERO;
        }

        return scalarMoney(
            """
            SELECT COALESCE(
                SUM(items.%s),
                0
            )
            FROM %s items
            JOIN %s categories
              ON categories.id =
                    items.%s
            WHERE categories.project_id = ?
            %s
            """.formatted(
                identifier(amount),
                identifier(itemTable),
                identifier(
                    categoryTable
                ),
                identifier(categoryId),
                categoryColumns
                        .contains(
                            "organization_id"
                        )
                    ? "AND categories.organization_id = ?"
                    : ""
            ),
            categoryColumns
                    .contains(
                        "organization_id"
                    )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private BigDecimal approvedExpenses(
        UUID organizationId,
        UUID projectId
    ) {
        String table =
            firstExistingTable(
                "expenses"
            );

        if (table == null) {
            return ZERO;
        }

        Set<String> columns =
            columns(table);

        String amount =
            firstColumn(
                columns,
                "total_amount",
                "amount",
                "gross_amount",
                "net_amount"
            );

        if (
            amount == null
            || !columns.contains(
                "project_id"
            )
            || !columns.contains(
                "status"
            )
        ) {
            return ZERO;
        }

        return scalarMoney(
            """
            SELECT COALESCE(
                SUM(%s),
                0
            )
            FROM %s
            WHERE project_id = ?
              AND UPPER(
                    CAST(status AS text)
                  ) = 'APPROVED'
            %s
            """.formatted(
                identifier(amount),
                identifier(table),
                columns.contains(
                    "organization_id"
                )
                    ? "AND organization_id = ?"
                    : ""
            ),
            columns.contains(
                "organization_id"
            )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private BigDecimal
        outstandingCommitments(
            UUID organizationId,
            UUID projectId
        ) {

        String poTable =
            firstExistingTable(
                "purchase_orders"
            );

        if (poTable == null) {
            return ZERO;
        }

        Set<String> poColumns =
            columns(poTable);

        String totalColumn =
            firstColumn(
                poColumns,
                "total",
                "total_amount"
            );

        if (
            totalColumn == null
            || !poColumns.contains(
                "project_id"
            )
            || !poColumns.contains(
                "status"
            )
        ) {
            return ZERO;
        }

        List<PoRow> purchaseOrders =
            jdbcTemplate.query(
                """
                SELECT
                    id,
                    %s AS po_total
                FROM %s
                WHERE project_id = ?
                  AND UPPER(
                        CAST(status AS text)
                      ) IN (
                        'APPROVED',
                        'ORDERED',
                        'PARTIALLY_RECEIVED',
                        'RECEIVED'
                      )
                %s
                """.formatted(
                    identifier(
                        totalColumn
                    ),
                    identifier(
                        poTable
                    ),
                    poColumns.contains(
                        "organization_id"
                    )
                        ? "AND organization_id = ?"
                        : ""
                ),
                (resultSet, rowNumber) ->
                    new PoRow(
                        resultSet.getObject(
                            "id",
                            UUID.class
                        ),
                        nullableMoney(
                            resultSet
                                .getBigDecimal(
                                    "po_total"
                                )
                        )
                    ),
                poColumns.contains(
                    "organization_id"
                )
                    ? new Object[] {
                        projectId,
                        organizationId
                    }
                    : new Object[] {
                        projectId
                    }
            );

        if (
            purchaseOrders.isEmpty()
        ) {
            return ZERO;
        }

        String expenseTable =
            firstExistingTable(
                "expenses"
            );

        Set<String> expenseColumns =
            expenseTable == null
                ? Set.of()
                : columns(
                    expenseTable
                );

        String expenseAmount =
            firstColumn(
                expenseColumns,
                "total_amount",
                "amount",
                "gross_amount",
                "net_amount"
            );

        BigDecimal commitment =
            ZERO;

        for (
            PoRow purchaseOrder
            : purchaseOrders
        ) {
            BigDecimal linkedExpenses =
                ZERO;

            if (
                expenseTable != null
                && expenseAmount
                    != null
                && expenseColumns
                    .contains(
                        "purchase_order_id"
                    )
                && expenseColumns
                    .contains(
                        "status"
                    )
            ) {
                linkedExpenses =
                    scalarMoney(
                        """
                        SELECT COALESCE(
                            SUM(%s),
                            0
                        )
                        FROM %s
                        WHERE purchase_order_id = ?
                          AND UPPER(
                                CAST(status AS text)
                              ) = 'APPROVED'
                        %s
                        """.formatted(
                            identifier(
                                expenseAmount
                            ),
                            identifier(
                                expenseTable
                            ),
                            expenseColumns
                                    .contains(
                                        "organization_id"
                                    )
                                ? "AND organization_id = ?"
                                : ""
                        ),
                        expenseColumns
                                .contains(
                                    "organization_id"
                                )
                            ? new Object[] {
                                purchaseOrder.id(),
                                organizationId
                            }
                            : new Object[] {
                                purchaseOrder.id()
                            }
                    );
            }

            BigDecimal outstanding =
                purchaseOrder.total()
                    .subtract(
                        linkedExpenses
                    );

            if (
                outstanding.signum()
                    > 0
            ) {
                commitment =
                    commitment.add(
                        outstanding
                    );
            }
        }

        return money(commitment);
    }

    private BigDecimal
        approvedChangeOrderImpact(
            UUID organizationId,
            UUID projectId,
            String requestedColumn
        ) {

        String table =
            firstExistingTable(
                "change_orders"
            );

        if (table == null) {
            return ZERO;
        }

        Set<String> columns =
            columns(table);

        if (
            !columns.contains(
                requestedColumn
            )
            || !columns.contains(
                "project_id"
            )
            || !columns.contains(
                "status"
            )
        ) {
            return ZERO;
        }

        return scalarMoney(
            """
            SELECT COALESCE(
                SUM(%s),
                0
            )
            FROM %s
            WHERE project_id = ?
              AND UPPER(
                    CAST(status AS text)
                  ) = 'APPROVED'
            %s
            """.formatted(
                identifier(
                    requestedColumn
                ),
                identifier(table),
                columns.contains(
                    "organization_id"
                )
                    ? "AND organization_id = ?"
                    : ""
            ),
            columns.contains(
                "organization_id"
            )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private BigDecimal
        originalContractValue(
            UUID organizationId,
            UUID projectId
        ) {

        Set<String> projectColumns =
            columns(
                "projects"
            );

        String projectColumn =
            firstColumn(
                projectColumns,
                "contract_value",
                "original_contract_value"
            );

        if (projectColumn != null) {
            return scalarMoney(
                """
                SELECT COALESCE(
                    %s,
                    0
                )
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                """.formatted(
                    identifier(
                        projectColumn
                    )
                ),
                projectId,
                organizationId
            );
        }

        String quoteTable =
            firstExistingTable(
                "quotes",
                "quotations"
            );

        if (quoteTable == null) {
            return ZERO;
        }

        Set<String> quoteColumns =
            columns(
                quoteTable
            );

        String total =
            firstColumn(
                quoteColumns,
                "total",
                "total_amount"
            );

        String contractFlag =
            firstColumn(
                quoteColumns,
                "contract_quote",
                "is_contract_quote"
            );

        if (
            total == null
            || contractFlag == null
            || !quoteColumns
                .contains(
                    "project_id"
                )
            || !quoteColumns
                .contains(
                    "status"
                )
        ) {
            return ZERO;
        }

        return scalarMoney(
            """
            SELECT COALESCE(
                MAX(%s),
                0
            )
            FROM %s
            WHERE project_id = ?
              AND %s = TRUE
              AND UPPER(
                    CAST(status AS text)
                  ) = 'ACCEPTED'
            %s
            """.formatted(
                identifier(total),
                identifier(
                    quoteTable
                ),
                identifier(
                    contractFlag
                ),
                quoteColumns
                        .contains(
                            "organization_id"
                        )
                    ? "AND organization_id = ?"
                    : ""
            ),
            quoteColumns
                    .contains(
                        "organization_id"
                    )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private BigDecimal totalInvoiced(
        UUID organizationId,
        UUID projectId
    ) {
        String table =
            firstExistingTable(
                "invoices"
            );

        if (table == null) {
            return ZERO;
        }

        Set<String> columns =
            columns(table);

        String total =
            firstColumn(
                columns,
                "total",
                "total_amount"
            );

        if (
            total == null
            || !columns.contains(
                "project_id"
            )
            || !columns.contains(
                "status"
            )
        ) {
            return ZERO;
        }

        return scalarMoney(
            """
            SELECT COALESCE(
                SUM(%s),
                0
            )
            FROM %s
            WHERE project_id = ?
              AND UPPER(
                    CAST(status AS text)
                  ) NOT IN (
                    'DRAFT',
                    'VOID',
                    'CANCELLED',
                    'CANCELED'
                  )
            %s
            """.formatted(
                identifier(total),
                identifier(table),
                columns.contains(
                    "organization_id"
                )
                    ? "AND organization_id = ?"
                    : ""
            ),
            columns.contains(
                "organization_id"
            )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private BigDecimal totalPaid(
        UUID organizationId,
        UUID projectId
    ) {
        String paymentTable =
            firstExistingTable(
                "payments"
            );

        String invoiceTable =
            firstExistingTable(
                "invoices"
            );

        if (
            paymentTable != null
            && invoiceTable != null
        ) {
            Set<String> paymentColumns =
                columns(
                    paymentTable
                );

            Set<String> invoiceColumns =
                columns(
                    invoiceTable
                );

            String amount =
                firstColumn(
                    paymentColumns,
                    "amount",
                    "paid_amount"
                );

            if (
                amount != null
                && paymentColumns
                    .contains(
                        "invoice_id"
                    )
                && invoiceColumns
                    .contains(
                        "project_id"
                    )
            ) {
                return scalarMoney(
                    """
                    SELECT COALESCE(
                        SUM(payments.%s),
                        0
                    )
                    FROM %s payments
                    JOIN %s invoices
                      ON invoices.id =
                            payments.invoice_id
                    WHERE invoices.project_id = ?
                    %s
                    """.formatted(
                        identifier(amount),
                        identifier(
                            paymentTable
                        ),
                        identifier(
                            invoiceTable
                        ),
                        invoiceColumns
                                .contains(
                                    "organization_id"
                                )
                            ? "AND invoices.organization_id = ?"
                            : ""
                    ),
                    invoiceColumns
                            .contains(
                                "organization_id"
                            )
                        ? new Object[] {
                            projectId,
                            organizationId
                        }
                        : new Object[] {
                            projectId
                        }
                );
            }
        }

        if (invoiceTable == null) {
            return ZERO;
        }

        Set<String> invoiceColumns =
            columns(invoiceTable);

        String amountPaid =
            firstColumn(
                invoiceColumns,
                "amount_paid",
                "paid_amount"
            );

        if (
            amountPaid == null
            || !invoiceColumns
                .contains(
                    "project_id"
                )
        ) {
            return ZERO;
        }

        return scalarMoney(
            """
            SELECT COALESCE(
                SUM(%s),
                0
            )
            FROM %s
            WHERE project_id = ?
            %s
            """.formatted(
                identifier(
                    amountPaid
                ),
                identifier(
                    invoiceTable
                ),
                invoiceColumns
                        .contains(
                            "organization_id"
                        )
                    ? "AND organization_id = ?"
                    : ""
            ),
            invoiceColumns
                    .contains(
                        "organization_id"
                    )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private DashboardDtos.ProjectHealth
        health(
            UUID organizationId,
            ProjectRow project,
            DashboardDtos.FinancialSnapshot
                financial
        ) {

        if (
            !"ACTIVE".equals(
                project.status()
            )
        ) {
            return new DashboardDtos
                .ProjectHealth(
                    "NOT_APPLICABLE",
                    List.of()
                );
        }

        List<String> critical =
            new ArrayList<>();

        List<String> atRisk =
            new ArrayList<>();

        if (
            financial.revisedBudget()
                    .signum()
                > 0
            && financial.forecastCost()
                .compareTo(
                    financial
                        .revisedBudget()
                )
                > 0
        ) {
            BigDecimal overrun =
                financial.forecastCost()
                    .subtract(
                        financial
                            .revisedBudget()
                    )
                    .divide(
                        financial
                            .revisedBudget(),
                        4,
                        RoundingMode.HALF_UP
                    );

            if (
                overrun.compareTo(
                    new BigDecimal(
                        "0.10"
                    )
                ) >= 0
            ) {
                critical.add(
                    "Forecast cost exceeds the revised budget by at least 10%."
                );
            } else {
                atRisk.add(
                    "Forecast cost exceeds the revised budget by less than 10%."
                );
            }
        }

        if (
            criticalIncidentCount(
                organizationId,
                project.id()
            ) > 0
        ) {
            critical.add(
                "A critical unresolved incident exists."
            );
        }

        if (
            project.expectedCompletion()
                != null
            && project.expectedCompletion()
                .isBefore(
                    LocalDate.now()
                        .minusDays(
                            SIGNIFICANT_DELAY_DAYS
                        )
                )
        ) {
            critical.add(
                "Expected completion is more than 30 days overdue."
            );
        }

        if (
            overdueTaskCount(
                organizationId,
                project.id(),
                true
            ) > 0
        ) {
            atRisk.add(
                "High-priority tasks are overdue."
            );
        }

        if (
            overdueMilestoneCount(
                organizationId,
                project.id()
            ) > 0
        ) {
            atRisk.add(
                "A milestone is overdue."
            );
        }

        if (!critical.isEmpty()) {
            return new DashboardDtos
                .ProjectHealth(
                    "CRITICAL",
                    critical
                );
        }

        if (!atRisk.isEmpty()) {
            return new DashboardDtos
                .ProjectHealth(
                    "AT_RISK",
                    atRisk
                );
        }

        return new DashboardDtos
            .ProjectHealth(
                "ON_TRACK",
                List.of()
            );
    }

    private long openTaskCount(
        UUID organizationId,
        UUID projectId
    ) {
        String table =
            taskTable();

        if (table == null) {
            return 0L;
        }

        Set<String> columns =
            columns(table);

        if (
            !hasColumns(
                columns,
                "project_id",
                "status"
            )
        ) {
            return 0L;
        }

        return scalarLong(
            """
            SELECT COUNT(*)
            FROM %s
            WHERE project_id = ?
              AND UPPER(
                    CAST(status AS text)
                  ) NOT IN (
                    'DONE',
                    'COMPLETED',
                    'CANCELLED',
                    'CANCELED'
                  )
            %s
            """.formatted(
                identifier(table),
                columns.contains(
                    "organization_id"
                )
                    ? "AND organization_id = ?"
                    : ""
            ),
            columns.contains(
                "organization_id"
            )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private long overdueTaskCount(
        UUID organizationId,
        UUID projectId,
        boolean highPriorityOnly
    ) {
        String table =
            taskTable();

        if (table == null) {
            return 0L;
        }

        Set<String> columns =
            columns(table);

        String due =
            firstColumn(
                columns,
                "due_date",
                "planned_end_date",
                "end_date",
                "target_date"
            );

        if (
            due == null
            || !hasColumns(
                columns,
                "project_id",
                "status"
            )
        ) {
            return 0L;
        }

        String priorityClause = "";

        if (highPriorityOnly) {
            String priority =
                firstColumn(
                    columns,
                    "priority"
                );

            if (priority == null) {
                return 0L;
            }

            priorityClause =
                """
                AND UPPER(
                    CAST(%s AS text)
                ) IN (
                    'HIGH',
                    'CRITICAL'
                )
                """.formatted(
                    identifier(
                        priority
                    )
                );
        }

        return scalarLong(
            """
            SELECT COUNT(*)
            FROM %s
            WHERE project_id = ?
              AND CAST(%s AS date)
                    < CURRENT_DATE
              AND UPPER(
                    CAST(status AS text)
                  ) NOT IN (
                    'DONE',
                    'COMPLETED',
                    'CANCELLED',
                    'CANCELED'
                  )
            %s
            %s
            """.formatted(
                identifier(table),
                identifier(due),
                priorityClause,
                columns.contains(
                    "organization_id"
                )
                    ? "AND organization_id = ?"
                    : ""
            ),
            columns.contains(
                "organization_id"
            )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private long upcomingMilestoneCount(
        UUID organizationId,
        UUID projectId
    ) {
        String table =
            milestoneTable();

        if (table == null) {
            return 0L;
        }

        Set<String> columns =
            columns(table);

        String due =
            milestoneDateColumn(
                columns
            );

        if (
            due == null
            || !hasColumns(
                columns,
                "project_id",
                "status"
            )
        ) {
            return 0L;
        }

        return scalarLong(
            """
            SELECT COUNT(*)
            FROM %s
            WHERE project_id = ?
              AND CAST(%s AS date)
                    BETWEEN CURRENT_DATE
                        AND CURRENT_DATE + 30
              AND UPPER(
                    CAST(status AS text)
                  ) NOT IN (
                    'DONE',
                    'COMPLETED',
                    'CANCELLED',
                    'CANCELED'
                  )
            %s
            """.formatted(
                identifier(table),
                identifier(due),
                columns.contains(
                    "organization_id"
                )
                    ? "AND organization_id = ?"
                    : ""
            ),
            columns.contains(
                "organization_id"
            )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private long overdueMilestoneCount(
        UUID organizationId,
        UUID projectId
    ) {
        String table =
            milestoneTable();

        if (table == null) {
            return 0L;
        }

        Set<String> columns =
            columns(table);

        String due =
            milestoneDateColumn(
                columns
            );

        if (
            due == null
            || !hasColumns(
                columns,
                "project_id",
                "status"
            )
        ) {
            return 0L;
        }

        return scalarLong(
            """
            SELECT COUNT(*)
            FROM %s
            WHERE project_id = ?
              AND CAST(%s AS date)
                    < CURRENT_DATE
              AND UPPER(
                    CAST(status AS text)
                  ) NOT IN (
                    'DONE',
                    'COMPLETED',
                    'CANCELLED',
                    'CANCELED'
                  )
            %s
            """.formatted(
                identifier(table),
                identifier(due),
                columns.contains(
                    "organization_id"
                )
                    ? "AND organization_id = ?"
                    : ""
            ),
            columns.contains(
                "organization_id"
            )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private long openIncidentCount(
        UUID organizationId,
        UUID projectId
    ) {
        String table =
            firstExistingTable(
                "site_incidents",
                "incidents"
            );

        if (table == null) {
            return 0L;
        }

        Set<String> columns =
            columns(table);

        if (
            !hasColumns(
                columns,
                "project_id",
                "status"
            )
        ) {
            return 0L;
        }

        return scalarLong(
            """
            SELECT COUNT(*)
            FROM %s
            WHERE project_id = ?
              AND UPPER(
                    CAST(status AS text)
                  ) NOT IN (
                    'RESOLVED',
                    'CLOSED'
                  )
            %s
            """.formatted(
                identifier(table),
                columns.contains(
                    "organization_id"
                )
                    ? "AND organization_id = ?"
                    : ""
            ),
            columns.contains(
                "organization_id"
            )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private long criticalIncidentCount(
        UUID organizationId,
        UUID projectId
    ) {
        String table =
            firstExistingTable(
                "site_incidents",
                "incidents"
            );

        if (table == null) {
            return 0L;
        }

        Set<String> columns =
            columns(table);

        if (
            !hasColumns(
                columns,
                "project_id",
                "status",
                "severity"
            )
        ) {
            return 0L;
        }

        return scalarLong(
            """
            SELECT COUNT(*)
            FROM %s
            WHERE project_id = ?
              AND UPPER(
                    CAST(severity AS text)
                  ) = 'CRITICAL'
              AND UPPER(
                    CAST(status AS text)
                  ) NOT IN (
                    'RESOLVED',
                    'CLOSED'
                  )
            %s
            """.formatted(
                identifier(table),
                columns.contains(
                    "organization_id"
                )
                    ? "AND organization_id = ?"
                    : ""
            ),
            columns.contains(
                "organization_id"
            )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private List<DashboardDtos.StatusCount>
        taskStatus(
            UUID organizationId,
            UUID projectId
        ) {

        String table =
            taskTable();

        if (table == null) {
            return List.of();
        }

        Set<String> columns =
            columns(table);

        if (
            !hasColumns(
                columns,
                "project_id",
                "status"
            )
        ) {
            return List.of();
        }

        return jdbcTemplate.query(
            """
            SELECT
                UPPER(
                    CAST(status AS text)
                ) AS item_status,
                COUNT(*) AS item_count
            FROM %s
            WHERE project_id = ?
            %s
            GROUP BY
                UPPER(
                    CAST(status AS text)
                )
            ORDER BY item_status
            """.formatted(
                identifier(table),
                columns.contains(
                    "organization_id"
                )
                    ? "AND organization_id = ?"
                    : ""
            ),
            (resultSet, rowNumber) ->
                new DashboardDtos
                    .StatusCount(
                        resultSet.getString(
                            "item_status"
                        ),
                        resultSet.getLong(
                            "item_count"
                        )
                    ),
            columns.contains(
                "organization_id"
            )
                ? new Object[] {
                    projectId,
                    organizationId
                }
                : new Object[] {
                    projectId
                }
        );
    }

    private List<DashboardDtos.StatusCount>
        projectStatus(
            List<ProjectRow> projects
        ) {

        Map<String, Long> counts =
            new LinkedHashMap<>();

        counts.put(
            "PLANNING",
            0L
        );

        counts.put(
            "ACTIVE",
            0L
        );

        counts.put(
            "ON_HOLD",
            0L
        );

        counts.put(
            "COMPLETED",
            0L
        );

        for (
            ProjectRow project
            : projects
        ) {
            if (
                counts.containsKey(
                    project.status()
                )
            ) {
                counts.put(
                    project.status(),
                    counts.get(
                        project.status()
                    ) + 1
                );
            }
        }

        return counts.entrySet()
            .stream()
            .map(
                entry ->
                    new DashboardDtos
                        .StatusCount(
                            entry.getKey(),
                            entry.getValue()
                        )
            )
            .toList();
    }

    private List<DashboardDtos.MonthlyValue>
        monthlyExpenses(
            UUID organizationId,
            UUID projectId
        ) {

        Map<String, BigDecimal> values =
            emptyMonths();

        String table =
            firstExistingTable(
                "expenses"
            );

        if (table == null) {
            return monthValues(
                values
            );
        }

        Set<String> columns =
            columns(table);

        String date =
            firstColumn(
                columns,
                "expense_date",
                "created_at"
            );

        String amount =
            firstColumn(
                columns,
                "total_amount",
                "amount",
                "gross_amount",
                "net_amount"
            );

        if (
            date == null
            || amount == null
            || !columns.contains(
                "status"
            )
        ) {
            return monthValues(
                values
            );
        }

        List<Object> arguments =
            new ArrayList<>();

        String projectClause = "";

        if (
            projectId != null
            && columns.contains(
                "project_id"
            )
        ) {
            projectClause =
                "AND project_id = ?";

            arguments.add(
                projectId
            );
        }

        String organizationClause =
            "";

        if (
            columns.contains(
                "organization_id"
            )
        ) {
            organizationClause =
                "AND organization_id = ?";

            arguments.add(
                organizationId
            );
        }

        List<MonthRow> rows =
            jdbcTemplate.query(
                """
                SELECT
                    TO_CHAR(
                        DATE_TRUNC(
                            'month',
                            %s
                        ),
                        'YYYY-MM'
                    ) AS month_key,
                    COALESCE(
                        SUM(%s),
                        0
                    ) AS month_value
                FROM %s
                WHERE %s >=
                    DATE_TRUNC(
                        'month',
                        CURRENT_DATE
                    ) - INTERVAL '11 months'
                  AND UPPER(
                        CAST(status AS text)
                      ) = 'APPROVED'
                %s
                %s
                GROUP BY
                    DATE_TRUNC(
                        'month',
                        %s
                    )
                ORDER BY
                    DATE_TRUNC(
                        'month',
                        %s
                    )
                """.formatted(
                    identifier(date),
                    identifier(amount),
                    identifier(table),
                    identifier(date),
                    projectClause,
                    organizationClause,
                    identifier(date),
                    identifier(date)
                ),
                (resultSet, rowNumber) ->
                    new MonthRow(
                        resultSet.getString(
                            "month_key"
                        ),
                        nullableMoney(
                            resultSet.getBigDecimal(
                                "month_value"
                            )
                        )
                    ),
                arguments.toArray()
            );

        for (MonthRow row : rows) {
            if (
                values.containsKey(
                    row.month()
                )
            ) {
                values.put(
                    row.month(),
                    money(
                        row.value()
                    )
                );
            }
        }

        return monthValues(values);
    }

    private List<DashboardDtos.MonthlyValue>
        monthlyRevenue(
            UUID organizationId,
            UUID projectId
        ) {

        Map<String, BigDecimal> values =
            emptyMonths();

        String paymentTable =
            firstExistingTable(
                "payments"
            );

        String invoiceTable =
            firstExistingTable(
                "invoices"
            );

        if (
            paymentTable == null
            || invoiceTable == null
        ) {
            return monthValues(
                values
            );
        }

        Set<String> paymentColumns =
            columns(paymentTable);

        Set<String> invoiceColumns =
            columns(invoiceTable);

        String date =
            firstColumn(
                paymentColumns,
                "payment_date",
                "paid_at",
                "created_at"
            );

        String amount =
            firstColumn(
                paymentColumns,
                "amount",
                "paid_amount"
            );

        if (
            date == null
            || amount == null
            || !paymentColumns
                .contains(
                    "invoice_id"
                )
        ) {
            return monthValues(
                values
            );
        }

        List<Object> arguments =
            new ArrayList<>();

        String organizationClause =
            "";

        if (
            invoiceColumns.contains(
                "organization_id"
            )
        ) {
            organizationClause =
                "AND invoices.organization_id = ?";

            arguments.add(
                organizationId
            );
        }

        String projectClause = "";

        if (
            projectId != null
            && invoiceColumns.contains(
                "project_id"
            )
        ) {
            projectClause =
                "AND invoices.project_id = ?";

            arguments.add(
                projectId
            );
        }

        List<MonthRow> rows =
            jdbcTemplate.query(
                """
                SELECT
                    TO_CHAR(
                        DATE_TRUNC(
                            'month',
                            payments.%s
                        ),
                        'YYYY-MM'
                    ) AS month_key,
                    COALESCE(
                        SUM(
                            payments.%s
                        ),
                        0
                    ) AS month_value
                FROM %s payments
                JOIN %s invoices
                  ON invoices.id =
                        payments.invoice_id
                WHERE payments.%s >=
                    DATE_TRUNC(
                        'month',
                        CURRENT_DATE
                    ) - INTERVAL '11 months'
                %s
                %s
                GROUP BY
                    DATE_TRUNC(
                        'month',
                        payments.%s
                    )
                ORDER BY
                    DATE_TRUNC(
                        'month',
                        payments.%s
                    )
                """.formatted(
                    identifier(date),
                    identifier(amount),
                    identifier(
                        paymentTable
                    ),
                    identifier(
                        invoiceTable
                    ),
                    identifier(date),
                    organizationClause,
                    projectClause,
                    identifier(date),
                    identifier(date)
                ),
                (resultSet, rowNumber) ->
                    new MonthRow(
                        resultSet.getString(
                            "month_key"
                        ),
                        nullableMoney(
                            resultSet.getBigDecimal(
                                "month_value"
                            )
                        )
                    ),
                arguments.toArray()
            );

        for (MonthRow row : rows) {
            if (
                values.containsKey(
                    row.month()
                )
            ) {
                values.put(
                    row.month(),
                    money(
                        row.value()
                    )
                );
            }
        }

        return monthValues(values);
    }

    private List<DashboardDtos.MonthlyValue>
        cumulative(
            List<DashboardDtos.MonthlyValue>
                monthly
        ) {

        BigDecimal running =
            ZERO;

        List<DashboardDtos.MonthlyValue>
            result =
                new ArrayList<>();

        for (
            DashboardDtos.MonthlyValue
                value
            : monthly
        ) {
            running =
                running.add(
                    value.value()
                );

            result.add(
                new DashboardDtos
                    .MonthlyValue(
                        value.month(),
                        money(running)
                    )
            );
        }

        return result;
    }

    private List<DashboardDtos.Deadline>
        upcomingDeadlines(
            UUID organizationId,
            UUID projectId,
            List<ProjectRow> projects,
            int limit
        ) {

        Map<UUID, String>
            projectNames =
                new HashMap<>();

        for (
            ProjectRow project
            : projects
        ) {
            projectNames.put(
                project.id(),
                project.name()
            );
        }

        List<DashboardDtos.Deadline>
            deadlines =
                new ArrayList<>();

        collectTaskDeadlines(
            organizationId,
            projectId,
            projectNames,
            deadlines
        );

        collectMilestoneDeadlines(
            organizationId,
            projectId,
            projectNames,
            deadlines
        );

        deadlines.sort(
            Comparator.comparing(
                DashboardDtos
                    .Deadline::dueDate
            )
        );

        if (
            deadlines.size()
                > limit
        ) {
            return new ArrayList<>(
                deadlines.subList(
                    0,
                    limit
                )
            );
        }

        return deadlines;
    }

    private void collectTaskDeadlines(
        UUID organizationId,
        UUID projectId,
        Map<UUID, String> projectNames,
        List<DashboardDtos.Deadline>
            target
    ) {
        String table =
            taskTable();

        if (table == null) {
            return;
        }

        Set<String> columns =
            columns(table);

        String due =
            firstColumn(
                columns,
                "due_date",
                "planned_end_date",
                "end_date",
                "target_date"
            );

        String title =
            firstColumn(
                columns,
                "title",
                "name",
                "task_number",
                "description"
            );

        String priority =
            firstColumn(
                columns,
                "priority"
            );

        if (
            due == null
            || !hasColumns(
                columns,
                "id",
                "project_id",
                "status"
            )
        ) {
            return;
        }

        List<Object> arguments =
            new ArrayList<>();

        String projectClause = "";

        if (projectId != null) {
            projectClause =
                "AND project_id = ?";

            arguments.add(
                projectId
            );
        }

        if (
            columns.contains(
                "organization_id"
            )
        ) {
            arguments.add(
                organizationId
            );
        }

        target.addAll(
            jdbcTemplate.query(
                """
                SELECT
                    id,
                    project_id,
                    %s AS item_title,
                    CAST(%s AS date)
                        AS due_value,
                    %s AS priority_value
                FROM %s
                WHERE CAST(%s AS date)
                        BETWEEN CURRENT_DATE
                            AND CURRENT_DATE + 30
                  AND UPPER(
                        CAST(status AS text)
                      ) NOT IN (
                        'DONE',
                        'COMPLETED',
                        'CANCELLED',
                        'CANCELED'
                      )
                %s
                %s
                """.formatted(
                    labelExpression(
                        title
                    ),
                    identifier(due),
                    priority == null
                        ? "'MEDIUM'"
                        : "CAST("
                            + identifier(
                                priority
                            )
                            + " AS text)",
                    identifier(table),
                    identifier(due),
                    projectClause,
                    columns.contains(
                        "organization_id"
                    )
                        ? "AND organization_id = ?"
                        : ""
                ),
                (resultSet, rowNumber) ->
                    deadline(
                        resultSet,
                        projectNames,
                        "TASK"
                    ),
                arguments.toArray()
            )
        );
    }

    private void collectMilestoneDeadlines(
        UUID organizationId,
        UUID projectId,
        Map<UUID, String> projectNames,
        List<DashboardDtos.Deadline>
            target
    ) {
        String table =
            milestoneTable();

        if (table == null) {
            return;
        }

        Set<String> columns =
            columns(table);

        String due =
            milestoneDateColumn(
                columns
            );

        String title =
            firstColumn(
                columns,
                "title",
                "name",
                "milestone_number",
                "description"
            );

        if (
            due == null
            || !hasColumns(
                columns,
                "id",
                "project_id",
                "status"
            )
        ) {
            return;
        }

        List<Object> arguments =
            new ArrayList<>();

        String projectClause = "";

        if (projectId != null) {
            projectClause =
                "AND project_id = ?";

            arguments.add(
                projectId
            );
        }

        if (
            columns.contains(
                "organization_id"
            )
        ) {
            arguments.add(
                organizationId
            );
        }

        target.addAll(
            jdbcTemplate.query(
                """
                SELECT
                    id,
                    project_id,
                    %s AS item_title,
                    CAST(%s AS date)
                        AS due_value,
                    'MEDIUM'
                        AS priority_value
                FROM %s
                WHERE CAST(%s AS date)
                        BETWEEN CURRENT_DATE
                            AND CURRENT_DATE + 30
                  AND UPPER(
                        CAST(status AS text)
                      ) NOT IN (
                        'DONE',
                        'COMPLETED',
                        'CANCELLED',
                        'CANCELED'
                      )
                %s
                %s
                """.formatted(
                    labelExpression(
                        title
                    ),
                    identifier(due),
                    identifier(table),
                    identifier(due),
                    projectClause,
                    columns.contains(
                        "organization_id"
                    )
                        ? "AND organization_id = ?"
                        : ""
                ),
                (resultSet, rowNumber) ->
                    deadline(
                        resultSet,
                        projectNames,
                        "MILESTONE"
                    ),
                arguments.toArray()
            )
        );
    }

    private DashboardDtos.Deadline
        deadline(
            ResultSet resultSet,
            Map<UUID, String> projectNames,
            String type
        ) throws SQLException {

        UUID id =
            resultSet.getObject(
                "id",
                UUID.class
            );

        UUID projectId =
            resultSet.getObject(
                "project_id",
                UUID.class
            );

        String title =
            resultSet.getString(
                "item_title"
            );

        if (
            title == null
            || title.isBlank()
        ) {
            title =
                id.toString();
        }

        Date due =
            resultSet.getDate(
                "due_value"
            );

        return new DashboardDtos
            .Deadline(
                id,
                projectId,
                projectNames.getOrDefault(
                    projectId,
                    "Project"
                ),
                type,
                title,
                due == null
                    ? null
                    : due.toLocalDate(),
                resultSet.getString(
                    "priority_value"
                )
            );
    }

    private List<ProjectRow> projects(
        UUID organizationId
    ) {
        Set<String> columns =
            columns(
                "projects"
            );

        String number =
            firstColumn(
                columns,
                "project_number",
                "number",
                "reference"
            );

        String progress =
            firstColumn(
                columns,
                "progress_percentage",
                "progress"
            );

        String start =
            firstColumn(
                columns,
                "start_date",
                "planned_start_date"
            );

        String end =
            firstColumn(
                columns,
                "expected_completion",
                "expected_completion_date",
                "planned_end_date",
                "end_date"
            );

        return jdbcTemplate.query(
            """
            SELECT
                id,
                %s AS project_number_value,
                name,
                UPPER(
                    CAST(status AS text)
                ) AS project_status,
                %s AS progress_value,
                %s AS start_value,
                %s AS end_value
            FROM projects
            WHERE organization_id = ?
            ORDER BY created_at DESC
            """.formatted(
                number == null
                    ? "CAST(id AS text)"
                    : "CAST("
                        + identifier(number)
                        + " AS text)",
                progress == null
                    ? "0"
                    : identifier(progress),
                start == null
                    ? "NULL::date"
                    : "CAST("
                        + identifier(start)
                        + " AS date)",
                end == null
                    ? "NULL::date"
                    : "CAST("
                        + identifier(end)
                        + " AS date)"
            ),
            this::projectRow,
            organizationId
        );
    }

    private ProjectRow project(
        UUID organizationId,
        UUID projectId
    ) {
        return projects(
            organizationId
        )
            .stream()
            .filter(
                project ->
                    project.id()
                        .equals(
                            projectId
                        )
            )
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND",
                        "The project does not exist."
                    )
            );
    }

    private ProjectRow projectRow(
        ResultSet resultSet,
        int rowNumber
    ) throws SQLException {

        Date start =
            resultSet.getDate(
                "start_value"
            );

        Date end =
            resultSet.getDate(
                "end_value"
            );

        BigDecimal progress =
            nullableMoney(
                resultSet.getBigDecimal(
                    "progress_value"
                )
            );

        return new ProjectRow(
            resultSet.getObject(
                "id",
                UUID.class
            ),
            resultSet.getString(
                "project_number_value"
            ),
            resultSet.getString(
                "name"
            ),
            resultSet.getString(
                "project_status"
            ),
            progress,
            start == null
                ? null
                : start.toLocalDate(),
            end == null
                ? null
                : end.toLocalDate()
        );
    }

    private String projectManager(
        UUID organizationId,
        UUID projectId
    ) {
        Set<String> projectColumns =
            columns(
                "projects"
            );

        String userColumn =
            firstColumn(
                projectColumns,
                "project_manager_user_id",
                "project_manager_id"
            );

        if (
            userColumn != null
            && tableExists(
                "app_users"
            )
        ) {
            Set<String> userColumns =
                columns(
                    "app_users"
                );

            if (
                userColumns.contains(
                    "first_name"
                )
                && userColumns.contains(
                    "last_name"
                )
            ) {
                List<String> values =
                    jdbcTemplate.query(
                        """
                        SELECT
                            CONCAT_WS(
                                ' ',
                                users.first_name,
                                users.last_name
                            )
                        FROM projects
                        JOIN app_users users
                          ON users.id =
                                projects.%s
                        WHERE projects.id = ?
                          AND projects.organization_id = ?
                        """.formatted(
                            identifier(
                                userColumn
                            )
                        ),
                        (resultSet, rowNumber) ->
                            resultSet
                                .getString(1),
                        projectId,
                        organizationId
                    );

                if (!values.isEmpty()) {
                    return values.getFirst();
                }
            }
        }

        return null;
    }

    private String organizationCurrency(
        UUID organizationId
    ) {
        Set<String> columns =
            columns(
                "organizations"
            );

        String currency =
            firstColumn(
                columns,
                "currency",
                "default_currency"
            );

        if (currency == null) {
            return "EUR";
        }

        List<String> values =
            jdbcTemplate.query(
                """
                SELECT CAST(%s AS text)
                FROM organizations
                WHERE id = ?
                """.formatted(
                    identifier(currency)
                ),
                (resultSet, rowNumber) ->
                    resultSet.getString(1),
                organizationId
            );

        if (
            values.isEmpty()
            || values.getFirst()
                == null
            || values.getFirst()
                .isBlank()
        ) {
            return "EUR";
        }

        return values.getFirst();
    }

    private Map<String, BigDecimal>
        emptyMonths() {

        Map<String, BigDecimal> values =
            new LinkedHashMap<>();

        YearMonth first =
            YearMonth.now()
                .minusMonths(11);

        for (int index = 0;
             index < 12;
             index++) {

            values.put(
                first
                    .plusMonths(index)
                    .toString(),
                ZERO
            );
        }

        return values;
    }

    private List<DashboardDtos.MonthlyValue>
        monthValues(
            Map<String, BigDecimal> values
        ) {
        return values.entrySet()
            .stream()
            .map(
                entry ->
                    new DashboardDtos
                        .MonthlyValue(
                            entry.getKey(),
                            money(
                                entry.getValue()
                            )
                        )
            )
            .toList();
    }

    private String taskTable() {
        return firstExistingTable(
            "project_tasks",
            "tasks"
        );
    }

    private String milestoneTable() {
        return firstExistingTable(
            "project_milestones",
            "milestones"
        );
    }

    private String milestoneDateColumn(
        Set<String> columns
    ) {
        return firstColumn(
            columns,
            "planned_date",
            "due_date",
            "target_date",
            "planned_end_date",
            "end_date"
        );
    }

    private String firstExistingTable(
        String... candidates
    ) {
        for (
            String candidate
            : candidates
        ) {
            if (
                tableExists(
                    candidate
                )
            ) {
                return candidate;
            }
        }

        return null;
    }

    private boolean tableExists(
        String table
    ) {
        Integer count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema =
                        'public'
                  AND table_name = ?
                """,
                Integer.class,
                table
            );

        return count != null
            && count > 0;
    }

    private Set<String> columns(
        String table
    ) {
        if (!tableExists(table)) {
            return Set.of();
        }

        return new HashSet<>(
            jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema =
                        'public'
                  AND table_name = ?
                """,
                String.class,
                table
            )
        );
    }

    private String firstColumn(
        Set<String> columns,
        String... candidates
    ) {
        for (
            String candidate
            : candidates
        ) {
            if (
                columns.contains(
                    candidate
                )
            ) {
                return candidate;
            }
        }

        return null;
    }

    private boolean hasColumns(
        Set<String> columns,
        String... required
    ) {
        for (
            String item
            : required
        ) {
            if (
                !columns.contains(
                    item
                )
            ) {
                return false;
            }
        }

        return true;
    }

    private String labelExpression(
        String column
    ) {
        if (column == null) {
            return "CAST(id AS text)";
        }

        return """
            COALESCE(
                NULLIF(
                    CAST(%s AS text),
                    ''
                ),
                CAST(id AS text)
            )
            """.formatted(
                identifier(column)
            );
    }

    private String identifier(
        String value
    ) {
        if (
            value == null
            || !IDENTIFIER_PATTERN
                .matcher(value)
                .matches()
        ) {
            throw new IllegalArgumentException(
                "Invalid SQL identifier."
            );
        }

        return "\""
            + value
            + "\"";
    }

    private BigDecimal scalarMoney(
        String sql,
        Object... arguments
    ) {
        BigDecimal value =
            jdbcTemplate.queryForObject(
                sql,
                BigDecimal.class,
                arguments
            );

        return nullableMoney(
            value
        );
    }

    private long scalarLong(
        String sql,
        Object... arguments
    ) {
        Long value =
            jdbcTemplate.queryForObject(
                sql,
                Long.class,
                arguments
            );

        return value == null
            ? 0L
            : value;
    }

    private static BigDecimal
        nullableMoney(
            BigDecimal value
        ) {
        return money(
            value == null
                ? BigDecimal.ZERO
                : value
        );
    }

    private static BigDecimal money(
        BigDecimal value
    ) {
        return value.setScale(
            2,
            RoundingMode.HALF_UP
        );
    }

    private static BigDecimal percentage(
        BigDecimal value,
        BigDecimal base
    ) {
        if (
            base == null
            || base.signum()
                == 0
        ) {
            return ZERO;
        }

        return value
            .multiply(
                BigDecimal.valueOf(
                    100
                )
            )
            .divide(
                base,
                2,
                RoundingMode.HALF_UP
            );
    }

    private record ProjectRow(
        UUID id,
        String projectNumber,
        String name,
        String status,
        BigDecimal progressPercentage,
        LocalDate startDate,
        LocalDate expectedCompletion
    ) {
    }

    private record PoRow(
        UUID id,
        BigDecimal total
    ) {
    }

    private record MonthRow(
        String month,
        BigDecimal value
    ) {
    }
}
