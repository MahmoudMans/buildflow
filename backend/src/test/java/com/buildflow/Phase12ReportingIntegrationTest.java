package com.buildflow;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;
import com.buildflow.report.dto.ReportDtos;
import com.buildflow.report.service.CsvExportService;
import com.buildflow.report.service.PdfReportService;
import com.buildflow.report.service.ProjectReportService;
import com.buildflow.shared.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase12ReportingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer(
            "postgres:17-alpine"
        );

    @Autowired
    AuthService authService;

    @Autowired
    OrganizationService organizationService;

    @Autowired
    ClientService clientService;

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectReportService projectReportService;

    @Autowired
    CsvExportService csvExportService;

    @Autowired
    PdfReportService pdfReportService;

    @Test
    void reportingIsTenantScopedAndProducesPdfAndCsv() {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Report",
                    "Owner",
                    "phase12@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.1",
                "phase12-test"
            );

        OrganizationDtos.OrganizationView
            organization =
                organizationService.create(
                    owner.user().getId(),
                    new OrganizationDtos
                        .CreateOrganizationRequest(
                            "Phase 12 Construction",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "Paris",
                            null,
                            "France",
                            "EUR",
                            "Europe/Paris",
                            null
                        )
                );

        ClientDtos.ClientView client =
            clientService.create(
                organization.id(),
                owner.user().getId(),
                new ClientDtos
                    .CreateClientRequest(
                        ClientType.COMPANY,
                        "Phase 12 Client",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    )
            );

        ProjectDtos.ProjectView project =
            projectService.create(
                organization.id(),
                owner.user().getId(),
                new ProjectDtos
                    .CreateProjectRequest(
                        client.id(),
                        "Phase 12 Project",
                        null,
                        ProjectType.RENOVATION,
                        ProjectPriority.HIGH,
                        null,
                        "Paris",
                        null,
                        "France",
                        null,
                        null,
                        "EUR",
                        null
                    )
            );

        assertPdf(
            projectReportService
                .projectSummary(
                    organization.id(),
                    owner.user().getId(),
                    project.id()
                )
        );

        assertPdf(
            projectReportService
                .financialReport(
                    organization.id(),
                    owner.user().getId(),
                    project.id()
                )
        );

        assertPdf(
            projectReportService
                .budgetReport(
                    organization.id(),
                    owner.user().getId(),
                    project.id()
                )
        );

        assertPdf(
            projectReportService
                .expenseReport(
                    organization.id(),
                    owner.user().getId(),
                    project.id()
                )
        );

        byte[] enginePdf =
            pdfReportService.render(
                "Quotation QTE-TEST",
                List.of(
                    PdfReportService.Section.of(
                        "Quotation",
                        List.of(
                            new PdfReportService
                                .Entry(
                                    "Status",
                                    "DRAFT"
                                ),
                            new PdfReportService
                                .Entry(
                                    "Total",
                                    "1000.00"
                                )
                        )
                    )
                )
            );

        assertThat(
            enginePdf
        ).startsWith(
            "%PDF".getBytes(
                StandardCharsets
                    .ISO_8859_1
            )
        );

        ReportDtos.ReportFile
            projectsCsv =
                csvExportService.export(
                    organization.id(),
                    owner.user().getId(),
                    "projects"
                );

        String projectsText =
            new String(
                projectsCsv.content(),
                StandardCharsets.UTF_8
            );

        assertThat(
            projectsText
        )
            .contains(
                "Phase 12 Project"
            );

        ReportDtos.ReportFile
            clientsCsv =
                csvExportService.export(
                    organization.id(),
                    owner.user().getId(),
                    "clients"
                );

        assertThat(
            new String(
                clientsCsv.content(),
                StandardCharsets.UTF_8
            )
        )
            .contains(
                "Phase 12 Client"
            );

        AuthService.AuthResult intruder =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Other",
                    "Owner",
                    "phase12-other@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.1",
                "phase12-test"
            );

        OrganizationDtos.OrganizationView
            otherOrganization =
                organizationService.create(
                    intruder.user().getId(),
                    new OrganizationDtos
                        .CreateOrganizationRequest(
                            "Other Tenant",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "Lyon",
                            null,
                            "France",
                            "EUR",
                            "Europe/Paris",
                            null
                        )
                );

        ClientDtos.ClientView
            otherClient =
                clientService.create(
                    otherOrganization.id(),
                    intruder.user().getId(),
                    new ClientDtos
                        .CreateClientRequest(
                            ClientType.COMPANY,
                            "Other Tenant Client",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                        )
                );

        projectService.create(
            otherOrganization.id(),
            intruder.user().getId(),
            new ProjectDtos
                .CreateProjectRequest(
                    otherClient.id(),
                    "Other Tenant Project",
                    null,
                    ProjectType.RENOVATION,
                    ProjectPriority.MEDIUM,
                    null,
                    "Lyon",
                    null,
                    "France",
                    null,
                    null,
                    "EUR",
                    null
                )
        );

        ReportDtos.ReportFile
            firstTenantProjects =
                csvExportService.export(
                    organization.id(),
                    owner.user().getId(),
                    "projects"
                );

        assertThat(
            new String(
                firstTenantProjects.content(),
                StandardCharsets.UTF_8
            )
        )
            .contains(
                "Phase 12 Project"
            )
            .doesNotContain(
                "Other Tenant Project"
            );

        assertThatThrownBy(
            () ->
                projectReportService
                    .projectSummary(
                        organization.id(),
                        intruder.user().getId(),
                        project.id()
                    )
        )
            .isInstanceOf(
                BusinessException.class
            );
    }

    private void assertPdf(
        ReportDtos.ReportFile file
    ) {
        assertThat(
            file.contentType()
        ).isEqualTo(
            "application/pdf"
        );

        assertThat(
            file.content()
        )
            .isNotEmpty()
            .startsWith(
                "%PDF".getBytes(
                    StandardCharsets
                        .ISO_8859_1
                )
            );
    }
}
