package com.buildflow.siteops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_site_reports")
public class DailySiteReport {

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
        name = "report_date",
        nullable = false
    )
    private LocalDate reportDate;

    @Column(
        name = "weather_text",
        length = 500
    )
    private String weatherText;

    @Column(
        name = "workers_on_site",
        nullable = false
    )
    private int workersOnSite;

    @Column(nullable = false)
    private String summary;

    @Column(name = "work_completed")
    private String workCompleted;

    private String delays;

    private String issues;

    @Column(name = "next_steps")
    private String nextSteps;

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

    @Column(
        name = "updated_at",
        nullable = false
    )
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected DailySiteReport() {
    }

    public static DailySiteReport create(
        UUID organizationId,
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
        Instant now
    ) {
        DailySiteReport report =
            new DailySiteReport();

        report.id =
            UUID.randomUUID();

        report.organizationId =
            organizationId;

        report.projectId =
            projectId;

        report.reportDate =
            reportDate;

        report.weatherText =
            clean(weatherText);

        report.workersOnSite =
            workersOnSite;

        report.summary =
            summary.trim();

        report.workCompleted =
            clean(workCompleted);

        report.delays =
            clean(delays);

        report.issues =
            clean(issues);

        report.nextSteps =
            clean(nextSteps);

        report.createdBy =
            createdBy;

        report.createdAt = now;
        report.updatedAt = now;

        return report;
    }

    public void update(
        String weatherText,
        Integer workersOnSite,
        String summary,
        String workCompleted,
        String delays,
        String issues,
        String nextSteps,
        Instant now
    ) {
        if (weatherText != null) {
            this.weatherText =
                clean(weatherText);
        }

        if (workersOnSite != null) {
            this.workersOnSite =
                workersOnSite;
        }

        if (summary != null) {
            this.summary =
                summary.trim();
        }

        if (workCompleted != null) {
            this.workCompleted =
                clean(workCompleted);
        }

        if (delays != null) {
            this.delays =
                clean(delays);
        }

        if (issues != null) {
            this.issues =
                clean(issues);
        }

        if (nextSteps != null) {
            this.nextSteps =
                clean(nextSteps);
        }

        updatedAt = now;
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

    public LocalDate getReportDate() {
        return reportDate;
    }

    public String getWeatherText() {
        return weatherText;
    }

    public int getWorkersOnSite() {
        return workersOnSite;
    }

    public String getSummary() {
        return summary;
    }

    public String getWorkCompleted() {
        return workCompleted;
    }

    public String getDelays() {
        return delays;
    }

    public String getIssues() {
        return issues;
    }

    public String getNextSteps() {
        return nextSteps;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
