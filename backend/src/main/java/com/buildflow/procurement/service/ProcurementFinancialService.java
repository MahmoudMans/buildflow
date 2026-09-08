package com.buildflow.procurement.service;

import com.buildflow.expense.repository.ExpenseRepository;
import com.buildflow.procurement.entity.PurchaseOrder;
import com.buildflow.procurement.entity.PurchaseOrderStatus;
import com.buildflow.procurement.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProcurementFinancialService {

    private static final Set<
        PurchaseOrderStatus
    > COMMITTED_STATUSES =
        Set.of(
            PurchaseOrderStatus.APPROVED,
            PurchaseOrderStatus.ORDERED,
            PurchaseOrderStatus.PARTIALLY_RECEIVED,
            PurchaseOrderStatus.RECEIVED
        );

    private final PurchaseOrderRepository
        purchaseOrderRepository;

    private final ExpenseRepository
        expenseRepository;

    public ProcurementFinancialService(
        PurchaseOrderRepository
            purchaseOrderRepository,
        ExpenseRepository expenseRepository
    ) {
        this.purchaseOrderRepository =
            purchaseOrderRepository;

        this.expenseRepository =
            expenseRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal outstandingCommitments(
        UUID organizationId,
        UUID projectId
    ) {
        List<PurchaseOrder> orders =
            purchaseOrderRepository
                .findByProjectIdAndOrganizationIdAndStatusIn(
                    projectId,
                    organizationId,
                    COMMITTED_STATUSES
                );

        BigDecimal total =
            BigDecimal.ZERO;

        for (
            PurchaseOrder order
            : orders
        ) {
            BigDecimal approvedExpenses =
                expenseRepository
                    .sumApprovedByPurchaseOrder(
                        order.getId(),
                        organizationId,
                        projectId
                    );

            if (
                approvedExpenses == null
            ) {
                approvedExpenses =
                    BigDecimal.ZERO;
            }

            BigDecimal commitment =
                order
                    .getTotal()
                    .subtract(
                        approvedExpenses
                    );

            if (
                commitment.signum()
                    < 0
            ) {
                commitment =
                    BigDecimal.ZERO;
            }

            total =
                total.add(commitment);
        }

        return total.setScale(
            4,
            RoundingMode.HALF_UP
        );
    }
}
