package com.buildflow.portal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ClientPortalDtos {

    private ClientPortalDtos() {
    }

    public record LinkClientUserRequest(
        @NotNull
        UUID userId
    ) {
    }

    public record PortalAccessView(
        UUID id,
        UUID clientId,
        UUID membershipId,
        Instant createdAt
    ) {
    }

    public record ProjectSummary(
        UUID id,
        String projectNumber,
        String name,
        String status,
        BigDecimal progressPercentage,
        LocalDate plannedEndDate
    ) {
    }

    public record MilestoneView(
        UUID id,
        String name,
        LocalDate plannedDate,
        String status
    ) {
    }

    public record ProgressUpdateView(
        UUID id,
        BigDecimal progressPercentage,
        String summary,
        String nextSteps,
        Instant createdAt
    ) {
    }

    public record ChangeOrderView(
        UUID id,
        String changeOrderNumber,
        String title,
        String description,
        String reason,
        BigDecimal clientPriceImpact,
        Integer scheduleImpactDays,
        String status,
        Instant updatedAt
    ) {
    }

    public record QuoteView(
        UUID id,
        String quoteNumber,
        String status,
        LocalDate issueDate,
        LocalDate validUntil,
        BigDecimal total
    ) {
    }

    public record InvoiceView(
        UUID id,
        String invoiceNumber,
        String status,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal total,
        BigDecimal amountPaid
    ) {
    }

    public record ProjectView(
        ProjectSummary project,
        List<MilestoneView> upcomingMilestones,
        List<ProgressUpdateView> recentUpdates,
        List<ChangeOrderView> changeOrders,
        List<QuoteView> quotes,
        List<InvoiceView> invoices
    ) {
    }

    public record DecisionRequest(
        @Size(max = 4000)
        String comment
    ) {
    }

    public record DecisionView(
        UUID changeOrderId,
        String decision,
        UUID clientUserId,
        String comment,
        Instant decidedAt
    ) {
    }
}
