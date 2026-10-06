package com.buildflow.audit.controller;

import com.buildflow.audit.dto.AuditSearchDtos;
import com.buildflow.audit.service.AuditLogSearchService;
import com.buildflow.audit.service.AuditSearchCriteria;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogSearchController {

    private final AuditLogSearchService auditLogSearchService;

    public AuditLogSearchController(
        AuditLogSearchService auditLogSearchService
    ) {
        this.auditLogSearchService = auditLogSearchService;
    }

    /**
     * from and to are days written like 2026-03-31, read in the
     * organization's time zone. q is free text.
     */
    @GetMapping("/search")
    public AuditSearchDtos.SearchPage search(
        @RequestHeader("X-Organization-Id")
        UUID organizationId,
        @RequestParam(required = false)
        UUID projectId,
        @RequestParam(required = false)
        UUID actorUserId,
        @RequestParam(required = false)
        String entityType,
        @RequestParam(required = false)
        String action,
        @RequestParam(required = false)
        String from,
        @RequestParam(required = false)
        String to,
        @RequestParam(required = false)
        String q,
        @RequestParam(required = false)
        Integer page,
        @RequestParam(required = false)
        Integer size,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return auditLogSearchService.search(
            organizationId,
            principal.userId(),
            AuditSearchCriteria.of(
                projectId,
                actorUserId,
                entityType,
                action,
                AuditSearchCriteria.parseDate(from),
                AuditSearchCriteria.parseDate(to),
                q,
                page,
                size
            )
        );
    }

    @GetMapping("/facets")
    public AuditSearchDtos.Facets facets(
        @RequestHeader("X-Organization-Id")
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return auditLogSearchService.facets(
            organizationId,
            principal.userId()
        );
    }
}
