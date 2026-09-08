package com.buildflow.changeorder.controller;

import com.buildflow.changeorder.dto.ChangeOrderDtos;
import com.buildflow.changeorder.service.ChangeOrderFinancialService;
import com.buildflow.changeorder.service.ChangeOrderService;
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
    "/api/v1/projects/{projectId}/change-orders"
)
public class ProjectChangeOrderController {

    private final ChangeOrderService
        changeOrderService;

    private final ChangeOrderFinancialService
        financialService;

    public ProjectChangeOrderController(
        ChangeOrderService
            changeOrderService,
        ChangeOrderFinancialService
            financialService
    ) {
        this.changeOrderService =
            changeOrderService;

        this.financialService =
            financialService;
    }

    @GetMapping
    public List<ChangeOrderDtos.Summary>
        list(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return changeOrderService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping
    public ResponseEntity<
        ChangeOrderDtos.View
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
        ChangeOrderDtos.CreateRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                changeOrderService.create(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }

    @GetMapping("/financial-summary")
    public ChangeOrderDtos.FinancialSummary
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

        return financialService.summary(
            organizationId,
            principal.userId(),
            projectId
        );
    }
}
