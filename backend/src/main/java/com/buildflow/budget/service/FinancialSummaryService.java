package com.buildflow.budget.service;

import com.buildflow.budget.dto.BudgetDtos;
import com.buildflow.expense.entity.ExpenseStatus;
import com.buildflow.expense.repository.ExpenseRepository;
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

    public FinancialSummaryService(
        BudgetAccessService accessService,
        BudgetCalculationService
            budgetCalculationService,
        ExpenseRepository expenseRepository
    ) {
        this.accessService =
            accessService;

        this.budgetCalculationService =
            budgetCalculationService;

        this.expenseRepository =
            expenseRepository;
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
            actualCost.setScale(
                4,
                RoundingMode.HALF_UP
            );

        BigDecimal originalBudget =
            project
                .getOriginalBudget()
                .setScale(
                    4,
                    RoundingMode.HALF_UP
                );

        BigDecimal effectiveBudget =
            project
                .isBudgetBaselineLocked()
                ? originalBudget
                : plannedBudget;

        BigDecimal revisedBudget =
            effectiveBudget;

        BigDecimal budgetRemaining =
            revisedBudget
                .subtract(actualCost)
                .setScale(
                    4,
                    RoundingMode.HALF_UP
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
                budgetRemaining
            );
    }
}
