package com.buildflow.dashboard.controller;

import com.buildflow.dashboard.dto.DashboardDtos;
import com.buildflow.dashboard.service.DashboardService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService
        dashboardService;

    public DashboardController(
        DashboardService dashboardService
    ) {
        this.dashboardService =
            dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardDtos.OrganizationDashboard
        organization(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return dashboardService
            .organization(
                organizationId,
                principal.userId()
            );
    }

    @GetMapping(
        "/projects/{projectId}/dashboard"
    )
    public DashboardDtos.ProjectDashboard
        project(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return dashboardService
            .project(
                organizationId,
                principal.userId(),
                projectId
            );
    }
}
