package com.buildflow.portal.controller;

import com.buildflow.document.dto.DocumentDtos;
import com.buildflow.document.service.ProjectDocumentService;
import com.buildflow.document.service.ProjectPhotoService;
import com.buildflow.portal.dto.ClientPortalDtos;
import com.buildflow.portal.service.ClientChangeOrderDecisionService;
import com.buildflow.portal.service.ClientPortalProjectService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/client-portal")
public class ClientPortalController {

    private final ClientPortalProjectService
        projectService;

    private final ProjectDocumentService
        documentService;

    private final ProjectPhotoService
        photoService;

    private final ClientChangeOrderDecisionService
        decisionService;

    public ClientPortalController(
        ClientPortalProjectService projectService,
        ProjectDocumentService documentService,
        ProjectPhotoService photoService,
        ClientChangeOrderDecisionService
            decisionService
    ) {
        this.projectService =
            projectService;

        this.documentService =
            documentService;

        this.photoService =
            photoService;

        this.decisionService =
            decisionService;
    }

    @GetMapping("/projects")
    public List<ClientPortalDtos.ProjectSummary>
        projects(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return projectService.projects(
            organizationId,
            principal.userId()
        );
    }

    @GetMapping("/projects/{projectId}")
    public ClientPortalDtos.ProjectView project(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return projectService.project(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @GetMapping(
        "/projects/{projectId}/documents"
    )
    public List<DocumentDtos.DocumentView>
        documents(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return documentService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @GetMapping(
        "/projects/{projectId}/photos"
    )
    public List<DocumentDtos.PhotoView>
        photos(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return photoService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping(
        "/projects/{projectId}/change-orders/{changeOrderId}/approve"
    )
    public ClientPortalDtos.DecisionView approve(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @PathVariable
        UUID changeOrderId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        ClientPortalDtos.DecisionRequest
            request
    ) {
        return decisionService.approve(
            organizationId,
            principal.userId(),
            projectId,
            changeOrderId,
            request.comment()
        );
    }

    @PostMapping(
        "/projects/{projectId}/change-orders/{changeOrderId}/reject"
    )
    public ClientPortalDtos.DecisionView reject(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @PathVariable
        UUID changeOrderId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        ClientPortalDtos.DecisionRequest
            request
    ) {
        return decisionService.reject(
            organizationId,
            principal.userId(),
            projectId,
            changeOrderId,
            request.comment()
        );
    }
}
