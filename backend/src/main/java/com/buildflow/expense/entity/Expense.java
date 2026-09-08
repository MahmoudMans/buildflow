package com.buildflow.expense.entity;

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
@Table(name = "expenses")
public class Expense {

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

    @Column(name = "supplier_id")
    private UUID supplierId;

    @Column(name = "purchase_order_id")
    private UUID purchaseOrderId;

    @Column(name = "receipt_document_id")
    private UUID receiptDocumentId;

    @Column(
        name = "expense_number",
        nullable = false,
        length = 50
    )
    private String expenseNumber;

    @Column(
        nullable = false,
        length = 500
    )
    private String description;

    @Column(
        name = "expense_date",
        nullable = false
    )
    private LocalDate expenseDate;

    @Column(
        name = "net_amount",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal netAmount;

    @Column(
        name = "tax_amount",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal taxAmount;

    @Column(
        name = "total_amount",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseStatus status;

    @Column(
        name = "created_by",
        nullable = false
    )
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "voided_by")
    private UUID voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

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

    protected Expense() {
    }

    public static Expense create(
        UUID organizationId,
        UUID projectId,
        UUID budgetCategoryId,
        String expenseNumber,
        String description,
        LocalDate expenseDate,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        UUID createdBy,
        Instant now
    ) {
        validateAmounts(
            netAmount,
            taxAmount
        );

        Expense expense =
            new Expense();

        expense.id =
            UUID.randomUUID();

        expense.organizationId =
            organizationId;

        expense.projectId =
            projectId;

        expense.budgetCategoryId =
            budgetCategoryId;

        expense.expenseNumber =
            expenseNumber;

        expense.description =
            description;

        expense.expenseDate =
            expenseDate;

        expense.netAmount =
            money(netAmount);

        expense.taxAmount =
            money(taxAmount);

        expense.totalAmount =
            total(
                netAmount,
                taxAmount
            );

        expense.status =
            ExpenseStatus.DRAFT;

        expense.createdBy =
            createdBy;

        expense.createdAt =
            now;

        expense.updatedAt =
            now;

        return expense;
    }

    public void updateDraft(
        UUID budgetCategoryId,
        String description,
        LocalDate expenseDate,
        BigDecimal netAmount,
        BigDecimal taxAmount,
        Instant now
    ) {
        requireStatus(
            ExpenseStatus.DRAFT,
            "Only a draft expense can be edited."
        );

        BigDecimal resultingNet =
            netAmount == null
                ? this.netAmount
                : netAmount;

        BigDecimal resultingTax =
            taxAmount == null
                ? this.taxAmount
                : taxAmount;

        validateAmounts(
            resultingNet,
            resultingTax
        );

        if (
            budgetCategoryId != null
        ) {
            this.budgetCategoryId =
                budgetCategoryId;
        }

        if (description != null) {
            this.description =
                description;
        }

        if (expenseDate != null) {
            this.expenseDate =
                expenseDate;
        }

        if (netAmount != null) {
            this.netAmount =
                money(netAmount);
        }

        if (taxAmount != null) {
            this.taxAmount =
                money(taxAmount);
        }

        this.totalAmount =
            total(
                resultingNet,
                resultingTax
            );

        this.updatedAt =
            now;
    }

    public void submit(
        Instant now
    ) {
        requireStatus(
            ExpenseStatus.DRAFT,
            "Only a draft expense can be submitted."
        );

        status =
            ExpenseStatus.SUBMITTED;

        updatedAt =
            now;
    }

    public void approve(
        UUID approverUserId,
        Instant now
    ) {
        requireStatus(
            ExpenseStatus.SUBMITTED,
            "Only a submitted expense can be approved."
        );

        status =
            ExpenseStatus.APPROVED;

        approvedBy =
            approverUserId;

        approvedAt =
            now;

        rejectionReason =
            null;

        updatedAt =
            now;
    }

    public void reject(
        String reason,
        Instant now
    ) {
        requireStatus(
            ExpenseStatus.SUBMITTED,
            "Only a submitted expense can be rejected."
        );

        status =
            ExpenseStatus.REJECTED;

        rejectionReason =
            reason;

        updatedAt =
            now;
    }

    public void voidExpense(
        String reason,
        UUID voidedByUserId,
        Instant now
    ) {
        requireStatus(
            ExpenseStatus.APPROVED,
            "Only an approved expense can be voided."
        );

        if (
            reason == null
            || reason.isBlank()
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "VOID_REASON_REQUIRED",
                "A reason is required when voiding an approved expense."
            );
        }

        status =
            ExpenseStatus.VOID;

        voidReason =
            reason.trim();

        voidedBy =
            voidedByUserId;

        voidedAt =
            now;

        updatedAt =
            now;
    }

    private void requireStatus(
        ExpenseStatus expected,
        String message
    ) {
        if (
            status != expected
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "INVALID_EXPENSE_STATE",
                message
            );
        }
    }

    private static void validateAmounts(
        BigDecimal netAmount,
        BigDecimal taxAmount
    ) {
        if (
            netAmount == null
            || netAmount.signum() < 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_EXPENSE_NET_AMOUNT",
                "Expense net amount cannot be negative."
            );
        }

        if (
            taxAmount == null
            || taxAmount.signum() < 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_EXPENSE_TAX_AMOUNT",
                "Expense tax amount cannot be negative."
            );
        }
    }

    private static BigDecimal total(
        BigDecimal netAmount,
        BigDecimal taxAmount
    ) {
        return netAmount
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

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getBudgetCategoryId() {
        return budgetCategoryId;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public UUID getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public UUID getReceiptDocumentId() {
        return receiptDocumentId;
    }

    public String getExpenseNumber() {
        return expenseNumber;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public ExpenseStatus getStatus() {
        return status;
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

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public UUID getVoidedBy() {
        return voidedBy;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
