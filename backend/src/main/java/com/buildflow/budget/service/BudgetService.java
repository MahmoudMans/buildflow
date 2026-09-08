package com.buildflow.budget.service;

import com.buildflow.budget.dto.BudgetDtos;
import com.buildflow.budget.entity.BudgetCategory;
import com.buildflow.budget.entity.BudgetItem;
import com.buildflow.budget.repository.BudgetCategoryRepository;
import com.buildflow.budget.repository.BudgetItemRepository;
import com.buildflow.expense.repository.ExpenseRepository;
import com.buildflow.project.entity.Project;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BudgetService {

    private final BudgetCategoryRepository
        categoryRepository;

    private final BudgetItemRepository
        itemRepository;

    private final ExpenseRepository
        expenseRepository;

    private final BudgetCalculationService
        calculationService;

    private final BudgetAccessService
        accessService;

    public BudgetService(
        BudgetCategoryRepository
            categoryRepository,
        BudgetItemRepository itemRepository,
        ExpenseRepository expenseRepository,
        BudgetCalculationService
            calculationService,
        BudgetAccessService accessService
    ) {
        this.categoryRepository =
            categoryRepository;

        this.itemRepository =
            itemRepository;

        this.expenseRepository =
            expenseRepository;

        this.calculationService =
            calculationService;

        this.accessService =
            accessService;
    }

    @Transactional(readOnly = true)
    public BudgetDtos.BudgetView getBudget(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        Project project =
            accessService.requireView(
                organizationId,
                projectId,
                userId
            );

        List<
            BudgetDtos.BudgetCategoryView
        > categories =
            categoryRepository
                .findByProjectIdAndOrganizationIdOrderBySortOrderAscCreatedAtAsc(
                    projectId,
                    organizationId
                )
                .stream()
                .map(
                    category ->
                        categoryView(
                            organizationId,
                            projectId,
                            category
                        )
                )
                .toList();

        return new BudgetDtos
            .BudgetView(
                projectId,
                project.isBudgetBaselineLocked(),
                calculationService
                    .totalPlannedAmount(
                        organizationId,
                        projectId
                    ),
                project.getOriginalBudget(),
                categories
            );
    }

    @Transactional
    public BudgetDtos.BudgetCategoryView
        createCategory(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            BudgetDtos.CreateCategoryRequest
                request
        ) {

        accessService.requireEdit(
            organizationId,
            projectId,
            userId
        );

        String name =
            request.name().trim();

        if (
            categoryRepository
                .existsByProjectIdAndOrganizationIdAndNameIgnoreCase(
                    projectId,
                    organizationId,
                    name
                )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "BUDGET_CATEGORY_EXISTS",
                "A budget category with this name already exists."
            );
        }

        BudgetCategory category =
            BudgetCategory.create(
                organizationId,
                projectId,
                name,
                clean(
                    request.description()
                ),
                request.sortOrder() == null
                    ? 0
                    : request.sortOrder(),
                Instant.now()
            );

        categoryRepository.save(
            category
        );

        return categoryView(
            organizationId,
            projectId,
            category
        );
    }

    @Transactional
    public BudgetDtos.BudgetCategoryView
        updateCategory(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            UUID categoryId,
            BudgetDtos.UpdateCategoryRequest
                request
        ) {

        accessService.requireEdit(
            organizationId,
            projectId,
            userId
        );

        BudgetCategory category =
            requireCategory(
                organizationId,
                projectId,
                categoryId
            );

        if (
            request.name() != null
            && !request
                .name()
                .trim()
                .equalsIgnoreCase(
                    category.getName()
                )
            && categoryRepository
                .existsByProjectIdAndOrganizationIdAndNameIgnoreCase(
                    projectId,
                    organizationId,
                    request.name().trim()
                )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "BUDGET_CATEGORY_EXISTS",
                "A budget category with this name already exists."
            );
        }

        category.update(
            cleanPreserveNull(
                request.name()
            ),
            cleanPreserveNull(
                request.description()
            ),
            request.sortOrder(),
            Instant.now()
        );

        return categoryView(
            organizationId,
            projectId,
            category
        );
    }

    @Transactional
    public void deleteCategory(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        UUID categoryId
    ) {
        accessService.requireEdit(
            organizationId,
            projectId,
            userId
        );

        BudgetCategory category =
            requireCategory(
                organizationId,
                projectId,
                categoryId
            );

        if (
            expenseRepository
                .existsByBudgetCategoryIdAndOrganizationId(
                    categoryId,
                    organizationId
                )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "BUDGET_CATEGORY_IN_USE",
                "A budget category referenced by an expense cannot be deleted."
            );
        }

        categoryRepository.delete(
            category
        );
    }

    @Transactional
    public BudgetDtos.BudgetItemView
        createItem(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            UUID categoryId,
            BudgetDtos.CreateItemRequest
                request
        ) {

        accessService.requireEdit(
            organizationId,
            projectId,
            userId
        );

        requireCategory(
            organizationId,
            projectId,
            categoryId
        );

        if (
            request.quantity() == null
            || request.unitCost() == null
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "BUDGET_ITEM_VALUES_REQUIRED",
                "Quantity and unit cost are required."
            );
        }

        BudgetItem item =
            BudgetItem.create(
                organizationId,
                projectId,
                categoryId,
                request
                    .description()
                    .trim(),
                request.quantity(),
                request.unitCost(),
                clean(
                    request.notes()
                ),
                Instant.now()
            );

        itemRepository.save(item);

        return BudgetDtos
            .BudgetItemView
            .from(item);
    }

    @Transactional
    public BudgetDtos.BudgetItemView
        updateItem(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            UUID itemId,
            BudgetDtos.UpdateItemRequest
                request
        ) {

        accessService.requireEdit(
            organizationId,
            projectId,
            userId
        );

        BudgetItem item =
            requireItem(
                organizationId,
                projectId,
                itemId
            );

        item.update(
            cleanPreserveNull(
                request.description()
            ),
            request.quantity(),
            request.unitCost(),
            cleanPreserveNull(
                request.notes()
            ),
            Instant.now()
        );

        return BudgetDtos
            .BudgetItemView
            .from(item);
    }

    @Transactional
    public void deleteItem(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        UUID itemId
    ) {
        accessService.requireEdit(
            organizationId,
            projectId,
            userId
        );

        BudgetItem item =
            requireItem(
                organizationId,
                projectId,
                itemId
            );

        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public BudgetCategory requireCategory(
        UUID organizationId,
        UUID projectId,
        UUID categoryId
    ) {
        return categoryRepository
            .findByIdAndProjectIdAndOrganizationId(
                categoryId,
                projectId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "BUDGET_CATEGORY_NOT_FOUND",
                        "The budget category does not exist or is not accessible."
                    )
            );
    }

    private BudgetItem requireItem(
        UUID organizationId,
        UUID projectId,
        UUID itemId
    ) {
        return itemRepository
            .findByIdAndProjectIdAndOrganizationId(
                itemId,
                projectId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "BUDGET_ITEM_NOT_FOUND",
                        "The budget item does not exist or is not accessible."
                    )
            );
    }

    private BudgetDtos.BudgetCategoryView
        categoryView(
            UUID organizationId,
            UUID projectId,
            BudgetCategory category
        ) {

        List<BudgetItem> items =
            itemRepository
                .findByBudgetCategoryIdAndProjectIdAndOrganizationIdOrderByCreatedAtAsc(
                    category.getId(),
                    projectId,
                    organizationId
                );

        BigDecimal total =
            calculationService
                .categoryPlannedAmount(
                    organizationId,
                    projectId,
                    category.getId()
                );

        return BudgetDtos
            .BudgetCategoryView
            .from(
                category,
                total,
                items
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
}
