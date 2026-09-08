package com.buildflow.siteops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "daily_report_photos")
public class DailyReportPhoto {

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
        name = "report_id",
        nullable = false
    )
    private UUID reportId;

    @Column(
        name = "storage_key",
        nullable = false
    )
    private UUID storageKey;

    @Column(
        name = "file_name",
        nullable = false,
        length = 255
    )
    private String fileName;

    @Column(
        name = "mime_type",
        nullable = false,
        length = 100
    )
    private String mimeType;

    @Column(
        name = "size_bytes",
        nullable = false
    )
    private long sizeBytes;

    @Column(length = 1000)
    private String caption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SitePhotoCategory category;

    @Column(
        name = "client_visible",
        nullable = false
    )
    private boolean clientVisible;

    @Column(
        nullable = false,
        columnDefinition = "bytea"
    )
    private byte[] content;

    @Column(
        name = "created_by",
        nullable = false
    )
    private UUID createdBy;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    protected DailyReportPhoto() {
    }

    public static DailyReportPhoto create(
        UUID organizationId,
        UUID projectId,
        UUID reportId,
        String fileName,
        String mimeType,
        byte[] content,
        String caption,
        SitePhotoCategory category,
        boolean clientVisible,
        UUID createdBy,
        Instant now
    ) {
        DailyReportPhoto photo =
            new DailyReportPhoto();

        photo.id =
            UUID.randomUUID();

        photo.organizationId =
            organizationId;

        photo.projectId =
            projectId;

        photo.reportId =
            reportId;

        photo.storageKey =
            UUID.randomUUID();

        photo.fileName =
            fileName;

        photo.mimeType =
            mimeType;

        photo.sizeBytes =
            content.length;

        photo.caption =
            clean(caption);

        photo.category =
            category;

        photo.clientVisible =
            clientVisible;

        photo.content =
            content.clone();

        photo.createdBy =
            createdBy;

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

    public UUID getReportId() {
        return reportId;
    }

    public UUID getStorageKey() {
        return storageKey;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getCaption() {
        return caption;
    }

    public SitePhotoCategory getCategory() {
        return category;
    }

    public boolean isClientVisible() {
        return clientVisible;
    }

    public byte[] getContent() {
        return content.clone();
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
