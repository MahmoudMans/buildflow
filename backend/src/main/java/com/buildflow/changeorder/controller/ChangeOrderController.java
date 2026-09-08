package com.buildflow.changeorder.controller;

import com.buildflow.changeorder.dto.ChangeOrderDtos;
import com.buildflow.changeorder.service.ChangeOrderService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/change-orders")
public class ChangeOrderController {

    private final ChangeOrderService
        changeOrderService;

    public ChangeOrderController(
        ChangeOrderService
            changeOrderService
    ) {
        this.changeOrderService =
            changeOrderService;
    }

    @GetMapping("/{changeOrderId}")
    public ChangeOrderDtos.View get(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID changeOrderId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return changeOrderService.get(
            organizationId,
            principal.userId(),
            changeOrderId
        );
    }

    @PatchMapping("/{changeOrderId}")
    public ChangeOrderDtos.View update(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID changeOrderId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        ChangeOrderDtos.UpdateRequest
            request
    ) {
        return changeOrderService.update(
            organizationId,
            principal.userId(),
            changeOrderId,
            request
        );
    }

    @PostMapping(
        "/{changeOrderId}/submit"
    )
    public ChangeOrderDtos.View submit(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID changeOrderId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return changeOrderService.submit(
            organizationId,
            principal.userId(),
            changeOrderId
        );
    }

    @PostMapping(
        "/{changeOrderId}/client-review"
    )
    public ChangeOrderDtos.View
        clientReview(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID changeOrderId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return changeOrderService
            .sendToClientReview(
                organizationId,
                principal.userId(),
                changeOrderId
            );
    }

    @PostMapping(
        "/{changeOrderId}/approve"
    )
    public ChangeOrderDtos.View approve(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID changeOrderId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return changeOrderService.approve(
            organizationId,
            principal.userId(),
            changeOrderId
        );
    }

    @PostMapping(
        "/{changeOrderId}/reject"
    )
    public ChangeOrderDtos.View reject(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID changeOrderId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return changeOrderService.reject(
            organizationId,
            principal.userId(),
            changeOrderId
        );
    }

    @PostMapping(
        "/{changeOrderId}/cancel"
    )
    public ChangeOrderDtos.View cancel(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID changeOrderId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        ChangeOrderDtos.CancelRequest
            request
    ) {
        return changeOrderService.cancel(
            organizationId,
            principal.userId(),
            changeOrderId,
            request
        );
    }
}
