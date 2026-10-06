package com.buildflow.changeorder.service;

import com.buildflow.changeorder.dto.ChangeOrderDtos;
import com.buildflow.changeorder.dto.ChangeOrderImpactDtos;
import com.buildflow.changeorder.entity.ChangeOrder;
import com.buildflow.changeorder.repository.ChangeOrderRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ChangeOrderImpactService {

    private final ChangeOrderService changeOrderService;

    private final ChangeOrderFinancialService financialService;

    private final ChangeOrderRepository changeOrderRepository;

    private final JdbcTemplate jdbcTemplate;

    public ChangeOrderImpactService(
        ChangeOrderService changeOrderService,
        ChangeOrderFinancialService financialService,
        ChangeOrderRepository changeOrderRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.changeOrderService = changeOrderService;
        this.financialService = financialService;
        this.changeOrderRepository = changeOrderRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public ChangeOrderImpactDtos.Impact impact(
        UUID organizationId,
        UUID userId,
        UUID changeOrderId
    ) {
        ChangeOrder changeOrder =
            changeOrderService.requireChangeOrder(
                organizationId,
                changeOrderId
            );

        UUID projectId = changeOrder.getProjectId();

        // Also checks that the caller may see the internal financials.
        ChangeOrderDtos.FinancialSummary summary =
            financialService.summary(
                organizationId,
                userId,
                projectId
            );

        ChangeOrderImpactCalculator.Result result =
            ChangeOrderImpactCalculator.calculate(
                new ChangeOrderImpactCalculator.Input(
                    changeOrder.getStatus(),
                    changeOrder.getCostImpact(),
                    changeOrder.getClientPriceImpact(),
                    changeOrder.getScheduleImpactDays(),
                    summary.revisedBudget(),
                    summary.revisedContractValue(),
                    summary.forecastCost(),
                    plannedEndDate(organizationId, projectId)
                )
            );

        return new ChangeOrderImpactDtos.Impact(
            changeOrder.getId(),
            projectId,
            changeOrder.getChangeOrderNumber(),
            changeOrder.getTitle(),
            changeOrder.getStatus(),
            result.pending(),
            result.alreadyApproved(),
            ChangeOrderImpactCalculator.money(
                changeOrder.getCostImpact()
            ),
            ChangeOrderImpactCalculator.money(
                changeOrder.getClientPriceImpact()
            ),
            changeOrder.getScheduleImpactDays(),
            result.changeOrderProfit(),
            result.changeOrderMarginPercentage(),
            result.current(),
            result.afterApproval(),
            result.warnings(),
            pipeline(organizationId, projectId)
        );
    }

    private ChangeOrderImpactDtos.Pipeline pipeline(
        UUID organizationId,
        UUID projectId
    ) {
        List<ChangeOrder> pending =
            changeOrderRepository
                .findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
                    projectId,
                    organizationId
                )
                .stream()
                .filter(
                    other ->
                        ChangeOrderImpactCalculator.isPending(
                            other.getStatus()
                        )
                )
                .toList();

        BigDecimal cost = ChangeOrderImpactCalculator.money(null);
        BigDecimal price = ChangeOrderImpactCalculator.money(null);

        for (ChangeOrder other : pending) {
            cost = cost.add(other.getCostImpact());
            price = price.add(other.getClientPriceImpact());
        }

        return new ChangeOrderImpactDtos.Pipeline(
            pending.size(),
            ChangeOrderImpactCalculator.money(cost),
            ChangeOrderImpactCalculator.money(price)
        );
    }

    private LocalDate plannedEndDate(
        UUID organizationId,
        UUID projectId
    ) {
        List<LocalDate> dates =
            jdbcTemplate.query(
                """
                SELECT planned_end_date
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                """,
                (rs, rowNumber) ->
                    rs.getObject("planned_end_date", LocalDate.class),
                projectId,
                organizationId
            );

        return dates.isEmpty() ? null : dates.get(0);
    }
}
