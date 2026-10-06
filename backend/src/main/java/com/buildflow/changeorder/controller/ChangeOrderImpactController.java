package com.buildflow.changeorder.controller;

import com.buildflow.changeorder.dto.ChangeOrderImpactDtos;
import com.buildflow.changeorder.service.ChangeOrderImpactService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/change-orders")
public class ChangeOrderImpactController {

    private final ChangeOrderImpactService impactService;

    public ChangeOrderImpactController(
        ChangeOrderImpactService impactService
    ) {
        this.impactService = impactService;
    }

    @GetMapping("/{changeOrderId}/impact")
    public ChangeOrderImpactDtos.Impact impact(
        @RequestHeader("X-Organization-Id")
        UUID organizationId,
        @PathVariable
        UUID changeOrderId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return impactService.impact(
            organizationId,
            principal.userId(),
            changeOrderId
        );
    }
}
