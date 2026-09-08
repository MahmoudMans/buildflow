package com.buildflow.commercial.controller;

import com.buildflow.commercial.dto.CommercialDtos;
import com.buildflow.commercial.service.RevenueMetricsService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
    "/api/v1/projects/{projectId}/commercial-summary"
)
public class CommercialSummaryController {

    private final RevenueMetricsService
        revenueMetricsService;

    public CommercialSummaryController(
        RevenueMetricsService
            revenueMetricsService
    ) {
        this.revenueMetricsService =
            revenueMetricsService;
    }

    @GetMapping
    public CommercialDtos.RevenueSummary
        summary(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return revenueMetricsService
            .summary(
                organizationId,
                principal.userId(),
                projectId
            );
    }
}
