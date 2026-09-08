package com.buildflow.commercial.repository;

import com.buildflow.commercial.entity.QuoteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteItemRepository
    extends JpaRepository<
        QuoteItem,
        UUID
    > {

    List<QuoteItem>
        findByQuotationIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID quotationId,
            UUID organizationId
        );

    Optional<QuoteItem>
        findByIdAndQuotationIdAndOrganizationId(
            UUID id,
            UUID quotationId,
            UUID organizationId
        );

    long countByQuotationIdAndOrganizationId(
        UUID quotationId,
        UUID organizationId
    );
}
