package com.buildflow.commercial.dto;

import com.buildflow.commercial.entity.InvoiceStatus;
import com.buildflow.commercial.entity.PaymentMethod;
import com.buildflow.commercial.entity.QuoteStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CommercialDtos {

    private CommercialDtos() {
    }

    public record CreateQuoteRequest(
        LocalDate issueDate,
        LocalDate validUntil,

        @Size(max = 5000)
        String notes,

        @Size(max = 5000)
        String terms,

        boolean contractQuote
    ) {
    }

    public record UpdateQuoteRequest(
        LocalDate issueDate,
        LocalDate validUntil,

        @Size(max = 5000)
        String notes,

        @Size(max = 5000)
        String terms,

        Boolean contractQuote
    ) {
    }

    public record LineRequest(

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

    public record UpdateLineRequest(

        @Size(min = 1, max = 500)
        String description,

        @DecimalMin("0.0001")
        BigDecimal quantity,

        @Size(min = 1, max = 50)
        String unit,

        @DecimalMin("0.0000")
        BigDecimal unitPrice,

        @DecimalMin("0.0000")
        @DecimalMax("100.0000")
        BigDecimal taxRate
    ) {
    }

    public record CancelRequest(

        @NotBlank
        @Size(max = 5000)
        String reason
    ) {
    }

    public record QuoteLineView(
        UUID id,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineSubtotal,
        BigDecimal taxAmount,
        BigDecimal lineTotal
    ) {
    }

    public record QuoteSummary(
        UUID id,
        String quoteNumber,
        QuoteStatus status,
        boolean contractQuote,
        LocalDate issueDate,
        LocalDate validUntil,
        BigDecimal total,
        Instant createdAt
    ) {
    }

    public record QuoteView(
        UUID id,
        UUID projectId,
        UUID clientId,
        String quoteNumber,
        QuoteStatus status,
        boolean contractQuote,
        LocalDate issueDate,
        LocalDate validUntil,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        String notes,
        String terms,
        UUID createdBy,
        Instant sentAt,
        Instant viewedAt,
        Instant acceptedAt,
        UUID acceptedBy,
        Instant rejectedAt,
        Instant expiredAt,
        Instant cancelledAt,
        String cancellationReason,
        Instant createdAt,
        List<QuoteLineView> items
    ) {
    }

    public record CreateInvoiceRequest(
        LocalDate issueDate,

        @NotNull
        LocalDate dueDate,

        @Size(max = 5000)
        String notes,

        @Size(max = 5000)
        String paymentInstructions
    ) {
    }

    public record CreateInvoiceFromQuoteRequest(
        LocalDate issueDate,

        @NotNull
        LocalDate dueDate,

        @Size(max = 5000)
        String notes,

        @Size(max = 5000)
        String paymentInstructions
    ) {
    }

    public record UpdateInvoiceRequest(
        LocalDate issueDate,
        LocalDate dueDate,

        @Size(max = 5000)
        String notes,

        @Size(max = 5000)
        String paymentInstructions
    ) {
    }

    public record VoidInvoiceRequest(

        @NotBlank
        @Size(max = 5000)
        String reason
    ) {
    }

    public record RecordPaymentRequest(
        LocalDate paymentDate,

        @NotNull
        @DecimalMin("0.0001")
        BigDecimal amount,

        @NotNull
        PaymentMethod method,

        @Size(max = 250)
        String reference,

        @Size(max = 5000)
        String notes
    ) {
    }

    public record InvoiceLineView(
        UUID id,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal taxRate,
        BigDecimal lineSubtotal,
        BigDecimal taxAmount,
        BigDecimal lineTotal
    ) {
    }

    public record PaymentView(
        UUID id,
        LocalDate paymentDate,
        BigDecimal amount,
        PaymentMethod method,
        String reference,
        String notes,
        UUID createdBy,
        Instant createdAt
    ) {
    }

    public record InvoiceSummary(
        UUID id,
        String invoiceNumber,
        InvoiceStatus status,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountOutstanding,
        Instant createdAt
    ) {
    }

    public record InvoiceView(
        UUID id,
        UUID projectId,
        UUID clientId,
        UUID sourceQuoteId,
        String invoiceNumber,
        InvoiceStatus status,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        BigDecimal amountPaid,
        BigDecimal amountOutstanding,
        String notes,
        String paymentInstructions,
        Instant issuedAt,
        Instant voidedAt,
        String voidReason,
        Instant createdAt,
        List<InvoiceLineView> items,
        List<PaymentView> payments
    ) {
    }

    public record RevenueSummary(
        UUID projectId,
        BigDecimal originalContractValue,
        BigDecimal approvedClientChangeOrders,
        BigDecimal revisedContractValue,
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal outstandingReceivables,
        BigDecimal forecastCost,
        BigDecimal forecastProfit,
        BigDecimal forecastMarginPercentage
    ) {
    }
}
