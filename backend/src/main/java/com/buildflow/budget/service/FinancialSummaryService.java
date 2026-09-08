package com.buildflow.budget.service;

import com.buildflow.budget.dto.BudgetDtos;
import com.buildflow.expense.entity.ExpenseStatus;
import com.buildflow.expense.repository.ExpenseRepository;
import com.buildflow.procurement.service.ProcurementFinancialService;
import com.buildflow.project.entity.Project;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class FinancialSummaryService {

    private final BudgetAccessService
        accessService;

    private final BudgetCalculationService
        budgetCalculationService;

    private final ExpenseRepository
        expenseRepository;

    private final ProcurementFinancialService
        procurementFinancialService;

    public FinancialSummaryService(
        BudgetAccessService accessService,
        BudgetCalculationService
            budgetCalculationService,
        ExpenseRepository expenseRepository,
        ProcurementFinancialService
            procurementFinancialService
    ) {
        this.accessService =
            accessService;

        this.budgetCalculationService =
            budgetCalculationService;

        this.expenseRepository =
            expenseRepository;

        this.procurementFinancialService =
            procurementFinancialService;
    }

    @Transactional(readOnly = true)
    public BudgetDtos.FinancialSummaryView
        summary(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        Project project =
            accessService.requireView(
                organizationId,
                projectId,
                userId
            );

        BigDecimal plannedBudget =
            budgetCalculationService
                .totalPlannedAmount(
                    organizationId,
                    projectId
                );

        BigDecimal actualCost =
            expenseRepository
                .sumProjectTotalByStatus(
                    projectId,
                    organizationId,
                    ExpenseStatus.APPROVED
                );

        if (actualCost == null) {
            actualCost =
                BigDecimal.ZERO;
        }

        actualCost =
            money(actualCost);

        BigDecimal originalBudget =
            money(
                project.getOriginalBudget()
            );

        BigDecimal effectiveBudget =
            project
                .isBudgetBaselineLocked()
                ? originalBudget
                : plannedBudget;

        BigDecimal revisedBudget =
            money(effectiveBudget);

        BigDecimal committedCost =
            procurementFinancialService
                .outstandingCommitments(
                    organizationId,
                    projectId
                );

        BigDecimal forecastCost =
            money(
                actualCost.add(
                    committedCost
                )
            );

        BigDecimal budgetRemaining =
            money(
                revisedBudget.subtract(
                    actualCost
                )
            );

        BigDecimal forecastVariance =
            money(
                revisedBudget.subtract(
                    forecastCost
                )
            );

        return new BudgetDtos
            .FinancialSummaryView(
                projectId,
                project
                    .isBudgetBaselineLocked(),
                plannedBudget,
                originalBudget,
                revisedBudget,
                actualCost,
                committedCost,
                forecastCost,
                budgetRemaining,
                forecastVariance
            );
    }

    private BigDecimal money(
        BigDecimal value
    ) {
        return value.setScale(
            4,
            RoundingMode.HALF_UP
        );
    }
}
