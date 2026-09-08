package com.buildflow.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goods_receipts")
public class GoodsReceipt {

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
        name = "received_date",
        nullable = false
    )
    private LocalDate receivedDate;

    @Column(
        name = "received_by_membership_id",
        nullable = false
    )
    private UUID receivedByMembershipId;

    private String notes;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    protected GoodsReceipt() {
    }

    public static GoodsReceipt create(
        UUID organizationId,
        UUID projectId,
        UUID purchaseOrderId,
        LocalDate receivedDate,
        UUID receivedByMembershipId,
        String notes,
        Instant now
    ) {
        GoodsReceipt receipt =
            new GoodsReceipt();

        receipt.id =
            UUID.randomUUID();

        receipt.organizationId =
            organizationId;

        receipt.projectId =
            projectId;

        receipt.purchaseOrderId =
            purchaseOrderId;

        receipt.receivedDate =
            receivedDate;

        receipt.receivedByMembershipId =
            receivedByMembershipId;

        receipt.notes =
            notes;

        receipt.createdAt =
            now;

        return receipt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public UUID getReceivedByMembershipId() {
        return receivedByMembershipId;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
