package com.buildflow.commercial.service;

import com.buildflow.budget.dto.BudgetDtos;
import com.buildflow.budget.service.FinancialSummaryService;
import com.buildflow.commercial.dto.CommercialDtos;
import com.buildflow.commercial.entity.InvoiceStatus;
import com.buildflow.commercial.repository.InvoiceRepository;
import com.buildflow.commercial.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.UUID;

@Service
public class RevenueMetricsService {

    private static final Set<InvoiceStatus>
        REVENUE_STATUSES =
            Set.of(
                InvoiceStatus.ISSUED,
                InvoiceStatus.PARTIALLY_PAID,
                InvoiceStatus.PAID,
                InvoiceStatus.OVERDUE
            );

    private final CommercialAccessService
        accessService;

    private final ProjectCommercialDataService
        projectDataService;

    private final InvoiceRepository
        invoiceRepository;

    private final PaymentRepository
        paymentRepository;

    private final FinancialSummaryService
        financialSummaryService;

    public RevenueMetricsService(
        CommercialAccessService accessService,
        ProjectCommercialDataService projectDataService,
        InvoiceRepository invoiceRepository,
        PaymentRepository paymentRepository,
        FinancialSummaryService financialSummaryService
    ) {
        this.accessService =
            accessService;

        this.projectDataService =
            projectDataService;

        this.invoiceRepository =
            invoiceRepository;

        this.paymentRepository =
            paymentRepository;

        this.financialSummaryService =
            financialSummaryService;
    }

    @Transactional(readOnly = true)
    public CommercialDtos.RevenueSummary summary(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        accessService.requireView(
            organizationId,
            projectId,
            userId
        );

        BigDecimal originalContractValue =
            projectDataService.contractValue(
                organizationId,
                projectId
            );

        BigDecimal approvedClientChanges =
            zero();

        BigDecimal revisedContractValue =
            money(
                originalContractValue.add(
                    approvedClientChanges
                )
            );

        BigDecimal totalInvoiced =
            nullableMoney(
                invoiceRepository
                    .sumTotalForStatuses(
                        projectId,
                        organizationId,
                        REVENUE_STATUSES
                    )
            );

        BigDecimal totalPaid =
            nullableMoney(
                paymentRepository
                    .sumProjectPayments(
                        projectId,
                        organizationId
                    )
            );

        BigDecimal outstanding =
            nullableMoney(
                invoiceRepository
                    .sumOutstandingForStatuses(
                        projectId,
                        organizationId,
                        REVENUE_STATUSES
                    )
            );

        BudgetDtos.FinancialSummaryView financial =
            financialSummaryService.summary(
                organizationId,
                userId,
                projectId
            );

        BigDecimal forecastCost =
            money(
                financial.forecastCost()
            );

        BigDecimal forecastProfit =
            money(
                revisedContractValue
                    .subtract(
                        forecastCost
                    )
            );

        BigDecimal margin =
            revisedContractValue.signum() == 0
                ? zero()
                : forecastProfit
                    .multiply(
                        BigDecimal.valueOf(100)
                    )
                    .divide(
                        revisedContractValue,
                        4,
                        RoundingMode.HALF_UP
                    );

        return new CommercialDtos.RevenueSummary(
            projectId,
            originalContractValue,
            approvedClientChanges,
            revisedContractValue,
            totalInvoiced,
            totalPaid,
            outstanding,
            forecastCost,
            forecastProfit,
            margin
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
