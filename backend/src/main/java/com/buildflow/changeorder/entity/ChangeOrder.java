package com.buildflow.changeorder.entity;

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
import java.util.UUID;

@Entity
@Table(name = "change_orders")
public class ChangeOrder {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(
        name = "change_order_number",
        nullable = false,
        length = 50
    )
    private String changeOrderNumber;

    @Column(nullable = false, length = 250)
    private String title;

    private String description;

    private String reason;

    @Column(
        name = "cost_impact",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal costImpact;

    @Column(
        name = "client_price_impact",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal clientPriceImpact;

    @Column(
        name = "schedule_impact_days",
        nullable = false
    )
    private int scheduleImpactDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeOrderStatus status;

    @Column(
        name = "requested_by",
        nullable = false
    )
    private UUID requestedBy;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "client_review_at")
    private Instant clientReviewAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

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

    protected ChangeOrder() {
    }

    public static ChangeOrder create(
        UUID organizationId,
        UUID projectId,
        String changeOrderNumber,
        String title,
        String description,
        String reason,
        BigDecimal costImpact,
        BigDecimal clientPriceImpact,
        int scheduleImpactDays,
        UUID requestedBy,
        Instant now
    ) {
        ChangeOrder changeOrder =
            new ChangeOrder();

        changeOrder.id =
            UUID.randomUUID();

        changeOrder.organizationId =
            organizationId;

        changeOrder.projectId =
            projectId;

        changeOrder.changeOrderNumber =
            changeOrderNumber;

        changeOrder.title =
            title.trim();

        changeOrder.description =
            clean(description);

        changeOrder.reason =
            clean(reason);

        changeOrder.costImpact =
            money(costImpact);

        changeOrder.clientPriceImpact =
            money(clientPriceImpact);

        changeOrder.scheduleImpactDays =
            scheduleImpactDays;

        changeOrder.status =
            ChangeOrderStatus.DRAFT;

        changeOrder.requestedBy =
            requestedBy;

        changeOrder.createdAt = now;
        changeOrder.updatedAt = now;

        return changeOrder;
    }

    public void updateDraft(
        String title,
        String description,
        String reason,
        BigDecimal costImpact,
        BigDecimal clientPriceImpact,
        Integer scheduleImpactDays,
        Instant now
    ) {
        requireDraft();

        if (title != null) {
            if (title.isBlank()) {
                throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "CHANGE_ORDER_TITLE_REQUIRED",
                    "Change-order title cannot be blank."
                );
            }

            this.title =
                title.trim();
        }

        if (description != null) {
            this.description =
                clean(description);
        }

        if (reason != null) {
            this.reason =
                clean(reason);
        }

        if (costImpact != null) {
            this.costImpact =
                money(costImpact);
        }

        if (clientPriceImpact != null) {
            this.clientPriceImpact =
                money(
                    clientPriceImpact
                );
        }

        if (scheduleImpactDays != null) {
            this.scheduleImpactDays =
                scheduleImpactDays;
        }

        updatedAt = now;
    }

    public void submit(
        Instant now
    ) {
        requireStatus(
            ChangeOrderStatus.DRAFT,
            "Only a draft change order can be submitted."
        );

        status =
            ChangeOrderStatus.SUBMITTED;

        submittedAt = now;
        updatedAt = now;
    }

    public void sendToClientReview(
        Instant now
    ) {
        requireStatus(
            ChangeOrderStatus.SUBMITTED,
            "Only a submitted change order can enter client review."
        );

        status =
            ChangeOrderStatus.CLIENT_REVIEW;

        clientReviewAt = now;
        updatedAt = now;
    }

    public void approve(
        UUID actorId,
        Instant now
    ) {
        requireStatus(
            ChangeOrderStatus.CLIENT_REVIEW,
            "Only a change order in client review can be approved."
        );

        status =
            ChangeOrderStatus.APPROVED;

        approvedBy = actorId;
        approvedAt = now;
        updatedAt = now;
    }

    public void reject(
        UUID actorId,
        Instant now
    ) {
        requireStatus(
            ChangeOrderStatus.CLIENT_REVIEW,
            "Only a change order in client review can be rejected."
        );

        status =
            ChangeOrderStatus.REJECTED;

        rejectedBy = actorId;
        rejectedAt = now;
        updatedAt = now;
    }

    public void cancel(
        UUID actorId,
        String cancellationReason,
        Instant now
    ) {
        if (
            status != ChangeOrderStatus.DRAFT
            && status
                != ChangeOrderStatus.SUBMITTED
            && status
                != ChangeOrderStatus.CLIENT_REVIEW
        ) {
            throw invalidState(
                "This change order can no longer be cancelled."
            );
        }

        if (
            cancellationReason == null
            || cancellationReason.isBlank()
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "CHANGE_ORDER_CANCELLATION_REASON_REQUIRED",
                "A cancellation reason is required."
            );
        }

        status =
            ChangeOrderStatus.CANCELLED;

        cancelledBy = actorId;
        cancelledAt = now;

        this.cancellationReason =
            cancellationReason.trim();

        updatedAt = now;
    }

    public void requireDraft() {
        if (
            status
                != ChangeOrderStatus.DRAFT
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "CHANGE_ORDER_IMMUTABLE",
                "Only a draft change order can be financially modified."
            );
        }
    }

    private void requireStatus(
        ChangeOrderStatus expected,
        String message
    ) {
        if (status != expected) {
            throw invalidState(
                message
            );
        }
    }

    private BusinessException invalidState(
        String message
    ) {
        return new BusinessException(
            HttpStatus.CONFLICT,
            "INVALID_CHANGE_ORDER_STATE",
            message
        );
    }

    private static String clean(
        String value
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private static BigDecimal money(
        BigDecimal value
    ) {
        if (value == null) {
            return BigDecimal.ZERO
                .setScale(4);
        }

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

    public String getChangeOrderNumber() {
        return changeOrderNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getReason() {
        return reason;
    }

    public BigDecimal getCostImpact() {
        return costImpact;
    }

    public BigDecimal getClientPriceImpact() {
        return clientPriceImpact;
    }

    public int getScheduleImpactDays() {
        return scheduleImpactDays;
    }

    public ChangeOrderStatus getStatus() {
        return status;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getClientReviewAt() {
        return clientReviewAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public UUID getRejectedBy() {
        return rejectedBy;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
