package com.buildflow.document.dto;

import com.buildflow.document.entity.DocumentCategory;
import com.buildflow.document.entity.ProjectPhotoCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record UploadDocumentRequest(

        @NotBlank
        @Size(max = 255)
        String fileName,

        @Size(max = 10000)
        String description,

        @NotNull
        DocumentCategory category,

        @NotBlank
        @Size(max = 150)
        String mimeType,

        @NotBlank
        String base64Data,

        boolean clientVisible
    ) {
    }

    public record DocumentView(
        UUID id,
        UUID projectId,
        String name,
        String description,
        DocumentCategory category,
        String mimeType,
        long sizeBytes,
        int version,
        boolean clientVisible,
        UUID uploadedBy,
        Instant createdAt
    ) {
    }

    public record UploadPhotoRequest(

        @NotBlank
        @Size(max = 255)
        String fileName,

        @NotBlank
        @Size(max = 150)
        String mimeType,

        @NotBlank
        String base64Data,

        @Size(max = 1000)
        String caption,

        LocalDate takenDate,

        @NotNull
        ProjectPhotoCategory category,

        boolean clientVisible
    ) {
    }

    public record PhotoView(
        UUID id,
        UUID projectId,
        UUID fileId,
        String caption,
        LocalDate takenDate,
        ProjectPhotoCategory category,
        boolean clientVisible,
        UUID uploadedBy,
        Instant createdAt
    ) {
    }

    public record FileContent(
        String fileName,
        String mimeType,
        long sizeBytes,
        byte[] content
    ) {
    }
}
