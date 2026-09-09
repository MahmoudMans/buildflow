package com.buildflow.report.service;

import com.buildflow.report.dto.ReportDtos;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CommercialReportService {

    private static final Pattern
        IDENTIFIER =
            Pattern.compile(
                "[a-z0-9_]+"
            );

    private final JdbcTemplate jdbcTemplate;

    private final ReportAccessService
        accessService;

    private final PdfReportService
        pdfReportService;

    public CommercialReportService(
        JdbcTemplate jdbcTemplate,
        ReportAccessService accessService,
        PdfReportService pdfReportService
    ) {
        this.jdbcTemplate =
            jdbcTemplate;

        this.accessService =
            accessService;

        this.pdfReportService =
            pdfReportService;
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportFile quote(
        UUID organizationId,
        UUID userId,
        UUID id
    ) {
        return commercial(
            organizationId,
            userId,
            id,
            config(
                "Quotation",
                List.of(
                    "quotes",
                    "quotations"
                ),
                List.of(
                    "quote_items",
                    "quotation_items"
                ),
                List.of(
                    "quote_id",
                    "quotation_id"
                ),
                fields(
                    "quote_number",
                    "status",
                    "issue_date",
                    "valid_until",
                    "subtotal",
                    "tax_amount",
                    "total",
                    "notes",
                    "terms"
                ),
                "quote_number",
                "quotation"
            )
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportFile invoice(
        UUID organizationId,
        UUID userId,
        UUID id
    ) {
        return commercial(
            organizationId,
            userId,
            id,
            config(
                "Invoice",
                List.of(
                    "invoices"
                ),
                List.of(
                    "invoice_items"
                ),
                List.of(
                    "invoice_id"
                ),
                fields(
                    "invoice_number",
                    "status",
                    "issue_date",
                    "due_date",
                    "subtotal",
                    "tax_amount",
                    "total",
                    "amount_paid",
                    "amount_outstanding",
                    "notes",
                    "payment_instructions"
                ),
                "invoice_number",
                "invoice"
            )
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportFile
        purchaseOrder(
            UUID organizationId,
            UUID userId,
            UUID id
        ) {
        return commercial(
            organizationId,
            userId,
            id,
            config(
                "Purchase Order",
                List.of(
                    "purchase_orders"
                ),
                List.of(
                    "purchase_order_items"
                ),
                List.of(
                    "purchase_order_id"
                ),
                fields(
                    "purchase_order_number",
                    "status",
                    "supplier_id",
                    "issue_date",
                    "expected_delivery_date",
                    "subtotal",
                    "tax_amount",
                    "total",
                    "notes"
                ),
                "purchase_order_number",
                "purchase-order"
            )
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportFile
        changeOrder(
            UUID organizationId,
            UUID userId,
            UUID id
        ) {
        return commercial(
            organizationId,
            userId,
            id,
            config(
                "Change Order",
                List.of(
                    "change_orders"
                ),
                List.of(),
                List.of(),
                fields(
                    "change_order_number",
                    "title",
                    "description",
                    "reason",
                    "cost_impact",
                    "client_price_impact",
                    "schedule_impact_days",
                    "status",
                    "approved_at"
                ),
                "change_order_number",
                "change-order"
            )
        );
    }

    @Transactional(readOnly = true)
    public ReportDtos.ReportFile
        dailySiteReport(
            UUID organizationId,
            UUID userId,
            UUID id
        ) {

        String table =
            firstExistingTable(
                List.of(
                    "daily_site_reports",
                    "site_reports"
                )
            );

        if (table == null) {
            notFound(
                "SITE_REPORT_NOT_FOUND"
            );
        }

        Map<String, Object> row =
            row(
                table,
                organizationId,
                id
            );

        UUID projectId =
            uuid(
                row.get(
                    "project_id"
                )
            );

        accessService
            .requireSiteReport(
                organizationId,
                userId,
                projectId
            );

        List<PdfReportService.Entry>
            entries =
                new ArrayList<>();

        for (
            String field
            : fields(
                "report_date",
                "weather_text",
                "workers_on_site",
                "summary",
                "work_completed",
                "delays",
                "issues",
                "next_steps",
                "created_at"
            )
        ) {
            if (
                row.containsKey(field)
            ) {
                entries.add(
                    entry(
                        label(field),
                        row.get(field)
                    )
                );
            }
        }

        byte[] content =
            pdfReportService.render(
                "BuildFlow Daily Site Report",
                List.of(
                    PdfReportService.Section.of(
                        "Site Report",
                        entries
                    )
                )
            );

        return pdf(
            content,
            "site-report-"
                + id
                + ".pdf"
        );
    }

    private ReportDtos.ReportFile
        commercial(
            UUID organizationId,
            UUID userId,
            UUID id,
            Config config
        ) {

        String table =
            firstExistingTable(
                config.tables()
            );

        if (table == null) {
            notFound(
                "REPORT_ENTITY_NOT_FOUND"
            );
        }

        Map<String, Object> row =
            row(
                table,
                organizationId,
                id
            );

        UUID projectId =
            uuid(
                row.get(
                    "project_id"
                )
            );

        if (projectId == null) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "REPORT_PROJECT_SCOPE_MISSING",
                "The document is not associated with a project."
            );
        }

        accessService
            .requireCommercialProject(
                organizationId,
                userId,
                projectId
            );

        List<PdfReportService.Section>
            sections =
                new ArrayList<>();

        List<PdfReportService.Entry>
            documentEntries =
                new ArrayList<>();

        for (
            String field
            : config.fields()
        ) {
            if (
                row.containsKey(field)
            ) {
                documentEntries.add(
                    entry(
                        label(field),
                        row.get(field)
                    )
                );
            }
        }

        sections.add(
            PdfReportService.Section.of(
                config.title(),
                documentEntries
            )
        );

        List<String> lineItems =
            lineItems(
                config,
                id
            );

        if (!lineItems.isEmpty()) {
            sections.add(
                PdfReportService
                    .Section
                    .lines(
                        "Items",
                        lineItems
                    )
            );
        }

        String number =
            value(
                row.get(
                    config.numberField()
                )
            );

        if (
            number.equals("—")
        ) {
            number =
                id.toString();
        }

        byte[] content =
            pdfReportService.render(
                "BuildFlow "
                    + config.title()
                    + " — "
                    + number,
                sections
            );

        return pdf(
            content,
            config.filePrefix()
                + "-"
                + number
                + ".pdf"
        );
    }

    private List<String> lineItems(
        Config config,
        UUID parentId
    ) {
        if (
            config.itemTables()
                .isEmpty()
        ) {
            return List.of();
        }

        String table =
            firstExistingTable(
                config.itemTables()
            );

        if (table == null) {
            return List.of();
        }

        Set<String> columns =
            columns(table);

        String foreignKey = null;

        for (
            String candidate
            : config.itemForeignKeys()
        ) {
            if (
                columns.contains(
                    candidate
                )
            ) {
                foreignKey =
                    candidate;

                break;
            }
        }

        if (foreignKey == null) {
            return List.of();
        }

        List<Map<String, Object>> rows =
            jdbcTemplate.queryForList(
                """
                SELECT *
                FROM %s
                WHERE %s = ?
                ORDER BY id
                """.formatted(
                    identifier(table),
                    identifier(
                        foreignKey
                    )
                ),
                parentId
            );

        List<String> result =
            new ArrayList<>();

        for (
            Map<String, Object> row
            : rows
        ) {
            List<String> parts =
                new ArrayList<>();

            for (
                String field
                : fields(
                    "description",
                    "quantity",
                    "unit",
                    "unit_price",
                    "tax_rate",
                    "line_total"
                )
            ) {
                if (
                    row.containsKey(field)
                ) {
                    parts.add(
                        label(field)
                            + ": "
                            + value(
                                row.get(field)
                            )
                    );
                }
            }

            if (!parts.isEmpty()) {
                result.add(
                    String.join(
                        " | ",
                        parts
                    )
                );
            }
        }

        return result;
    }

    private Map<String, Object> row(
        String table,
        UUID organizationId,
        UUID id
    ) {
        Set<String> columns =
            columns(table);

        List<Map<String, Object>> rows;

        if (
            columns.contains(
                "organization_id"
            )
        ) {
            rows =
                jdbcTemplate.queryForList(
                    """
                    SELECT *
                    FROM %s
                    WHERE id = ?
                      AND organization_id = ?
                    """.formatted(
                        identifier(table)
                    ),
                    id,
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
                    SELECT entity.*
                    FROM %s entity
                    JOIN projects
                      ON projects.id =
                            entity.project_id
                    WHERE entity.id = ?
                      AND projects.organization_id = ?
                    """.formatted(
                        identifier(table)
                    ),
                    id,
                    organizationId
                );

        } else {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "REPORT_TENANT_SCOPE_MISSING",
                "The report source does not expose tenant scope."
            );
        }

        if (rows.size() != 1) {
            notFound(
                "REPORT_ENTITY_NOT_FOUND"
            );
        }

        Map<String, Object> normalized =
            new LinkedHashMap<>();

        rows.getFirst()
            .forEach(
                (key, value) ->
                    normalized.put(
                        key.toLowerCase(),
                        value
                    )
            );

        return normalized;
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

    private Config config(
        String title,
        List<String> tables,
        List<String> itemTables,
        List<String> itemForeignKeys,
        List<String> fields,
        String numberField,
        String filePrefix
    ) {
        return new Config(
            title,
            tables,
            itemTables,
            itemForeignKeys,
            fields,
            numberField,
            filePrefix
        );
    }

    private List<String> fields(
        String... fields
    ) {
        return List.of(fields);
    }

    private PdfReportService.Entry entry(
        String label,
        Object value
    ) {
        return new PdfReportService
            .Entry(
                label,
                value(value)
            );
    }

    private String value(
        Object value
    ) {
        return value == null
            ? "—"
            : String.valueOf(value);
    }

    private String label(
        String value
    ) {
        String[] parts =
            value.split("_");

        List<String> result =
            new ArrayList<>();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            result.add(
                Character.toUpperCase(
                    part.charAt(0)
                )
                    + part.substring(1)
            );
        }

        return String.join(
            " ",
            result
        );
    }

    private UUID uuid(
        Object value
    ) {
        if (
            value instanceof UUID uuid
        ) {
            return uuid;
        }

        if (value == null) {
            return null;
        }

        return UUID.fromString(
            String.valueOf(value)
        );
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

    private ReportDtos.ReportFile pdf(
        byte[] content,
        String filename
    ) {
        return new ReportDtos
            .ReportFile(
                content,
                filename.replaceAll(
                    "[^A-Za-z0-9._-]",
                    "-"
                ),
                "application/pdf"
            );
    }

    private void notFound(
        String code
    ) {
        throw new BusinessException(
            HttpStatus.NOT_FOUND,
            code,
            "The report source could not be found."
        );
    }

    private record Config(
        String title,
        List<String> tables,
        List<String> itemTables,
        List<String> itemForeignKeys,
        List<String> fields,
        String numberField,
        String filePrefix
    ) {
    }
}
