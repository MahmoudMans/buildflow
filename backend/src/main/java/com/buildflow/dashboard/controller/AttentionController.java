package com.buildflow.dashboard.controller;

import com.buildflow.dashboard.dto.AttentionDtos;
import com.buildflow.dashboard.service.AttentionService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class AttentionController {

    private final AttentionService attentionService;

    public AttentionController(AttentionService attentionService) {
        this.attentionService = attentionService;
    }

    @GetMapping("/attention")
    public AttentionDtos.AttentionSummary attention(
        @RequestHeader("X-Organization-Id")
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return attentionService.summary(
            organizationId,
            principal.userId()
        );
    }
}
