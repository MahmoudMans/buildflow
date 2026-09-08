package com.buildflow.procurement.entity;

import com.buildflow.shared.error.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

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
        name = "supplier_id",
        nullable = false
    )
    private UUID supplierId;

    @Column(
        name = "purchase_order_number",
        nullable = false,
        length = 50
    )
    private String purchaseOrderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderStatus status;

    @Column(
        name = "issue_date",
        nullable = false
    )
    private LocalDate issueDate;

    @Column(
        name = "expected_delivery_date"
    )
    private LocalDate expectedDeliveryDate;

    @Column(
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal subtotal;

    @Column(
        name = "tax_amount",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal taxAmount;

    @Column(
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal total;

    private String notes;

    @Column(
        name = "created_by",
        nullable = false
    )
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "ordered_at")
    private Instant orderedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected PurchaseOrder() {
    }

    public static PurchaseOrder create(
        UUID organizationId,
        UUID projectId,
        UUID supplierId,
        String purchaseOrderNumber,
        LocalDate issueDate,
        LocalDate expectedDeliveryDate,
        String notes,
        UUID createdBy,
        Instant now
    ) {
        validateDates(
            issueDate,
            expectedDeliveryDate
        );

        PurchaseOrder order =
            new PurchaseOrder();

        order.id =
            UUID.randomUUID();

        order.organizationId =
            organizationId;

        order.projectId =
            projectId;

        order.supplierId =
            supplierId;

        order.purchaseOrderNumber =
            purchaseOrderNumber;

        order.status =
            PurchaseOrderStatus.DRAFT;

        order.issueDate =
            issueDate;

        order.expectedDeliveryDate =
            expectedDeliveryDate;

        order.subtotal =
            zero();

        order.taxAmount =
            zero();

        order.total =
            zero();

        order.notes =
            notes;

        order.createdBy =
            createdBy;

        order.createdAt =
            now;

        order.updatedAt =
            now;

        return order;
    }

    public void updateDraft(
        UUID supplierId,
        LocalDate issueDate,
        LocalDate expectedDeliveryDate,
        String notes,
        Instant now
    ) {
        requireDraft();

        LocalDate resultingIssue =
            issueDate == null
                ? this.issueDate
                : issueDate;

        LocalDate resultingExpected =
            expectedDeliveryDate == null
                ? this.expectedDeliveryDate
                : expectedDeliveryDate;

        validateDates(
            resultingIssue,
            resultingExpected
        );

        if (supplierId != null) {
            this.supplierId =
                supplierId;
        }

        if (issueDate != null) {
            this.issueDate =
                issueDate;
        }

        if (expectedDeliveryDate != null) {
            this.expectedDeliveryDate =
                expectedDeliveryDate;
        }

        if (notes != null) {
            this.notes =
                notes;
        }

        this.updatedAt =
            now;
    }

    public void updateTotals(
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        Instant now
    ) {
        requireDraft();

        this.subtotal =
            money(subtotal);

        this.taxAmount =
            money(taxAmount);

        this.total =
            money(total);

        this.updatedAt =
            now;
    }

    public void submit(
        Instant now
    ) {
        requireStatus(
            PurchaseOrderStatus.DRAFT,
            "Only a draft purchase order can be submitted."
        );

        if (
            total.signum() <= 0
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PURCHASE_ORDER_EMPTY",
                "A purchase order must contain at least one positive-value item before submission."
            );
        }

        status =
            PurchaseOrderStatus.PENDING_APPROVAL;

        updatedAt =
            now;
    }

    public void approve(
        UUID approvedBy,
        Instant now
    ) {
        requireStatus(
            PurchaseOrderStatus.PENDING_APPROVAL,
            "Only a purchase order pending approval can be approved."
        );

        status =
            PurchaseOrderStatus.APPROVED;

        this.approvedBy =
            approvedBy;

        approvedAt =
            now;

        updatedAt =
            now;
    }

    public void order(
        Instant now
    ) {
        requireStatus(
            PurchaseOrderStatus.APPROVED,
            "Only an approved purchase order can be ordered."
        );

        status =
            PurchaseOrderStatus.ORDERED;

        orderedAt =
            now;

        updatedAt =
            now;
    }

    public void updateReceiptStatus(
        boolean fullyReceived,
        Instant now
    ) {
        if (
            status != PurchaseOrderStatus.ORDERED
            && status
                != PurchaseOrderStatus.PARTIALLY_RECEIVED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PURCHASE_ORDER_NOT_RECEIVABLE",
                "Goods can only be received against an ordered purchase order."
            );
        }

        status =
            fullyReceived
                ? PurchaseOrderStatus.RECEIVED
                : PurchaseOrderStatus.PARTIALLY_RECEIVED;

        updatedAt =
            now;
    }

    public void cancel(
        String reason,
        Instant now
    ) {
        if (
            status == PurchaseOrderStatus.PARTIALLY_RECEIVED
            || status == PurchaseOrderStatus.RECEIVED
            || status == PurchaseOrderStatus.CANCELLED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PURCHASE_ORDER_CANNOT_BE_CANCELLED",
                "This purchase order can no longer be cancelled."
            );
        }

        if (
            reason == null
            || reason.isBlank()
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "PURCHASE_ORDER_CANCELLATION_REASON_REQUIRED",
                "A cancellation reason is required."
            );
        }

        status =
            PurchaseOrderStatus.CANCELLED;

        cancellationReason =
            reason.trim();

        cancelledAt =
            now;

        updatedAt =
            now;
    }

    public void requireDraft() {
        requireStatus(
            PurchaseOrderStatus.DRAFT,
            "Only a draft purchase order can be modified."
        );
    }

    private void requireStatus(
        PurchaseOrderStatus expected,
        String message
    ) {
        if (status != expected) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "INVALID_PURCHASE_ORDER_STATE",
                message
            );
        }
    }

    private static void validateDates(
        LocalDate issueDate,
        LocalDate expectedDeliveryDate
    ) {
        if (issueDate == null) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "PURCHASE_ORDER_ISSUE_DATE_REQUIRED",
                "Purchase order issue date is required."
            );
        }

        if (
            expectedDeliveryDate != null
            && expectedDeliveryDate
                .isBefore(issueDate)
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PURCHASE_ORDER_DATES",
                "Expected delivery date cannot be before the issue date."
            );
        }
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO
            .setScale(4);
    }

    private static BigDecimal money(
        BigDecimal value
    ) {
        return value.setScale(
            4,
            RoundingMode.HALF_UP
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public String getPurchaseOrderNumber() {
        return purchaseOrderNumber;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getNotes() {
        return notes;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getOrderedAt() {
        return orderedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
