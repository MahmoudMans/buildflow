package com.buildflow.document.service;

import com.buildflow.document.dto.DocumentDtos;
import com.buildflow.document.entity.DocumentCategory;
import com.buildflow.document.entity.ProjectDocument;
import com.buildflow.document.repository.ProjectDocumentRepository;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.shared.error.BusinessException;
import com.buildflow.storage.ObjectStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProjectDocumentService {

    private static final long MAX_FILE_SIZE =
        15L * 1024L * 1024L;

    private static final Map<
        String,
        Set<String>
    > MIME_EXTENSIONS =
        Map.of(
            "application/pdf",
            Set.of(".pdf"),

            "image/jpeg",
            Set.of(
                ".jpg",
                ".jpeg"
            ),

            "image/png",
            Set.of(".png"),

            "image/webp",
            Set.of(".webp"),

            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            Set.of(".docx"),

            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            Set.of(".xlsx")
        );

    private final ProjectDocumentRepository
        repository;

    private final DocumentAccessService
        accessService;

    private final ObjectStorageService
        storageService;

    public ProjectDocumentService(
        ProjectDocumentRepository repository,
        DocumentAccessService accessService,
        ObjectStorageService storageService
    ) {
        this.repository =
            repository;

        this.accessService =
            accessService;

        this.storageService =
            storageService;
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentView> list(
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

        List<ProjectDocument> documents =
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

        return documents
            .stream()
            .map(this::toView)
            .toList();
    }

    @Transactional
    public DocumentDtos.DocumentView upload(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        DocumentDtos.UploadDocumentRequest
            request
    ) {
        accessService
            .requireDocumentManage(
                organizationId,
                projectId,
                userId
            );

        return uploadValidated(
            organizationId,
            userId,
            projectId,
            request.fileName(),
            request.description(),
            request.category(),
            request.mimeType(),
            request.base64Data(),
            request.clientVisible()
        );
    }

    @Transactional
    public DocumentDtos.DocumentView
        uploadForPhoto(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            String fileName,
            String description,
            String mimeType,
            String base64Data,
            boolean clientVisible
        ) {

        accessService.requirePhotoManage(
            organizationId,
            projectId,
            userId
        );

        return uploadValidated(
            organizationId,
            userId,
            projectId,
            fileName,
            description,
            DocumentCategory.PHOTO,
            mimeType,
            base64Data,
            clientVisible
        );
    }

    @Transactional(readOnly = true)
    public DocumentDtos.FileContent content(
        UUID organizationId,
        UUID userId,
        UUID documentId
    ) {
        ProjectDocument document =
            repository
                .findByIdAndOrganizationId(
                    documentId,
                    organizationId
                )
                .orElseThrow(
                    () ->
                        new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "PROJECT_DOCUMENT_NOT_FOUND",
                            "The requested document does not exist."
                        )
                );

        OrganizationMembership membership =
            accessService.requireView(
                organizationId,
                document.getProjectId(),
                userId
            );

        if (
            membership.getRole()
                == OrganizationRole.CLIENT
            && !document.isClientVisible()
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "INTERNAL_DOCUMENT_FORBIDDEN",
                "This document is not authorized for client access."
            );
        }

        ObjectStorageService.StoredObject stored =
            storageService.get(
                document
                    .getStorageKey()
                    .toString()
            );

        return new DocumentDtos
            .FileContent(
                document.getName(),
                document.getMimeType(),
                document.getSizeBytes(),
                stored.content()
            );
    }

    private DocumentDtos.DocumentView
        uploadValidated(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            String fileName,
            String description,
            DocumentCategory category,
            String mimeType,
            String base64Data,
            boolean clientVisible
        ) {

        String cleanName =
            fileName.trim();

        String cleanMime =
            mimeType
                .trim()
                .toLowerCase(
                    Locale.ROOT
                );

        validateType(
            cleanName,
            cleanMime
        );

        byte[] content =
            decode(
                base64Data
            );

        if (
            content.length == 0
            || content.length
                > MAX_FILE_SIZE
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "PROJECT_DOCUMENT_SIZE_INVALID",
                "Project files must be between 1 byte and 15 MB."
            );
        }

        int version =
            repository
                .findTopByProjectIdAndOrganizationIdAndNameOrderByDocumentVersionDesc(
                    projectId,
                    organizationId,
                    cleanName
                )
                .map(
                    previous ->
                        previous
                            .getDocumentVersion()
                            + 1
                )
                .orElse(1);

        UUID storageKey =
            UUID.randomUUID();

        storageService.put(
            storageKey.toString(),
            cleanMime,
            content
        );

        try {
            ProjectDocument document =
                repository.save(
                    ProjectDocument.create(
                        organizationId,
                        projectId,
                        cleanName,
                        description,
                        category,
                        storageKey,
                        cleanMime,
                        content.length,
                        version,
                        clientVisible,
                        userId,
                        Instant.now()
                    )
                );

            return toView(document);
        } catch (RuntimeException exception) {
            try {
                storageService.delete(
                    storageKey.toString()
                );
            } catch (RuntimeException ignored) {
                // Original persistence exception
                // remains authoritative.
            }

            throw exception;
        }
    }

    private void validateType(
        String fileName,
        String mimeType
    ) {
        Set<String> extensions =
            MIME_EXTENSIONS.get(
                mimeType
            );

        if (extensions == null) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "PROJECT_DOCUMENT_MIME_INVALID",
                "The uploaded file MIME type is not supported."
            );
        }

        String lowercaseName =
            fileName.toLowerCase(
                Locale.ROOT
            );

        boolean valid =
            extensions.stream()
                .anyMatch(
                    lowercaseName::endsWith
                );

        if (!valid) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "PROJECT_DOCUMENT_EXTENSION_INVALID",
                "The file extension does not match the permitted MIME type."
            );
        }
    }

    private byte[] decode(
        String base64Data
    ) {
        String value =
            base64Data.trim();

        int comma =
            value.indexOf(',');

        if (
            value.startsWith("data:")
            && comma >= 0
        ) {
            value =
                value.substring(
                    comma + 1
                );
        }

        try {
            return Base64
                .getDecoder()
                .decode(value);
        } catch (
            IllegalArgumentException exception
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "PROJECT_DOCUMENT_DATA_INVALID",
                "The uploaded file data is not valid Base64."
            );
        }
    }

    private DocumentDtos.DocumentView toView(
        ProjectDocument document
    ) {
        return new DocumentDtos.DocumentView(
            document.getId(),
            document.getProjectId(),
            document.getName(),
            document.getDescription(),
            document.getCategory(),
            document.getMimeType(),
            document.getSizeBytes(),
            document.getDocumentVersion(),
            document.isClientVisible(),
            document.getUploadedBy(),
            document.getCreatedAt()
        );
    }
}
