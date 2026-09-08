package com.buildflow.siteops.dto;

import com.buildflow.siteops.entity.IncidentSeverity;
import com.buildflow.siteops.entity.IncidentStatus;
import com.buildflow.siteops.entity.IncidentType;
import com.buildflow.siteops.entity.SitePhotoCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class SiteOperationDtos {

    private SiteOperationDtos() {
    }

    public record CreateDailyReportRequest(

        @NotNull
        LocalDate reportDate,

        @Size(max = 500)
        String weatherText,

        @Min(0)
        @Max(100000)
        int workersOnSite,

        @NotBlank
        @Size(max = 20000)
        String summary,

        @Size(max = 20000)
        String workCompleted,

        @Size(max = 20000)
        String delays,

        @Size(max = 20000)
        String issues,

        @Size(max = 20000)
        String nextSteps
    ) {
    }

    public record UpdateDailyReportRequest(

        @Size(max = 500)
        String weatherText,

        @Min(0)
        @Max(100000)
        Integer workersOnSite,

        @Size(min = 1, max = 20000)
        String summary,

        @Size(max = 20000)
        String workCompleted,

        @Size(max = 20000)
        String delays,

        @Size(max = 20000)
        String issues,

        @Size(max = 20000)
        String nextSteps
    ) {
    }

    public record DailyReportView(
        UUID id,
        UUID projectId,
        LocalDate reportDate,
        String weatherText,
        int workersOnSite,
        String summary,
        String workCompleted,
        String delays,
        String issues,
        String nextSteps,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record UploadPhotoRequest(

        @NotBlank
        @Size(max = 255)
        String fileName,

        @NotBlank
        @Size(max = 100)
        String mimeType,

        @NotBlank
        String base64Data,

        @Size(max = 1000)
        String caption,

        @NotNull
        SitePhotoCategory category,

        boolean clientVisible
    ) {
    }

    public record PhotoView(
        UUID id,
        UUID reportId,
        UUID storageKey,
        String fileName,
        String mimeType,
        long sizeBytes,
        String caption,
        SitePhotoCategory category,
        boolean clientVisible,
        UUID createdBy,
        Instant createdAt
    ) {
    }

    public record CreateIncidentRequest(

        @NotNull
        IncidentType type,

        @NotNull
        IncidentSeverity severity,

        @NotBlank
        @Size(max = 250)
        String title,

        @NotBlank
        @Size(max = 20000)
        String description,

        Instant occurredAt,

        UUID assignedTo,

        boolean clientVisible
    ) {
    }

    public record UpdateIncidentRequest(
        IncidentType type,
        IncidentSeverity severity,

        @Size(min = 1, max = 250)
        String title,

        @Size(min = 1, max = 20000)
        String description,

        UUID assignedTo,
        Boolean clientVisible
    ) {
    }

    public record ResolveIncidentRequest(

        @NotBlank
        @Size(max = 20000)
        String resolution
    ) {
    }

    public record IncidentView(
        UUID id,
        UUID projectId,
        String incidentNumber,
        IncidentType type,
        IncidentSeverity severity,
        String title,
        String description,
        Instant occurredAt,
        Instant reportedAt,
        IncidentStatus status,
        UUID reportedBy,
        UUID assignedTo,
        String resolution,
        Instant resolvedAt,
        boolean clientVisible,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record CreateProgressUpdateRequest(

        @NotBlank
        @Size(max = 20000)
        String summary,

        @Size(max = 20000)
        String blockers,

        @Size(max = 20000)
        String nextSteps
    ) {
    }

    public record ProgressUpdateView(
        UUID id,
        UUID projectId,
        BigDecimal progressPercentageSnapshot,
        String summary,
        String blockers,
        String nextSteps,
        UUID createdBy,
        Instant createdAt
    ) {
    }
}
