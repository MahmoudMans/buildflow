package com.buildflow.audit.aspect;

import com.buildflow.audit.service.ActivityAuditRecorder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;

@Aspect
@Component
public class TrackedActionAspect {

    private final ActivityAuditRecorder recorder;

    private final ApplicationEventPublisher
        eventPublisher;

    private final JdbcTemplate jdbcTemplate;

    public TrackedActionAspect(
        ActivityAuditRecorder recorder,
        ApplicationEventPublisher
            eventPublisher,
        JdbcTemplate jdbcTemplate
    ) {
        this.recorder =
            recorder;

        this.eventPublisher =
            eventPublisher;

        this.jdbcTemplate =
            jdbcTemplate;
    }

    @Around(
        "execution(public * com.buildflow..service..*(..))"
            + " && !within(com.buildflow.audit..*)"
            + " && !within(com.buildflow.activity..*)"
            + " && !within(com.buildflow.notification..*)"
    )
    public Object track(
        ProceedingJoinPoint joinPoint
    ) throws Throwable {

        String service =
            joinPoint
                .getTarget()
                .getClass()
                .getSimpleName();

        String method =
            joinPoint
                .getSignature()
                .getName();

        String action =
            action(
                service,
                method
            );

        if (action == null) {
            return joinPoint.proceed();
        }

        Object result =
            joinPoint.proceed();

        Object[] arguments =
            joinPoint.getArgs();

        UUID organizationId =
            uuidArgument(
                arguments,
                0
            );

        UUID actorUserId =
            uuidArgument(
                arguments,
                1
            );

        String entityType =
            entityType(service);

        UUID entityId =
            readUuid(
                result,
                "id",
                "getId",
                "changeOrderId"
            );

        if (entityId == null) {
            entityId =
                lastUuid(arguments);
        }

        UUID projectId =
            readUuid(
                result,
                "projectId",
                "getProjectId"
            );

        if (
            projectId == null
            && service.equals(
                "ProjectService"
            )
        ) {
            projectId =
                entityId;
        }

        if (
            projectId == null
            && method.equals(
                "create"
            )
            && arguments.length >= 3
        ) {
            Object candidate =
                arguments[2];

            if (candidate instanceof UUID id) {
                projectId = id;
            }
        }

        if (
            projectId == null
            && organizationId != null
            && entityId != null
        ) {
            projectId =
                resolveProjectId(
                    service,
                    organizationId,
                    entityId
                );
        }

        if (organizationId == null) {
            return result;
        }

        String newValues =
            summarize(result);

        String message =
            humanize(action);

        recorder.record(
            organizationId,
            projectId,
            actorUserId,
            action,
            entityType,
            entityId,
            null,
            newValues,
            requestIp(),
            message
        );

        eventPublisher.publishEvent(
            new TrackedActionEvent(
                organizationId,
                projectId,
                actorUserId,
                action,
                entityType,
                entityId,
                newValues
            )
        );

        return result;
    }

