package com.buildflow.changeorder.dto;

import com.buildflow.changeorder.entity.ChangeOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ChangeOrderDtos {

    private ChangeOrderDtos() {
    }

    public record CreateRequest(

        @NotBlank
        @Size(max = 250)
        String title,

        @Size(max = 10000)
        String description,

        @Size(max = 10000)
        String reason,

        @NotNull
        BigDecimal costImpact,

        @NotNull
        BigDecimal clientPriceImpact,

        int scheduleImpactDays
    ) {
    }

    public record UpdateRequest(

        @Size(min = 1, max = 250)
        String title,

        @Size(max = 10000)
        String description,

        @Size(max = 10000)
        String reason,

        BigDecimal costImpact,

        BigDecimal clientPriceImpact,

        Integer scheduleImpactDays
    ) {
    }

    public record CancelRequest(

        @NotBlank
        @Size(max = 5000)
        String reason
    ) {
    }

    public record Summary(
        UUID id,
        String changeOrderNumber,
        String title,
        ChangeOrderStatus status,
        BigDecimal costImpact,
        BigDecimal clientPriceImpact,
        int scheduleImpactDays,
        Instant createdAt
    ) {
    }

    public record View(
        UUID id,
        UUID projectId,
        String changeOrderNumber,
        String title,
        String description,
        String reason,
        BigDecimal costImpact,
        BigDecimal clientPriceImpact,
        int scheduleImpactDays,
        ChangeOrderStatus status,
        UUID requestedBy,
        Instant submittedAt,
        Instant clientReviewAt,
        UUID approvedBy,
        Instant approvedAt,
        UUID rejectedBy,
        Instant rejectedAt,
        UUID cancelledBy,
        Instant cancelledAt,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record FinancialSummary(
        UUID projectId,

        BigDecimal originalBudget,
        BigDecimal approvedBudgetChangeOrders,
        BigDecimal revisedBudget,

        BigDecimal originalContractValue,
        BigDecimal approvedClientChangeOrders,
        BigDecimal revisedContractValue,

        BigDecimal forecastCost,
        BigDecimal forecastProfit,
        BigDecimal forecastMarginPercentage
    ) {
    }
}
