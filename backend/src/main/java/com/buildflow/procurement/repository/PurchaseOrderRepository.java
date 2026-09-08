package com.buildflow.procurement.repository;

import com.buildflow.procurement.entity.PurchaseOrder;
import com.buildflow.procurement.entity.PurchaseOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseOrderRepository
    extends JpaRepository<
        PurchaseOrder,
        UUID
    > {

    List<PurchaseOrder>
        findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID projectId,
            UUID organizationId
        );

    Optional<PurchaseOrder>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    Optional<PurchaseOrder>
        findByIdAndProjectIdAndOrganizationId(
            UUID id,
            UUID projectId,
            UUID organizationId
        );

    List<PurchaseOrder>
        findByProjectIdAndOrganizationIdAndStatusIn(
            UUID projectId,
            UUID organizationId,
            Collection<PurchaseOrderStatus> statuses
        );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select purchaseOrder
        from PurchaseOrder purchaseOrder
        where purchaseOrder.id = :id
          and purchaseOrder.organizationId = :organizationId
        """)
    Optional<PurchaseOrder> findForUpdate(
        @Param("id")
        UUID id,
        @Param("organizationId")
        UUID organizationId
    );
}
