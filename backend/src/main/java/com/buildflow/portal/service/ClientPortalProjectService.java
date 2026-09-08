package com.buildflow.portal.service;

import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.portal.dto.ClientPortalDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ClientPortalProjectService {

    private final ClientPortalAccessService
        accessService;

    private final JdbcTemplate
        jdbcTemplate;

    public ClientPortalProjectService(
        ClientPortalAccessService accessService,
        JdbcTemplate jdbcTemplate
    ) {
        this.accessService =
            accessService;

        this.jdbcTemplate =
            jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<ClientPortalDtos.ProjectSummary>
        projects(
            UUID organizationId,
            UUID userId
        ) {

        OrganizationMembership membership =
            accessService
                .requireClientMembership(
                    organizationId,
                    userId
                );

        return jdbcTemplate.query(
            """
            SELECT
                p.id,
                p.project_number,
                p.name,
                p.status,
                p.progress_percentage,
                p.planned_end_date
            FROM projects p
            JOIN client_portal_accesses a
              ON a.organization_id =
                    p.organization_id
             AND a.client_id =
                    p.client_id
            WHERE p.organization_id = ?
              AND a.membership_id = ?
              AND p.archived_at IS NULL
            ORDER BY p.created_at DESC
            """,
            (resultSet, rowNumber) ->
                new ClientPortalDtos
                    .ProjectSummary(
                        resultSet.getObject(
                            "id",
                            UUID.class
                        ),
                        resultSet.getString(
                            "project_number"
                        ),
                        resultSet.getString(
                            "name"
                        ),
                        resultSet.getString(
                            "status"
                        ),
                        resultSet.getBigDecimal(
                            "progress_percentage"
                        ),
                        localDate(
                            resultSet.getDate(
                                "planned_end_date"
                            )
                        )
                    ),
            organizationId,
            membership.getId()
        );
    }

    @Transactional(readOnly = true)
    public ClientPortalDtos.ProjectView project(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        accessService.requireProject(
            organizationId,
            userId,
            projectId
        );

        ClientPortalDtos.ProjectSummary project =
            jdbcTemplate.queryForObject(
                """
                SELECT
                    id,
                    project_number,
                    name,
                    status,
                    progress_percentage,
                    planned_end_date
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                  AND archived_at IS NULL
                """,
                (resultSet, rowNumber) ->
                    new ClientPortalDtos
                        .ProjectSummary(
                            resultSet.getObject(
                                "id",
                                UUID.class
                            ),
                            resultSet.getString(
                                "project_number"
                            ),
                            resultSet.getString(
                                "name"
                            ),
                            resultSet.getString(
                                "status"
                            ),
                            resultSet.getBigDecimal(
                                "progress_percentage"
                            ),
                            localDate(
                                resultSet.getDate(
                                    "planned_end_date"
                                )
                            )
                        ),
                projectId,
                organizationId
            );

        return new ClientPortalDtos
            .ProjectView(
                project,
                milestones(
                    organizationId,
                    projectId
                ),
                recentUpdates(
                    organizationId,
                    projectId
                ),
                changeOrders(
                    organizationId,
                    projectId
                ),
                quotes(
                    organizationId,
                    projectId
                ),
                invoices(
                    organizationId,
                    projectId
                )
            );
    }

    private List<ClientPortalDtos.MilestoneView>
        milestones(
            UUID organizationId,
            UUID projectId
        ) {

        String table = null;

        if (tableExists(
            "project_milestones"
        )) {
            table =
                "project_milestones";
        } else if (
            tableExists("milestones")
        ) {
            table =
                "milestones";
        }

        if (table == null) {
            return List.of();
        }

        String sql =
            """
            SELECT
                id,
                name,
                planned_date,
                status
            FROM %s
            WHERE organization_id = ?
              AND project_id = ?
              AND client_visible = TRUE
              AND status IN (
                    'UPCOMING',
                    'IN_PROGRESS'
              )
            ORDER BY
                planned_date ASC NULLS LAST
            LIMIT 8
            """.formatted(table);

        return jdbcTemplate.query(
            sql,
            (resultSet, rowNumber) ->
                new ClientPortalDtos
                    .MilestoneView(
                        resultSet.getObject(
                            "id",
                            UUID.class
                        ),
                        resultSet.getString(
                            "name"
                        ),
                        localDate(
                            resultSet.getDate(
                                "planned_date"
                            )
                        ),
                        resultSet.getString(
                            "status"
                        )
                    ),
            organizationId,
            projectId
        );
    }

    private List<
        ClientPortalDtos.ProgressUpdateView
    > recentUpdates(
        UUID organizationId,
        UUID projectId
    ) {
        if (!tableExists(
            "project_progress_updates"
        )) {
            return List.of();
        }

        return jdbcTemplate.query(
            """
            SELECT
                id,
                progress_percentage_snapshot,
                summary,
                next_steps,
                created_at
            FROM project_progress_updates
            WHERE organization_id = ?
              AND project_id = ?
            ORDER BY created_at DESC
            LIMIT 8
            """,
            (resultSet, rowNumber) ->
                new ClientPortalDtos
                    .ProgressUpdateView(
                        resultSet.getObject(
                            "id",
                            UUID.class
                        ),
                        resultSet.getBigDecimal(
                            "progress_percentage_snapshot"
                        ),
                        resultSet.getString(
                            "summary"
                        ),
                        resultSet.getString(
                            "next_steps"
                        ),
                        instant(
                            resultSet.getTimestamp(
                                "created_at"
                            )
                        )
                    ),
            organizationId,
            projectId
        );
    }

    private List<ClientPortalDtos.ChangeOrderView>
        changeOrders(
            UUID organizationId,
            UUID projectId
        ) {
        if (!tableExists(
            "change_orders"
        )) {
            return List.of();
        }

        return jdbcTemplate.query(
            """
            SELECT
                id,
                change_order_number,
                title,
                description,
                reason,
                client_price_impact,
                schedule_impact_days,
                status,
                updated_at
            FROM change_orders
            WHERE organization_id = ?
              AND project_id = ?
              AND status IN (
                    'CLIENT_REVIEW',
                    'APPROVED',
                    'REJECTED'
              )
            ORDER BY created_at DESC
            """,
            (resultSet, rowNumber) ->
                new ClientPortalDtos
                    .ChangeOrderView(
                        resultSet.getObject(
                            "id",
                            UUID.class
                        ),
                        resultSet.getString(
                            "change_order_number"
                        ),
                        resultSet.getString(
                            "title"
                        ),
                        resultSet.getString(
                            "description"
                        ),
                        resultSet.getString(
                            "reason"
                        ),
                        resultSet.getBigDecimal(
                            "client_price_impact"
                        ),
                        resultSet.getObject(
                            "schedule_impact_days",
                            Integer.class
                        ),
                        resultSet.getString(
                            "status"
                        ),
                        instant(
                            resultSet.getTimestamp(
                                "updated_at"
                            )
                        )
                    ),
            organizationId,
            projectId
        );
    }

    private List<ClientPortalDtos.QuoteView>
        quotes(
            UUID organizationId,
            UUID projectId
        ) {
        if (!tableExists("quotes")) {
            return List.of();
        }

        return jdbcTemplate.query(
            """
            SELECT
                id,
                quote_number,
                status,
                issue_date,
                valid_until,
                total
            FROM quotes
            WHERE organization_id = ?
              AND project_id = ?
              AND status <> 'DRAFT'
            ORDER BY created_at DESC
            """,
            (resultSet, rowNumber) ->
                new ClientPortalDtos
                    .QuoteView(
                        resultSet.getObject(
                            "id",
                            UUID.class
                        ),
                        resultSet.getString(
                            "quote_number"
                        ),
                        resultSet.getString(
                            "status"
                        ),
                        localDate(
                            resultSet.getDate(
                                "issue_date"
                            )
                        ),
                        localDate(
                            resultSet.getDate(
                                "valid_until"
                            )
                        ),
                        resultSet.getBigDecimal(
                            "total"
                        )
                    ),
            organizationId,
            projectId
        );
    }

    private List<ClientPortalDtos.InvoiceView>
        invoices(
            UUID organizationId,
            UUID projectId
        ) {
        if (!tableExists("invoices")) {
            return List.of();
        }

        return jdbcTemplate.query(
            """
            SELECT
                id,
                invoice_number,
                status,
                issue_date,
                due_date,
                total,
                amount_paid
            FROM invoices
            WHERE organization_id = ?
              AND project_id = ?
              AND status <> 'DRAFT'
            ORDER BY created_at DESC
            """,
            (resultSet, rowNumber) ->
                new ClientPortalDtos
                    .InvoiceView(
                        resultSet.getObject(
                            "id",
                            UUID.class
                        ),
                        resultSet.getString(
                            "invoice_number"
                        ),
                        resultSet.getString(
                            "status"
                        ),
                        localDate(
                            resultSet.getDate(
                                "issue_date"
                            )
                        ),
                        localDate(
                            resultSet.getDate(
                                "due_date"
                            )
                        ),
                        resultSet.getBigDecimal(
                            "total"
                        ),
                        resultSet.getBigDecimal(
                            "amount_paid"
                        )
                    ),
            organizationId,
            projectId
        );
    }

    private boolean tableExists(
        String table
    ) {
        String value =
            jdbcTemplate.queryForObject(
                "SELECT to_regclass(?)",
                String.class,
                "public." + table
            );

        return value != null;
    }

    private static LocalDate localDate(
        Date value
    ) {
        return value == null
            ? null
            : value.toLocalDate();
    }

    private static Instant instant(
        Timestamp value
    ) {
        return value == null
            ? null
            : value.toInstant();
    }
}
