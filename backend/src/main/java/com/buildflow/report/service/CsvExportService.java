package com.buildflow.report.service;

import com.buildflow.report.dto.ReportDtos;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CsvExportService {

    private static final Pattern
        IDENTIFIER =
            Pattern.compile(
                "[a-z0-9_]+"
            );

    private final JdbcTemplate jdbcTemplate;

    private final ReportAccessService
        accessService;

    public CsvExportService(
        JdbcTemplate jdbcTemplate,
        ReportAccessService accessService
    ) {
        this.jdbcTemplate =
            jdbcTemplate;

        this.accessService =
            accessService;
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportFile export(
        UUID organizationId,
        UUID userId,
        String resource
    ) {
        accessService
            .requireOrganizationExport(
                organizationId,
                userId
            );

        ExportType type =
            parse(resource);

        ExportData data =
            switch (type) {
                case PROJECTS ->
                    regular(
                        organizationId,
                        List.of(
                            "projects"
                        ),
                        fields(
                            "id",
                            "project_number",
                            "name",
                            "status",
                            "priority",
                            "city",
                            "start_date",
                            "expected_completion",
                            "progress_percentage"
                        )
                    );

                case CLIENTS ->
                    clients(
                        organizationId
                    );

                case EXPENSES ->
                    regular(
                        organizationId,
                        List.of(
                            "expenses"
                        ),
                        fields(
                            "id",
                            "project_id",
                            "expense_number",
                            "description",
                            "expense_date",
                            "net_amount",
                            "tax_amount",
                            "total_amount",
                            "status",
                            "purchase_order_id"
                        )
                    );

                case INVOICES ->
                    regular(
                        organizationId,
                        List.of(
                            "invoices"
                        ),
                        fields(
                            "id",
                            "project_id",
                            "invoice_number",
                            "status",
                            "issue_date",
                            "due_date",
                            "subtotal",
                            "tax_amount",
                            "total",
                            "amount_paid",
                            "amount_outstanding"
                        )
                    );

                case PAYMENTS ->
                    payments(
                        organizationId
                    );

                case PURCHASE_ORDERS ->
                    regular(
                        organizationId,
                        List.of(
                            "purchase_orders"
                        ),
                        fields(
                            "id",
                            "project_id",
                            "supplier_id",
                            "purchase_order_number",
                            "status",
                            "issue_date",
                            "expected_delivery_date",
                            "subtotal",
                            "tax_amount",
                            "total"
                        )
                    );

                case TASKS ->
                    regular(
                        organizationId,
                        List.of(
                            "project_tasks",
                            "tasks"
                        ),
                        fields(
                            "id",
                            "project_id",
                            "title",
                            "name",
                            "status",
                            "priority",
                            "due_date",
                            "planned_end_date",
                            "progress_percentage"
                        )
                    );
            };

        byte[] content =
            encode(
                data.columns(),
                data.rows()
            );

        return new ReportDtos
            .ReportFile(
                content,
                type.name()
                    .toLowerCase(
                        Locale.ROOT
                    )
                    .replace(
                        '_',
                        '-'
                    )
                    + ".csv",
                "text/csv"
            );
    }

    private ExportData clients(
        UUID organizationId
    ) {
        String table =
            firstExistingTable(
                List.of(
                    "clients"
                )
            );

        if (table == null) {
            return new ExportData(
                List.of(),
                List.of()
            );
        }

        /*
         * Client naming evolved independently from the
         * reporting module, so the CSV exporter must not
         * assume the business name is literally stored in
         * a column called "name".
         *
         * Read the actual table definition in ordinal order
         * and export its business fields while removing only
         * tenant/audit implementation columns.
         */
        List<String> availableColumns =
            jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                ORDER BY ordinal_position
                """,
                String.class,
                table
            );

        Set<String> technicalColumns =
            Set.of(
                "organization_id",
                "created_at",
                "updated_at",
                "created_by",
                "updated_by",
                "version",
                "archived_at",
                "deleted_at"
            );

        List<String> selectedColumns =
            availableColumns
                .stream()
                .filter(
                    column ->
                        !technicalColumns
                            .contains(
                                column
                            )
                )
                .toList();

        return regular(
            organizationId,
            List.of(table),
            selectedColumns
        );
    }

    private ExportData regular(
        UUID organizationId,
        List<String> tables,
        List<String> requestedFields
    ) {
        String table =
            firstExistingTable(
                tables
            );

        if (table == null) {
            return new ExportData(
                List.of(),
                List.of()
            );
        }

        Set<String> columns =
            columns(table);

        List<String> selected =
            requestedFields
                .stream()
                .filter(
                    columns::contains
                )
                .distinct()
                .toList();

        if (selected.isEmpty()) {
            return new ExportData(
                List.of(),
                List.of()
            );
        }

        String select =
            selected.stream()
                .map(
                    field ->
                        "source."
                            + identifier(
                                field
                            )
                            + " AS "
                            + identifier(
                                field
                            )
                )
                .reduce(
                    (left, right) ->
                        left
                            + ", "
                            + right
                )
                .orElseThrow();

        List<Map<String, Object>> rows;

        if (
            columns.contains(
                "organization_id"
            )
        ) {
            rows =
                jdbcTemplate.queryForList(
                    """
                    SELECT %s
                    FROM %s source
                    WHERE source.organization_id = ?
                    ORDER BY source.id
                    """.formatted(
                        select,
                        identifier(table)
                    ),
                    organizationId
                );

        } else if (
            columns.contains(
                "project_id"
            )
        ) {
            rows =
                jdbcTemplate.queryForList(
                    """
                    SELECT %s
                    FROM %s source
                    JOIN projects
                      ON projects.id =
                            source.project_id
                    WHERE projects.organization_id = ?
                    ORDER BY source.id
                    """.formatted(
                        select,
                        identifier(table)
                    ),
                    organizationId
                );

        } else {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "EXPORT_TENANT_SCOPE_MISSING",
                "The export source does not expose tenant scope."
            );
        }

        return normalize(
            selected,
            rows
        );
    }

    private ExportData payments(
        UUID organizationId
    ) {
        String paymentTable =
            firstExistingTable(
                List.of(
                    "payments"
                )
            );

        String invoiceTable =
            firstExistingTable(
                List.of(
                    "invoices"
                )
            );

        if (
            paymentTable == null
            || invoiceTable == null
        ) {
            return new ExportData(
                List.of(),
                List.of()
            );
        }

        Set<String> paymentColumns =
            columns(
                paymentTable
            );

        List<String> selected =
            fields(
                "id",
                "invoice_id",
                "payment_date",
                "amount",
                "method",
                "reference",
                "notes",
                "created_at"
            )
                .stream()
                .filter(
                    paymentColumns::contains
                )
                .toList();

        if (selected.isEmpty()) {
            return new ExportData(
                List.of(),
                List.of()
            );
        }

        String select =
            selected.stream()
                .map(
                    field ->
                        "payments."
                            + identifier(
                                field
                            )
                            + " AS "
                            + identifier(
                                field
                            )
                )
                .reduce(
                    (left, right) ->
                        left
                            + ", "
                            + right
                )
                .orElseThrow();

        List<Map<String, Object>> rows =
            jdbcTemplate.queryForList(
                """
                SELECT %s
                FROM %s payments
                JOIN %s invoices
                  ON invoices.id =
                        payments.invoice_id
                WHERE invoices.organization_id = ?
                ORDER BY payments.id
                """.formatted(
                    select,
                    identifier(
                        paymentTable
                    ),
                    identifier(
                        invoiceTable
                    )
                ),
                organizationId
            );

        return normalize(
            selected,
            rows
        );
    }

    private ExportData normalize(
        List<String> columns,
        List<Map<String, Object>> rows
    ) {
        List<Map<String, Object>>
            normalized =
                new ArrayList<>();

        for (
            Map<String, Object> row
            : rows
        ) {
            Map<String, Object> result =
                new LinkedHashMap<>();

            for (
                String column
                : columns
            ) {
                Object value =
                    row.get(column);

                result.put(
                    column,
                    value
                );
            }

            normalized.add(result);
        }

        return new ExportData(
            columns,
            normalized
        );
    }

    private byte[] encode(
        List<String> columns,
        List<Map<String, Object>> rows
    ) {
        StringBuilder csv =
            new StringBuilder();

        if (!columns.isEmpty()) {
            csv.append(
                columns.stream()
                    .map(
                        this::escape
                    )
                    .reduce(
                        (left, right) ->
                            left
                                + ","
                                + right
                    )
                    .orElse("")
            );

            csv.append('\n');
        }

        for (
            Map<String, Object> row
            : rows
        ) {
            List<String> values =
                new ArrayList<>();

            for (
                String column
                : columns
            ) {
                Object value =
                    row.get(column);

                values.add(
                    escape(
                        value == null
                            ? ""
                            : String.valueOf(
                                value
                            )
                    )
                );
            }

            csv.append(
                String.join(
                    ",",
                    values
                )
            );

            csv.append('\n');
        }

        return csv.toString()
            .getBytes(
                StandardCharsets.UTF_8
            );
    }

    private String escape(
        String value
    ) {
        boolean quote =
            value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");

        String escaped =
            value.replace(
                "\"",
                "\"\""
            );

        return quote
            ? "\""
                + escaped
                + "\""
            : escaped;
    }

    private ExportType parse(
        String value
    ) {
        try {
            return ExportType.valueOf(
                value
                    .trim()
                    .replace(
                        '-',
                        '_'
                    )
                    .toUpperCase(
                        Locale.ROOT
                    )
            );
        } catch (
            IllegalArgumentException
                exception
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "UNSUPPORTED_EXPORT",
                "The requested CSV export is not supported."
            );
        }
    }

    private String firstExistingTable(
        List<String> candidates
    ) {
        for (
            String candidate
            : candidates
        ) {
            Integer count =
                jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = ?
                    """,
                    Integer.class,
                    candidate
                );

            if (
                count != null
                && count > 0
            ) {
                return candidate;
            }
        }

        return null;
    }

    private Set<String> columns(
        String table
    ) {
        return Set.copyOf(
            jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                """,
                String.class,
                table
            )
        );
    }

    private List<String> fields(
        String... values
    ) {
        return List.of(values);
    }

    private String identifier(
        String value
    ) {
        if (
            value == null
            || !IDENTIFIER
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

    private enum ExportType {
        PROJECTS,
        CLIENTS,
        EXPENSES,
        INVOICES,
        PAYMENTS,
        PURCHASE_ORDERS,
        TASKS
    }

    private record ExportData(
        List<String> columns,
        List<Map<String, Object>> rows
    ) {
    }
}
