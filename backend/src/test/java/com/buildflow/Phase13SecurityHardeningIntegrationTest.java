package com.buildflow;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;

import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;

import com.buildflow.dashboard.service.DashboardService;

import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;

import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;

import com.buildflow.report.service.CsvExportService;
import com.buildflow.report.service.ProjectReportService;

import com.buildflow.shared.error.BusinessException;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase13SecurityHardeningIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer(
            "postgres:17-alpine"
        );

    @Autowired
    AuthService authService;

    @Autowired
    OrganizationService
        organizationService;

    @Autowired
    ClientService clientService;

    @Autowired
    ProjectService projectService;

    @Autowired
    DashboardService dashboardService;

    @Autowired
    ProjectReportService
        projectReportService;

    @Autowired
    CsvExportService csvExportService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Test
    void tenantIdorSqlInputAndAuditBoundariesHold() {

        AuthService.AuthResult ownerA =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Phase",
                    "Thirteen A",
                    "phase13-a@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.1",
                "phase13-test"
            );

        OrganizationDtos.OrganizationView
            organizationA =
                organizationService.create(
                    ownerA.user().getId(),
                    organization(
                        "Phase 13 Tenant A",
                        "Paris"
                    )
                );

        ClientDtos.ClientView clientA =
            clientService.create(
                organizationA.id(),
                ownerA.user().getId(),
                new ClientDtos
                    .CreateClientRequest(
                        ClientType.COMPANY,
                        "Phase 13 Client A",
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

        String sqlLikeProjectName =
            "Phase 13 '); DROP TABLE projects; --";

        ProjectDtos.ProjectView projectA =
            projectService.create(
                organizationA.id(),
                ownerA.user().getId(),
                new ProjectDtos
                    .CreateProjectRequest(
                        clientA.id(),
                        sqlLikeProjectName,
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

        String storedProjectName =
            jdbcTemplate.queryForObject(
                """
                SELECT name
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                """,
                String.class,
                projectA.id(),
                organizationA.id()
            );

        assertThat(
            storedProjectName
        ).isEqualTo(
            sqlLikeProjectName
        );

        assertProjectsTableExists();

        AuthService.AuthResult ownerB =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Phase",
                    "Thirteen B",
                    "phase13-b@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.2",
                "phase13-test"
            );

        OrganizationDtos.OrganizationView
            organizationB =
                organizationService.create(
                    ownerB.user().getId(),
                    organization(
                        "Phase 13 Tenant B",
                        "Lyon"
                    )
                );

        assertThatThrownBy(
            () ->
                dashboardService.project(
                    organizationA.id(),
                    ownerB.user().getId(),
                    projectA.id()
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        assertThatThrownBy(
            () ->
                dashboardService.project(
                    organizationB.id(),
                    ownerB.user().getId(),
                    projectA.id()
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        assertThatThrownBy(
            () ->
                projectReportService
                    .projectSummary(
                        organizationA.id(),
                        ownerB.user().getId(),
                        projectA.id()
                    )
        )
            .isInstanceOf(
                BusinessException.class
            );

        assertThatThrownBy(
            () ->
                csvExportService.export(
                    organizationA.id(),
                    ownerB.user().getId(),
                    "projects"
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        assertThatThrownBy(
            () ->
                csvExportService.export(
                    organizationA.id(),
                    ownerA.user().getId(),
                    "projects; DROP TABLE projects; --"
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        assertProjectsTableExists();

        Long tenantAProjects =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM projects
                WHERE organization_id = ?
                """,
                Long.class,
                organizationA.id()
            );

        Long tenantBProjects =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM projects
                WHERE organization_id = ?
                """,
                Long.class,
                organizationB.id()
            );

        assertThat(
            tenantAProjects
        ).isEqualTo(
            1L
        );

        assertThat(
            tenantBProjects
        ).isEqualTo(
            0L
        );

        Long auditEntries =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM audit_logs
                WHERE organization_id = ?
                  AND entity_id = ?
                """,
                Long.class,
                organizationA.id(),
                projectA.id()
            );

        assertThat(
            auditEntries
        )
            .isNotNull()
            .isGreaterThan(
                0L
            );
    }

    private OrganizationDtos
        .CreateOrganizationRequest
        organization(
            String name,
            String city
        ) {

        return new OrganizationDtos
            .CreateOrganizationRequest(
                name,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                city,
                null,
                "France",
                "EUR",
                "Europe/Paris",
                null
            );
    }

    private void assertProjectsTableExists() {

        Integer tableCount =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'projects'
                """,
                Integer.class
            );

        assertThat(
            tableCount
        ).isEqualTo(
            1
        );
    }
}
