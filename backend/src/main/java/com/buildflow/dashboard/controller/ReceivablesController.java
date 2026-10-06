package com.buildflow.dashboard.controller;

import com.buildflow.dashboard.dto.ReceivablesDtos;
import com.buildflow.dashboard.service.ReceivablesService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class ReceivablesController {

    private final ReceivablesService receivablesService;

    public ReceivablesController(ReceivablesService receivablesService) {
        this.receivablesService = receivablesService;
    }

    @GetMapping("/receivables")
    public ReceivablesDtos.ReceivablesSummary receivables(
        @RequestHeader("X-Organization-Id")
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return receivablesService.summary(
            organizationId,
            principal.userId()
        );
    }
}
