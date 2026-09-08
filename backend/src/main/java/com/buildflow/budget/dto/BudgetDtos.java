package com.buildflow.budget.dto;

import com.buildflow.budget.entity.BudgetCategory;
import com.buildflow.budget.entity.BudgetItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class BudgetDtos {

    private BudgetDtos() {
    }

    public record CreateCategoryRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 5000)
        String description,

        @Min(0)
        Integer sortOrder

    ) {
    }

    public record UpdateCategoryRequest(

        @Size(
            min = 1,
            max = 200
        )
        String name,

        @Size(max = 5000)
        String description,

        @Min(0)
        Integer sortOrder

    ) {
    }

    public record CreateItemRequest(

        @NotBlank
        @Size(max = 500)
        String description,

        @DecimalMin("0.0000")
        BigDecimal quantity,

        @DecimalMin("0.0000")
        BigDecimal unitCost,

        @Size(max = 5000)
        String notes

    ) {
    }

    public record UpdateItemRequest(

        @Size(
            min = 1,
            max = 500
        )
        String description,

        @DecimalMin("0.0000")
        BigDecimal quantity,

        @DecimalMin("0.0000")
        BigDecimal unitCost,

        @Size(max = 5000)
        String notes

    ) {
    }

    public record BudgetItemView(
        UUID id,
        UUID categoryId,
        String description,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal plannedAmount,
        String notes,
        Instant createdAt
    ) {
        public static BudgetItemView from(
            BudgetItem item
        ) {
            return new BudgetItemView(
                item.getId(),
                item.getBudgetCategoryId(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getPlannedAmount(),
                item.getNotes(),
                item.getCreatedAt()
            );
        }
    }

    public record BudgetCategoryView(
        UUID id,
        String name,
        String description,
        int sortOrder,
        BigDecimal plannedAmount,
        List<BudgetItemView> items,
        Instant createdAt
    ) {
        public static BudgetCategoryView from(
            BudgetCategory category,
            BigDecimal plannedAmount,
            List<BudgetItem> items
        ) {
            return new BudgetCategoryView(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getSortOrder(),
                plannedAmount,
                items
                    .stream()
                    .map(
                        BudgetItemView::from
                    )
                    .toList(),
                category.getCreatedAt()
            );
        }
    }

    public record BudgetView(
        UUID projectId,
        boolean baselineLocked,
        BigDecimal plannedBudget,
        BigDecimal originalBudget,
        List<BudgetCategoryView> categories
    ) {
    }

    public record FinancialSummaryView(
        UUID projectId,
        boolean budgetBaselineLocked,
        BigDecimal plannedBudget,
        BigDecimal originalBudget,
        BigDecimal revisedBudget,
        BigDecimal actualCost,
        BigDecimal budgetRemaining
    ) {
    }
}
