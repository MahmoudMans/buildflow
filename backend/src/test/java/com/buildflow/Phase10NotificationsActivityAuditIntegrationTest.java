package com.buildflow;

import com.buildflow.activity.service.ProjectActivityService;
import com.buildflow.audit.service.AuditLogService;
import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.notification.service.NotificationService;
import com.buildflow.notification.service.ReminderScheduler;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;
import com.buildflow.siteops.dto.SiteOperationDtos;
import com.buildflow.siteops.entity.IncidentSeverity;
import com.buildflow.siteops.entity.IncidentType;
import com.buildflow.siteops.service.DailySiteReportService;
import com.buildflow.siteops.service.SiteIncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class Phase10NotificationsActivityAuditIntegrationTest {

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
    SiteIncidentService
        incidentService;

    @Autowired
    NotificationService
        notificationService;

    @Autowired
    ProjectActivityService
        activityService;

    @Autowired
    AuditLogService
        auditLogService;

    @Autowired
    ReminderScheduler
        reminderScheduler;

    @Test
    void importantActionsCreateActivityAuditAndNotifications()
        throws Exception {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Activity",
                    "Owner",
                    "phase10@example.test",
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
                            "Phase 10 Construction",
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
                        "Phase 10 Client",
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
                        "Phase 10 Project",
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

        dailySiteReportService.create(
            organization.id(),
            owner.user().getId(),
            project.id(),
            new SiteOperationDtos
                .CreateDailyReportRequest(
                    LocalDate.now(),
                    "Dry",
                    11,
                    "Daily site progress.",
                    "Electrical work completed.",
                    null,
                    null,
                    "Continue finishing."
                )
        );

        assertThat(
            activityService.list(
                organization.id(),
                owner.user().getId(),
                project.id()
            )
        )
            .extracting(
                view -> view.action()
            )
            .contains(
                "SITE_REPORT_CREATED"
            );

        assertThat(
            auditLogService.list(
                organization.id(),
                owner.user().getId(),
                project.id()
            )
        )
            .extracting(
                view -> view.action()
            )
            .contains(
                "SITE_REPORT_CREATED"
            );

        notificationService.notifyUser(
            organization.id(),
            owner.user().getId(),
            "TEST_NOTIFICATION",
            "Phase 10 notification",
            "Notification center test.",
            "PROJECT",
            project.id(),
            "phase10-test-notification"
        );

        notificationService.notifyUser(
            organization.id(),
            owner.user().getId(),
            "TEST_NOTIFICATION",
            "Phase 10 notification",
            "Duplicate should be suppressed.",
            "PROJECT",
            project.id(),
            "phase10-test-notification"
        );

        assertThat(
            notificationService
                .unreadCount(
                    organization.id(),
                    owner.user().getId()
                )
                .count()
        ).isEqualTo(1);

        var notification =
            notificationService.list(
                organization.id(),
                owner.user().getId()
            ).getFirst();

        notificationService.markRead(
            organization.id(),
            owner.user().getId(),
            notification.id()
        );

        assertThat(
            notificationService
                .unreadCount(
                    organization.id(),
                    owner.user().getId()
                )
                .count()
        ).isZero();

        incidentService.create(
            organization.id(),
            owner.user().getId(),
            project.id(),
            new SiteOperationDtos
                .CreateIncidentRequest(
                    IncidentType.SAFETY,
                    IncidentSeverity.CRITICAL,
                    "Critical safety condition",
                    "A critical condition requires immediate review.",
                    null,
                    null,
                    false
                )
        );

        assertThat(
            notificationService.list(
                organization.id(),
                owner.user().getId()
            )
        )
            .extracting(
                view -> view.type()
            )
            .contains(
                "CRITICAL_INCIDENT"
            );

        assertThat(
            activityService.list(
                organization.id(),
                owner.user().getId(),
                project.id()
            )
        )
            .extracting(
                view -> view.action()
            )
            .contains(
                "INCIDENT_CREATED"
            );

        assertThat(
            auditLogService.list(
                organization.id(),
                owner.user().getId(),
                project.id()
            )
        )
            .extracting(
                view -> view.action()
            )
            .contains(
                "INCIDENT_CREATED"
            );

        ReminderScheduler.RunResult result =
            reminderScheduler
                .runMaintenance();

        assertThat(result)
            .isNotNull();

        assertThat(
            ReminderScheduler.class
                .getMethod(
                    "scheduledMaintenance"
                )
                .isAnnotationPresent(
                    Scheduled.class
                )
        ).isTrue();

        notificationService.markAllRead(
            organization.id(),
            owner.user().getId()
        );

        assertThat(
            notificationService
                .unreadCount(
                    organization.id(),
                    owner.user().getId()
                )
                .count()
        ).isZero();
    }
}
