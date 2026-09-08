package com.buildflow.procurement.dto;

import com.buildflow.procurement.entity.GoodsReceipt;
import com.buildflow.procurement.entity.GoodsReceiptItem;
import com.buildflow.procurement.entity.PurchaseOrder;
import com.buildflow.procurement.entity.PurchaseOrderItem;
import com.buildflow.procurement.entity.PurchaseOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ProcurementDtos {

    private ProcurementDtos() {
    }

    public record CreatePurchaseOrderRequest(

        @NotNull
        UUID supplierId,

        LocalDate issueDate,

        LocalDate expectedDeliveryDate,

        @Size(max = 5000)
        String notes

    ) {
    }

    public record UpdatePurchaseOrderRequest(

        UUID supplierId,

        LocalDate issueDate,

        LocalDate expectedDeliveryDate,

        @Size(max = 5000)
        String notes

    ) {
    }

    public record CreateItemRequest(

        @NotBlank
        @Size(max = 500)
        String description,

        @NotNull
        @DecimalMin("0.0001")
        BigDecimal quantity,

        @NotBlank
        @Size(max = 50)
        String unit,

        @NotNull
        @DecimalMin("0.0000")
        BigDecimal unitPrice,

        @NotNull
        @DecimalMin("0.0000")
        @DecimalMax("100.0000")
        BigDecimal taxRate

    ) {
    }

    public record UpdateItemRequest(

        @Size(
            min = 1,
            max = 500
        )
        String description,

        @DecimalMin("0.0001")
        BigDecimal quantity,

        @Size(
            min = 1,
            max = 50
        )
        String unit,

        @DecimalMin("0.0000")
        BigDecimal unitPrice,

        @DecimalMin("0.0000")
        @DecimalMax("100.0000")
        BigDecimal taxRate

    ) {
    }

    public record CancelPurchaseOrderRequest(

        @NotBlank
        @Size(max = 5000)
        String reason

    ) {
    }

    public record ReceiptItemRequest(

        @NotNull
        UUID purchaseOrderItemId,

        @NotNull
        @DecimalMin("0.0001")
        BigDecimal quantityReceived

    ) {
    }

    public record CreateGoodsReceiptRequest(

        LocalDate receivedDate,

        @Size(max = 5000)
        String notes,

        @NotEmpty
        List<
            @Valid
            ReceiptItemRequest
        > items

    ) {
    }

    public record PurchaseOrderItemView(
        UUID id,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineSubtotal,
        BigDecimal taxAmount,
        BigDecimal lineTotal,
        BigDecimal quantityReceived,
        BigDecimal quantityRemaining
    ) {
    }

    public record GoodsReceiptItemView(
        UUID purchaseOrderItemId,
        String description,
        BigDecimal quantityReceived
    ) {
    }

    public record GoodsReceiptView(
        UUID id,
        LocalDate receivedDate,
        UUID receivedByMembershipId,
        String notes,
        Instant createdAt,
        List<GoodsReceiptItemView> items
    ) {
    }

    public record PurchaseOrderSummary(
        UUID id,
        String purchaseOrderNumber,
        UUID supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        LocalDate issueDate,
        LocalDate expectedDeliveryDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        Instant createdAt
    ) {
    }

    public record PurchaseOrderView(
        UUID id,
        UUID projectId,
        UUID supplierId,
        String supplierName,
        String purchaseOrderNumber,
        PurchaseOrderStatus status,
        LocalDate issueDate,
        LocalDate expectedDeliveryDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        String notes,
        UUID createdBy,
        UUID approvedBy,
        Instant approvedAt,
        Instant orderedAt,
        Instant cancelledAt,
        String cancellationReason,
        Instant createdAt,
        List<PurchaseOrderItemView> items,
        List<GoodsReceiptView> receipts
    ) {
    }
}
