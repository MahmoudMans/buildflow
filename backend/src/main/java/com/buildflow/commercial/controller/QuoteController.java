package com.buildflow.commercial.controller;

import com.buildflow.commercial.dto.CommercialDtos;
import com.buildflow.commercial.service.InvoiceService;
import com.buildflow.commercial.service.QuoteService;
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
@RequestMapping("/api/v1/quotes")
public class QuoteController {

    private final QuoteService quoteService;
    private final InvoiceService invoiceService;

    public QuoteController(
        QuoteService quoteService,
        InvoiceService invoiceService
    ) {
        this.quoteService = quoteService;
        this.invoiceService = invoiceService;
    }

    @GetMapping("/{quoteId}")
    public CommercialDtos.QuoteView get(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID quoteId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return quoteService.get(
            organizationId,
            principal.userId(),
            quoteId
        );
    }

    @PatchMapping("/{quoteId}")
    public CommercialDtos.QuoteView update(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID quoteId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        CommercialDtos.UpdateQuoteRequest
            request
    ) {
        return quoteService.update(
            organizationId,
            principal.userId(),
            quoteId,
            request
        );
    }

    @PostMapping("/{quoteId}/items")
    public CommercialDtos.QuoteView addItem(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID quoteId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        CommercialDtos.LineRequest request
    ) {
        return quoteService.addItem(
            organizationId,
            principal.userId(),
            quoteId,
            request
        );
    }

    @PatchMapping(
        "/{quoteId}/items/{itemId}"
    )
    public CommercialDtos.QuoteView
        updateItem(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID quoteId,
            @PathVariable
            UUID itemId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            CommercialDtos.UpdateLineRequest
                request
        ) {

        return quoteService.updateItem(
            organizationId,
            principal.userId(),
            quoteId,
            itemId,
            request
        );
    }

    @DeleteMapping(
        "/{quoteId}/items/{itemId}"
    )
    public CommercialDtos.QuoteView
        deleteItem(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID quoteId,
            @PathVariable
            UUID itemId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return quoteService.deleteItem(
            organizationId,
            principal.userId(),
            quoteId,
            itemId
        );
    }

    @PostMapping("/{quoteId}/send")
    public CommercialDtos.QuoteView send(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID quoteId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return quoteService.send(
            organizationId,
            principal.userId(),
            quoteId
        );
    }

    @PostMapping("/{quoteId}/view")
    public CommercialDtos.QuoteView view(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID quoteId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return quoteService.markViewed(
            organizationId,
            principal.userId(),
            quoteId
        );
    }

    @PostMapping("/{quoteId}/accept")
    public CommercialDtos.QuoteView accept(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID quoteId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return quoteService.accept(
            organizationId,
            principal.userId(),
            quoteId
        );
    }

    @PostMapping("/{quoteId}/reject")
    public CommercialDtos.QuoteView reject(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID quoteId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return quoteService.reject(
            organizationId,
            principal.userId(),
            quoteId
        );
    }

    @PostMapping("/{quoteId}/expire")
    public CommercialDtos.QuoteView expire(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID quoteId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return quoteService.expire(
            organizationId,
            principal.userId(),
            quoteId
        );
    }

    @PostMapping("/{quoteId}/cancel")
    public CommercialDtos.QuoteView cancel(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID quoteId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        CommercialDtos.CancelRequest
            request
    ) {
        return quoteService.cancel(
            organizationId,
            principal.userId(),
            quoteId,
            request
        );
    }

    @PostMapping("/{quoteId}/create-invoice")
    public CommercialDtos.InvoiceView
        createInvoice(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID quoteId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            CommercialDtos
                .CreateInvoiceFromQuoteRequest
                request
        ) {

        return invoiceService
            .createFromQuote(
                organizationId,
                principal.userId(),
                quoteId,
                request
            );
    }
}
