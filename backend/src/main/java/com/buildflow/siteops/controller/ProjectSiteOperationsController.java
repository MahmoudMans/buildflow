package com.buildflow.siteops.controller;

import com.buildflow.security.BuildFlowPrincipal;
import com.buildflow.siteops.dto.SiteOperationDtos;
import com.buildflow.siteops.service.DailySiteReportService;
import com.buildflow.siteops.service.ProjectProgressUpdateService;
import com.buildflow.siteops.service.SiteIncidentService;
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
    "/api/v1/projects/{projectId}"
)
public class ProjectSiteOperationsController {

    private final DailySiteReportService
        reportService;

    private final SiteIncidentService
        incidentService;

    private final ProjectProgressUpdateService
        progressUpdateService;

    public ProjectSiteOperationsController(
        DailySiteReportService
            reportService,
        SiteIncidentService
            incidentService,
        ProjectProgressUpdateService
            progressUpdateService
    ) {
        this.reportService =
            reportService;

        this.incidentService =
            incidentService;

        this.progressUpdateService =
            progressUpdateService;
    }

    @GetMapping("/site-reports")
    public List<
        SiteOperationDtos.DailyReportView
    > reports(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return reportService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping("/site-reports")
    public ResponseEntity<
        SiteOperationDtos.DailyReportView
    > createReport(
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
        SiteOperationDtos
            .CreateDailyReportRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                reportService.create(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }

    @GetMapping("/incidents")
    public List<
        SiteOperationDtos.IncidentView
    > incidents(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return incidentService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping("/incidents")
    public ResponseEntity<
        SiteOperationDtos.IncidentView
    > createIncident(
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
        SiteOperationDtos
            .CreateIncidentRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                incidentService.create(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }

    @GetMapping("/progress-updates")
    public List<
        SiteOperationDtos.ProgressUpdateView
    > progressUpdates(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return progressUpdateService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping("/progress-updates")
    public ResponseEntity<
        SiteOperationDtos.ProgressUpdateView
    > createProgressUpdate(
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
        SiteOperationDtos
            .CreateProgressUpdateRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                progressUpdateService.create(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }
}
