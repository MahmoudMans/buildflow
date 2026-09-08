package com.buildflow.commercial.entity;

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
@Table(name = "quotation_items")
public class QuoteItem {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "quotation_id", nullable = false)
    private UUID quotationId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate;

    @Column(name = "line_subtotal", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineSubtotal;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected QuoteItem() {
    }

    public static QuoteItem create(
        UUID organizationId,
        UUID projectId,
        UUID quotationId,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        Instant now
    ) {
        QuoteItem item = new QuoteItem();

        item.id = UUID.randomUUID();
        item.organizationId = organizationId;
        item.projectId = projectId;
        item.quotationId = quotationId;
        item.description = description;
        item.quantity = quantity;
        item.unit = unit;
        item.unitPrice = unitPrice;
        item.taxRate = taxRate;
        item.recalculate();
        item.createdAt = now;
        item.updatedAt = now;

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
            this.description = description;
        }

        if (quantity != null) {
            this.quantity = quantity;
        }

        if (unit != null) {
            this.unit = unit;
        }

        if (unitPrice != null) {
            this.unitPrice = unitPrice;
        }

        if (taxRate != null) {
            this.taxRate = taxRate;
        }

        recalculate();
        updatedAt = now;
    }

    private void recalculate() {
        if (
            quantity == null
            || quantity.signum() <= 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_QUOTE_QUANTITY",
                "Quotation line quantity must be greater than zero."
            );
        }

        if (
            unit == null
            || unit.isBlank()
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "QUOTE_UNIT_REQUIRED",
                "Quotation line unit is required."
            );
        }

        if (
            unitPrice == null
            || unitPrice.signum() < 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_QUOTE_UNIT_PRICE",
                "Quotation unit price cannot be negative."
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
                "INVALID_QUOTE_TAX_RATE",
                "Quotation tax rate must be between 0 and 100."
            );
        }

        quantity = money(quantity);
        unitPrice = money(unitPrice);

        taxRate = taxRate.setScale(
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

    public UUID getQuotationId() {
        return quotationId;
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
