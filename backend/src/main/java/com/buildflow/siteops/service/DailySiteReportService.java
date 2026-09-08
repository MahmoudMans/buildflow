package com.buildflow.siteops.service;

import com.buildflow.shared.error.BusinessException;
import com.buildflow.siteops.dto.SiteOperationDtos;
import com.buildflow.siteops.entity.DailySiteReport;
import com.buildflow.siteops.repository.DailySiteReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DailySiteReportService {

    private final DailySiteReportRepository
        reportRepository;

    private final SiteOperationsAccessService
        accessService;

    public DailySiteReportService(
        DailySiteReportRepository
            reportRepository,
        SiteOperationsAccessService
            accessService
    ) {
        this.reportRepository =
            reportRepository;

        this.accessService =
            accessService;
    }

    @Transactional(readOnly = true)
    public List<SiteOperationDtos.DailyReportView>
        list(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        accessService.requireView(
            organizationId,
            projectId,
            userId
        );

        return reportRepository
            .findByProjectIdAndOrganizationIdOrderByReportDateDesc(
                projectId,
                organizationId
            )
            .stream()
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public SiteOperationDtos.DailyReportView get(
        UUID organizationId,
        UUID userId,
        UUID reportId
    ) {
        DailySiteReport report =
            requireReport(
                organizationId,
                reportId
            );

        accessService.requireView(
            organizationId,
            report.getProjectId(),
            userId
        );

        return toView(report);
    }

    @Transactional
    public SiteOperationDtos.DailyReportView create(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        SiteOperationDtos
            .CreateDailyReportRequest request
    ) {
        accessService.requireManage(
            organizationId,
            projectId,
            userId
        );

        if (
            reportRepository
                .existsByProjectIdAndOrganizationIdAndReportDate(
                    projectId,
                    organizationId,
                    request.reportDate()
                )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "DAILY_SITE_REPORT_ALREADY_EXISTS",
                "A site report already exists for this project and date."
            );
        }

        DailySiteReport report =
            DailySiteReport.create(
                organizationId,
                projectId,
                request.reportDate(),
                request.weatherText(),
                request.workersOnSite(),
                request.summary(),
                request.workCompleted(),
                request.delays(),
                request.issues(),
                request.nextSteps(),
                userId,
                Instant.now()
            );

        report =
            reportRepository.save(
                report
            );

        return toView(report);
    }

    @Transactional
    public SiteOperationDtos.DailyReportView update(
        UUID organizationId,
        UUID userId,
        UUID reportId,
        SiteOperationDtos
            .UpdateDailyReportRequest request
    ) {
        DailySiteReport report =
            requireReport(
                organizationId,
                reportId
            );

        accessService.requireManage(
            organizationId,
            report.getProjectId(),
            userId
        );

        report.update(
            request.weatherText(),
            request.workersOnSite(),
            request.summary(),
            request.workCompleted(),
            request.delays(),
            request.issues(),
            request.nextSteps(),
            Instant.now()
        );

        return toView(report);
    }

    @Transactional(readOnly = true)
    public DailySiteReport requireReport(
        UUID organizationId,
        UUID reportId
    ) {
        return reportRepository
            .findByIdAndOrganizationId(
                reportId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "DAILY_SITE_REPORT_NOT_FOUND",
                        "The requested daily site report does not exist."
                    )
            );
    }

    private SiteOperationDtos.DailyReportView
        toView(
            DailySiteReport report
        ) {

        return new SiteOperationDtos
            .DailyReportView(
                report.getId(),
                report.getProjectId(),
                report.getReportDate(),
                report.getWeatherText(),
                report.getWorkersOnSite(),
                report.getSummary(),
                report.getWorkCompleted(),
                report.getDelays(),
                report.getIssues(),
                report.getNextSteps(),
                report.getCreatedBy(),
                report.getCreatedAt(),
                report.getUpdatedAt()
            );
    }
}
