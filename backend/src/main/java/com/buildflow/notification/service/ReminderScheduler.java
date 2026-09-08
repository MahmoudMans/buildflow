package com.buildflow.notification.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ReminderScheduler {

    private static final Pattern
        IDENTIFIER_PATTERN =
            Pattern.compile(
                "[a-z0-9_]+"
            );

    private final JdbcTemplate jdbcTemplate;

    private final NotificationService
        notificationService;

    public ReminderScheduler(
        JdbcTemplate jdbcTemplate,
        NotificationService
            notificationService
    ) {
        this.jdbcTemplate =
            jdbcTemplate;

        this.notificationService =
            notificationService;
    }

    @Scheduled(
        cron = "0 */15 * * * *"
    )
    @Transactional(
        propagation =
            Propagation.REQUIRES_NEW
    )
    public void scheduledMaintenance() {
        executeMaintenance();
    }

    @Transactional(
        propagation =
            Propagation.REQUIRES_NEW
    )
    public RunResult runMaintenance() {
        return executeMaintenance();
    }

    private RunResult executeMaintenance() {
        int invoices =
            processInvoices();

        int tasks =
            processTasks();

        int milestones =
            processMilestones();

        int quotations =
            processQuotations();

        return new RunResult(
            invoices,
            tasks,
            milestones,
            quotations
        );
    }

    private int processInvoices() {
        String table =
            firstExistingTable(
                "invoices"
            );

        if (table == null) {
            return 0;
        }

        Set<String> columns =
            columns(table);

        if (
            !hasColumns(
                columns,
                "id",
                "organization_id",
                "project_id",
                "status"
            )
        ) {
            return 0;
        }

        String dueColumn =
            firstColumn(
                columns,
                "due_date",
                "due_at"
            );

        if (dueColumn == null) {
            return 0;
        }

        String labelColumn =
            firstColumn(
                columns,
                "invoice_number",
                "number",
                "reference"
            );

        String tableSql =
            identifier(table);

        String dueSql =
            identifier(dueColumn);

        String labelSql =
            labelExpression(
                labelColumn
            );

        List<Row> rows =
            jdbcTemplate.query(
                """
                SELECT
                    id,
                    organization_id,
                    project_id,
                    %s AS row_label
                FROM %s
                WHERE CAST(%s AS date)
                        < CURRENT_DATE
                  AND UPPER(
                        CAST(status AS text)
                      ) IN (
                        'ISSUED',
                        'PARTIALLY_PAID'
                      )
                """.formatted(
                    labelSql,
                    tableSql,
                    dueSql
                ),
                this::mapRow
            );

        int changed = 0;

        for (Row row : rows) {
            String updateSql;

            if (
                columns.contains(
                    "updated_at"
                )
            ) {
                updateSql =
                    """
                    UPDATE %s
                    SET status = 'OVERDUE',
                        updated_at =
                            CURRENT_TIMESTAMP
                    WHERE id = ?
                      AND organization_id = ?
                      AND UPPER(
                            CAST(status AS text)
                          ) IN (
                            'ISSUED',
                            'PARTIALLY_PAID'
                          )
                    """.formatted(
                        tableSql
                    );
            } else {
                updateSql =
                    """
                    UPDATE %s
                    SET status = 'OVERDUE'
                    WHERE id = ?
                      AND organization_id = ?
                      AND UPPER(
                            CAST(status AS text)
                          ) IN (
                            'ISSUED',
                            'PARTIALLY_PAID'
                          )
                    """.formatted(
                        tableSql
                    );
            }

            int updated =
                jdbcTemplate.update(
                    updateSql,
                    row.id(),
                    row.organizationId()
                );

            if (updated == 1) {
                changed++;

                notifyProject(
                    row,
                    "INVOICE_OVERDUE",
                    "Invoice overdue",
                    "Invoice "
                        + row.label()
                        + " is overdue.",
                    "INVOICE",
                    "invoice-overdue:"
                );
            }
        }

        return changed;
    }

    private int processTasks() {
        String table =
            firstExistingTable(
                "project_tasks",
                "tasks"
            );

        if (table == null) {
            return 0;
        }

        Set<String> columns =
            columns(table);

        if (
            !hasColumns(
                columns,
                "id",
                "organization_id",
                "project_id",
                "status"
            )
        ) {
            return 0;
        }

        String dueColumn =
            firstColumn(
                columns,
                "due_date",
                "planned_end_date",
                "end_date",
                "target_date"
            );

        if (dueColumn == null) {
            return 0;
        }

        String labelColumn =
            firstColumn(
                columns,
                "title",
                "name",
                "task_number",
                "reference",
                "description"
            );

        List<Row> rows =
            jdbcTemplate.query(
                """
                SELECT
                    id,
                    organization_id,
                    project_id,
                    %s AS row_label
                FROM %s
                WHERE CAST(%s AS date)
                        < CURRENT_DATE
                  AND UPPER(
                        CAST(status AS text)
                      ) NOT IN (
                        'DONE',
                        'COMPLETED',
                        'CANCELLED',
                        'CANCELED'
                      )
                """.formatted(
                    labelExpression(
                        labelColumn
                    ),
                    identifier(table),
                    identifier(dueColumn)
                ),
                this::mapRow
            );

        for (Row row : rows) {
            notifyProject(
                row,
                "TASK_OVERDUE",
                "Task overdue",
                "Task \""
                    + row.label()
                    + "\" is overdue.",
                "TASK",
                "task-overdue:"
            );
        }

        return rows.size();
    }

    private int processMilestones() {
        String table =
            firstExistingTable(
                "project_milestones",
                "milestones"
            );

        if (table == null) {
            return 0;
        }

        Set<String> columns =
            columns(table);

        if (
            !hasColumns(
                columns,
                "id",
                "organization_id",
                "project_id",
                "status"
            )
        ) {
            return 0;
        }

        String dueColumn =
            firstColumn(
                columns,
                "planned_date",
                "due_date",
                "target_date",
                "planned_end_date",
                "end_date"
            );

        if (dueColumn == null) {
            return 0;
        }

        String labelColumn =
            firstColumn(
                columns,
                "title",
                "name",
                "milestone_number",
                "reference",
                "description"
            );

        String tableSql =
            identifier(table);

        String dueSql =
            identifier(dueColumn);

        String labelSql =
            labelExpression(
                labelColumn
            );

        List<Row> missed =
            jdbcTemplate.query(
                """
                SELECT
                    id,
                    organization_id,
                    project_id,
                    %s AS row_label
                FROM %s
                WHERE CAST(%s AS date)
                        < CURRENT_DATE
                  AND UPPER(
                        CAST(status AS text)
                      ) NOT IN (
                        'DONE',
                        'COMPLETED',
                        'CANCELLED',
                        'CANCELED'
                      )
                """.formatted(
                    labelSql,
                    tableSql,
                    dueSql
                ),
                this::mapRow
            );

        for (Row row : missed) {
            notifyProject(
                row,
                "MILESTONE_MISSED",
                "Milestone missed",
                "Milestone \""
                    + row.label()
                    + "\" is overdue.",
                "MILESTONE",
                "milestone-missed:"
            );
        }

        List<Row> dueTomorrow =
            jdbcTemplate.query(
                """
                SELECT
                    id,
                    organization_id,
                    project_id,
                    %s AS row_label
                FROM %s
                WHERE CAST(%s AS date)
                        = CURRENT_DATE + 1
                  AND UPPER(
                        CAST(status AS text)
                      ) NOT IN (
                        'DONE',
                        'COMPLETED',
                        'CANCELLED',
                        'CANCELED'
                      )
                """.formatted(
                    labelSql,
                    tableSql,
                    dueSql
                ),
                this::mapRow
            );

        for (
            Row row
            : dueTomorrow
        ) {
            notifyProject(
                row,
                "MILESTONE_DUE_TOMORROW",
                "Milestone due tomorrow",
                "Milestone \""
                    + row.label()
                    + "\" is due tomorrow.",
                "MILESTONE",
                "milestone-due-tomorrow:"
            );
        }

        return missed.size();
    }

    private int processQuotations() {
        String table =
            firstExistingTable(
                "quotes",
                "quotations"
            );

        if (table == null) {
            return 0;
        }

        Set<String> columns =
            columns(table);

        if (
            !hasColumns(
                columns,
                "id",
                "organization_id",
                "project_id",
                "status"
            )
        ) {
            return 0;
        }

        String expiryColumn =
            firstColumn(
                columns,
                "valid_until",
                "expires_at",
                "expiry_date",
                "expiration_date"
            );

        if (expiryColumn == null) {
            return 0;
        }

        String labelColumn =
            firstColumn(
                columns,
                "quote_number",
                "quotation_number",
                "number",
                "reference"
            );

        String tableSql =
            identifier(table);

        List<Row> rows =
            jdbcTemplate.query(
                """
                SELECT
                    id,
                    organization_id,
                    project_id,
                    %s AS row_label
                FROM %s
                WHERE CAST(%s AS date)
                        < CURRENT_DATE
                  AND UPPER(
                        CAST(status AS text)
                      ) IN (
                        'SENT',
                        'VIEWED',
                        'ISSUED'
                      )
                """.formatted(
                    labelExpression(
                        labelColumn
                    ),
                    tableSql,
                    identifier(
                        expiryColumn
                    )
                ),
                this::mapRow
            );

        int changed = 0;

        for (Row row : rows) {
            String updateSql;

            if (
                columns.contains(
                    "updated_at"
                )
            ) {
                updateSql =
                    """
                    UPDATE %s
                    SET status = 'EXPIRED',
                        updated_at =
                            CURRENT_TIMESTAMP
                    WHERE id = ?
                      AND organization_id = ?
                      AND UPPER(
                            CAST(status AS text)
                          ) IN (
                            'SENT',
                            'VIEWED',
                            'ISSUED'
                          )
                    """.formatted(
                        tableSql
                    );
            } else {
                updateSql =
                    """
                    UPDATE %s
                    SET status = 'EXPIRED'
                    WHERE id = ?
                      AND organization_id = ?
                      AND UPPER(
                            CAST(status AS text)
                          ) IN (
                            'SENT',
                            'VIEWED',
                            'ISSUED'
                          )
                    """.formatted(
                        tableSql
                    );
            }

            int updated =
                jdbcTemplate.update(
                    updateSql,
                    row.id(),
                    row.organizationId()
                );

            if (updated == 1) {
                changed++;

                notifyProject(
                    row,
                    "QUOTE_EXPIRED",
                    "Quotation expired",
                    "Quotation "
                        + row.label()
                        + " expired.",
                    "QUOTE",
                    "quote-expired:"
                );
            }
        }

        return changed;
    }

    private void notifyProject(
        Row row,
        String type,
        String title,
        String message,
        String entityType,
        String sourcePrefix
    ) {
        if (
            row.projectId()
                == null
        ) {
            return;
        }

        notificationService
            .notifyProjectStakeholders(
                row.organizationId(),
                row.projectId(),
                type,
                title,
                message,
                entityType,
                row.id(),
                sourcePrefix
                    + row.id()
            );
    }

    private Row mapRow(
        java.sql.ResultSet resultSet,
        int rowNumber
    ) throws java.sql.SQLException {

        String label =
            resultSet.getString(
                "row_label"
            );

        UUID id =
            resultSet.getObject(
                "id",
                UUID.class
            );

        if (
            label == null
            || label.isBlank()
        ) {
            label =
                id.toString();
        }

        return new Row(
            id,
            resultSet.getObject(
                "organization_id",
                UUID.class
            ),
            resultSet.getObject(
                "project_id",
                UUID.class
            ),
            label
        );
    }

    private String firstExistingTable(
        String... candidates
    ) {
        for (
            String candidate
            : candidates
        ) {
            if (
                tableExists(
                    candidate
                )
            ) {
                return candidate;
            }
        }

        return null;
    }

    private boolean tableExists(
        String table
    ) {
        Integer count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema =
                        'public'
                  AND table_name = ?
                """,
                Integer.class,
                table
            );

        return count != null
            && count > 0;
    }

    private Set<String> columns(
        String table
    ) {
        return new HashSet<>(
            jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema =
                        'public'
                  AND table_name = ?
                """,
                String.class,
                table
            )
        );
    }

    private boolean hasColumns(
        Set<String> columns,
        String... required
    ) {
        for (
            String value
            : required
        ) {
            if (
                !columns.contains(
                    value
                )
            ) {
                return false;
            }
        }

        return true;
    }

    private String firstColumn(
        Set<String> columns,
        String... candidates
    ) {
        for (
            String candidate
            : candidates
        ) {
            if (
                columns.contains(
                    candidate
                )
            ) {
                return candidate;
            }
        }

        return null;
    }

    private String labelExpression(
        String column
    ) {
        if (column == null) {
            return "CAST(id AS text)";
        }

        return """
            COALESCE(
                NULLIF(
                    CAST(%s AS text),
                    ''
                ),
                CAST(id AS text)
            )
            """.formatted(
                identifier(column)
            );
    }

    private String identifier(
        String value
    ) {
        if (
            value == null
            || !IDENTIFIER_PATTERN
                .matcher(value)
                .matches()
        ) {
            throw new IllegalArgumentException(
                "Invalid SQL identifier."
            );
        }

        return "\""
            + value
            + "\"";
    }

    private record Row(
        UUID id,
        UUID organizationId,
        UUID projectId,
        String label
    ) {
    }

    public record RunResult(
        int invoicesOverdue,
        int overdueTasks,
        int missedMilestones,
        int expiredQuotations
    ) {
    }
}
