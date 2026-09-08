package com.buildflow.expense.repository;

import com.buildflow.expense.entity.Expense;
import com.buildflow.expense.entity.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository
    extends JpaRepository<
        Expense,
        UUID
    > {

    List<Expense>
        findByProjectIdAndOrganizationIdOrderByExpenseDateDescCreatedAtDesc(
            UUID projectId,
            UUID organizationId
        );

    Optional<Expense>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    Optional<Expense>
        findByIdAndProjectIdAndOrganizationId(
            UUID id,
            UUID projectId,
            UUID organizationId
        );

    boolean
        existsByBudgetCategoryIdAndOrganizationId(
            UUID budgetCategoryId,
            UUID organizationId
        );

    @Query("""
        select sum(expense.totalAmount)
        from Expense expense
        where expense.projectId = :projectId
          and expense.organizationId = :organizationId
          and expense.status = :status
        """)
    BigDecimal sumProjectTotalByStatus(
        @Param("projectId")
        UUID projectId,
        @Param("organizationId")
        UUID organizationId,
        @Param("status")
        ExpenseStatus status
    );

    @Query("""
        select sum(expense.totalAmount)
        from Expense expense
        where expense.purchaseOrderId = :purchaseOrderId
          and expense.organizationId = :organizationId
          and expense.projectId = :projectId
          and expense.status = com.buildflow.expense.entity.ExpenseStatus.APPROVED
        """)
    BigDecimal sumApprovedByPurchaseOrder(
        @Param("purchaseOrderId")
        UUID purchaseOrderId,
        @Param("organizationId")
        UUID organizationId,
        @Param("projectId")
        UUID projectId
    );
}
