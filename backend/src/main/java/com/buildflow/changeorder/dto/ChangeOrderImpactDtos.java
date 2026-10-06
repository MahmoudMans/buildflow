package com.buildflow.changeorder.dto;

import com.buildflow.changeorder.entity.ChangeOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * What approving one change order would do to the project figures.
 *
 * Internal numbers (cost, margin): never returned to the client role.
 */
public final class ChangeOrderImpactDtos {

    private ChangeOrderImpactDtos() {
    }

    /**
     * The project money position at one point in time.
     *
     * budget and contractValue are the revised figures (original plus
     * approved change orders). forecastCost is spent plus committed.
     * plannedProfit is contract value minus budget; forecastProfit is
     * contract value minus forecast cost; budgetHeadroom is budget minus
     * forecast cost.
     */
    public record Position(
        BigDecimal budget,
        BigDecimal contractValue,
        BigDecimal plannedProfit,
        BigDecimal plannedMarginPercentage,
        BigDecimal forecastCost,
        BigDecimal forecastProfit,
        BigDecimal forecastMarginPercentage,
        BigDecimal budgetHeadroom,
        LocalDate plannedEndDate
    ) {
    }

    /** Change orders on the project that can still be approved. */
    public record Pipeline(
        int pendingCount,
        BigDecimal pendingCostImpact,
        BigDecimal pendingClientPriceImpact
    ) {
    }

    public record Impact(
        UUID changeOrderId,
        UUID projectId,
        String changeOrderNumber,
        String title,
        ChangeOrderStatus status,

        /* Can still be approved: draft, submitted or in client review. */
        boolean pending,

        /* Already approved: the project figures already include it. */
        boolean alreadyApproved,

        BigDecimal costImpact,
        BigDecimal clientPriceImpact,
        int scheduleImpactDays,

        BigDecimal changeOrderProfit,

        /* Null when the client price impact is zero. */
        BigDecimal changeOrderMarginPercentage,

        Position current,
        Position afterApproval,

        List<String> warnings,
        Pipeline pipeline
    ) {
    }
}
