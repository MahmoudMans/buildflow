package com.buildflow.commercial.entity;

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
@Table(name = "invoices")
public class Invoice {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "source_quote_id")
    private UUID sourceQuoteId;

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountPaid;

    @Column(name = "amount_outstanding", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountOutstanding;

    private String notes;

    @Column(name = "payment_instructions")
    private String paymentInstructions;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason")
    private String voidReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Invoice() {
    }

    public static Invoice create(
        UUID organizationId,
        UUID projectId,
        UUID clientId,
        UUID sourceQuoteId,
        String invoiceNumber,
        LocalDate issueDate,
        LocalDate dueDate,
        String notes,
        String paymentInstructions,
        Instant now
    ) {
        validateDates(
            issueDate,
            dueDate
        );

        Invoice invoice = new Invoice();

        invoice.id = UUID.randomUUID();
        invoice.organizationId = organizationId;
        invoice.projectId = projectId;
        invoice.clientId = clientId;
        invoice.sourceQuoteId = sourceQuoteId;
        invoice.invoiceNumber = invoiceNumber;
        invoice.status = InvoiceStatus.DRAFT;
        invoice.issueDate = issueDate;
        invoice.dueDate = dueDate;
        invoice.subtotal = zero();
        invoice.taxAmount = zero();
        invoice.total = zero();
        invoice.amountPaid = zero();
        invoice.amountOutstanding = zero();
        invoice.notes = notes;
        invoice.paymentInstructions = paymentInstructions;
        invoice.createdAt = now;
        invoice.updatedAt = now;

        return invoice;
    }

    public void updateDraft(
        LocalDate issueDate,
        LocalDate dueDate,
        String notes,
        String paymentInstructions,
        Instant now
    ) {
        requireFinanciallyEditable();

        LocalDate resultingIssue =
            issueDate == null
                ? this.issueDate
                : issueDate;

        LocalDate resultingDue =
            dueDate == null
                ? this.dueDate
                : dueDate;

        validateDates(
            resultingIssue,
            resultingDue
        );

        if (issueDate != null) {
            this.issueDate = issueDate;
        }

        if (dueDate != null) {
            this.dueDate = dueDate;
        }

        if (notes != null) {
            this.notes = notes;
        }

        if (paymentInstructions != null) {
            this.paymentInstructions =
                paymentInstructions;
        }

        updatedAt = now;
    }

    public void updateTotals(
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        Instant now
    ) {
        requireFinanciallyEditable();

        this.subtotal = money(subtotal);
        this.taxAmount = money(taxAmount);
        this.total = money(total);
        this.amountOutstanding =
            money(
                this.total.subtract(
                    amountPaid
                )
            );

        updatedAt = now;
    }

    public void issue(
        Instant now
    ) {
        requireStatus(
            InvoiceStatus.DRAFT,
            "Only a draft invoice can be issued."
        );

        if (total.signum() <= 0) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "INVOICE_EMPTY",
                "An invoice must contain at least one positive-value line before issue."
            );
        }

        amountOutstanding = total;
        status = InvoiceStatus.ISSUED;
        issuedAt = now;
        updatedAt = now;
    }

    public void applyPayment(
        BigDecimal amount,
        Instant now
    ) {
        if (
            status == InvoiceStatus.DRAFT
            || status == InvoiceStatus.VOID
            || status == InvoiceStatus.PAID
        ) {
            throw invalidState(
                "Payments cannot be recorded against this invoice."
            );
        }

        if (
            amount == null
            || amount.signum() <= 0
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PAYMENT_AMOUNT",
                "Payment amount must be greater than zero."
            );
        }

        BigDecimal normalized =
            money(amount);

        if (
            normalized.compareTo(
                amountOutstanding
            ) > 0
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PAYMENT_EXCEEDS_OUTSTANDING",
                "Payment cannot exceed the invoice outstanding amount."
            );
        }

        amountPaid =
            money(
                amountPaid.add(
                    normalized
                )
            );

        amountOutstanding =
            money(
                total.subtract(
                    amountPaid
                )
            );

        if (
            amountOutstanding.signum() == 0
        ) {
            status = InvoiceStatus.PAID;
        } else if (
            status != InvoiceStatus.OVERDUE
        ) {
            status =
                InvoiceStatus.PARTIALLY_PAID;
        }

        updatedAt = now;
    }

    public void markOverdue(
        LocalDate today,
        Instant now
    ) {
        if (
            status != InvoiceStatus.ISSUED
            && status
                != InvoiceStatus.PARTIALLY_PAID
        ) {
            throw invalidState(
                "Only an issued or partially paid invoice can become overdue."
            );
        }

        if (
            !dueDate.isBefore(today)
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "INVOICE_NOT_OVERDUE",
                "The invoice due date has not passed."
            );
        }

        status = InvoiceStatus.OVERDUE;
        updatedAt = now;
    }

    public void voidInvoice(
        String reason,
        Instant now
    ) {
        if (
            status != InvoiceStatus.ISSUED
            && status != InvoiceStatus.OVERDUE
        ) {
            throw invalidState(
                "Only an unpaid issued invoice can be voided."
            );
        }

        if (
            amountPaid.signum() != 0
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PAID_INVOICE_CANNOT_BE_VOIDED",
                "An invoice with payments cannot be voided."
            );
        }

        if (
            reason == null
            || reason.isBlank()
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVOICE_VOID_REASON_REQUIRED",
                "A reason is required to void an invoice."
            );
        }

        status = InvoiceStatus.VOID;
        amountOutstanding = zero();
        voidReason = reason.trim();
        voidedAt = now;
        updatedAt = now;
    }

    public void requireFinanciallyEditable() {
        if (status != InvoiceStatus.DRAFT) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "INVOICE_FINANCIAL_CONTENT_IMMUTABLE",
                "Only a draft invoice can be financially modified."
            );
        }
    }

    private void requireStatus(
        InvoiceStatus expected,
        String message
    ) {
        if (status != expected) {
            throw invalidState(message);
        }
    }

    private BusinessException invalidState(
        String message
    ) {
        return new BusinessException(
            HttpStatus.CONFLICT,
            "INVALID_INVOICE_STATE",
            message
        );
    }

    private static void validateDates(
        LocalDate issueDate,
        LocalDate dueDate
    ) {
        if (
            issueDate == null
            || dueDate == null
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVOICE_DATES_REQUIRED",
                "Invoice issue date and due date are required."
            );
        }

        if (
            dueDate.isBefore(issueDate)
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_INVOICE_DATES",
                "Invoice due date cannot be before its issue date."
            );
        }
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(4);
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

    public UUID getClientId() {
        return clientId;
    }

    public UUID getSourceQuoteId() {
        return sourceQuoteId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
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

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getAmountOutstanding() {
        return amountOutstanding;
    }

    public String getNotes() {
        return notes;
    }

    public String getPaymentInstructions() {
        return paymentInstructions;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
