package com.buildflow.organization.controller;

import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.InvitationService;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
    "/api/v1/organizations"
)
public class OrganizationController {

    private final OrganizationService
        organizationService;

    private final InvitationService
        invitationService;

    public OrganizationController(
        OrganizationService
            organizationService,
        InvitationService
            invitationService
    ) {
        this.organizationService =
            organizationService;
        this.invitationService =
            invitationService;
    }

    @GetMapping
    public List<
        OrganizationDtos.OrganizationSummary
    > list(
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return organizationService
            .listForUser(
                principal.userId()
            );
    }

    @PostMapping
    public ResponseEntity<
        OrganizationDtos.OrganizationView
    > create(
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        OrganizationDtos
            .CreateOrganizationRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                organizationService
                    .create(
                        principal
                            .userId(),
                        request
                    )
            );
    }

    @GetMapping("/{organizationId}")
    public OrganizationDtos.OrganizationView
        get(
            @PathVariable
            UUID organizationId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return organizationService.get(
            organizationId,
            principal.userId()
        );
    }

    @PatchMapping("/{organizationId}")
    public OrganizationDtos.OrganizationView
        update(
            @PathVariable
            UUID organizationId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            OrganizationDtos
                .UpdateOrganizationRequest
                request
        ) {

        return organizationService.update(
            organizationId,
            principal.userId(),
            request
        );
    }

    @GetMapping(
        "/{organizationId}/members"
    )
    public List<
        OrganizationDtos.MemberView
    > members(
        @PathVariable
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return organizationService
            .listMembers(
                organizationId,
                principal.userId()
            );
    }

    @PatchMapping(
        "/{organizationId}/members/{membershipId}/role"
    )
    public OrganizationDtos.MemberView
        changeRole(
            @PathVariable
            UUID organizationId,
            @PathVariable
            UUID membershipId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            OrganizationDtos
                .ChangeRoleRequest
                request
        ) {

        return organizationService
            .changeRole(
                organizationId,
                membershipId,
                principal.userId(),
                request.role()
            );
    }

    @PostMapping(
        "/{organizationId}/members/{membershipId}/deactivate"
    )
    public OrganizationDtos.MemberView
        deactivateMember(
            @PathVariable
            UUID organizationId,
            @PathVariable
            UUID membershipId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return organizationService
            .deactivateMember(
                organizationId,
                membershipId,
                principal.userId()
            );
    }

    @PostMapping(
        "/{organizationId}/members/{membershipId}/activate"
    )
    public OrganizationDtos.MemberView
        activateMember(
            @PathVariable
            UUID organizationId,
            @PathVariable
            UUID membershipId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return organizationService
            .activateMember(
                organizationId,
                membershipId,
                principal.userId()
            );
    }

    @GetMapping(
        "/{organizationId}/invitations"
    )
    public List<
        OrganizationDtos.InvitationView
    > invitations(
        @PathVariable
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return invitationService.list(
            organizationId,
            principal.userId()
        );
    }

    @PostMapping(
        "/{organizationId}/invitations"
    )
    public ResponseEntity<
        OrganizationDtos.InvitationView
    > invite(
        @PathVariable
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        OrganizationDtos
            .InviteMemberRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                invitationService.invite(
                    organizationId,
                    principal.userId(),
                    request
                )
            );
    }

    @PostMapping(
        "/{organizationId}/invitations/{invitationId}/resend"
    )
    public OrganizationDtos.InvitationView
        resendInvitation(
            @PathVariable
            UUID organizationId,
            @PathVariable
            UUID invitationId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return invitationService.resend(
            organizationId,
            invitationId,
            principal.userId()
        );
    }

    @PostMapping(
        "/{organizationId}/invitations/{invitationId}/revoke"
    )
    public ResponseEntity<Void>
        revokeInvitation(
            @PathVariable
            UUID organizationId,
            @PathVariable
            UUID invitationId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        invitationService.revoke(
            organizationId,
            invitationId,
            principal.userId()
        );

        return ResponseEntity
            .noContent()
            .build();
    }
}
