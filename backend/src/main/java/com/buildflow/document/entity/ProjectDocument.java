package com.buildflow.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_documents")
public class ProjectDocument {

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
        nullable = false,
        length = 255
    )
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentCategory category;

    @Column(
        name = "storage_key",
        nullable = false
    )
    private UUID storageKey;

    @Column(
        name = "mime_type",
        nullable = false,
        length = 150
    )
    private String mimeType;

    @Column(
        name = "size_bytes",
        nullable = false
    )
    private long sizeBytes;

    @Column(
        name = "version",
        nullable = false
    )
    private int documentVersion;

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

    protected ProjectDocument() {
    }

    public static ProjectDocument create(
        UUID organizationId,
        UUID projectId,
        String name,
        String description,
        DocumentCategory category,
        UUID storageKey,
        String mimeType,
        long sizeBytes,
        int documentVersion,
        boolean clientVisible,
        UUID uploadedBy,
        Instant now
    ) {
        ProjectDocument document =
            new ProjectDocument();

        document.id =
            UUID.randomUUID();

        document.organizationId =
            organizationId;

        document.projectId =
            projectId;

        document.name =
            name.trim();

        document.description =
            clean(description);

        document.category =
            category;

        document.storageKey =
            storageKey;

        document.mimeType =
            mimeType;

        document.sizeBytes =
            sizeBytes;

        document.documentVersion =
            documentVersion;

        document.clientVisible =
            clientVisible;

        document.uploadedBy =
            uploadedBy;

        document.createdAt =
            now;

        return document;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public UUID getStorageKey() {
        return storageKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public int getDocumentVersion() {
        return documentVersion;
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
