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
import com.buildflow.task.dto.TaskDtos;
import com.buildflow.task.entity.TaskPriority;
import com.buildflow.task.entity.TaskStatus;
import com.buildflow.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase3PlanningIntegrationTest {

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
    TaskService taskService;

    @Test
    void weightedProgressExcludesCancelledTasksAndSubtaskDepthIsLimited() {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Planning",
                    "Owner",
                    "phase3@example.test",
                    "StrongPassword123!"
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
                            "Phase 3 Construction",
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
                owner
                    .user()
                    .getId(),
                new ClientDtos.CreateClientRequest(
                    ClientType.COMPANY,
                    "Planning Client",
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
                    "Planning Project",
                    null,
                    ProjectType.RENOVATION,
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

        TaskDtos.TaskView taskA =
            taskService.create(
                organization.id(),
                owner
                    .user()
                    .getId(),
                project.id(),
                new TaskDtos.CreateTaskRequest(
                    null,
                    null,
                    "Task A",
                    null,
                    TaskPriority.MEDIUM,
                    null,
                    null,
                    null,
                    1
                )
            );

        TaskDtos.TaskView taskB =
            taskService.create(
                organization.id(),
                owner
                    .user()
                    .getId(),
                project.id(),
                new TaskDtos.CreateTaskRequest(
                    null,
                    null,
                    "Task B",
                    null,
                    TaskPriority.HIGH,
                    null,
                    null,
                    null,
                    3
                )
            );

        taskService.transition(
            organization.id(),
            owner
                .user()
                .getId(),
            taskA.id(),
            TaskStatus.IN_PROGRESS
        );

        taskService.updateProgress(
            organization.id(),
            owner
                .user()
                .getId(),
            taskA.id(),
            new TaskDtos.UpdateProgressRequest(
                new BigDecimal("50")
            )
        );

        taskService.transition(
            organization.id(),
            owner
                .user()
                .getId(),
            taskB.id(),
            TaskStatus.IN_PROGRESS
        );

        taskService.transition(
            organization.id(),
            owner
                .user()
                .getId(),
            taskB.id(),
            TaskStatus.DONE
        );

        ProjectDtos.ProjectView
            calculated =
                projectService.get(
                    organization.id(),
                    owner
                        .user()
                        .getId(),
                    project.id()
                );

        assertThat(
            calculated.progressPercentage()
        ).isEqualByComparingTo(
            "87.50"
        );

        TaskDtos.TaskView cancelledTask =
            taskService.create(
                organization.id(),
                owner
                    .user()
                    .getId(),
                project.id(),
                new TaskDtos.CreateTaskRequest(
                    null,
                    null,
                    "Cancelled Task",
                    null,
                    TaskPriority.LOW,
                    null,
                    null,
                    null,
                    10
                )
            );

        taskService.transition(
            organization.id(),
            owner
                .user()
                .getId(),
            cancelledTask.id(),
            TaskStatus.CANCELLED
        );

        calculated =
            projectService.get(
                organization.id(),
                owner
                    .user()
                    .getId(),
                project.id()
            );

        assertThat(
            calculated.progressPercentage()
        ).isEqualByComparingTo(
            "87.50"
        );

        TaskDtos.TaskView child =
            taskService.create(
                organization.id(),
                owner
                    .user()
                    .getId(),
                project.id(),
                new TaskDtos.CreateTaskRequest(
                    null,
                    taskA.id(),
                    "Child Task",
                    null,
                    TaskPriority.MEDIUM,
                    null,
                    null,
                    null,
                    1
                )
            );

        assertThatThrownBy(
            () ->
                taskService.create(
                    organization.id(),
                    owner
                        .user()
                        .getId(),
                    project.id(),
                    new TaskDtos.CreateTaskRequest(
                        null,
                        child.id(),
                        "Grandchild Task",
                        null,
                        TaskPriority.MEDIUM,
                        null,
                        null,
                        null,
                        1
                    )
                )
        )
            .isInstanceOf(
                BusinessException.class
            )
            .hasMessageContaining(
                "one level of subtasks"
            );
    }
}
