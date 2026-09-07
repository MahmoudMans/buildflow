package com.buildflow.organization.controller;

import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.InvitationService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    "/api/v1/invitations"
)
public class InvitationController {

    private final InvitationService
        invitationService;

    public InvitationController(
        InvitationService
            invitationService
    ) {
        this.invitationService =
            invitationService;
    }

    @PostMapping("/accept")
    public OrganizationDtos.OrganizationSummary
        accept(
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            OrganizationDtos
                .AcceptInvitationRequest
                request
        ) {

        return invitationService.accept(
            request.token(),
            principal.userId()
        );
    }
}
