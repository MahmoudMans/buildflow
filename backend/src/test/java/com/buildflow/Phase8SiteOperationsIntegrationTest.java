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
import com.buildflow.shared.error.BusinessException;
import com.buildflow.siteops.dto.SiteOperationDtos;
import com.buildflow.siteops.entity.IncidentSeverity;
import com.buildflow.siteops.entity.IncidentStatus;
import com.buildflow.siteops.entity.IncidentType;
import com.buildflow.siteops.entity.SitePhotoCategory;
import com.buildflow.siteops.service.DailyReportPhotoService;
import com.buildflow.siteops.service.DailySiteReportService;
import com.buildflow.siteops.service.ProjectProgressUpdateService;
import com.buildflow.siteops.service.SiteIncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase8SiteOperationsIntegrationTest {

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
    DailySiteReportService
        dailySiteReportService;

    @Autowired
    DailyReportPhotoService
        photoService;

    @Autowired
    SiteIncidentService
        incidentService;

    @Autowired
    ProjectProgressUpdateService
        progressUpdateService;

    @Test
    void managesDailyConstructionActivityAndIncidentWorkflow() {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Site",
                    "Owner",
                    "phase8@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.1",
                "test"
            );

        OrganizationDtos.OrganizationView
            organization =
                organizationService.create(
                    owner.user().getId(),
                    new OrganizationDtos
                        .CreateOrganizationRequest(
                            "Phase 8 Construction",
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
                        "Phase 8 Client",
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
                        "Phase 8 Project",
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

        LocalDate today =
            LocalDate.now();

        SiteOperationDtos.DailyReportView
            report =
                dailySiteReportService.create(
                    organization.id(),
                    owner.user().getId(),
                    project.id(),
                    new SiteOperationDtos
                        .CreateDailyReportRequest(
                            today,
                            "Dry and clear",
                            14,
                            "Interior renovation continued.",
                            "Electrical rough-in completed.",
                            "No material delays.",
                            "Minor access restriction.",
                            "Begin wall finishing."
                        )
                );

        assertThat(
            report.reportDate()
        ).isEqualTo(today);

        assertThat(
            report.workersOnSite()
        ).isEqualTo(14);

        assertThat(
            dailySiteReportService.list(
                organization.id(),
                owner.user().getId(),
                project.id()
            )
        ).hasSize(1);

        assertThatThrownBy(
            () ->
                dailySiteReportService.create(
                    organization.id(),
                    owner.user().getId(),
                    project.id(),
                    new SiteOperationDtos
                        .CreateDailyReportRequest(
                            today,
                            "Dry",
                            1,
                            "Duplicate report",
                            null,
                            null,
                            null,
                            null
                        )
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        byte[] imageBytes =
            new byte[] {
                (byte) 0x89,
                0x50,
                0x4E,
                0x47
            };

        SiteOperationDtos.PhotoView photo =
            photoService.upload(
                organization.id(),
                owner.user().getId(),
                report.id(),
                new SiteOperationDtos
                    .UploadPhotoRequest(
                        "progress.png",
                        "image/png",
                        Base64
                            .getEncoder()
                            .encodeToString(
                                imageBytes
                            ),
                        "Electrical progress",
                        SitePhotoCategory.PROGRESS,
                        false
                    )
            );

        assertThat(
            photo.fileName()
        ).isEqualTo(
            "progress.png"
        );

        assertThat(
            photo.sizeBytes()
        ).isEqualTo(
            imageBytes.length
        );

        assertThat(
            photoService.content(
                organization.id(),
                owner.user().getId(),
                photo.id()
            ).getContent()
        ).containsExactly(
            imageBytes
        );

        SiteOperationDtos.ProgressUpdateView
            progressUpdate =
                progressUpdateService.create(
                    organization.id(),
                    owner.user().getId(),
                    project.id(),
                    new SiteOperationDtos
                        .CreateProgressUpdateRequest(
                            "Site works progressing according to plan.",
                            "Access remains restricted on floor two.",
                            "Continue finishes and inspections."
                        )
                );

        assertThat(
            progressUpdate
                .progressPercentageSnapshot()
        ).isNotNull();

        SiteOperationDtos.IncidentView
            incident =
                incidentService.create(
                    organization.id(),
                    owner.user().getId(),
                    project.id(),
                    new SiteOperationDtos
                        .CreateIncidentRequest(
                            IncidentType.SAFETY,
                            IncidentSeverity.HIGH,
                            "Temporary access obstruction",
                            "A delivery temporarily obstructed a marked access route.",
                            Instant.now()
                                .minusSeconds(60),
                            owner.user().getId(),
                            false
                        )
                );

        assertThat(
            incident.incidentNumber()
        ).startsWith(
            "INC-"
        );

        assertThat(
            incident.status()
        ).isEqualTo(
            IncidentStatus.OPEN
        );

        incident =
            incidentService.investigate(
                organization.id(),
                owner.user().getId(),
                incident.id()
            );

        assertThat(
            incident.status()
        ).isEqualTo(
            IncidentStatus.INVESTIGATING
        );

        incident =
            incidentService.requireAction(
                organization.id(),
                owner.user().getId(),
                incident.id()
            );

        assertThat(
            incident.status()
        ).isEqualTo(
            IncidentStatus.ACTION_REQUIRED
        );

        incident =
            incidentService.resolve(
                organization.id(),
                owner.user().getId(),
                incident.id(),
                new SiteOperationDtos
                    .ResolveIncidentRequest(
                        "Delivery moved and access route inspected."
                    )
            );

        assertThat(
            incident.status()
        ).isEqualTo(
            IncidentStatus.RESOLVED
        );

        assertThat(
            incident.resolution()
        ).isNotBlank();

        incident =
            incidentService.close(
                organization.id(),
                owner.user().getId(),
                incident.id()
            );

        assertThat(
            incident.status()
        ).isEqualTo(
            IncidentStatus.CLOSED
        );

        final java.util.UUID closedIncidentId =
            incident.id();

        assertThatThrownBy(
            () ->
                incidentService.update(
                    organization.id(),
                    owner.user().getId(),
                    closedIncidentId,
                    new SiteOperationDtos
                        .UpdateIncidentRequest(
                            null,
                            IncidentSeverity.LOW,
                            null,
                            null,
                            null,
                            null
                        )
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        OrganizationDtos.OrganizationView
            secondOrganization =
                organizationService.create(
                    owner.user().getId(),
                    new OrganizationDtos
                        .CreateOrganizationRequest(
                            "Phase 8 Isolation",
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

        assertThatThrownBy(
            () ->
                incidentService.get(
                    secondOrganization.id(),
                    owner.user().getId(),
                    closedIncidentId
                )
        )
            .isInstanceOf(
                BusinessException.class
            );
    }
}
