package com.buildflow.commercial.repository;

import com.buildflow.commercial.entity.Invoice;
import com.buildflow.commercial.entity.InvoiceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository
    extends JpaRepository<
        Invoice,
        UUID
    > {

    List<Invoice>
        findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID projectId,
            UUID organizationId
        );

    Optional<Invoice>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select invoice
        from Invoice invoice
        where invoice.id = :id
          and invoice.organizationId = :organizationId
        """)
    Optional<Invoice> findForUpdate(
        @Param("id")
        UUID id,
        @Param("organizationId")
        UUID organizationId
    );

    @Query("""
        select sum(invoice.total)
        from Invoice invoice
        where invoice.projectId = :projectId
          and invoice.organizationId = :organizationId
          and invoice.status in :statuses
        """)
    BigDecimal sumTotalForStatuses(
        @Param("projectId")
        UUID projectId,
        @Param("organizationId")
        UUID organizationId,
        @Param("statuses")
        Collection<InvoiceStatus> statuses
    );

    @Query("""
        select sum(invoice.amountOutstanding)
        from Invoice invoice
        where invoice.projectId = :projectId
          and invoice.organizationId = :organizationId
          and invoice.status in :statuses
        """)
    BigDecimal sumOutstandingForStatuses(
        @Param("projectId")
        UUID projectId,
        @Param("organizationId")
        UUID organizationId,
        @Param("statuses")
        Collection<InvoiceStatus> statuses
    );
}
