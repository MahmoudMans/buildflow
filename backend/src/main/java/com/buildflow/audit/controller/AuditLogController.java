package com.buildflow.audit.controller;

import com.buildflow.audit.dto.AuditDtos;
import com.buildflow.audit.service.AuditLogService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditLogController {

    private final AuditLogService
        auditLogService;

    public AuditLogController(
        AuditLogService
            auditLogService
    ) {
        this.auditLogService =
            auditLogService;
    }

    @GetMapping
    public List<AuditDtos.View> list(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @RequestParam(
            required = false
        )
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return auditLogService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }
}
