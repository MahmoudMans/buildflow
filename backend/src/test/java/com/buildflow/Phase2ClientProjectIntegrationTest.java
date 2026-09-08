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
import com.buildflow.project.entity.ProjectStatus;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;
import com.buildflow.shared.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase2ClientProjectIntegrationTest {

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

    @Test
    void projectNumbersAreSequentialAndTenantIsolationIsEnforced() {

        AuthService.AuthResult
            ownerA =
                authService.register(
                    new AuthDtos.RegisterRequest(
                        "Owner",
                        "Alpha",
                        "owner-alpha@example.test",
                        "StrongPassword123!"
                    ),
                    "127.0.0.1",
                    "test"
                );

        AuthService.AuthResult
            ownerB =
                authService.register(
                    new AuthDtos.RegisterRequest(
                        "Owner",
                        "Beta",
                        "owner-beta@example.test",
                        "StrongPassword456!"
                    ),
                    "127.0.0.1",
                    "test"
                );

        OrganizationDtos.OrganizationView
            organizationA =
                organizationService.create(
                    ownerA
                        .user()
                        .getId(),
                    new OrganizationDtos
                        .CreateOrganizationRequest(
                            "Alpha Construction",
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

        OrganizationDtos.OrganizationView
            organizationB =
                organizationService.create(
                    ownerB
                        .user()
                        .getId(),
                    new OrganizationDtos
                        .CreateOrganizationRequest(
                            "Beta Construction",
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

        ClientDtos.ClientView client =
            clientService.create(
                organizationA.id(),
                ownerA
                    .user()
                    .getId(),
                new ClientDtos.CreateClientRequest(
                    ClientType.COMPANY,
                    "Example Hotels",
                    null,
                    null,
                    "contact@example.test",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            );

        ProjectDtos.ProjectView first =
            projectService.create(
                organizationA.id(),
                ownerA
                    .user()
                    .getId(),
                new ProjectDtos.CreateProjectRequest(
                    client.id(),
                    "Hotel Renovation",
                    null,
                    ProjectType.RENOVATION,
                    ProjectPriority.HIGH,
                    null,
                    "Paris",
                    null,
                    "France",
                    LocalDate.of(
                        2026,
                        9,
                        15
                    ),
                    LocalDate.of(
                        2026,
                        12,
                        20
                    ),
                    "EUR",
                    null
                )
            );

        ProjectDtos.ProjectView second =
            projectService.create(
                organizationA.id(),
                ownerA
                    .user()
                    .getId(),
                new ProjectDtos.CreateProjectRequest(
                    client.id(),
                    "Lobby Renovation",
                    null,
                    ProjectType.INTERIOR,
                    ProjectPriority.MEDIUM,
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

        assertThat(
            first.projectNumber()
        )
            .startsWith(
                "PRJ-"
            )
            .endsWith(
                "-0001"
            );

        assertThat(
            second.projectNumber()
        )
            .endsWith(
                "-0002"
            );

        assertThatThrownBy(
            () ->
                clientService.get(
                    organizationB.id(),
                    ownerB
                        .user()
                        .getId(),
                    client.id()
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        assertThatThrownBy(
            () ->
                projectService.get(
                    organizationB.id(),
                    ownerB
                        .user()
                        .getId(),
                    first.id()
                )
        )
            .isInstanceOf(
                BusinessException.class
            );
    }

    @Test
    void projectLifecycleFollowsAuthoritativeTransitions() {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Project",
                    "Owner",
                    "lifecycle@example.test",
                    "StrongPassword789!"
                ),
                "127.0.0.1",
                "test"
            );

        OrganizationDtos.OrganizationView
            organization =
                organizationService.create(
                    owner
                        .user()
                        .getId(),
                    new OrganizationDtos
                        .CreateOrganizationRequest(
                            "Lifecycle Construction",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
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
                owner
                    .user()
                    .getId(),
                new ClientDtos.CreateClientRequest(
                    ClientType.COMPANY,
                    "Lifecycle Client",
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
                owner
                    .user()
                    .getId(),
                new ProjectDtos.CreateProjectRequest(
                    client.id(),
                    "Lifecycle Project",
                    null,
                    ProjectType.NEW_CONSTRUCTION,
                    ProjectPriority.MEDIUM,
                    null,
                    null,
                    null,
                    "France",
                    null,
                    null,
                    "EUR",
                    null
                )
            );

        assertThat(
            project.status()
        ).isEqualTo(
            ProjectStatus.DRAFT
        );

        project =
            projectService.transition(
                organization.id(),
                owner
                    .user()
                    .getId(),
                project.id(),
                ProjectStatus.PLANNING
            );

        assertThat(
            project.status()
        ).isEqualTo(
            ProjectStatus.PLANNING
        );

        project =
            projectService.transition(
                organization.id(),
                owner
                    .user()
                    .getId(),
                project.id(),
                ProjectStatus.ACTIVE
            );

        assertThat(
            project.status()
        ).isEqualTo(
            ProjectStatus.ACTIVE
        );

        ProjectDtos.ProjectView
            activeProject =
                project;

        assertThatThrownBy(
            () ->
                projectService.transition(
                    organization.id(),
                    owner
                        .user()
                        .getId(),
                    activeProject.id(),
                    ProjectStatus.ARCHIVED
                )
        )
            .isInstanceOf(
                BusinessException.class
            )
            .hasMessageContaining(
                "cannot transition"
            );
    }
}
