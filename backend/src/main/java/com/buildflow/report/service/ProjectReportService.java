package com.buildflow.report.service;

import com.buildflow.dashboard.dto.DashboardDtos;
import com.buildflow.dashboard.service.DashboardService;
import com.buildflow.report.dto.ReportDtos;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectReportService {

    private final DashboardService
        dashboardService;

    private final PdfReportService
        pdfReportService;

    private final ReportAccessService
        accessService;

    private final JdbcTemplate jdbcTemplate;

    public ProjectReportService(
        DashboardService dashboardService,
        PdfReportService pdfReportService,
        ReportAccessService accessService,
        JdbcTemplate jdbcTemplate
    ) {
        this.dashboardService =
            dashboardService;

        this.pdfReportService =
            pdfReportService;

        this.accessService =
            accessService;

        this.jdbcTemplate =
            jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportFile
        projectSummary(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        accessService
            .requireProjectReport(
                organizationId,
                userId,
                projectId
            );

        DashboardDtos.ProjectDashboard
            dashboard =
                dashboardService.project(
                    organizationId,
                    userId,
                    projectId
                );

        List<PdfReportService.Section>
            sections =
                new ArrayList<>();

        sections.add(
            PdfReportService.Section.of(
                "Project",
                List.of(
                    entry(
                        "Project Number",
                        dashboard.projectNumber()
                    ),
                    entry(
                        "Project Name",
                        dashboard.projectName()
                    ),
                    entry(
                        "Status",
                        dashboard.status()
                    ),
                    entry(
                        "Progress",
                        dashboard
                            .progressPercentage()
                            + "%"
                    ),
                    entry(
                        "Start Date",
                        dashboard.startDate()
                    ),
                    entry(
                        "Expected Completion",
                        dashboard
                            .expectedCompletion()
                    ),
                    entry(
                        "Project Manager",
                        dashboard
                            .projectManager()
                    ),
                    entry(
                        "Health",
                        dashboard
                            .health()
                            .status()
                    )
                )
            )
        );

        sections.add(
            PdfReportService.Section.lines(
                "Health Notes",
                dashboard
                    .health()
                    .reasons()
                    .isEmpty()
                    ? List.of(
                        "No active project-health warnings."
                    )
                    : dashboard
                        .health()
                        .reasons()
            )
        );

        sections.add(
            PdfReportService.Section.of(
                "Operations",
                List.of(
                    entry(
                        "Open Tasks",
                        dashboard.openTasks()
                    ),
                    entry(
                        "Overdue Tasks",
                        dashboard.overdueTasks()
                    ),
                    entry(
                        "Upcoming Milestones",
                        dashboard
                            .upcomingMilestones()
                    ),
                    entry(
                        "Open Incidents",
                        dashboard.openIncidents()
                    )
                )
            )
        );

        sections.add(
            financialSection(
                dashboard
            )
        );

        List<String> deadlines =
            dashboard
                .upcomingDeadlines()
                .stream()
                .map(
                    deadline ->
                        deadline.type()
                            + " | "
                            + deadline.title()
                            + " | "
                            + deadline.dueDate()
                )
                .toList();

        sections.add(
            PdfReportService.Section.lines(
                "Upcoming Deadlines",
                deadlines.isEmpty()
                    ? List.of(
                        "No deadlines due within the next 30 days."
                    )
                    : deadlines
            )
        );

        byte[] content =
            pdfReportService.render(
                "BuildFlow Project Summary — "
                    + dashboard
                        .projectNumber(),
                sections
            );

        return pdf(
            content,
            "project-"
                + dashboard
                    .projectNumber()
                + "-summary.pdf"
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportFile
        financialReport(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        accessService
            .requireFinancialProject(
                organizationId,
                userId,
                projectId
            );

        DashboardDtos.ProjectDashboard
            dashboard =
                dashboardService.project(
                    organizationId,
                    userId,
                    projectId
                );

        List<PdfReportService.Section>
            sections =
                new ArrayList<>();

        sections.add(
            PdfReportService.Section.of(
                "Project",
                List.of(
                    entry(
                        "Project Number",
                        dashboard.projectNumber()
                    ),
                    entry(
                        "Project Name",
                        dashboard.projectName()
                    ),
                    entry(
                        "Currency",
                        dashboard.currency()
                    )
                )
            )
        );

        sections.add(
            financialSection(
                dashboard
            )
        );

        sections.add(
            PdfReportService.Section.lines(
                "Monthly Approved Expenses",
                dashboard
                    .monthlyExpenses()
                    .stream()
                    .map(
                        value ->
                            value.month()
                                + " | "
                                + money(
                                    value.value()
                                )
                    )
                    .toList()
            )
        );

        sections.add(
            PdfReportService.Section.lines(
                "Cost Evolution",
                dashboard
                    .costEvolution()
                    .stream()
                    .map(
                        value ->
                            value.month()
                                + " | "
                                + money(
                                    value.value()
                                )
                    )
                    .toList()
            )
        );

        byte[] content =
            pdfReportService.render(
                "BuildFlow Project Financial Report — "
                    + dashboard
                        .projectNumber(),
                sections
            );

        return pdf(
            content,
            "project-"
                + dashboard
                    .projectNumber()
                + "-financial.pdf"
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportFile
        budgetReport(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        accessService
            .requireFinancialProject(
                organizationId,
                userId,
                projectId
            );

        DashboardDtos.ProjectDashboard
            dashboard =
                dashboardService.project(
                    organizationId,
                    userId,
                    projectId
                );

        DashboardDtos.FinancialSnapshot
            financial =
                dashboard.financial();

        byte[] content =
            pdfReportService.render(
                "BuildFlow Budget Report — "
                    + dashboard
                        .projectNumber(),
                List.of(
                    PdfReportService.Section.of(
                        "Budget Summary",
                        List.of(
                            entry(
                                "Original Budget",
                                money(
                                    financial
                                        .originalBudget()
                                )
                            ),
                            entry(
                                "Revised Budget",
                                money(
                                    financial
                                        .revisedBudget()
                                )
                            ),
                            entry(
                                "Actual Cost",
                                money(
                                    financial
                                        .actualCost()
                                )
                            ),
                            entry(
                                "Committed Cost",
                                money(
                                    financial
                                        .committedCost()
                                )
                            ),
                            entry(
                                "Forecast Cost",
                                money(
                                    financial
                                        .forecastCost()
                                )
                            ),
                            entry(
                                "Budget Remaining",
                                money(
                                    financial
                                        .budgetRemaining()
                                )
                            ),
                            entry(
                                "Forecast Budget Variance",
                                money(
                                    financial
                                        .forecastBudgetVariance()
                                )
                            )
                        )
                    )
                )
            );

        return pdf(
            content,
            "project-"
                + dashboard
                    .projectNumber()
                + "-budget.pdf"
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportFile
        expenseReport(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        accessService
            .requireFinancialProject(
                organizationId,
                userId,
                projectId
            );

        DashboardDtos.ProjectDashboard
            dashboard =
                dashboardService.project(
                    organizationId,
                    userId,
                    projectId
                );

        List<String> rows =
            expenseLines(
                organizationId,
                projectId
            );

        byte[] content =
            pdfReportService.render(
                "BuildFlow Expense Report — "
                    + dashboard
                        .projectNumber(),
                List.of(
                    PdfReportService.Section.of(
                        "Summary",
                        List.of(
                            entry(
                                "Approved Actual Cost",
                                money(
                                    dashboard
                                        .financial()
                                        .actualCost()
                                )
                            ),
                            entry(
                                "Currency",
                                dashboard.currency()
                            )
                        )
                    ),
                    PdfReportService.Section.lines(
                        "Expenses",
                        rows.isEmpty()
                            ? List.of(
                                "No expenses recorded."
                            )
                            : rows
                    )
                )
            );

        return pdf(
            content,
            "project-"
                + dashboard
                    .projectNumber()
                + "-expenses.pdf"
        );
    }

    private PdfReportService.Section
        financialSection(
            DashboardDtos.ProjectDashboard
                dashboard
        ) {

        DashboardDtos.FinancialSnapshot
            financial =
                dashboard.financial();

        return PdfReportService.Section.of(
            "Financial Position",
            List.of(
                entry(
                    "Original Contract Value",
                    money(
                        financial
                            .originalContractValue()
                    )
                ),
                entry(
                    "Approved Client Change Orders",
                    money(
                        financial
                            .approvedClientChangeOrders()
                    )
                ),
                entry(
                    "Revised Contract Value",
                    money(
                        financial
                            .revisedContractValue()
                    )
                ),
                entry(
                    "Original Budget",
                    money(
                        financial
                            .originalBudget()
                    )
                ),
                entry(
                    "Revised Budget",
                    money(
                        financial
                            .revisedBudget()
                    )
                ),
                entry(
                    "Actual Cost",
                    money(
                        financial
                            .actualCost()
                    )
                ),
                entry(
                    "Committed Cost",
                    money(
                        financial
                            .committedCost()
                    )
                ),
                entry(
                    "Forecast Cost",
                    money(
                        financial
                            .forecastCost()
                    )
                ),
                entry(
                    "Total Invoiced",
                    money(
                        financial
                            .totalInvoiced()
                    )
                ),
                entry(
                    "Total Paid",
                    money(
                        financial
                            .totalPaid()
                    )
                ),
                entry(
                    "Outstanding Receivables",
                    money(
                        financial
                            .outstandingReceivables()
                    )
                ),
                entry(
                    "Forecast Profit",
                    money(
                        financial
                            .forecastProfit()
                    )
                ),
                entry(
                    "Forecast Margin",
                    financial
                        .forecastMarginPercentage()
                        + "%"
                )
            )
        );
    }

    private List<String> expenseLines(
        UUID organizationId,
        UUID projectId
    ) {
        Integer exists =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'expenses'
                """,
                Integer.class
            );

        if (
            exists == null
            || exists == 0
        ) {
            return List.of();
        }

        List<Map<String, Object>> rows =
            jdbcTemplate.queryForList(
                """
                SELECT *
                FROM expenses
                WHERE organization_id = ?
                  AND project_id = ?
                ORDER BY expense_date, created_at
                """,
                organizationId,
                projectId
            );

        List<String> result =
            new ArrayList<>();

        for (
            Map<String, Object> row
            : rows
        ) {
            result.add(
                value(
                    row,
                    "expense_number"
                )
                    + " | "
                    + value(
                        row,
                        "expense_date"
                    )
                    + " | "
                    + value(
                        row,
                        "status"
                    )
                    + " | "
                    + value(
                        row,
                        "description"
                    )
                    + " | "
                    + value(
                        row,
                        "total_amount"
                    )
            );
        }

        return result;
    }

    private PdfReportService.Entry entry(
        String label,
        Object value
    ) {
        return new PdfReportService
            .Entry(
                label,
                value == null
                    ? "—"
                    : String.valueOf(
                        value
                    )
            );
    }

    private String value(
        Map<String, Object> row,
        String key
    ) {
        Object value =
            row.get(key);

        return value == null
            ? "—"
            : String.valueOf(value);
    }

    private String money(
        BigDecimal value
    ) {
        return value == null
            ? "0.00"
            : value
                .setScale(
                    2
                )
                .toPlainString();
    }

    private ReportDtos.ReportFile pdf(
        byte[] content,
        String filename
    ) {
        return new ReportDtos
            .ReportFile(
                content,
                safeFilename(
                    filename
                ),
                "application/pdf"
            );
    }

    private String safeFilename(
        String value
    ) {
        return value.replaceAll(
            "[^A-Za-z0-9._-]",
            "-"
        );
    }
}
