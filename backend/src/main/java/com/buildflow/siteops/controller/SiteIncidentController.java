package com.buildflow.siteops.controller;

import com.buildflow.security.BuildFlowPrincipal;
import com.buildflow.siteops.dto.SiteOperationDtos;
import com.buildflow.siteops.service.SiteIncidentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class SiteIncidentController {

    private final SiteIncidentService
        incidentService;

    public SiteIncidentController(
        SiteIncidentService
            incidentService
    ) {
        this.incidentService =
            incidentService;
    }

    @GetMapping("/{incidentId}")
    public SiteOperationDtos.IncidentView get(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID incidentId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return incidentService.get(
            organizationId,
            principal.userId(),
            incidentId
        );
    }

    @PostMapping("/{incidentId}/update")
    public SiteOperationDtos.IncidentView update(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID incidentId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        SiteOperationDtos
            .UpdateIncidentRequest request
    ) {
        return incidentService.update(
            organizationId,
            principal.userId(),
            incidentId,
            request
        );
    }

    @PostMapping("/{incidentId}/investigate")
    public SiteOperationDtos.IncidentView investigate(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID incidentId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return incidentService.investigate(
            organizationId,
            principal.userId(),
            incidentId
        );
    }

    @PostMapping(
        "/{incidentId}/action-required"
    )
    public SiteOperationDtos.IncidentView
        requireAction(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID incidentId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return incidentService
            .requireAction(
                organizationId,
                principal.userId(),
                incidentId
            );
    }

    @PostMapping("/{incidentId}/resolve")
    public SiteOperationDtos.IncidentView resolve(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID incidentId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        SiteOperationDtos
            .ResolveIncidentRequest request
    ) {
        return incidentService.resolve(
            organizationId,
            principal.userId(),
            incidentId,
            request
        );
    }

    @PostMapping("/{incidentId}/close")
    public SiteOperationDtos.IncidentView close(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID incidentId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return incidentService.close(
            organizationId,
            principal.userId(),
            incidentId
        );
    }
}
