package com.buildflow.notification.service;

import com.buildflow.audit.aspect.TrackedActionEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TrackedActionNotificationListener {

    private final NotificationService
        notificationService;

    public TrackedActionNotificationListener(
        NotificationService
            notificationService
    ) {
        this.notificationService =
            notificationService;
    }

    @EventListener
    public void onAction(
        TrackedActionEvent event
    ) {
        if (event.projectId() == null) {
            return;
        }

        switch (event.action()) {
            case "INCIDENT_CREATED" -> {
                if (
                    event.newValues() != null
                    && event.newValues()
                        .contains(
                            "severity=CRITICAL"
                        )
                ) {
                    notificationService
                        .notifyProjectStakeholders(
                            event.organizationId(),
                            event.projectId(),
                            "CRITICAL_INCIDENT",
                            "Critical incident reported",
                            "A critical site incident was reported.",
                            event.entityType(),
                            event.entityId(),
                            "critical-incident:"
                                + event.entityId()
                        );
                }
            }

            case "CLIENT_CHANGE_ORDER_APPROVED" ->
                notificationService
                    .notifyProjectStakeholders(
                        event.organizationId(),
                        event.projectId(),
                        "CHANGE_ORDER_APPROVED",
                        "Client approved change order",
                        "A client approved a project change order.",
                        event.entityType(),
                        event.entityId(),
                        "client-co-approved:"
                            + event.entityId()
                    );

            case "CLIENT_CHANGE_ORDER_REJECTED" ->
                notificationService
                    .notifyProjectStakeholders(
                        event.organizationId(),
                        event.projectId(),
                        "CHANGE_ORDER_REJECTED",
                        "Client rejected change order",
                        "A client rejected a project change order.",
                        event.entityType(),
                        event.entityId(),
                        "client-co-rejected:"
                            + event.entityId()
                    );

            case "INVOICE_ISSUED" ->
                notificationService
                    .notifyProjectStakeholders(
                        event.organizationId(),
                        event.projectId(),
                        "INVOICE_ISSUED",
                        "Invoice issued",
                        "A project invoice was issued.",
                        event.entityType(),
                        event.entityId(),
                        "invoice-issued:"
                            + event.entityId()
                    );

            case "TASK_ASSIGNED" ->
                notificationService
                    .notifyProjectStakeholders(
                        event.organizationId(),
                        event.projectId(),
                        "TASK_ASSIGNED",
                        "Task assignment updated",
                        "A project task assignment changed.",
                        event.entityType(),
                        event.entityId(),
                        "task-assigned:"
                            + event.entityId()
                    );

            default -> {
            }
        }
    }
}
