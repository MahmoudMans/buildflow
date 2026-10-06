package com.buildflow.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class ReceivablesDtos {

    private ReceivablesDtos() {
    }

    /**
     * Unpaid invoices grouped by how long they are past their due date.
     * Keys: CURRENT, DAYS_1_30, DAYS_31_60, DAYS_61_90, DAYS_90_PLUS.
     */
    public record AgingBucket(
        String key,
        String label,
        long invoices,
        BigDecimal amount
    ) {
    }

    /**
     * Outstanding amount that falls due in one calendar month ("YYYY-MM").
     */
    public record ExpectedCollection(
        String month,
        long invoices,
        BigDecimal amount
    ) {
    }

    public record ProjectReceivable(
        UUID projectId,
        String projectNumber,
        String projectName,
        long openInvoices,
        BigDecimal outstanding,
        BigDecimal overdue,
        long oldestOverdueDays
    ) {
    }

    public record ReceivablesSummary(
        boolean available,
        BigDecimal totalOutstanding,
        BigDecimal totalOverdue,
        long openInvoices,
        long overdueInvoices,
        BigDecimal overduePercentage,
        List<AgingBucket> aging,
        List<ExpectedCollection> expected,
        BigDecimal laterAmount,
        List<ProjectReceivable> projects
    ) {
    }
}
