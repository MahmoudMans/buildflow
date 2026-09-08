package com.buildflow.client.repository;

import com.buildflow.client.entity.Client;
import com.buildflow.client.entity.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository
    extends JpaRepository<Client, UUID> {

    Optional<Client>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    List<Client>
        findByOrganizationIdOrderByCreatedAtDesc(
            UUID organizationId
        );

    List<Client>
        findByOrganizationIdAndStatusOrderByCreatedAtDesc(
            UUID organizationId,
            ClientStatus status
        );
}
