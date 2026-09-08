package com.buildflow.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "project_photos")
public class ProjectPhoto {

    @Id
    private UUID id;

    @Column(
        name = "organization_id",
        nullable = false
    )
    private UUID organizationId;

    @Column(
        name = "project_id",
        nullable = false
    )
    private UUID projectId;

    @Column(
        name = "file_id",
        nullable = false
    )
    private UUID fileId;

    @Column(length = 1000)
    private String caption;

    @Column(name = "taken_date")
    private LocalDate takenDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectPhotoCategory category;

    @Column(
        name = "client_visible",
        nullable = false
    )
    private boolean clientVisible;

    @Column(
        name = "uploaded_by",
        nullable = false
    )
    private UUID uploadedBy;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    protected ProjectPhoto() {
    }

    public static ProjectPhoto create(
        UUID organizationId,
        UUID projectId,
        UUID fileId,
        String caption,
        LocalDate takenDate,
        ProjectPhotoCategory category,
        boolean clientVisible,
        UUID uploadedBy,
        Instant now
    ) {
        ProjectPhoto photo =
            new ProjectPhoto();

        photo.id =
            UUID.randomUUID();

        photo.organizationId =
            organizationId;

        photo.projectId =
            projectId;

        photo.fileId =
            fileId;

        photo.caption =
            clean(caption);

        photo.takenDate =
            takenDate;

        photo.category =
            category;

        photo.clientVisible =
            clientVisible;

        photo.uploadedBy =
            uploadedBy;

        photo.createdAt =
            now;

        return photo;
    }

    private static String clean(
        String value
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public String getCaption() {
        return caption;
    }

    public LocalDate getTakenDate() {
        return takenDate;
    }

    public ProjectPhotoCategory getCategory() {
        return category;
    }

    public boolean isClientVisible() {
        return clientVisible;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
