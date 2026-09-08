package com.buildflow.commercial.controller;

import com.buildflow.commercial.dto.CommercialDtos;
import com.buildflow.commercial.service.InvoiceService;
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
    "/api/v1/projects/{projectId}/invoices"
)
public class ProjectInvoiceController {

    private final InvoiceService
        invoiceService;

    public ProjectInvoiceController(
        InvoiceService invoiceService
    ) {
        this.invoiceService =
            invoiceService;
    }

    @GetMapping
    public List<
        CommercialDtos.InvoiceSummary
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
        return invoiceService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping
    public ResponseEntity<
        CommercialDtos.InvoiceView
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
        CommercialDtos.CreateInvoiceRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                invoiceService.create(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }
}
