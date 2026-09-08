package com.buildflow.budget.repository;

import com.buildflow.budget.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetCategoryRepository
    extends JpaRepository<
        BudgetCategory,
        UUID
    > {

    List<BudgetCategory>
        findByProjectIdAndOrganizationIdOrderBySortOrderAscCreatedAtAsc(
            UUID projectId,
            UUID organizationId
        );

    Optional<BudgetCategory>
        findByIdAndProjectIdAndOrganizationId(
            UUID id,
            UUID projectId,
            UUID organizationId
        );

    boolean existsByProjectIdAndOrganizationIdAndNameIgnoreCase(
        UUID projectId,
        UUID organizationId,
        String name
    );
}
