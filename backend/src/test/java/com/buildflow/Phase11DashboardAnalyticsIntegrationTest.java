package com.buildflow;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.dashboard.dto.DashboardDtos;
import com.buildflow.dashboard.service.DashboardService;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;
import com.buildflow.siteops.dto.SiteOperationDtos;
import com.buildflow.siteops.entity.IncidentSeverity;
import com.buildflow.siteops.entity.IncidentType;
import com.buildflow.siteops.service.SiteIncidentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class Phase11DashboardAnalyticsIntegrationTest {

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
    SiteIncidentService incidentService;

    @Autowired
    DashboardService dashboardService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Test
    void dashboardFiguresReconcileWithUnderlyingRecords() {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Dashboard",
                    "Owner",
                    "phase11@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.1",
                "phase11-test"
            );

        OrganizationDtos.OrganizationView
            organization =
                organizationService.create(
                    owner.user().getId(),
                    new OrganizationDtos
                        .CreateOrganizationRequest(
                            "Phase 11 Construction",
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
                        "Phase 11 Client",
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
                        "Phase 11 Project",
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

        entityManager.flush();

        int activated =
            jdbcTemplate.update(
                """
                UPDATE projects
                SET status = 'ACTIVE'
                WHERE id = ?
                  AND organization_id = ?
                """,
                project.id(),
                organization.id()
            );

        assertThat(activated)
            .isEqualTo(1);

        DashboardDtos
            .OrganizationDashboard
            organizationDashboard =
                dashboardService
                    .organization(
                        organization.id(),
                        owner.user().getId()
                    );

        DashboardDtos
            .ProjectDashboard
            projectDashboard =
                dashboardService
                    .project(
                        organization.id(),
                        owner.user().getId(),
                        project.id()
                    );

        Long authoritativeActiveProjects =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM projects
                WHERE organization_id = ?
                  AND status = 'ACTIVE'
                """,
                Long.class,
                organization.id()
            );

        assertThat(
            organizationDashboard
                .activeProjects()
        ).isEqualTo(
            authoritativeActiveProjects
        );

        assertThat(
            organizationDashboard
                .projectStatus()
        )
            .filteredOn(
                item ->
                    item.status()
                        .equals(
                            "ACTIVE"
                        )
            )
            .singleElement()
            .extracting(
                DashboardDtos
                    .StatusCount::count
            )
            .isEqualTo(1L);

        assertThat(
            projectDashboard
                .projectId()
        ).isEqualTo(
            project.id()
        );

        assertThat(
            projectDashboard
                .health()
                .status()
        ).isEqualTo(
            "ON_TRACK"
        );

        assertThat(
            organizationDashboard
                .totalContractValue()
        ).isEqualByComparingTo(
            projectDashboard
                .financial()
                .revisedContractValue()
        );

        assertThat(
            organizationDashboard
                .actualCosts()
        ).isEqualByComparingTo(
            projectDashboard
                .financial()
                .actualCost()
        );

        assertThat(
            organizationDashboard
                .outstandingInvoices()
        ).isEqualByComparingTo(
            projectDashboard
                .financial()
                .outstandingReceivables()
        );

        assertThat(
            projectDashboard
                .financial()
                .forecastCost()
        ).isEqualByComparingTo(
            projectDashboard
                .financial()
                .actualCost()
                .add(
                    projectDashboard
                        .financial()
                        .committedCost()
                )
        );

        assertThat(
            projectDashboard
                .financial()
                .forecastProfit()
        ).isEqualByComparingTo(
            projectDashboard
                .financial()
                .revisedContractValue()
                .subtract(
                    projectDashboard
                        .financial()
                        .forecastCost()
                )
        );

        assertThat(
            organizationDashboard
                .monthlyRevenue()
        ).hasSize(12);

        assertThat(
            organizationDashboard
                .monthlyExpenses()
        ).hasSize(12);

        assertThat(
            projectDashboard
                .monthlyExpenses()
        ).hasSize(12);

        incidentService.create(
            organization.id(),
            owner.user().getId(),
            project.id(),
            new SiteOperationDtos
                .CreateIncidentRequest(
                    IncidentType.SAFETY,
                    IncidentSeverity.CRITICAL,
                    "Critical dashboard incident",
                    "This incident drives project health to critical.",
                    null,
                    null,
                    false
                )
        );

        DashboardDtos.ProjectDashboard
            criticalProject =
                dashboardService
                    .project(
                        organization.id(),
                        owner.user().getId(),
                        project.id()
                    );

        assertThat(
            criticalProject
                .health()
                .status()
        ).isEqualTo(
            "CRITICAL"
        );

        Long authoritativeOpenIncidents =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM site_incidents
                WHERE organization_id = ?
                  AND project_id = ?
                  AND status NOT IN (
                        'RESOLVED',
                        'CLOSED'
                      )
                """,
                Long.class,
                organization.id(),
                project.id()
            );

        assertThat(
            criticalProject
                .openIncidents()
        ).isEqualTo(
            authoritativeOpenIncidents
        );

        DashboardDtos
            .OrganizationDashboard
            criticalOrganization =
                dashboardService
                    .organization(
                        organization.id(),
                        owner.user().getId()
                    );

        assertThat(
            criticalOrganization
                .projectsAtRisk()
        ).isEqualTo(1L);

        assertThat(
            criticalOrganization
                .openIncidents()
        ).isEqualTo(
            authoritativeOpenIncidents
        );

        assertThat(
            projectDashboard
                .financial()
                .actualCost()
        ).isEqualByComparingTo(
            BigDecimal.ZERO
        );
    }
}
