package com.buildflow.procurement.controller;

import com.buildflow.procurement.dto.ProcurementDtos;
import com.buildflow.procurement.service.PurchaseOrderService;
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
@RequestMapping(
    "/api/v1/purchase-orders"
)
public class PurchaseOrderController {

    private final PurchaseOrderService
        purchaseOrderService;

    public PurchaseOrderController(
        PurchaseOrderService
            purchaseOrderService
    ) {
        this.purchaseOrderService =
            purchaseOrderService;
    }

    @GetMapping("/{purchaseOrderId}")
    public ProcurementDtos.PurchaseOrderView
        get(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID purchaseOrderId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return purchaseOrderService.get(
            organizationId,
            principal.userId(),
            purchaseOrderId
        );
    }

    @PatchMapping("/{purchaseOrderId}")
    public ProcurementDtos.PurchaseOrderView
        update(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID purchaseOrderId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            ProcurementDtos
                .UpdatePurchaseOrderRequest
                request
        ) {

        return purchaseOrderService.update(
            organizationId,
            principal.userId(),
            purchaseOrderId,
            request
        );
    }

    @PostMapping(
        "/{purchaseOrderId}/items"
    )
    public ProcurementDtos.PurchaseOrderView
        addItem(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID purchaseOrderId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            ProcurementDtos
                .CreateItemRequest
                request
        ) {

        return purchaseOrderService.addItem(
            organizationId,
            principal.userId(),
            purchaseOrderId,
            request
        );
    }

    @PatchMapping(
        "/{purchaseOrderId}/items/{itemId}"
    )
    public ProcurementDtos.PurchaseOrderView
        updateItem(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID purchaseOrderId,
            @PathVariable
            UUID itemId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            ProcurementDtos
                .UpdateItemRequest
                request
        ) {

        return purchaseOrderService
            .updateItem(
                organizationId,
                principal.userId(),
                purchaseOrderId,
                itemId,
                request
            );
    }

    @DeleteMapping(
        "/{purchaseOrderId}/items/{itemId}"
    )
    public ProcurementDtos.PurchaseOrderView
        deleteItem(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID purchaseOrderId,
            @PathVariable
            UUID itemId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return purchaseOrderService
            .deleteItem(
                organizationId,
                principal.userId(),
                purchaseOrderId,
                itemId
            );
    }

    @PostMapping(
        "/{purchaseOrderId}/submit"
    )
    public ProcurementDtos.PurchaseOrderView
        submit(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID purchaseOrderId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return purchaseOrderService.submit(
            organizationId,
            principal.userId(),
            purchaseOrderId
        );
    }

    @PostMapping(
        "/{purchaseOrderId}/approve"
    )
    public ProcurementDtos.PurchaseOrderView
        approve(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID purchaseOrderId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return purchaseOrderService.approve(
            organizationId,
            principal.userId(),
            purchaseOrderId
        );
    }

    @PostMapping(
        "/{purchaseOrderId}/order"
    )
    public ProcurementDtos.PurchaseOrderView
        order(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID purchaseOrderId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return purchaseOrderService
            .markOrdered(
                organizationId,
                principal.userId(),
                purchaseOrderId
            );
    }

    @PostMapping(
        "/{purchaseOrderId}/cancel"
    )
    public ProcurementDtos.PurchaseOrderView
        cancel(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID purchaseOrderId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            ProcurementDtos
                .CancelPurchaseOrderRequest
                request
        ) {

        return purchaseOrderService.cancel(
            organizationId,
            principal.userId(),
            purchaseOrderId,
            request
        );
    }

    @PostMapping(
        "/{purchaseOrderId}/receipts"
    )
    public ProcurementDtos.PurchaseOrderView
        receive(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID purchaseOrderId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            ProcurementDtos
                .CreateGoodsReceiptRequest
                request
        ) {

        return purchaseOrderService.receive(
            organizationId,
            principal.userId(),
            purchaseOrderId,
            request
        );
    }
}
