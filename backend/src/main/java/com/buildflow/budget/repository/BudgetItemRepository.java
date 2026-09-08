package com.buildflow.budget.repository;

import com.buildflow.budget.entity.BudgetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetItemRepository
    extends JpaRepository<
        BudgetItem,
        UUID
    > {

    List<BudgetItem>
        findByBudgetCategoryIdAndProjectIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID budgetCategoryId,
            UUID projectId,
            UUID organizationId
        );

    Optional<BudgetItem>
        findByIdAndProjectIdAndOrganizationId(
            UUID id,
            UUID projectId,
            UUID organizationId
        );

    @Query("""
        select sum(item.plannedAmount)
        from BudgetItem item
        where item.projectId = :projectId
          and item.organizationId = :organizationId
        """)
    BigDecimal sumProjectPlannedAmount(
        @Param("projectId")
        UUID projectId,
        @Param("organizationId")
        UUID organizationId
    );

    @Query("""
        select sum(item.plannedAmount)
        from BudgetItem item
        where item.projectId = :projectId
          and item.organizationId = :organizationId
          and item.budgetCategoryId = :categoryId
        """)
    BigDecimal sumCategoryPlannedAmount(
        @Param("projectId")
        UUID projectId,
        @Param("organizationId")
        UUID organizationId,
        @Param("categoryId")
        UUID categoryId
    );
}