    private String action(
        String service,
        String method
    ) {
        String lower =
            method.toLowerCase(
                Locale.ROOT
            );

        /*
         * Access/authorization services validate permissions;
         * they are not business mutations.
         *
         * Their method arguments do not necessarily follow the
         * organizationId/userId/entityId convention used by
         * business services, so auditing them would both create
         * false events and misclassify IDs as actorUserId.
         */
        if (
            service.endsWith(
                "AccessService"
            )
            || service.endsWith(
                "AuthorizationService"
            )
            || service.endsWith(
                "PermissionService"
            )
        ) {
            return null;
        }

        if (
            service.contains(
                "DailySiteReport"
            )
        ) {
            return switch (lower) {
                case "create" ->
                    "SITE_REPORT_CREATED";

                case "update" ->
                    "SITE_REPORT_UPDATED";

                default -> null;
            };
        }

        if (
            service.contains(
                "SiteIncident"
            )
        ) {
            return switch (lower) {
                case "create" ->
                    "INCIDENT_CREATED";

                case "update" ->
                    "INCIDENT_UPDATED";

                case "investigate" ->
                    "INCIDENT_INVESTIGATING";

                case "requireaction" ->
                    "INCIDENT_ACTION_REQUIRED";

                case "resolve" ->
                    "INCIDENT_RESOLVED";

                case "close" ->
                    "INCIDENT_CLOSED";

                default -> null;
            };
        }

        if (
            service.contains(
                "ClientChangeOrderDecision"
            )
        ) {
            return switch (lower) {
                case "approve" ->
                    "CLIENT_CHANGE_ORDER_APPROVED";

                case "reject" ->
                    "CLIENT_CHANGE_ORDER_REJECTED";

                default -> null;
            };
        }

        if (
            service.contains(
                "ChangeOrder"
            )
        ) {
            if (
                lower.equals("create")
            ) {
                return "CHANGE_ORDER_CREATED";
            }

            if (
                lower.contains("submit")
            ) {
                return "CHANGE_ORDER_SUBMITTED";
            }

            if (
                lower.contains("review")
            ) {
                return "CHANGE_ORDER_CLIENT_REVIEW";
            }

            if (
                lower.contains("approve")
            ) {
                return "CHANGE_ORDER_APPROVED";
            }

            if (
                lower.contains("reject")
            ) {
                return "CHANGE_ORDER_REJECTED";
            }

            if (
                lower.contains("cancel")
            ) {
                return "CHANGE_ORDER_CANCELLED";
            }

            return null;
        }

        if (
            service.contains(
                "Invoice"
            )
        ) {
            if (
                lower.contains("void")
            ) {
                return "INVOICE_VOIDED";
            }

            if (
                lower.contains("issue")
            ) {
                return "INVOICE_ISSUED";
            }

            if (
                lower.contains("create")
            ) {
                return "INVOICE_CREATED";
            }

            return null;
        }

        if (
            service.contains(
                "Payment"
            )
            && (
                lower.contains("create")
                || lower.contains("record")
            )
        ) {
            return "PAYMENT_RECORDED";
        }

        if (
            service.contains(
                "PurchaseOrder"
            )
        ) {
            if (
                lower.contains("approve")
            ) {
                return "PURCHASE_ORDER_APPROVED";
            }

            if (
                lower.contains("reject")
            ) {
                return "PURCHASE_ORDER_REJECTED";
            }

            if (
                lower.contains("create")
            ) {
                return "PURCHASE_ORDER_CREATED";
            }

            return null;
        }

        if (
            service.contains(
                "Expense"
            )
        ) {
            if (
                lower.contains("approve")
            ) {
                return "EXPENSE_APPROVED";
            }

            if (
                lower.contains("reject")
            ) {
                return "EXPENSE_REJECTED";
            }

            if (
                lower.contains("create")
            ) {
                return "EXPENSE_CREATED";
            }

            return null;
        }

        if (
            service.contains(
                "Budget"
            )
            && (
                lower.contains("create")
                || lower.contains("update")
                || lower.contains("set")
            )
        ) {
            return "PROJECT_BUDGET_CHANGED";
        }

        if (
            service.contains(
                "Task"
            )
        ) {
            if (
                lower.contains("assign")
            ) {
                return "TASK_ASSIGNED";
            }

            if (
                lower.contains("complete")
            ) {
                return "TASK_COMPLETED";
            }

            if (
                lower.contains("status")
                || lower.contains(
                    "transition"
                )
            ) {
                return "TASK_STATUS_CHANGED";
            }

            if (
                lower.equals("create")
            ) {
                return "TASK_CREATED";
            }

            if (
                lower.equals("update")
            ) {
                return "TASK_UPDATED";
            }

            return null;
        }

        if (
            service.contains(
                "Milestone"
            )
        ) {
            if (
                lower.contains("complete")
            ) {
                return "MILESTONE_COMPLETED";
            }

            if (
                lower.equals("create")
            ) {
                return "MILESTONE_CREATED";
            }

            if (
                lower.equals("update")
                || lower.contains("status")
            ) {
                return "MILESTONE_UPDATED";
            }

            return null;
        }

        if (
            service.equals(
                "ProjectService"
            )
        ) {
            if (
                lower.equals("create")
            ) {
                return "PROJECT_CREATED";
            }

            if (
                lower.contains("status")
                || lower.contains(
                    "transition"
                )
            ) {
                return "PROJECT_STATUS_CHANGED";
            }

            if (
                lower.contains("archive")
            ) {
                return "PROJECT_ARCHIVED";
            }

            if (
                lower.equals("update")
            ) {
                return "PROJECT_UPDATED";
            }

            return null;
        }

        if (
            service.contains(
                "ProjectDocument"
            )
            && lower.contains("upload")
        ) {
            return "DOCUMENT_UPLOADED";
        }

        if (
            service.contains(
                "ProjectPhoto"
            )
            && lower.contains("upload")
        ) {
            return "PROJECT_PHOTO_UPLOADED";
        }

        if (
            (
                service.contains(
                    "Membership"
                )
                || service.contains(
                    "Organization"
                )
            )
            && lower.contains("role")
            && (
                lower.startsWith("change")
                || lower.startsWith("update")
                || lower.startsWith("set")
                || lower.startsWith("assign")
            )
        ) {
            return "ROLE_CHANGED";
        }

        return null;
    }

