package com.buildflow.document.controller;

import com.buildflow.document.dto.DocumentDtos;
import com.buildflow.document.service.ProjectDocumentService;
import com.buildflow.document.service.ProjectPhotoService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class ProjectDocumentController {

    private final ProjectDocumentService
        documentService;

    private final ProjectPhotoService
        photoService;

    public ProjectDocumentController(
        ProjectDocumentService documentService,
        ProjectPhotoService photoService
    ) {
        this.documentService =
            documentService;

        this.photoService =
            photoService;
    }

    @GetMapping(
        "/api/v1/projects/{projectId}/documents"
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

    @PostMapping(
        "/api/v1/projects/{projectId}/documents"
    )
    public ResponseEntity<
        DocumentDtos.DocumentView
    > uploadDocument(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        DocumentDtos.UploadDocumentRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                documentService.upload(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }

    @GetMapping(
        "/api/v1/documents/{documentId}/content"
    )
    public ResponseEntity<byte[]> content(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID documentId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        DocumentDtos.FileContent content =
            documentService.content(
                organizationId,
                principal.userId(),
                documentId
            );

        HttpHeaders headers =
            new HttpHeaders();

        headers.setContentType(
            MediaType.parseMediaType(
                content.mimeType()
            )
        );

        headers.setContentDisposition(
            ContentDisposition
                .inline()
                .filename(
                    content.fileName()
                )
                .build()
        );

        headers.setContentLength(
            content.sizeBytes()
        );

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(
                content.content()
            );
    }

    @GetMapping(
        "/api/v1/projects/{projectId}/photos"
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
        "/api/v1/projects/{projectId}/photos"
    )
    public ResponseEntity<
        DocumentDtos.PhotoView
    > uploadPhoto(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        DocumentDtos.UploadPhotoRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                photoService.upload(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }
}
