package com.buildflow.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "goods_receipt_items")
public class GoodsReceiptItem {

    @Id
    private UUID id;

    @Column(
        name = "organization_id",
        nullable = false
    )
    private UUID organizationId;

    @Column(
        name = "project_id",
        nullable = false
    )
    private UUID projectId;

    @Column(
        name = "purchase_order_id",
        nullable = false
    )
    private UUID purchaseOrderId;

    @Column(
        name = "goods_receipt_id",
        nullable = false
    )
    private UUID goodsReceiptId;

    @Column(
        name = "purchase_order_item_id",
        nullable = false
    )
    private UUID purchaseOrderItemId;

    @Column(
        name = "quantity_received",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal quantityReceived;

    protected GoodsReceiptItem() {
    }

    public static GoodsReceiptItem create(
        UUID organizationId,
        UUID projectId,
        UUID purchaseOrderId,
        UUID goodsReceiptId,
        UUID purchaseOrderItemId,
        BigDecimal quantityReceived
    ) {
        GoodsReceiptItem item =
            new GoodsReceiptItem();

        item.id =
            UUID.randomUUID();

        item.organizationId =
            organizationId;

        item.projectId =
            projectId;

        item.purchaseOrderId =
            purchaseOrderId;

        item.goodsReceiptId =
            goodsReceiptId;

        item.purchaseOrderItemId =
            purchaseOrderItemId;

        item.quantityReceived =
            quantityReceived;

        return item;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPurchaseOrderItemId() {
        return purchaseOrderItemId;
    }

    public BigDecimal getQuantityReceived() {
        return quantityReceived;
    }
}
