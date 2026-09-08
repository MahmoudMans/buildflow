package com.buildflow.client.repository;

import com.buildflow.client.entity.ClientContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientContactRepository
    extends JpaRepository<
        ClientContact,
        UUID
    > {

    List<ClientContact>
        findByClientIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID clientId,
            UUID organizationId
        );

    Optional<ClientContact>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    long countByClientIdAndOrganizationId(
        UUID clientId,
        UUID organizationId
    );

    @Modifying
    @Query("""
        update ClientContact contact
        set contact.primary = false
        where contact.clientId = :clientId
          and contact.organizationId = :organizationId
          and contact.primary = true
        """)
    int clearPrimaryContact(
        @Param("clientId")
        UUID clientId,
        @Param("organizationId")
        UUID organizationId
    );
}
