package com.buildflow.commercial.repository;

import com.buildflow.commercial.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository
    extends JpaRepository<
        Payment,
        UUID
    > {

    List<Payment>
        findByInvoiceIdAndOrganizationIdOrderByPaymentDateDescCreatedAtDesc(
            UUID invoiceId,
            UUID organizationId
        );

    @Query("""
        select sum(payment.amount)
        from Payment payment
        where payment.projectId = :projectId
          and payment.organizationId = :organizationId
        """)
    BigDecimal sumProjectPayments(
        @Param("projectId")
        UUID projectId,
        @Param("organizationId")
        UUID organizationId
    );
}
