package com.buildflow;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.changeorder.dto.ChangeOrderDtos;
import com.buildflow.changeorder.entity.ChangeOrderStatus;
import com.buildflow.changeorder.service.ChangeOrderFinancialService;
import com.buildflow.changeorder.service.ChangeOrderService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.commercial.dto.CommercialDtos;
import com.buildflow.commercial.service.RevenueMetricsService;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase7ChangeOrderIntegrationTest {

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
    ChangeOrderService
        changeOrderService;

    @Autowired
    ChangeOrderFinancialService
        changeOrderFinancialService;

    @Autowired
    RevenueMetricsService
        revenueMetricsService;

    @Autowired
    EntityManager entityManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void approvedChangeOrderModifiesRevisedFinancials() {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Change",
                    "Owner",
                    "phase7@example.test",
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
                            "Phase 7 Construction",
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
                        "Phase 7 Client",
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
                        "Phase 7 Project",
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

        int updatedProjectRows =
            jdbcTemplate.update(
                """
                UPDATE projects
                SET original_budget = ?,
                    contract_value = ?
                WHERE id = ?
                  AND organization_id = ?
                """,
                new BigDecimal("10000"),
                new BigDecimal("20000"),
                project.id(),
                organization.id()
            );

        assertThat(
            updatedProjectRows
        ).isEqualTo(1);

        entityManager.clear();

        ChangeOrderDtos.FinancialSummary
            before =
                changeOrderFinancialService
                    .summary(
                        organization.id(),
                        owner.user().getId(),
                        project.id()
                    );

        assertThat(
            before.revisedBudget()
        ).isEqualByComparingTo(
            "10000.0000"
        );

        assertThat(
            before.revisedContractValue()
        ).isEqualByComparingTo(
            "20000.0000"
        );

        ChangeOrderDtos.View changeOrder =
            changeOrderService.create(
                organization.id(),
                owner.user().getId(),
                project.id(),
                new ChangeOrderDtos
                    .CreateRequest(
                        "Additional marble flooring",
                        "Upgrade requested during execution.",
                        "Client requested upgraded finish.",
                        new BigDecimal("4000"),
                        new BigDecimal("6500"),
                        5
                    )
            );

        assertThat(
            changeOrder.changeOrderNumber()
        ).startsWith(
            "CO-"
        );

        assertThat(
            changeOrder.status()
        ).isEqualTo(
            ChangeOrderStatus.DRAFT
        );

        ChangeOrderDtos.FinancialSummary
            draftSummary =
                changeOrderFinancialService
                    .summary(
                        organization.id(),
                        owner.user().getId(),
                        project.id()
                    );

        assertThat(
            draftSummary
                .approvedBudgetChangeOrders()
        ).isEqualByComparingTo(
            "0.0000"
        );

        changeOrder =
            changeOrderService.submit(
                organization.id(),
                owner.user().getId(),
                changeOrder.id()
            );

        assertThat(
            changeOrder.status()
        ).isEqualTo(
            ChangeOrderStatus.SUBMITTED
        );

        changeOrder =
            changeOrderService
                .sendToClientReview(
                    organization.id(),
                    owner.user().getId(),
                    changeOrder.id()
                );

        assertThat(
            changeOrder.status()
        ).isEqualTo(
            ChangeOrderStatus.CLIENT_REVIEW
        );

        changeOrder =
            changeOrderService.approve(
                organization.id(),
                owner.user().getId(),
                changeOrder.id()
            );

        assertThat(
            changeOrder.status()
        ).isEqualTo(
            ChangeOrderStatus.APPROVED
        );

        ChangeOrderDtos.FinancialSummary
            approvedSummary =
                changeOrderFinancialService
                    .summary(
                        organization.id(),
                        owner.user().getId(),
                        project.id()
                    );

        assertThat(
            approvedSummary
                .approvedBudgetChangeOrders()
        ).isEqualByComparingTo(
            "4000.0000"
        );

        assertThat(
            approvedSummary
                .revisedBudget()
        ).isEqualByComparingTo(
            "14000.0000"
        );

        assertThat(
            approvedSummary
                .approvedClientChangeOrders()
        ).isEqualByComparingTo(
            "6500.0000"
        );

        assertThat(
            approvedSummary
                .revisedContractValue()
        ).isEqualByComparingTo(
            "26500.0000"
        );

        assertThat(
            approvedSummary
                .forecastProfit()
        ).isEqualByComparingTo(
            "26500.0000"
        );

        CommercialDtos.RevenueSummary
            revenue =
                revenueMetricsService
                    .summary(
                        organization.id(),
                        owner.user().getId(),
                        project.id()
                    );

        assertThat(
            revenue
                .approvedClientChangeOrders()
        ).isEqualByComparingTo(
            "6500.0000"
        );

        assertThat(
            revenue
                .revisedContractValue()
        ).isEqualByComparingTo(
            "26500.0000"
        );

        final java.util.UUID
            approvedChangeOrderId =
                changeOrder.id();

        assertThatThrownBy(
            () ->
                changeOrderService.update(
                    organization.id(),
                    owner.user().getId(),
                    approvedChangeOrderId,
                    new ChangeOrderDtos
                        .UpdateRequest(
                            "Forbidden mutation",
                            null,
                            null,
                            new BigDecimal(
                                "999"
                            ),
                            null,
                            null
                        )
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        ChangeOrderDtos.View rejected =
            changeOrderService.create(
                organization.id(),
                owner.user().getId(),
                project.id(),
                new ChangeOrderDtos
                    .CreateRequest(
                        "Rejected optional scope",
                        null,
                        null,
                        new BigDecimal("1000"),
                        new BigDecimal("1500"),
                        2
                    )
            );

        rejected =
            changeOrderService.submit(
                organization.id(),
                owner.user().getId(),
                rejected.id()
            );

        rejected =
            changeOrderService
                .sendToClientReview(
                    organization.id(),
                    owner.user().getId(),
                    rejected.id()
                );

        rejected =
            changeOrderService.reject(
                organization.id(),
                owner.user().getId(),
                rejected.id()
            );

        assertThat(
            rejected.status()
        ).isEqualTo(
            ChangeOrderStatus.REJECTED
        );

        ChangeOrderDtos.FinancialSummary
            afterRejection =
                changeOrderFinancialService
                    .summary(
                        organization.id(),
                        owner.user().getId(),
                        project.id()
                    );

        assertThat(
            afterRejection
                .revisedBudget()
        ).isEqualByComparingTo(
            "14000.0000"
        );

        assertThat(
            afterRejection
                .revisedContractValue()
        ).isEqualByComparingTo(
            "26500.0000"
        );
    }
}
