package com.buildflow.budget.service;

import com.buildflow.budget.repository.BudgetItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class BudgetCalculationService {

    private final BudgetItemRepository
        budgetItemRepository;

    public BudgetCalculationService(
        BudgetItemRepository
            budgetItemRepository
    ) {
        this.budgetItemRepository =
            budgetItemRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalPlannedAmount(
        UUID organizationId,
        UUID projectId
    ) {
        BigDecimal total =
            budgetItemRepository
                .sumProjectPlannedAmount(
                    projectId,
                    organizationId
                );

        if (total == null) {
            return BigDecimal.ZERO
                .setScale(4);
        }

        return total.setScale(
            4,
            RoundingMode.HALF_UP
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal categoryPlannedAmount(
        UUID organizationId,
        UUID projectId,
        UUID categoryId
    ) {
        BigDecimal total =
            budgetItemRepository
                .sumCategoryPlannedAmount(
                    projectId,
                    organizationId,
                    categoryId
                );

        if (total == null) {
            return BigDecimal.ZERO
                .setScale(4);
        }

        return total.setScale(
            4,
            RoundingMode.HALF_UP
        );
    }
}
