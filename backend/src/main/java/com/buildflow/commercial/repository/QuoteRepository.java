package com.buildflow.commercial.repository;

import com.buildflow.commercial.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository
    extends JpaRepository<
        Quote,
        UUID
    > {

    List<Quote>
        findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID projectId,
            UUID organizationId
        );

    Optional<Quote>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );
}
