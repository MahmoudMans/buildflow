package com.buildflow.budget.entity;

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
@Table(name = "budget_items")
public class BudgetItem {

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
        name = "budget_category_id",
        nullable = false
    )
    private UUID budgetCategoryId;

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
        name = "unit_cost",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal unitCost;

    @Column(
        name = "planned_amount",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal plannedAmount;

    private String notes;

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

    protected BudgetItem() {
    }

    public static BudgetItem create(
        UUID organizationId,
        UUID projectId,
        UUID budgetCategoryId,
        String description,
        BigDecimal quantity,
        BigDecimal unitCost,
        String notes,
        Instant now
    ) {
        validateMoneyInputs(
            quantity,
            unitCost
        );

        BudgetItem item =
            new BudgetItem();

        item.id =
            UUID.randomUUID();

        item.organizationId =
            organizationId;

        item.projectId =
            projectId;

        item.budgetCategoryId =
            budgetCategoryId;

        item.description =
            description;

        item.quantity =
            money(quantity);

        item.unitCost =
            money(unitCost);

        item.plannedAmount =
            calculate(
                quantity,
                unitCost
            );

        item.notes =
            notes;

        item.createdAt =
            now;

        item.updatedAt =
            now;

        return item;
    }

    public void update(
        String description,
        BigDecimal quantity,
        BigDecimal unitCost,
        String notes,
        Instant now
    ) {
        BigDecimal resultingQuantity =
            quantity == null
                ? this.quantity
                : quantity;

        BigDecimal resultingUnitCost =
            unitCost == null
                ? this.unitCost
                : unitCost;

        validateMoneyInputs(
            resultingQuantity,
            resultingUnitCost
        );

        if (description != null) {
            this.description =
                description;
        }

        if (quantity != null) {
            this.quantity =
                money(quantity);
        }

        if (unitCost != null) {
            this.unitCost =
                money(unitCost);
        }

        if (notes != null) {
            this.notes =
                notes;
        }

        this.plannedAmount =
            calculate(
                resultingQuantity,
                resultingUnitCost
            );

        this.updatedAt =
            now;
    }

    private static void validateMoneyInputs(
        BigDecimal quantity,
        BigDecimal unitCost
    ) {
        if (
            quantity == null
            || quantity.signum() < 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_BUDGET_QUANTITY",
                "Budget quantity cannot be negative."
            );
        }

        if (
            unitCost == null
            || unitCost.signum() < 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_BUDGET_UNIT_COST",
                "Budget unit cost cannot be negative."
            );
        }
    }

    private static BigDecimal calculate(
        BigDecimal quantity,
        BigDecimal unitCost
    ) {
        return quantity
            .multiply(unitCost)
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

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getBudgetCategoryId() {
        return budgetCategoryId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
