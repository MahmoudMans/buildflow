package com.buildflow.supplier.repository;

import com.buildflow.supplier.entity.Supplier;
import com.buildflow.supplier.entity.SupplierStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository
    extends JpaRepository<
        Supplier,
        UUID
    > {

    List<Supplier>
        findByOrganizationIdOrderByNameAsc(
            UUID organizationId
        );

    List<Supplier>
        findByOrganizationIdAndStatusOrderByNameAsc(
            UUID organizationId,
            SupplierStatus status
        );

    Optional<Supplier>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );
}
