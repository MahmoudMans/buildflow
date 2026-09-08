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
import com.buildflow.expense.service.ExpenseService;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.procurement.dto.ProcurementDtos;
import com.buildflow.procurement.entity.PurchaseOrderStatus;
import com.buildflow.procurement.service.PurchaseOrderService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectStatus;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;
import com.buildflow.supplier.dto.SupplierDtos;
import com.buildflow.supplier.service.SupplierService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class Phase5ProcurementIntegrationTest {

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
    SupplierService supplierService;

    @Autowired
    PurchaseOrderService
        purchaseOrderService;

    @Autowired
    ExpenseService expenseService;

    @Autowired
    FinancialSummaryService
        financialSummaryService;

    @Test
    void purchaseOrderCommitmentsAndGoodsReceiptsAreCalculatedCorrectly() {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Procurement",
                    "Owner",
                    "phase5@example.test",
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
                            "Phase 5 Construction",
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
                    "Procurement Client",
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
                    "Procurement Project",
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
                                null,
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
                    "Project material budget",
                    new BigDecimal(
                        "20"
                    ),
                    new BigDecimal(
                        "100"
                    ),
                    null
                )
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

        SupplierDtos.SupplierView
            supplier =
                supplierService.create(
                    organization.id(),
                    owner
                        .user()
                        .getId(),
                    new SupplierDtos
                        .CreateSupplierRequest(
                            "Material Supplier",
                            "John Supplier",
                            "supplier@example.test",
                            null,
                            null,
                            null,
                            "30 days",
                            null
                        )
                );

        ProcurementDtos.PurchaseOrderView
            order =
                purchaseOrderService.create(
                    organization.id(),
                    owner
                        .user()
                        .getId(),
                    project.id(),
                    new ProcurementDtos
                        .CreatePurchaseOrderRequest(
                            supplier.id(),
                            LocalDate.of(
                                2026,
                                9,
                                8
                            ),
                            LocalDate.of(
                                2026,
                                9,
                                15
                            ),
                            null
                        )
                );

        order =
            purchaseOrderService.addItem(
                organization.id(),
                owner
                    .user()
                    .getId(),
                order.id(),
                new ProcurementDtos
                    .CreateItemRequest(
                        "Flooring",
                        new BigDecimal(
                            "10"
                        ),
                        "m2",
                        new BigDecimal(
                            "100"
                        ),
                        new BigDecimal(
                            "20"
                        )
                    )
            );

        assertThat(
            order.total()
        ).isEqualByComparingTo(
            "1200.0000"
        );

        order =
            purchaseOrderService.submit(
                organization.id(),
                owner
                    .user()
                    .getId(),
                order.id()
            );

        order =
            purchaseOrderService.approve(
                organization.id(),
                owner
                    .user()
                    .getId(),
                order.id()
            );

        assertThat(
            order.status()
        ).isEqualTo(
            PurchaseOrderStatus.APPROVED
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

        assertThat(
            summary.committedCost()
        ).isEqualByComparingTo(
            "1200.0000"
        );

        assertThat(
            summary.forecastCost()
        ).isEqualByComparingTo(
            "1200.0000"
        );

        order =
            purchaseOrderService.markOrdered(
                organization.id(),
                owner
                    .user()
                    .getId(),
                order.id()
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
                        "Partial supplier invoice",
                        LocalDate.of(
                            2026,
                            9,
                            9
                        ),
                        new BigDecimal(
                            "400"
                        ),
                        new BigDecimal(
                            "80"
                        ),
                        supplier.id(),
                        order.id()
                    )
            );

        expenseService.submit(
            organization.id(),
            owner
                .user()
                .getId(),
            expense.id()
        );

        expenseService.approve(
            organization.id(),
            owner
                .user()
                .getId(),
            expense.id()
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
            "480.0000"
        );

        assertThat(
            summary.committedCost()
        ).isEqualByComparingTo(
            "720.0000"
        );

        assertThat(
            summary.forecastCost()
        ).isEqualByComparingTo(
            "1200.0000"
        );

        ProcurementDtos.PurchaseOrderItemView
            item =
                order.items().getFirst();

        order =
            purchaseOrderService.receive(
                organization.id(),
                owner
                    .user()
                    .getId(),
                order.id(),
                new ProcurementDtos
                    .CreateGoodsReceiptRequest(
                        LocalDate.of(
                            2026,
                            9,
                            10
                        ),
                        null,
                        List.of(
                            new ProcurementDtos
                                .ReceiptItemRequest(
                                    item.id(),
                                    new BigDecimal(
                                        "4"
                                    )
                                )
                        )
                    )
            );

        assertThat(
            order.status()
        ).isEqualTo(
            PurchaseOrderStatus.PARTIALLY_RECEIVED
        );

        order =
            purchaseOrderService.receive(
                organization.id(),
                owner
                    .user()
                    .getId(),
                order.id(),
                new ProcurementDtos
                    .CreateGoodsReceiptRequest(
                        LocalDate.of(
                            2026,
                            9,
                            11
                        ),
                        null,
                        List.of(
                            new ProcurementDtos
                                .ReceiptItemRequest(
                                    item.id(),
                                    new BigDecimal(
                                        "6"
                                    )
                                )
                        )
                    )
            );

        assertThat(
            order.status()
        ).isEqualTo(
            PurchaseOrderStatus.RECEIVED
        );

        assertThat(
            order
                .items()
                .getFirst()
                .quantityReceived()
        ).isEqualByComparingTo(
            "10.0000"
        );

        assertThat(
            order
                .items()
                .getFirst()
                .quantityRemaining()
        ).isEqualByComparingTo(
            "0.0000"
        );
    }
}
