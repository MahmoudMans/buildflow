package com.buildflow.procurement.repository;

import com.buildflow.procurement.entity.GoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GoodsReceiptItemRepository
    extends JpaRepository<
        GoodsReceiptItem,
        UUID
    > {

    List<GoodsReceiptItem>
        findByGoodsReceiptId(
            UUID goodsReceiptId
        );

    @Query("""
        select sum(receiptItem.quantityReceived)
        from GoodsReceiptItem receiptItem
        where receiptItem.purchaseOrderItemId = :purchaseOrderItemId
          and receiptItem.organizationId = :organizationId
        """)
    BigDecimal sumReceivedQuantity(
        @Param("purchaseOrderItemId")
        UUID purchaseOrderItemId,
        @Param("organizationId")
        UUID organizationId
    );
}
