package com.buildflow.expense.dto;

import com.buildflow.expense.entity.Expense;
import com.buildflow.expense.entity.ExpenseStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record CreateExpenseRequest(

        @NotNull
        UUID budgetCategoryId,

        @NotBlank
        @Size(max = 500)
        String description,

        @NotNull
        LocalDate expenseDate,

        @NotNull
        @DecimalMin("0.0000")
        BigDecimal netAmount,

        @NotNull
        @DecimalMin("0.0000")
        BigDecimal taxAmount

    ) {
    }

    public record UpdateExpenseRequest(

        UUID budgetCategoryId,

        @Size(
            min = 1,
            max = 500
        )
        String description,

        LocalDate expenseDate,

        @DecimalMin("0.0000")
        BigDecimal netAmount,

        @DecimalMin("0.0000")
        BigDecimal taxAmount

    ) {
    }

    public record RejectExpenseRequest(

        @Size(max = 5000)
        String reason

    ) {
    }

    public record VoidExpenseRequest(

        @NotBlank
        @Size(max = 5000)
        String reason

    ) {
    }

    public record ExpenseView(
        UUID id,
        UUID projectId,
        UUID budgetCategoryId,
        String expenseNumber,
        String description,
        LocalDate expenseDate,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        ExpenseStatus status,
        UUID createdBy,
        UUID approvedBy,
        Instant approvedAt,
        String rejectionReason,
        String voidReason,
        UUID voidedBy,
        Instant voidedAt,
        Instant createdAt
    ) {
        public static ExpenseView from(
            Expense expense
        ) {
            return new ExpenseView(
                expense.getId(),
                expense.getProjectId(),
                expense.getBudgetCategoryId(),
                expense.getExpenseNumber(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getNetAmount(),
                expense.getTaxAmount(),
                expense.getTotalAmount(),
                expense.getStatus(),
                expense.getCreatedBy(),
                expense.getApprovedBy(),
                expense.getApprovedAt(),
                expense.getRejectionReason(),
                expense.getVoidReason(),
                expense.getVoidedBy(),
                expense.getVoidedAt(),
                expense.getCreatedAt()
            );
        }
    }
}
