package com.buildflow.report.controller;

import com.buildflow.report.dto.ReportDtos;
import com.buildflow.report.service.CommercialReportService;
import com.buildflow.report.service.CsvExportService;
import com.buildflow.report.service.ProjectReportService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ProjectReportService
        projectReportService;

    private final CommercialReportService
        commercialReportService;

    private final CsvExportService
        csvExportService;

    public ReportController(
        ProjectReportService
            projectReportService,
        CommercialReportService
            commercialReportService,
        CsvExportService
            csvExportService
    ) {
        this.projectReportService =
            projectReportService;

        this.commercialReportService =
            commercialReportService;

        this.csvExportService =
            csvExportService;
    }

    @GetMapping(
        value =
            "/reports/projects/{projectId}/summary.pdf",
        produces =
            MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]>
        projectSummary(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return response(
            projectReportService
                .projectSummary(
                    organizationId,
                    principal.userId(),
                    projectId
                )
        );
    }

    @GetMapping(
        value =
            "/reports/projects/{projectId}/financial.pdf",
        produces =
            MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]>
        financialReport(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return response(
            projectReportService
                .financialReport(
                    organizationId,
                    principal.userId(),
                    projectId
                )
        );
    }

    @GetMapping(
        value =
            "/reports/projects/{projectId}/budget.pdf",
        produces =
            MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]>
        budgetReport(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return response(
            projectReportService
                .budgetReport(
                    organizationId,
                    principal.userId(),
                    projectId
                )
        );
    }

    @GetMapping(
        value =
            "/reports/projects/{projectId}/expenses.pdf",
        produces =
            MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]>
        expenseReport(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return response(
            projectReportService
                .expenseReport(
                    organizationId,
                    principal.userId(),
                    projectId
                )
        );
    }

    @GetMapping(
        value =
            "/reports/quotes/{id}.pdf",
        produces =
            MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> quote(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID id,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return response(
            commercialReportService
                .quote(
                    organizationId,
                    principal.userId(),
                    id
                )
        );
    }

    @GetMapping(
        value =
            "/reports/invoices/{id}.pdf",
        produces =
            MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> invoice(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID id,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return response(
            commercialReportService
                .invoice(
                    organizationId,
                    principal.userId(),
                    id
                )
        );
    }

    @GetMapping(
        value =
            "/reports/purchase-orders/{id}.pdf",
        produces =
            MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]>
        purchaseOrder(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID id,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return response(
            commercialReportService
                .purchaseOrder(
                    organizationId,
                    principal.userId(),
                    id
                )
        );
    }

    @GetMapping(
        value =
            "/reports/change-orders/{id}.pdf",
        produces =
            MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]>
        changeOrder(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID id,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return response(
            commercialReportService
                .changeOrder(
                    organizationId,
                    principal.userId(),
                    id
                )
        );
    }

    @GetMapping(
        value =
            "/reports/site-reports/{id}.pdf",
        produces =
            MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]>
        dailySiteReport(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID id,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return response(
            commercialReportService
                .dailySiteReport(
                    organizationId,
                    principal.userId(),
                    id
                )
        );
    }

    @GetMapping(
        value =
            "/exports/{resource}.csv",
        produces = "text/csv"
    )
    public ResponseEntity<byte[]> csv(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        String resource,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return response(
            csvExportService.export(
                organizationId,
                principal.userId(),
                resource
            )
        );
    }

    private ResponseEntity<byte[]> response(
        ReportDtos.ReportFile file
    ) {
        MediaType mediaType =
            MediaType.parseMediaType(
                file.contentType()
            );

        ContentDisposition disposition =
            ContentDisposition
                .attachment()
                .filename(
                    file.filename(),
                    StandardCharsets.UTF_8
                )
                .build();

        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                disposition.toString()
            )
            .contentLength(
                file.content().length
            )
            .body(
                file.content()
            );
    }
}
