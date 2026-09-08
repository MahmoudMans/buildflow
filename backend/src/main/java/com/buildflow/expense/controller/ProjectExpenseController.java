package com.buildflow.expense.controller;

import com.buildflow.expense.dto.ExpenseDtos;
import com.buildflow.expense.service.ExpenseService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
    "/api/v1/projects/{projectId}/expenses"
)
public class ProjectExpenseController {

    private final ExpenseService
        expenseService;

    public ProjectExpenseController(
        ExpenseService expenseService
    ) {
        this.expenseService =
            expenseService;
    }

    @GetMapping
    public List<
        ExpenseDtos.ExpenseView
    > list(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return expenseService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping
    public ResponseEntity<
        ExpenseDtos.ExpenseView
    > create(
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
        ExpenseDtos.CreateExpenseRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                expenseService.create(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }
}
