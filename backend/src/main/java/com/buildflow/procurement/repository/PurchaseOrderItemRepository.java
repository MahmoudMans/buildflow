package com.buildflow.procurement.repository;

import com.buildflow.procurement.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderItemRepository
    extends JpaRepository<
        PurchaseOrderItem,
        UUID
    > {

    List<PurchaseOrderItem>
        findByPurchaseOrderIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID purchaseOrderId,
            UUID organizationId
        );

    Optional<PurchaseOrderItem>
        findByIdAndPurchaseOrderIdAndOrganizationId(
            UUID id,
            UUID purchaseOrderId,
            UUID organizationId
        );

    long countByPurchaseOrderIdAndOrganizationId(
        UUID purchaseOrderId,
        UUID organizationId
    );
}
