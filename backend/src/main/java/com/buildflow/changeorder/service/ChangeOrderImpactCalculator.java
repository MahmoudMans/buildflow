package com.buildflow.changeorder.service;

import com.buildflow.changeorder.dto.ChangeOrderImpactDtos;
import com.buildflow.changeorder.entity.ChangeOrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure arithmetic behind the change-order impact preview.
 *
 * Approving a change order raises the budget by its cost impact and the
 * contract value by its client price impact. The forecast cost (spent plus
 * committed) is raised by the full cost impact as well: that is the
 * conservative reading, and it assumes the cost of the extra work has not
 * been committed or spent yet. Budget and forecast move together, so the
 * budget headroom stays as it is; what changes is the profit, by exactly
 * the profit of the change order itself.
 *
 * Warnings, only for change orders that can still be approved:
 * the change order sells for less than it costs, the forecast would exceed
 * the budget, the forecast would turn into a loss, the planned margin would
 * fall by more than one percentage point, or the schedule slips.
 */
public final class ChangeOrderImpactCalculator {

    public static final String LOSES_MONEY = "CHANGE_ORDER_LOSES_MONEY";
    public static final String OVER_BUDGET = "FORECAST_OVER_BUDGET";
    public static final String FORECAST_LOSS = "FORECAST_LOSS";
    public static final String MARGIN_DROPS = "MARGIN_DROPS";
    public static final String SCHEDULE_DELAY = "SCHEDULE_DELAY";

    /** A drop of more than this many percentage points is worth a warning. */
    private static final BigDecimal MARGIN_DROP_POINTS =
        new BigDecimal("1.0000");

    private static final BigDecimal HUNDRED =
        BigDecimal.valueOf(100);

    private ChangeOrderImpactCalculator() {
    }

    public record Input(
        ChangeOrderStatus status,
        BigDecimal costImpact,
        BigDecimal clientPriceImpact,
        int scheduleImpactDays,
        BigDecimal revisedBudget,
        BigDecimal revisedContractValue,
        BigDecimal forecastCost,
        LocalDate plannedEndDate
    ) {
    }

    public record Result(
        boolean pending,
        boolean alreadyApproved,
        BigDecimal changeOrderProfit,
        BigDecimal changeOrderMarginPercentage,
        ChangeOrderImpactDtos.Position current,
        ChangeOrderImpactDtos.Position afterApproval,
        List<String> warnings
    ) {
    }

    public static boolean isPending(ChangeOrderStatus status) {
        return status == ChangeOrderStatus.DRAFT
            || status == ChangeOrderStatus.SUBMITTED
            || status == ChangeOrderStatus.CLIENT_REVIEW;
    }

    public static Result calculate(Input input) {

        boolean pending = isPending(input.status());

        boolean alreadyApproved =
            input.status() == ChangeOrderStatus.APPROVED;

        BigDecimal cost = money(input.costImpact());
        BigDecimal price = money(input.clientPriceImpact());

        BigDecimal budget = money(input.revisedBudget());
        BigDecimal contract = money(input.revisedContractValue());
        BigDecimal forecastCost = money(input.forecastCost());

        ChangeOrderImpactDtos.Position current =
            position(
                budget,
                contract,
                forecastCost,
                input.plannedEndDate()
            );

        ChangeOrderImpactDtos.Position after =
            pending
                ? position(
                    budget.add(cost),
                    contract.add(price),
                    forecastCost.add(cost),
                    input.plannedEndDate() == null
                        ? null
                        : input.plannedEndDate()
                            .plusDays(input.scheduleImpactDays())
                )
                : current;

        BigDecimal profit = money(price.subtract(cost));

        BigDecimal margin =
            price.signum() == 0
                ? null
                : percentage(profit, price);

        List<String> warnings = new ArrayList<>();

        if (pending) {

            if (profit.signum() < 0) {
                warnings.add(LOSES_MONEY);
            }

            if (after.budgetHeadroom().signum() < 0) {
                warnings.add(OVER_BUDGET);
            }

            if (after.forecastProfit().signum() < 0) {
                warnings.add(FORECAST_LOSS);
            }

            // Planned margin (contract value minus budget) is the stable
            // measure: the forecast margin is close to 100% early on, when
            // little has been spent.
            BigDecimal change =
                after.plannedMarginPercentage()
                    .subtract(current.plannedMarginPercentage());

            if (change.compareTo(MARGIN_DROP_POINTS.negate()) < 0) {
                warnings.add(MARGIN_DROPS);
            }

            if (input.scheduleImpactDays() > 0) {
                warnings.add(SCHEDULE_DELAY);
            }
        }

        return new Result(
            pending,
            alreadyApproved,
            profit,
            margin,
            current,
            after,
            List.copyOf(warnings)
        );
    }

    private static ChangeOrderImpactDtos.Position position(
        BigDecimal budget,
        BigDecimal contract,
        BigDecimal forecastCost,
        LocalDate plannedEndDate
    ) {
        BigDecimal plannedProfit = money(contract.subtract(budget));
        BigDecimal forecastProfit = money(contract.subtract(forecastCost));

        return new ChangeOrderImpactDtos.Position(
            budget,
            contract,
            plannedProfit,
            percentage(plannedProfit, contract),
            forecastCost,
            forecastProfit,
            percentage(forecastProfit, contract),
            money(budget.subtract(forecastCost)),
            plannedEndDate
        );
    }

    private static BigDecimal percentage(
        BigDecimal amount,
        BigDecimal total
    ) {
        if (total.signum() == 0) {
            return zero();
        }

        return amount
            .multiply(HUNDRED)
            .divide(total, 4, RoundingMode.HALF_UP);
    }

    static BigDecimal money(BigDecimal value) {
        return value == null
            ? zero()
            : value.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(4);
    }
}
