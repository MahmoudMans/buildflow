package com.buildflow.portal.controller;

import com.buildflow.portal.dto.ClientPortalDtos;
import com.buildflow.portal.service.ClientPortalAccessService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientPortalAdminController {

    private final ClientPortalAccessService
        accessService;

    public ClientPortalAdminController(
        ClientPortalAccessService accessService
    ) {
        this.accessService =
            accessService;
    }

    @PostMapping(
        "/{clientId}/portal-access"
    )
    public ClientPortalDtos.PortalAccessView
        link(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID clientId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            ClientPortalDtos
                .LinkClientUserRequest request
        ) {

        return accessService.link(
            organizationId,
            principal.userId(),
            clientId,
            request.userId()
        );
    }
}
