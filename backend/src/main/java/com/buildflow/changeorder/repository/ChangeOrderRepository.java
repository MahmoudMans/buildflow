package com.buildflow.changeorder.repository;

import com.buildflow.changeorder.entity.ChangeOrder;
import com.buildflow.changeorder.entity.ChangeOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChangeOrderRepository
    extends JpaRepository<
        ChangeOrder,
        UUID
    > {

    List<ChangeOrder>
        findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID projectId,
            UUID organizationId
        );

    Optional<ChangeOrder>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    @Query("""
        select sum(changeOrder.costImpact)
        from ChangeOrder changeOrder
        where changeOrder.projectId = :projectId
          and changeOrder.organizationId = :organizationId
          and changeOrder.status = :status
        """)
    BigDecimal sumCostImpactForStatus(
        @Param("projectId")
        UUID projectId,
        @Param("organizationId")
        UUID organizationId,
        @Param("status")
        ChangeOrderStatus status
    );

    @Query("""
        select sum(changeOrder.clientPriceImpact)
        from ChangeOrder changeOrder
        where changeOrder.projectId = :projectId
          and changeOrder.organizationId = :organizationId
          and changeOrder.status = :status
        """)
    BigDecimal sumClientPriceImpactForStatus(
        @Param("projectId")
        UUID projectId,
        @Param("organizationId")
        UUID organizationId,
        @Param("status")
        ChangeOrderStatus status
    );
}
