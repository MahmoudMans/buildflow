package com.buildflow.budget.controller;

import com.buildflow.budget.dto.BudgetDtos;
import com.buildflow.budget.service.BudgetService;
import com.buildflow.budget.service.FinancialSummaryService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
    "/api/v1/projects/{projectId}"
)
public class BudgetController {

    private final BudgetService
        budgetService;

    private final FinancialSummaryService
        financialSummaryService;

    public BudgetController(
        BudgetService budgetService,
        FinancialSummaryService
            financialSummaryService
    ) {
        this.budgetService =
            budgetService;

        this.financialSummaryService =
            financialSummaryService;
    }

    @GetMapping("/budget")
    public BudgetDtos.BudgetView budget(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return budgetService.getBudget(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping(
        "/budget/categories"
    )
    public ResponseEntity<
        BudgetDtos.BudgetCategoryView
    > createCategory(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        BudgetDtos.CreateCategoryRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                budgetService
                    .createCategory(
                        organizationId,
                        principal.userId(),
                        projectId,
                        request
                    )
            );
    }

    @PatchMapping(
        "/budget/categories/{categoryId}"
    )
    public BudgetDtos.BudgetCategoryView
        updateCategory(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID categoryId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            BudgetDtos.UpdateCategoryRequest
                request
        ) {
        return budgetService
            .updateCategory(
                organizationId,
                principal.userId(),
                projectId,
                categoryId,
                request
            );
    }

    @DeleteMapping(
        "/budget/categories/{categoryId}"
    )
    public ResponseEntity<Void>
        deleteCategory(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID categoryId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        budgetService.deleteCategory(
            organizationId,
            principal.userId(),
            projectId,
            categoryId
        );

        return ResponseEntity
            .noContent()
            .build();
    }

    @PostMapping(
        "/budget/categories/{categoryId}/items"
    )
    public ResponseEntity<
        BudgetDtos.BudgetItemView
    > createItem(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @PathVariable
        UUID categoryId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        BudgetDtos.CreateItemRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                budgetService.createItem(
                    organizationId,
                    principal.userId(),
                    projectId,
                    categoryId,
                    request
                )
            );
    }

    @PatchMapping(
        "/budget/items/{itemId}"
    )
    public BudgetDtos.BudgetItemView
        updateItem(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID itemId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            BudgetDtos.UpdateItemRequest
                request
        ) {

        return budgetService.updateItem(
            organizationId,
            principal.userId(),
            projectId,
            itemId,
            request
        );
    }

    @DeleteMapping(
        "/budget/items/{itemId}"
    )
    public ResponseEntity<Void>
        deleteItem(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID itemId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        budgetService.deleteItem(
            organizationId,
            principal.userId(),
            projectId,
            itemId
        );

        return ResponseEntity
            .noContent()
            .build();
    }

    @GetMapping("/financial-summary")
    public BudgetDtos.FinancialSummaryView
        financialSummary(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {
        return financialSummaryService
            .summary(
                organizationId,
                principal.userId(),
                projectId
            );
    }
}
