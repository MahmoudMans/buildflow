package com.buildflow;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.budget.dto.BudgetDtos;
import com.buildflow.budget.service.BudgetService;
import com.buildflow.budget.service.FinancialSummaryService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.expense.dto.ExpenseDtos;
import com.buildflow.expense.entity.ExpenseStatus;
import com.buildflow.expense.service.ExpenseService;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase4BudgetExpenseIntegrationTest {

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
    BudgetService budgetService;

    @Autowired
    ExpenseService expenseService;

    @Autowired
    FinancialSummaryService
        financialSummaryService;

    @Test
    void activationLocksBudgetAndApprovedExpensesDriveActualCost() {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Budget",
                    "Owner",
                    "phase4@example.test",
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
                            "Phase 4 Construction",
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
                    "Financial Client",
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
                    "Financial Project",
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

        BudgetDtos.BudgetCategoryView
            category =
                budgetService
                    .createCategory(
                        organization.id(),
                        owner
                            .user()
                            .getId(),
                        project.id(),
                        new BudgetDtos
                            .CreateCategoryRequest(
                                "Materials",
                                "Construction materials",
                                1
                            )
                    );

        budgetService.createItem(
            organization.id(),
            owner
                .user()
                .getId(),
            project.id(),
            category.id(),
            new BudgetDtos
                .CreateItemRequest(
                    "Flooring",
                    new BigDecimal(
                        "10"
                    ),
                    new BigDecimal(
                        "100"
                    ),
                    null
                )
        );

        BudgetDtos.BudgetView draftBudget =
            budgetService.getBudget(
                organization.id(),
                owner
                    .user()
                    .getId(),
                project.id()
            );

        assertThat(
            draftBudget.plannedBudget()
        ).isEqualByComparingTo(
            "1000.0000"
        );

        projectService.transition(
            organization.id(),
            owner
                .user()
                .getId(),
            project.id(),
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
            project.originalBudget()
        ).isEqualByComparingTo(
            "1000.0000"
        );

        BudgetDtos.BudgetView
            lockedBudget =
                budgetService.getBudget(
                    organization.id(),
                    owner
                        .user()
                        .getId(),
                    project.id()
                );

        assertThat(
            lockedBudget.baselineLocked()
        ).isTrue();

        assertThatThrownBy(
            () ->
                budgetService.createItem(
                    organization.id(),
                    owner
                        .user()
                        .getId(),
                    lockedBudget.projectId(),
                    category.id(),
                    new BudgetDtos
                        .CreateItemRequest(
                            "Late item",
                            BigDecimal.ONE,
                            BigDecimal.ONE,
                            null
                        )
                )
        )
            .isInstanceOf(
                BusinessException.class
            )
            .hasMessageContaining(
                "original project budget is locked"
            );

        ExpenseDtos.ExpenseView expense =
            expenseService.create(
                organization.id(),
                owner
                    .user()
                    .getId(),
                project.id(),
                new ExpenseDtos
                    .CreateExpenseRequest(
                        category.id(),
                        "Flooring invoice",
                        LocalDate.of(
                            2026,
                            9,
                            8
                        ),
                        new BigDecimal(
                            "200"
                        ),
                        new BigDecimal(
                            "40"
                        )
                    )
            );

        assertThat(
            expense.status()
        ).isEqualTo(
            ExpenseStatus.DRAFT
        );

        BudgetDtos.FinancialSummaryView
            summary =
                financialSummaryService
                    .summary(
                        organization.id(),
                        owner
                            .user()
                            .getId(),
                        project.id()
                    );

        assertThat(
            summary.actualCost()
        ).isEqualByComparingTo(
            "0.0000"
        );

        expenseService.submit(
            organization.id(),
            owner
                .user()
                .getId(),
            expense.id()
        );

        expense =
            expenseService.approve(
                organization.id(),
                owner
                    .user()
                    .getId(),
                expense.id()
            );

        assertThat(
            expense.status()
        ).isEqualTo(
            ExpenseStatus.APPROVED
        );

        summary =
            financialSummaryService
                .summary(
                    organization.id(),
                    owner
                        .user()
                        .getId(),
                    project.id()
                );

        assertThat(
            summary.actualCost()
        ).isEqualByComparingTo(
            "240.0000"
        );

        assertThat(
            summary.budgetRemaining()
        ).isEqualByComparingTo(
            "760.0000"
        );

        expenseService.voidExpense(
            organization.id(),
            owner
                .user()
                .getId(),
            expense.id(),
            new ExpenseDtos
                .VoidExpenseRequest(
                    "Duplicate supplier invoice"
                )
        );

        summary =
            financialSummaryService
                .summary(
                    organization.id(),
                    owner
                        .user()
                        .getId(),
                    project.id()
                );

        assertThat(
            summary.actualCost()
        ).isEqualByComparingTo(
            "0.0000"
        );

        assertThat(
            summary.budgetRemaining()
        ).isEqualByComparingTo(
            "1000.0000"
        );
    }
}
