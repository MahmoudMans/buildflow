package com.buildflow.client.controller;

import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.service.ClientService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService
        clientService;

    public ClientController(
        ClientService clientService
    ) {
        this.clientService =
            clientService;
    }

    @GetMapping
    public List<
        ClientDtos.ClientSummary
    > list(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return clientService.list(
            organizationId,
            principal.userId()
        );
    }

    @PostMapping
    public ResponseEntity<
        ClientDtos.ClientView
    > create(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        ClientDtos.CreateClientRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                clientService.create(
                    organizationId,
                    principal.userId(),
                    request
                )
            );
    }

    @GetMapping("/{clientId}")
    public ClientDtos.ClientView get(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID clientId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return clientService.get(
            organizationId,
            principal.userId(),
            clientId
        );
    }

    @PatchMapping("/{clientId}")
    public ClientDtos.ClientView update(
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
        ClientDtos.UpdateClientRequest
            request
    ) {
        return clientService.update(
            organizationId,
            principal.userId(),
            clientId,
            request
        );
    }

    @PostMapping("/{clientId}/archive")
    public ResponseEntity<Void> archive(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID clientId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        clientService.archive(
            organizationId,
            principal.userId(),
            clientId
        );

        return ResponseEntity
            .noContent()
            .build();
    }

    @PostMapping(
        "/{clientId}/contacts"
    )
    public ResponseEntity<
        ClientDtos.ContactView
    > addContact(
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
        ClientDtos.CreateContactRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                clientService.addContact(
                    organizationId,
                    principal.userId(),
                    clientId,
                    request
                )
            );
    }

    @PatchMapping(
        "/contacts/{contactId}"
    )
    public ClientDtos.ContactView
        updateContact(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID contactId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            ClientDtos.UpdateContactRequest
                request
        ) {

        return clientService
            .updateContact(
                organizationId,
                principal.userId(),
                contactId,
                request
            );
    }

    @DeleteMapping(
        "/contacts/{contactId}"
    )
    public ResponseEntity<Void>
        deleteContact(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID contactId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        clientService.deleteContact(
            organizationId,
            principal.userId(),
            contactId
        );

        return ResponseEntity
            .noContent()
            .build();
    }
}
