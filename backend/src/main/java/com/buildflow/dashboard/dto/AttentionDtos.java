package com.buildflow.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AttentionDtos {

    private AttentionDtos() {
    }

    public record AttentionItem(
        String kind,
        String severity,
        UUID entityId,
        UUID projectId,
        String projectNumber,
        String projectName,
        String title,
        String detail,
        LocalDate date,
        BigDecimal amount
    ) {
    }

    public record AttentionSummary(
        boolean financeVisible,
        long overdueTasks,
        long openIncidents,
        long overdueInvoices,
        BigDecimal overdueInvoiceAmount,
        long changeOrdersAwaitingClient,
        long projectsAtRisk,
        List<AttentionItem> items
    ) {
    }
}
