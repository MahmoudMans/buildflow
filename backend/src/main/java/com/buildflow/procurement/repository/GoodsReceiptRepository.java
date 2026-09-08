package com.buildflow.procurement.repository;

import com.buildflow.procurement.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoodsReceiptRepository
    extends JpaRepository<
        GoodsReceipt,
        UUID
    > {

    List<GoodsReceipt>
        findByPurchaseOrderIdAndOrganizationIdOrderByReceivedDateDescCreatedAtDesc(
            UUID purchaseOrderId,
            UUID organizationId
        );

    boolean existsByPurchaseOrderIdAndOrganizationId(
        UUID purchaseOrderId,
        UUID organizationId
    );
}
