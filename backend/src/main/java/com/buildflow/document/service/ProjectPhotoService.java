package com.buildflow.document.service;

import com.buildflow.document.dto.DocumentDtos;
import com.buildflow.document.entity.ProjectPhoto;
import com.buildflow.document.repository.ProjectPhotoRepository;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectPhotoService {

    private final ProjectPhotoRepository
        repository;

    private final ProjectDocumentService
        documentService;

    private final DocumentAccessService
        accessService;

    public ProjectPhotoService(
        ProjectPhotoRepository repository,
        ProjectDocumentService documentService,
        DocumentAccessService accessService
    ) {
        this.repository =
            repository;

        this.documentService =
            documentService;

        this.accessService =
            accessService;
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.PhotoView> list(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        OrganizationMembership membership =
            accessService.requireView(
                organizationId,
                projectId,
                userId
            );

        List<ProjectPhoto> photos =
            membership.getRole()
                == OrganizationRole.CLIENT
                ? repository
                    .findByProjectIdAndOrganizationIdAndClientVisibleTrueOrderByCreatedAtDesc(
                        projectId,
                        organizationId
                    )
                : repository
                    .findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
                        projectId,
                        organizationId
                    );

        return photos
            .stream()
            .map(this::toView)
            .toList();
    }

    @Transactional
    public DocumentDtos.PhotoView upload(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        DocumentDtos.UploadPhotoRequest
            request
    ) {
        DocumentDtos.DocumentView file =
            documentService
                .uploadForPhoto(
                    organizationId,
                    userId,
                    projectId,
                    request.fileName(),
                    request.caption(),
                    request.mimeType(),
                    request.base64Data(),
                    request.clientVisible()
                );

        ProjectPhoto photo =
            repository.save(
                ProjectPhoto.create(
                    organizationId,
                    projectId,
                    file.id(),
                    request.caption(),
                    request.takenDate(),
                    request.category(),
                    request.clientVisible(),
                    userId,
                    Instant.now()
                )
            );

        return toView(photo);
    }

    private DocumentDtos.PhotoView toView(
        ProjectPhoto photo
    ) {
        return new DocumentDtos.PhotoView(
            photo.getId(),
            photo.getProjectId(),
            photo.getFileId(),
            photo.getCaption(),
            photo.getTakenDate(),
            photo.getCategory(),
            photo.isClientVisible(),
            photo.getUploadedBy(),
            photo.getCreatedAt()
        );
    }
}