    private String entityType(
        String service
    ) {
        String value =
            service.endsWith("Service")
                ? service.substring(
                    0,
                    service.length() - 7
                )
                : service;

        return value
            .replaceAll(
                "([a-z])([A-Z])",
                "$1_$2"
            )
            .toUpperCase(
                Locale.ROOT
            );
    }

    private String humanize(
        String action
    ) {
        String readable =
            action
                .toLowerCase(
                    Locale.ROOT
                )
                .replace(
                    '_',
                    ' '
                );

        return Character
            .toUpperCase(
                readable.charAt(0)
            )
            + readable.substring(1);
    }

    private UUID uuidArgument(
        Object[] arguments,
        int uuidIndex
    ) {
        int current = 0;

        for (Object argument : arguments) {
            if (
                argument
                    instanceof UUID uuid
            ) {
                if (
                    current
                        == uuidIndex
                ) {
                    return uuid;
                }

                current++;
            }
        }

        return null;
    }

    private UUID lastUuid(
        Object[] arguments
    ) {
        UUID result = null;

        for (Object argument : arguments) {
            if (
                argument
                    instanceof UUID uuid
            ) {
                result = uuid;
            }
        }

        return result;
    }

    private UUID readUuid(
        Object target,
        String... methods
    ) {
        if (target == null) {
            return null;
        }

        for (String method : methods) {
            try {
                Method candidate =
                    target
                        .getClass()
                        .getMethod(method);

                Object result =
                    candidate.invoke(
                        target
                    );

                if (
                    result
                        instanceof UUID uuid
                ) {
                    return uuid;
                }
            } catch (
                ReflectiveOperationException
                    ignored
            ) {
            }
        }

        return null;
    }

    private UUID resolveProjectId(
        String service,
        UUID organizationId,
        UUID entityId
    ) {
        String[] tables;

        if (
            service.contains(
                "ChangeOrder"
            )
        ) {
            tables =
                new String[] {
                    "change_orders"
                };
        } else if (
            service.contains(
                "Invoice"
            )
        ) {
            tables =
                new String[] {
                    "invoices"
                };
        } else if (
            service.contains(
                "Expense"
            )
        ) {
            tables =
                new String[] {
                    "expenses"
                };
        } else if (
            service.contains(
                "PurchaseOrder"
            )
        ) {
            tables =
                new String[] {
                    "purchase_orders"
                };
        } else if (
            service.contains(
                "DailySiteReport"
            )
        ) {
            tables =
                new String[] {
                    "daily_site_reports"
                };
        } else if (
            service.contains(
                "SiteIncident"
            )
        ) {
            tables =
                new String[] {
                    "site_incidents"
                };
        } else if (
            service.contains(
                "ProjectDocument"
            )
        ) {
            tables =
                new String[] {
                    "project_documents"
                };
        } else if (
            service.contains(
                "ProjectPhoto"
            )
        ) {
            tables =
                new String[] {
                    "project_photos"
                };
        } else if (
            service.contains(
                "Task"
            )
        ) {
            tables =
                new String[] {
                    "project_tasks",
                    "tasks"
                };
        } else if (
            service.contains(
                "Milestone"
            )
        ) {
            tables =
                new String[] {
                    "project_milestones",
                    "milestones"
                };
        } else {
            return null;
        }

        for (String table : tables) {
            try {
                String exists =
                    jdbcTemplate
                        .queryForObject(
                            "SELECT to_regclass(?)",
                            String.class,
                            "public." + table
                        );

                if (exists == null) {
                    continue;
                }

                UUID projectId =
                    jdbcTemplate.query(
                        """
                        SELECT project_id
                        FROM %s
                        WHERE id = ?
                          AND organization_id = ?
                        """.formatted(
                            table
                        ),
                        resultSet ->
                            resultSet.next()
                                ? resultSet
                                    .getObject(
                                        1,
                                        UUID.class
                                    )
                                : null,
                        entityId,
                        organizationId
                    );

                if (projectId != null) {
                    return projectId;
                }
            } catch (
                DataAccessException
                    ignored
            ) {
            }
        }

        return null;
    }

    private String summarize(
        Object result
    ) {
        if (result == null) {
            return null;
        }

        String value =
            String.valueOf(result);

        if (
            value.length()
                <= 8000
        ) {
            return value;
        }

        return value.substring(
            0,
            8000
        );
    }

    private String requestIp() {
        if (
            RequestContextHolder
                .getRequestAttributes()
                instanceof
                ServletRequestAttributes
                    attributes
        ) {
            return attributes
                .getRequest()
                .getRemoteAddr();
        }

        return null;
    }
}
