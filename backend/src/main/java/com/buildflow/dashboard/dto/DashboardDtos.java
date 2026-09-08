package com.buildflow.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record FinancialSnapshot(
        BigDecimal originalBudget,
        BigDecimal revisedBudget,
        BigDecimal actualCost,
        BigDecimal committedCost,
        BigDecimal forecastCost,
        BigDecimal budgetRemaining,
        BigDecimal forecastBudgetVariance,
        BigDecimal originalContractValue,
        BigDecimal approvedClientChangeOrders,
        BigDecimal revisedContractValue,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal outstandingReceivables,
        BigDecimal forecastProfit,
        BigDecimal forecastMarginPercentage
    ) {
    }

    public record ProjectHealth(
        String status,
        List<String> reasons
    ) {
    }

    public record StatusCount(
        String status,
        long count
    ) {
    }

    public record MonthlyValue(
        String month,
        BigDecimal value
    ) {
    }

    public record BudgetPerformance(
        UUID projectId,
        String projectNumber,
        String projectName,
        BigDecimal revisedBudget,
        BigDecimal forecastCost,
        BigDecimal variance,
        BigDecimal variancePercentage
    ) {
    }

    public record Deadline(
        UUID id,
        UUID projectId,
        String projectName,
        String type,
        String title,
        LocalDate dueDate,
        String priority
    ) {
    }

    public record BudgetUtilization(
        BigDecimal originalBudget,
        BigDecimal revisedBudget,
        BigDecimal actualCost,
        BigDecimal forecastCost
    ) {
    }

    public record OrganizationDashboard(
        long activeProjects,
        BigDecimal totalContractValue,
        BigDecimal forecastRevenue,
        BigDecimal actualCosts,
        BigDecimal outstandingInvoices,
        long projectsAtRisk,
        long overdueTasks,
        long openIncidents,
        String currency,
        List<StatusCount> projectStatus,
        List<MonthlyValue> monthlyRevenue,
        List<MonthlyValue> monthlyExpenses,
        List<BudgetPerformance> budgetPerformance,
        List<Deadline> upcomingDeadlines
    ) {
    }

    public record ProjectDashboard(
        UUID projectId,
        String projectNumber,
        String projectName,
        String status,
        BigDecimal progressPercentage,
        LocalDate startDate,
        LocalDate expectedCompletion,
        String projectManager,
        String currency,
        ProjectHealth health,
        FinancialSnapshot financial,
        long openTasks,
        long overdueTasks,
        long upcomingMilestones,
        long openIncidents,
        BigDecimal outstandingInvoices,
        List<StatusCount> taskStatus,
        List<MonthlyValue> monthlyExpenses,
        List<MonthlyValue> costEvolution,
        BudgetUtilization budgetUtilization,
        List<Deadline> upcomingDeadlines
    ) {
    }
}
