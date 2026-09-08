package com.buildflow.commercial.repository;

import com.buildflow.commercial.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceItemRepository
    extends JpaRepository<
        InvoiceItem,
        UUID
    > {

    List<InvoiceItem>
        findByInvoiceIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID invoiceId,
            UUID organizationId
        );

    Optional<InvoiceItem>
        findByIdAndInvoiceIdAndOrganizationId(
            UUID id,
            UUID invoiceId,
            UUID organizationId
        );

    long countByInvoiceIdAndOrganizationId(
        UUID invoiceId,
        UUID organizationId
    );
}
