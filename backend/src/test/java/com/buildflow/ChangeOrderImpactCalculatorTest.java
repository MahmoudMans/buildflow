package com.buildflow;

import com.buildflow.changeorder.entity.ChangeOrderStatus;
import com.buildflow.changeorder.service.ChangeOrderImpactCalculator;
import com.buildflow.changeorder.service.ChangeOrderImpactCalculator.Input;
import com.buildflow.changeorder.service.ChangeOrderImpactCalculator.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeOrderImpactCalculatorTest {

    private static final LocalDate END = LocalDate.of(2027, 6, 30);

    private static BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    private static Result calculate(
        ChangeOrderStatus status,
        String cost,
        String price,
        int days,
        String budget,
        String contract,
        String forecastCost,
        LocalDate end
    ) {
        return ChangeOrderImpactCalculator.calculate(
            new Input(
                status,
                cost == null ? null : n(cost),
                price == null ? null : n(price),
                days,
                budget == null ? null : n(budget),
                contract == null ? null : n(contract),
                forecastCost == null ? null : n(forecastCost),
                end
            )
        );
    }

    @Test
    void approvalRaisesBudgetContractCostAndEndDate() {

        Result result =
            calculate(
                ChangeOrderStatus.SUBMITTED,
                "10000", "15000", 14,
                "85000", "108000", "0",
                END
            );

        assertThat(result.pending()).isTrue();
        assertThat(result.alreadyApproved()).isFalse();

        assertThat(result.changeOrderProfit())
            .isEqualByComparingTo("5000");
        assertThat(result.changeOrderMarginPercentage())
            .isEqualByComparingTo("33.3333");

        assertThat(result.current().plannedProfit())
            .isEqualByComparingTo("23000");
        assertThat(result.current().plannedMarginPercentage())
            .isEqualByComparingTo("21.2963");
        assertThat(result.current().forecastMarginPercentage())
            .isEqualByComparingTo("100");
        assertThat(result.current().budgetHeadroom())
            .isEqualByComparingTo("85000");
        assertThat(result.current().plannedEndDate()).isEqualTo(END);

        assertThat(result.afterApproval().budget())
            .isEqualByComparingTo("95000");
        assertThat(result.afterApproval().contractValue())
            .isEqualByComparingTo("123000");
        assertThat(result.afterApproval().plannedProfit())
            .isEqualByComparingTo("28000");
        assertThat(result.afterApproval().plannedMarginPercentage())
            .isEqualByComparingTo("22.7642");
        assertThat(result.afterApproval().forecastCost())
            .isEqualByComparingTo("10000");
        assertThat(result.afterApproval().forecastProfit())
            .isEqualByComparingTo("113000");
        assertThat(result.afterApproval().forecastMarginPercentage())
            .isEqualByComparingTo("91.8699");
        assertThat(result.afterApproval().plannedEndDate())
            .isEqualTo(LocalDate.of(2027, 7, 14));

        assertThat(result.warnings()).containsExactly("SCHEDULE_DELAY");
    }

    @Test
    void budgetHeadroomIsUnchangedBecauseBudgetAndForecastMoveTogether() {

        Result result =
            calculate(
                ChangeOrderStatus.CLIENT_REVIEW,
                "4000", "9000", 0,
                "50000", "60000", "30000",
                null
            );

        assertThat(result.current().budgetHeadroom())
            .isEqualByComparingTo("20000");
        assertThat(result.afterApproval().budgetHeadroom())
            .isEqualByComparingTo("20000");

        // The profit moves by exactly the profit of the change order.
        assertThat(result.afterApproval().forecastProfit()
            .subtract(result.current().forecastProfit()))
            .isEqualByComparingTo("5000");
    }

    @Test
    void sellingBelowCostAndLosingMarginAreFlagged() {

        Result result =
            calculate(
                ChangeOrderStatus.DRAFT,
                "3000", "2000", 0,
                "85000", "108000", "0",
                END
            );

        assertThat(result.changeOrderProfit())
            .isEqualByComparingTo("-1000");
        assertThat(result.changeOrderMarginPercentage())
            .isEqualByComparingTo("-50");
        assertThat(result.afterApproval().plannedMarginPercentage())
            .isEqualByComparingTo("20");

        assertThat(result.warnings())
            .containsExactly(
                "CHANGE_ORDER_LOSES_MONEY",
                "MARGIN_DROPS"
            );

        assertThat(result.afterApproval().plannedEndDate())
            .isEqualTo(END);
    }

    @Test
    void anOverrunningProjectGetsEveryRelevantWarning() {

        Result result =
            calculate(
                ChangeOrderStatus.CLIENT_REVIEW,
                "10000", "8000", 0,
                "50000", "52000", "55000",
                null
            );

        assertThat(result.current().budgetHeadroom())
            .isEqualByComparingTo("-5000");
        assertThat(result.afterApproval().forecastProfit())
            .isEqualByComparingTo("-5000");
        assertThat(result.afterApproval().plannedEndDate()).isNull();

        assertThat(result.warnings())
            .containsExactly(
                "CHANGE_ORDER_LOSES_MONEY",
                "FORECAST_OVER_BUDGET",
                "FORECAST_LOSS",
                "MARGIN_DROPS"
            );
    }

    @Test
    void marginFallingByExactlyOnePointIsNotWorthAWarning() {

        // 10.00% planned margin before and after.
        Result unchanged =
            calculate(
                ChangeOrderStatus.DRAFT,
                "0", "0", 0,
                "90000", "100000", "0",
                null
            );

        assertThat(unchanged.warnings()).isEmpty();
    }

    @Test
    void approvedRejectedAndCancelledChangeOrdersChangeNothing() {

        Result approved =
            calculate(
                ChangeOrderStatus.APPROVED,
                "5000", "8000", 3,
                "85000", "108000", "0",
                END
            );

        assertThat(approved.pending()).isFalse();
        assertThat(approved.alreadyApproved()).isTrue();
        assertThat(approved.afterApproval()).isEqualTo(approved.current());
        assertThat(approved.warnings()).isEmpty();

        for (ChangeOrderStatus status : new ChangeOrderStatus[] {
            ChangeOrderStatus.REJECTED,
            ChangeOrderStatus.CANCELLED
        }) {

            Result result =
                calculate(
                    status,
                    "5000", "1", 30,
                    "85000", "108000", "0",
                    END
                );

            assertThat(result.pending()).as(status.name()).isFalse();
            assertThat(result.alreadyApproved())
                .as(status.name())
                .isFalse();
            assertThat(result.afterApproval())
                .as(status.name())
                .isEqualTo(result.current());
            assertThat(result.warnings())
                .as(status.name())
                .isEmpty();
        }
    }

    @Test
    void zeroContractValueAndZeroPriceAreHandled() {

        Result result =
            calculate(
                ChangeOrderStatus.SUBMITTED,
                "-2000", "0", 0,
                "0", "0", "0",
                null
            );

        assertThat(result.changeOrderMarginPercentage()).isNull();
        assertThat(result.changeOrderProfit())
            .isEqualByComparingTo("2000");
        assertThat(result.current().plannedMarginPercentage())
            .isEqualByComparingTo("0");
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void missingAmountsCountAsZero() {

        Result result =
            calculate(
                ChangeOrderStatus.DRAFT,
                null, null, 0,
                null, null, null,
                null
            );

        assertThat(result.afterApproval().budget())
            .isEqualByComparingTo("0");
        assertThat(result.afterApproval().contractValue())
            .isEqualByComparingTo("0");
        assertThat(result.warnings()).isEmpty();
    }
}
