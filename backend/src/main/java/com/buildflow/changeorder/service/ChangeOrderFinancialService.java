package com.buildflow.changeorder.service;

import com.buildflow.budget.dto.BudgetDtos;
import com.buildflow.budget.service.FinancialSummaryService;
import com.buildflow.changeorder.dto.ChangeOrderDtos;
import com.buildflow.changeorder.entity.ChangeOrderStatus;
import com.buildflow.changeorder.repository.ChangeOrderRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class ChangeOrderFinancialService {

    private final ChangeOrderRepository
        changeOrderRepository;

    private final ChangeOrderAccessService
        accessService;

    private final FinancialSummaryService
        financialSummaryService;

    private final JdbcTemplate
        jdbcTemplate;

    public ChangeOrderFinancialService(
        ChangeOrderRepository
            changeOrderRepository,
        ChangeOrderAccessService
            accessService,
        FinancialSummaryService
            financialSummaryService,
        JdbcTemplate jdbcTemplate
    ) {
        this.changeOrderRepository =
            changeOrderRepository;

        this.accessService =
            accessService;

        this.financialSummaryService =
            financialSummaryService;

        this.jdbcTemplate =
            jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public ChangeOrderDtos.FinancialSummary
        summary(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        accessService.requireFinancialView(
            organizationId,
            projectId,
            userId
        );

        BigDecimal originalBudget =
            projectMoney(
                organizationId,
                projectId,
                "original_budget"
            );

        BigDecimal
            approvedBudgetChangeOrders =
                nullableMoney(
                    changeOrderRepository
                        .sumCostImpactForStatus(
                            projectId,
                            organizationId,
                            ChangeOrderStatus.APPROVED
                        )
                );

        BigDecimal revisedBudget =
            money(
                originalBudget.add(
                    approvedBudgetChangeOrders
                )
            );

        BigDecimal originalContractValue =
            projectMoney(
                organizationId,
                projectId,
                "contract_value"
            );

        BigDecimal
            approvedClientChangeOrders =
                nullableMoney(
                    changeOrderRepository
                        .sumClientPriceImpactForStatus(
                            projectId,
                            organizationId,
                            ChangeOrderStatus.APPROVED
                        )
                );

        BigDecimal revisedContractValue =
            money(
                originalContractValue.add(
                    approvedClientChangeOrders
                )
            );

        BudgetDtos.FinancialSummaryView
            baseFinancial =
                financialSummaryService
                    .summary(
                        organizationId,
                        userId,
                        projectId
                    );

        BigDecimal forecastCost =
            money(
                baseFinancial
                    .forecastCost()
            );

        BigDecimal forecastProfit =
            money(
                revisedContractValue
                    .subtract(
                        forecastCost
                    )
            );

        BigDecimal forecastMargin =
            revisedContractValue
                    .signum()
                == 0
                ? zero()
                : forecastProfit
                    .multiply(
                        BigDecimal.valueOf(
                            100
                        )
                    )
                    .divide(
                        revisedContractValue,
                        4,
                        RoundingMode.HALF_UP
                    );

        return new ChangeOrderDtos
            .FinancialSummary(
                projectId,
                originalBudget,
                approvedBudgetChangeOrders,
                revisedBudget,
                originalContractValue,
                approvedClientChangeOrders,
                revisedContractValue,
                forecastCost,
                forecastProfit,
                forecastMargin
            );
    }

    @Transactional(readOnly = true)
    public BigDecimal
        approvedClientPriceImpact(
            UUID organizationId,
            UUID projectId
        ) {

        return nullableMoney(
            changeOrderRepository
                .sumClientPriceImpactForStatus(
                    projectId,
                    organizationId,
                    ChangeOrderStatus.APPROVED
                )
        );
    }

    private BigDecimal projectMoney(
        UUID organizationId,
        UUID projectId,
        String column
    ) {
        if (
            !column.equals(
                "original_budget"
            )
            && !column.equals(
                "contract_value"
            )
        ) {
            throw new IllegalArgumentException(
                "Unsupported project financial column."
            );
        }

        BigDecimal value =
            jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(
                    %s,
                    0
                )
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                """
                    .formatted(column),
                BigDecimal.class,
                projectId,
                organizationId
            );

        return nullableMoney(
            value
        );
    }

    private BigDecimal nullableMoney(
        BigDecimal value
    ) {
        return value == null
            ? zero()
            : money(value);
    }

    private BigDecimal money(
        BigDecimal value
    ) {
        return value.setScale(
            4,
            RoundingMode.HALF_UP
        );
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO
            .setScale(4);
    }
}
