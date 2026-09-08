package com.buildflow.commercial.controller;

import com.buildflow.commercial.dto.CommercialDtos;
import com.buildflow.commercial.service.InvoiceService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService
        invoiceService;

    public InvoiceController(
        InvoiceService invoiceService
    ) {
        this.invoiceService =
            invoiceService;
    }

    @GetMapping("/{invoiceId}")
    public CommercialDtos.InvoiceView get(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID invoiceId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return invoiceService.get(
            organizationId,
            principal.userId(),
            invoiceId
        );
    }

    @PatchMapping("/{invoiceId}")
    public CommercialDtos.InvoiceView update(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID invoiceId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        CommercialDtos.UpdateInvoiceRequest
            request
    ) {
        return invoiceService.update(
            organizationId,
            principal.userId(),
            invoiceId,
            request
        );
    }

    @PostMapping("/{invoiceId}/items")
    public CommercialDtos.InvoiceView
        addItem(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID invoiceId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            CommercialDtos.LineRequest request
        ) {

        return invoiceService.addItem(
            organizationId,
            principal.userId(),
            invoiceId,
            request
        );
    }

    @PatchMapping(
        "/{invoiceId}/items/{itemId}"
    )
    public CommercialDtos.InvoiceView
        updateItem(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID invoiceId,
            @PathVariable
            UUID itemId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            CommercialDtos.UpdateLineRequest
                request
        ) {

        return invoiceService.updateItem(
            organizationId,
            principal.userId(),
            invoiceId,
            itemId,
            request
        );
    }

    @DeleteMapping(
        "/{invoiceId}/items/{itemId}"
    )
    public CommercialDtos.InvoiceView
        deleteItem(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID invoiceId,
            @PathVariable
            UUID itemId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return invoiceService.deleteItem(
            organizationId,
            principal.userId(),
            invoiceId,
            itemId
        );
    }

    @PostMapping("/{invoiceId}/issue")
    public CommercialDtos.InvoiceView issue(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID invoiceId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return invoiceService.issue(
            organizationId,
            principal.userId(),
            invoiceId
        );
    }

    @PostMapping("/{invoiceId}/void")
    public CommercialDtos.InvoiceView
        voidInvoice(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID invoiceId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            CommercialDtos.VoidInvoiceRequest
                request
        ) {

        return invoiceService
            .voidInvoice(
                organizationId,
                principal.userId(),
                invoiceId,
                request
            );
    }

    @PostMapping("/{invoiceId}/mark-overdue")
    public CommercialDtos.InvoiceView
        markOverdue(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID invoiceId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return invoiceService
            .markOverdue(
                organizationId,
                principal.userId(),
                invoiceId
            );
    }

    @PostMapping("/{invoiceId}/payments")
    public CommercialDtos.InvoiceView
        recordPayment(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID invoiceId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            CommercialDtos.RecordPaymentRequest
                request
        ) {

        return invoiceService
            .recordPayment(
                organizationId,
                principal.userId(),
                invoiceId,
                request
            );
    }
}
