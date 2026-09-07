package com.buildflow.organization.controller;

import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
    "/api/v1/tenant"
)
public class TenantContextController {

    private final OrganizationAccessService
        accessService;

    public TenantContextController(
        OrganizationAccessService
            accessService
    ) {
        this.accessService =
            accessService;
    }

    @GetMapping("/context")
    public OrganizationDtos.TenantContextView
        context(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        OrganizationMembership membership =
            accessService
                .requireMembership(
                    organizationId,
                    principal.userId()
                );

        return new OrganizationDtos
            .TenantContextView(
                OrganizationDtos
                    .OrganizationView
                    .from(
                        membership
                            .getOrganization()
                    ),
                membership.getRole()
            );
    }
}
