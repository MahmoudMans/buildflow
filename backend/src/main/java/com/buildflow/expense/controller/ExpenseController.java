package com.buildflow.expense.controller;

import com.buildflow.expense.dto.ExpenseDtos;
import com.buildflow.expense.service.ExpenseService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService
        expenseService;

    public ExpenseController(
        ExpenseService expenseService
    ) {
        this.expenseService =
            expenseService;
    }

    @GetMapping("/{expenseId}")
    public ExpenseDtos.ExpenseView get(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID expenseId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return expenseService.get(
            organizationId,
            principal.userId(),
            expenseId
        );
    }

    @PatchMapping("/{expenseId}")
    public ExpenseDtos.ExpenseView update(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID expenseId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        ExpenseDtos.UpdateExpenseRequest
            request
    ) {
        return expenseService.update(
            organizationId,
            principal.userId(),
            expenseId,
            request
        );
    }

    @PostMapping("/{expenseId}/submit")
    public ExpenseDtos.ExpenseView submit(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID expenseId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return expenseService.submit(
            organizationId,
            principal.userId(),
            expenseId
        );
    }

    @PostMapping("/{expenseId}/approve")
    public ExpenseDtos.ExpenseView approve(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID expenseId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return expenseService.approve(
            organizationId,
            principal.userId(),
            expenseId
        );
    }

    @PostMapping("/{expenseId}/reject")
    public ExpenseDtos.ExpenseView reject(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID expenseId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        ExpenseDtos.RejectExpenseRequest
            request
    ) {
        return expenseService.reject(
            organizationId,
            principal.userId(),
            expenseId,
            request
        );
    }

    @PostMapping("/{expenseId}/void")
    public ExpenseDtos.ExpenseView voidExpense(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID expenseId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        ExpenseDtos.VoidExpenseRequest
            request
    ) {
        return expenseService
            .voidExpense(
                organizationId,
                principal.userId(),
                expenseId,
                request
            );
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteDraft(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID expenseId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        expenseService.deleteDraft(
            organizationId,
            principal.userId(),
            expenseId
        );

        return ResponseEntity
            .noContent()
            .build();
    }
}
