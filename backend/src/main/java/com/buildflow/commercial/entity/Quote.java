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
@Table(name = "quotations")
public class Quote {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "quote_number", nullable = false, length = 50)
    private String quoteNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status;

    @Column(name = "contract_quote", nullable = false)
    private boolean contractQuote;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    private String notes;

    private String terms;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "viewed_at")
    private Instant viewedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "accepted_by")
    private UUID acceptedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Quote() {
    }

    public static Quote create(
        UUID organizationId,
        UUID projectId,
        UUID clientId,
        String quoteNumber,
        LocalDate issueDate,
        LocalDate validUntil,
        String notes,
        String terms,
        boolean contractQuote,
        UUID createdBy,
        Instant now
    ) {
        validateDates(
            issueDate,
            validUntil
        );

        Quote quote = new Quote();

        quote.id = UUID.randomUUID();
        quote.organizationId = organizationId;
        quote.projectId = projectId;
        quote.clientId = clientId;
        quote.quoteNumber = quoteNumber;
        quote.status = QuoteStatus.DRAFT;
        quote.contractQuote = contractQuote;
        quote.issueDate = issueDate;
        quote.validUntil = validUntil;
        quote.subtotal = zero();
        quote.taxAmount = zero();
        quote.total = zero();
        quote.notes = notes;
        quote.terms = terms;
        quote.createdBy = createdBy;
        quote.createdAt = now;
        quote.updatedAt = now;

        return quote;
    }

    public void updateDraft(
        LocalDate issueDate,
        LocalDate validUntil,
        String notes,
        String terms,
        Boolean contractQuote,
        Instant now
    ) {
        requireFinanciallyEditable();

        LocalDate resultingIssue =
            issueDate == null
                ? this.issueDate
                : issueDate;

        LocalDate resultingValid =
            validUntil == null
                ? this.validUntil
                : validUntil;

        validateDates(
            resultingIssue,
            resultingValid
        );

        if (issueDate != null) {
            this.issueDate = issueDate;
        }

        if (validUntil != null) {
            this.validUntil = validUntil;
        }

        if (notes != null) {
            this.notes = notes;
        }

        if (terms != null) {
            this.terms = terms;
        }

        if (contractQuote != null) {
            this.contractQuote = contractQuote;
        }

        this.updatedAt = now;
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
        this.updatedAt = now;
    }

    public void send(
        LocalDate today,
        Instant now
    ) {
        requireStatus(
            QuoteStatus.DRAFT,
            "Only a draft quotation can be sent."
        );

        if (total.signum() <= 0) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "QUOTE_EMPTY",
                "A quotation must contain at least one positive-value line before it can be sent."
            );
        }

        if (
            validUntil != null
            && validUntil.isBefore(today)
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "QUOTE_ALREADY_EXPIRED",
                "An expired quotation cannot be sent."
            );
        }

        status = QuoteStatus.SENT;
        sentAt = now;
        updatedAt = now;
    }

    public void markViewed(
        Instant now
    ) {
        if (status == QuoteStatus.VIEWED) {
            return;
        }

        requireStatus(
            QuoteStatus.SENT,
            "Only a sent quotation can be marked as viewed."
        );

        status = QuoteStatus.VIEWED;
        viewedAt = now;
        updatedAt = now;
    }

    public void accept(
        LocalDate today,
        UUID acceptedBy,
        Instant now
    ) {
        if (
            status != QuoteStatus.SENT
            && status != QuoteStatus.VIEWED
        ) {
            throw invalidState(
                "Only a sent or viewed quotation can be accepted."
            );
        }

        if (
            validUntil != null
            && validUntil.isBefore(today)
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "QUOTE_EXPIRED",
                "An expired quotation cannot be accepted."
            );
        }

        status = QuoteStatus.ACCEPTED;
        this.acceptedBy = acceptedBy;
        acceptedAt = now;
        updatedAt = now;
    }

    public void reject(
        Instant now
    ) {
        if (
            status != QuoteStatus.SENT
            && status != QuoteStatus.VIEWED
        ) {
            throw invalidState(
                "Only a sent or viewed quotation can be rejected."
            );
        }

        status = QuoteStatus.REJECTED;
        rejectedAt = now;
        updatedAt = now;
    }

    public void expire(
        LocalDate today,
        Instant now
    ) {
        if (
            status != QuoteStatus.SENT
            && status != QuoteStatus.VIEWED
        ) {
            throw invalidState(
                "Only a sent or viewed quotation can expire."
            );
        }

        if (
            validUntil == null
            || !validUntil.isBefore(today)
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "QUOTE_NOT_EXPIRED",
                "The quotation validity date has not expired."
            );
        }

        status = QuoteStatus.EXPIRED;
        expiredAt = now;
        updatedAt = now;
    }

    public void cancel(
        String reason,
        Instant now
    ) {
        if (
            status != QuoteStatus.DRAFT
            && status != QuoteStatus.SENT
            && status != QuoteStatus.VIEWED
        ) {
            throw invalidState(
                "This quotation can no longer be cancelled."
            );
        }

        status = QuoteStatus.CANCELLED;
        cancellationReason = reason;
        cancelledAt = now;
        updatedAt = now;
    }

    public void requireFinanciallyEditable() {
        if (status != QuoteStatus.DRAFT) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "QUOTE_FINANCIAL_CONTENT_IMMUTABLE",
                "Only a draft quotation can be financially modified."
            );
        }
    }

    private void requireStatus(
        QuoteStatus expected,
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
            "INVALID_QUOTE_STATE",
            message
        );
    }

    private static void validateDates(
        LocalDate issueDate,
        LocalDate validUntil
    ) {
        if (issueDate == null) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "QUOTE_ISSUE_DATE_REQUIRED",
                "Quotation issue date is required."
            );
        }

        if (
            validUntil != null
            && validUntil.isBefore(issueDate)
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_QUOTE_DATES",
                "Quotation validity date cannot be before its issue date."
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

    public String getQuoteNumber() {
        return quoteNumber;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public boolean isContractQuote() {
        return contractQuote;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getValidUntil() {
        return validUntil;
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

    public String getTerms() {
        return terms;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getViewedAt() {
        return viewedAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public UUID getAcceptedBy() {
        return acceptedBy;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
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
