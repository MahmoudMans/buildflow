package com.buildflow;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.commercial.dto.CommercialDtos;
import com.buildflow.commercial.entity.InvoiceStatus;
import com.buildflow.commercial.entity.PaymentMethod;
import com.buildflow.commercial.entity.QuoteStatus;
import com.buildflow.commercial.service.InvoiceService;
import com.buildflow.commercial.service.QuoteService;
import com.buildflow.commercial.service.RevenueMetricsService;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
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
class Phase6CommercialWorkflowIntegrationTest {

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
    QuoteService quoteService;

    @Autowired
    InvoiceService invoiceService;

    @Autowired
    RevenueMetricsService
        revenueMetricsService;

    @Test
    void acceptedQuoteInvoiceAndPaymentsCompleteCommercialFlow() {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Commercial",
                    "Owner",
                    "phase6@example.test",
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
                            "Phase 6 Construction",
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
                        "Commercial Client",
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
                        "Commercial Project",
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

        CommercialDtos.QuoteView quote =
            quoteService.create(
                organization.id(),
                owner.user().getId(),
                project.id(),
                new CommercialDtos
                    .CreateQuoteRequest(
                        LocalDate.of(
                            2026,
                            9,
                            8
                        ),
                        LocalDate.of(
                            2026,
                            10,
                            8
                        ),
                        "Primary project quotation",
                        "Payment within 30 days",
                        true
                    )
            );

        quote =
            quoteService.addItem(
                organization.id(),
                owner.user().getId(),
                quote.id(),
                new CommercialDtos
                    .LineRequest(
                        "Renovation package",
                        new BigDecimal("10"),
                        "unit",
                        new BigDecimal("100"),
                        new BigDecimal("20")
                    )
            );

        assertThat(
            quote.total()
        ).isEqualByComparingTo(
            "1200.0000"
        );

        quote =
            quoteService.send(
                organization.id(),
                owner.user().getId(),
                quote.id()
            );

        assertThat(
            quote.status()
        ).isEqualTo(
            QuoteStatus.SENT
        );

        quote =
            quoteService.accept(
                organization.id(),
                owner.user().getId(),
                quote.id()
            );

        assertThat(
            quote.status()
        ).isEqualTo(
            QuoteStatus.ACCEPTED
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
            revenue.originalContractValue()
        ).isEqualByComparingTo(
            "1200.0000"
        );

        UUIDValues ids =
            new UUIDValues(
                organization.id(),
                owner.user().getId(),
                quote.id()
            );

        assertThatThrownBy(
            () ->
                quoteService.addItem(
                    ids.organizationId(),
                    ids.userId(),
                    ids.documentId(),
                    new CommercialDtos
                        .LineRequest(
                            "Forbidden change",
                            BigDecimal.ONE,
                            "unit",
                            BigDecimal.ONE,
                            BigDecimal.ZERO
                        )
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        CommercialDtos.InvoiceView invoice =
            invoiceService.createFromQuote(
                organization.id(),
                owner.user().getId(),
                quote.id(),
                new CommercialDtos
                    .CreateInvoiceFromQuoteRequest(
                        LocalDate.of(
                            2026,
                            9,
                            8
                        ),
                        LocalDate.of(
                            2026,
                            10,
                            8
                        ),
                        null,
                        "Bank transfer"
                    )
            );

        assertThat(
            invoice.total()
        ).isEqualByComparingTo(
            "1200.0000"
        );

        invoice =
            invoiceService.issue(
                organization.id(),
                owner.user().getId(),
                invoice.id()
            );

        assertThat(
            invoice.status()
        ).isEqualTo(
            InvoiceStatus.ISSUED
        );

        UUIDValues invoiceIds =
            new UUIDValues(
                organization.id(),
                owner.user().getId(),
                invoice.id()
            );

        assertThatThrownBy(
            () ->
                invoiceService.addItem(
                    invoiceIds.organizationId(),
                    invoiceIds.userId(),
                    invoiceIds.documentId(),
                    new CommercialDtos
                        .LineRequest(
                            "Forbidden invoice change",
                            BigDecimal.ONE,
                            "unit",
                            BigDecimal.ONE,
                            BigDecimal.ZERO
                        )
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        invoice =
            invoiceService.recordPayment(
                organization.id(),
                owner.user().getId(),
                invoice.id(),
                new CommercialDtos
                    .RecordPaymentRequest(
                        LocalDate.of(
                            2026,
                            9,
                            9
                        ),
                        new BigDecimal("480"),
                        PaymentMethod.BANK_TRANSFER,
                        "PAY-001",
                        null
                    )
            );

        assertThat(
            invoice.status()
        ).isEqualTo(
            InvoiceStatus.PARTIALLY_PAID
        );

        assertThat(
            invoice.amountOutstanding()
        ).isEqualByComparingTo(
            "720.0000"
        );

        UUIDValues paymentIds =
            new UUIDValues(
                organization.id(),
                owner.user().getId(),
                invoice.id()
            );

        assertThatThrownBy(
            () ->
                invoiceService.recordPayment(
                    paymentIds.organizationId(),
                    paymentIds.userId(),
                    paymentIds.documentId(),
                    new CommercialDtos
                        .RecordPaymentRequest(
                            LocalDate.of(
                                2026,
                                9,
                                10
                            ),
                            new BigDecimal("721"),
                            PaymentMethod.BANK_TRANSFER,
                            "PAY-OVER",
                            null
                        )
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        invoice =
            invoiceService.recordPayment(
                organization.id(),
                owner.user().getId(),
                invoice.id(),
                new CommercialDtos
                    .RecordPaymentRequest(
                        LocalDate.of(
                            2026,
                            9,
                            10
                        ),
                        new BigDecimal("720"),
                        PaymentMethod.BANK_TRANSFER,
                        "PAY-002",
                        null
                    )
            );

        assertThat(
            invoice.status()
        ).isEqualTo(
            InvoiceStatus.PAID
        );

        assertThat(
            invoice.amountOutstanding()
        ).isEqualByComparingTo(
            "0.0000"
        );

        revenue =
            revenueMetricsService
                .summary(
                    organization.id(),
                    owner.user().getId(),
                    project.id()
                );

        assertThat(
            revenue.totalInvoiced()
        ).isEqualByComparingTo(
            "1200.0000"
        );

        assertThat(
            revenue.totalPaid()
        ).isEqualByComparingTo(
            "1200.0000"
        );

        assertThat(
            revenue.outstandingReceivables()
        ).isEqualByComparingTo(
            "0.0000"
        );
    }

    private record UUIDValues(
        java.util.UUID organizationId,
        java.util.UUID userId,
        java.util.UUID documentId
    ) {
    }
}
