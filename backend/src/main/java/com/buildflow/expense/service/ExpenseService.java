package com.buildflow.expense.service;

import com.buildflow.budget.service.BudgetService;
import com.buildflow.expense.dto.ExpenseDtos;
import com.buildflow.expense.entity.Expense;
import com.buildflow.expense.entity.ExpenseStatus;
import com.buildflow.expense.repository.ExpenseRepository;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.procurement.entity.PurchaseOrder;
import com.buildflow.procurement.entity.PurchaseOrderStatus;
import com.buildflow.procurement.repository.PurchaseOrderRepository;
import com.buildflow.shared.error.BusinessException;
import com.buildflow.supplier.entity.Supplier;
import com.buildflow.supplier.service.SupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository
        expenseRepository;

    private final ExpenseAccessService
        accessService;

    private final ExpenseNumberService
        numberService;

    private final BudgetService
        budgetService;

    private final SupplierService
        supplierService;

    private final PurchaseOrderRepository
        purchaseOrderRepository;

    public ExpenseService(
        ExpenseRepository expenseRepository,
        ExpenseAccessService accessService,
        ExpenseNumberService numberService,
        BudgetService budgetService,
        SupplierService supplierService,
        PurchaseOrderRepository
            purchaseOrderRepository
    ) {
        this.expenseRepository =
            expenseRepository;

        this.accessService =
            accessService;

        this.numberService =
            numberService;

        this.budgetService =
            budgetService;

        this.supplierService =
            supplierService;

        this.purchaseOrderRepository =
            purchaseOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<
        ExpenseDtos.ExpenseView
    > list(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        accessService.requireView(
            organizationId,
            projectId,
            userId
        );

        return expenseRepository
            .findByProjectIdAndOrganizationIdOrderByExpenseDateDescCreatedAtDesc(
                projectId,
                organizationId
            )
            .stream()
            .map(
                ExpenseDtos
                    .ExpenseView
                    ::from
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseDtos.ExpenseView get(
        UUID organizationId,
        UUID userId,
        UUID expenseId
    ) {
        Expense expense =
            requireExpense(
                organizationId,
                expenseId
            );

        accessService.requireView(
            organizationId,
            expense.getProjectId(),
            userId
        );

        return ExpenseDtos
            .ExpenseView
            .from(expense);
    }

    @Transactional
    public ExpenseDtos.ExpenseView create(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        ExpenseDtos.CreateExpenseRequest
            request
    ) {
        OrganizationMembership actor =
            accessService
                .requireCreateOrSubmit(
                    organizationId,
                    projectId,
                    userId
                );

        budgetService.requireCategory(
            organizationId,
            projectId,
            request.budgetCategoryId()
        );

        ProcurementLink link =
            resolveProcurementLink(
                organizationId,
                projectId,
                request.supplierId(),
                request.purchaseOrderId()
            );

        ZoneId zone =
            ZoneId.of(
                actor
                    .getOrganization()
                    .getTimezone()
            );

        int year =
            LocalDate.now(zone)
                .getYear();

        String expenseNumber =
            numberService
                .nextExpenseNumber(
                    organizationId,
                    year
                );

        Expense expense =
            Expense.create(
                organizationId,
                projectId,
                request.budgetCategoryId(),
                link.supplierId(),
                link.purchaseOrderId(),
                expenseNumber,
                request.description().trim(),
                request.expenseDate(),
                request.netAmount(),
                request.taxAmount(),
                userId,
                Instant.now()
            );

        expenseRepository.save(
            expense
        );

        return ExpenseDtos
            .ExpenseView
            .from(expense);
    }

    @Transactional
    public ExpenseDtos.ExpenseView update(
        UUID organizationId,
        UUID userId,
        UUID expenseId,
        ExpenseDtos.UpdateExpenseRequest
            request
    ) {
        Expense expense =
            requireExpense(
                organizationId,
                expenseId
            );

        accessService
            .requireCreateOrSubmit(
                organizationId,
                expense.getProjectId(),
                userId
            );

        if (
            request.budgetCategoryId()
                != null
        ) {
            budgetService.requireCategory(
                organizationId,
                expense.getProjectId(),
                request.budgetCategoryId()
            );
        }

        expense.updateDraft(
            request.budgetCategoryId(),
            cleanPreserveNull(
                request.description()
            ),
            request.expenseDate(),
            request.netAmount(),
            request.taxAmount(),
            Instant.now()
        );

        return ExpenseDtos
            .ExpenseView
            .from(expense);
    }

    @Transactional
    public ExpenseDtos.ExpenseView submit(
        UUID organizationId,
        UUID userId,
        UUID expenseId
    ) {
        Expense expense =
            requireExpense(
                organizationId,
                expenseId
            );

        accessService
            .requireCreateOrSubmit(
                organizationId,
                expense.getProjectId(),
                userId
            );

        expense.submit(
            Instant.now()
        );

        return ExpenseDtos
            .ExpenseView
            .from(expense);
    }

    @Transactional
    public ExpenseDtos.ExpenseView approve(
        UUID organizationId,
        UUID userId,
        UUID expenseId
    ) {
        Expense expense =
            requireExpense(
                organizationId,
                expenseId
            );

        accessService.requireApproval(
            organizationId,
            expense.getProjectId(),
            userId
        );

        expense.approve(
            userId,
            Instant.now()
        );

        return ExpenseDtos
            .ExpenseView
            .from(expense);
    }

    @Transactional
    public ExpenseDtos.ExpenseView reject(
        UUID organizationId,
        UUID userId,
        UUID expenseId,
        ExpenseDtos.RejectExpenseRequest
            request
    ) {
        Expense expense =
            requireExpense(
                organizationId,
                expenseId
            );

        accessService.requireApproval(
            organizationId,
            expense.getProjectId(),
            userId
        );

        expense.reject(
            clean(
                request.reason()
            ),
            Instant.now()
        );

        return ExpenseDtos
            .ExpenseView
            .from(expense);
    }

    @Transactional
    public ExpenseDtos.ExpenseView voidExpense(
        UUID organizationId,
        UUID userId,
        UUID expenseId,
        ExpenseDtos.VoidExpenseRequest
            request
    ) {
        Expense expense =
            requireExpense(
                organizationId,
                expenseId
            );

        accessService.requireApproval(
            organizationId,
            expense.getProjectId(),
            userId
        );

        expense.voidExpense(
            request.reason(),
            userId,
            Instant.now()
        );

        return ExpenseDtos
            .ExpenseView
            .from(expense);
    }

    @Transactional
    public void deleteDraft(
        UUID organizationId,
        UUID userId,
        UUID expenseId
    ) {
        Expense expense =
            requireExpense(
                organizationId,
                expenseId
            );

        accessService
            .requireCreateOrSubmit(
                organizationId,
                expense.getProjectId(),
                userId
            );

        if (
            expense.getStatus()
                != ExpenseStatus.DRAFT
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "EXPENSE_DELETE_FORBIDDEN",
                "Only a draft expense can be deleted."
            );
        }

        expenseRepository.delete(
            expense
        );
    }

    private ProcurementLink
        resolveProcurementLink(
            UUID organizationId,
            UUID projectId,
            UUID requestedSupplierId,
            UUID purchaseOrderId
        ) {

        if (purchaseOrderId == null) {
            if (
                requestedSupplierId
                    == null
            ) {
                return new ProcurementLink(
                    null,
                    null
                );
            }

            Supplier supplier =
                supplierService
                    .requireSupplier(
                        organizationId,
                        requestedSupplierId
                    );

            return new ProcurementLink(
                supplier.getId(),
                null
            );
        }

        PurchaseOrder order =
            purchaseOrderRepository
                .findByIdAndProjectIdAndOrganizationId(
                    purchaseOrderId,
                    projectId,
                    organizationId
                )
                .orElseThrow(
                    () ->
                        new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "PURCHASE_ORDER_NOT_FOUND",
                            "The purchase order does not exist in this project."
                        )
                );

        if (
            order.getStatus()
                == PurchaseOrderStatus.CANCELLED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "CANCELLED_PURCHASE_ORDER_EXPENSE",
                "An expense cannot be linked to a cancelled purchase order."
            );
        }

        if (
            requestedSupplierId
                != null
            && !requestedSupplierId
                .equals(
                    order.getSupplierId()
                )
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "EXPENSE_SUPPLIER_MISMATCH",
                "The expense supplier must match the purchase order supplier."
            );
        }

        return new ProcurementLink(
            order.getSupplierId(),
            order.getId()
        );
    }

    private Expense requireExpense(
        UUID organizationId,
        UUID expenseId
    ) {
        return expenseRepository
            .findByIdAndOrganizationId(
                expenseId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "EXPENSE_NOT_FOUND",
                        "The expense does not exist or is not accessible."
                    )
            );
    }

    private String clean(
        String value
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private String cleanPreserveNull(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private record ProcurementLink(
        UUID supplierId,
        UUID purchaseOrderId
    ) {
    }
}
