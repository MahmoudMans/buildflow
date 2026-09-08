package com.buildflow.procurement.entity;

import com.buildflow.shared.error.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {

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
        nullable = false,
        length = 500
    )
    private String description;

    @Column(
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal quantity;

    @Column(
        nullable = false,
        length = 50
    )
    private String unit;

    @Column(
        name = "unit_price",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal unitPrice;

    @Column(
        name = "tax_rate",
        nullable = false,
        precision = 7,
        scale = 4
    )
    private BigDecimal taxRate;

    @Column(
        name = "line_subtotal",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal lineSubtotal;

    @Column(
        name = "tax_amount",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal taxAmount;

    @Column(
        name = "line_total",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal lineTotal;

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

    protected PurchaseOrderItem() {
    }

    public static PurchaseOrderItem create(
        UUID organizationId,
        UUID projectId,
        UUID purchaseOrderId,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        Instant now
    ) {
        PurchaseOrderItem item =
            new PurchaseOrderItem();

        item.id =
            UUID.randomUUID();

        item.organizationId =
            organizationId;

        item.projectId =
            projectId;

        item.purchaseOrderId =
            purchaseOrderId;

        item.description =
            description;

        item.quantity =
            quantity;

        item.unit =
            unit;

        item.unitPrice =
            unitPrice;

        item.taxRate =
            taxRate;

        item.recalculate();

        item.createdAt =
            now;

        item.updatedAt =
            now;

        return item;
    }

    public void update(
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        Instant now
    ) {
        if (description != null) {
            this.description =
                description;
        }

        if (quantity != null) {
            this.quantity =
                quantity;
        }

        if (unit != null) {
            this.unit =
                unit;
        }

        if (unitPrice != null) {
            this.unitPrice =
                unitPrice;
        }

        if (taxRate != null) {
            this.taxRate =
                taxRate;
        }

        recalculate();

        updatedAt =
            now;
    }

    private void recalculate() {
        validate();

        quantity =
            money(quantity);

        unitPrice =
            money(unitPrice);

        taxRate =
            taxRate.setScale(
                4,
                RoundingMode.HALF_UP
            );

        lineSubtotal =
            quantity
                .multiply(unitPrice)
                .setScale(
                    4,
                    RoundingMode.HALF_UP
                );

        taxAmount =
            lineSubtotal
                .multiply(taxRate)
                .divide(
                    BigDecimal.valueOf(100),
                    4,
                    RoundingMode.HALF_UP
                );

        lineTotal =
            lineSubtotal
                .add(taxAmount)
                .setScale(
                    4,
                    RoundingMode.HALF_UP
                );
    }

    private void validate() {
        if (
            quantity == null
            || quantity.signum() <= 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PURCHASE_ORDER_QUANTITY",
                "Purchase order item quantity must be greater than zero."
            );
        }

        if (
            unit == null
            || unit.isBlank()
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "PURCHASE_ORDER_UNIT_REQUIRED",
                "Purchase order item unit is required."
            );
        }

        if (
            unitPrice == null
            || unitPrice.signum() < 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PURCHASE_ORDER_UNIT_PRICE",
                "Purchase order unit price cannot be negative."
            );
        }

        if (
            taxRate == null
            || taxRate.signum() < 0
            || taxRate.compareTo(
                BigDecimal.valueOf(100)
            ) > 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PURCHASE_ORDER_TAX_RATE",
                "Purchase order tax rate must be between 0 and 100."
            );
        }
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

    public UUID getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getLineSubtotal() {
        return lineSubtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
