package com.buildflow.portal.service;

import com.buildflow.portal.dto.ClientPortalDtos;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ClientChangeOrderDecisionService {

    private final ClientPortalAccessService
        accessService;

    private final JdbcTemplate
        jdbcTemplate;

    public ClientChangeOrderDecisionService(
        ClientPortalAccessService accessService,
        JdbcTemplate jdbcTemplate
    ) {
        this.accessService =
            accessService;

        this.jdbcTemplate =
            jdbcTemplate;
    }

    @Transactional
    public ClientPortalDtos.DecisionView approve(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        UUID changeOrderId,
        String comment
    ) {
        return decide(
            organizationId,
            userId,
            projectId,
            changeOrderId,
            "APPROVED",
            comment
        );
    }

    @Transactional
    public ClientPortalDtos.DecisionView reject(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        UUID changeOrderId,
        String comment
    ) {
        return decide(
            organizationId,
            userId,
            projectId,
            changeOrderId,
            "REJECTED",
            comment
        );
    }

    private ClientPortalDtos.DecisionView decide(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        UUID changeOrderId,
        String decision,
        String comment
    ) {
        accessService.requireProject(
            organizationId,
            userId,
            projectId
        );

        String status =
            jdbcTemplate.query(
                """
                SELECT status
                FROM change_orders
                WHERE id = ?
                  AND organization_id = ?
                  AND project_id = ?
                """,
                resultSet ->
                    resultSet.next()
                        ? resultSet.getString(1)
                        : null,
                changeOrderId,
                organizationId,
                projectId
            );

        if (status == null) {
            throw new BusinessException(
                HttpStatus.NOT_FOUND,
                "CLIENT_CHANGE_ORDER_NOT_FOUND",
                "The change order is not available."
            );
        }

        if (
            !"CLIENT_REVIEW".equals(
                status
            )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "CLIENT_CHANGE_ORDER_NOT_REVIEWABLE",
                "Only a change order in client review can receive a client decision."
            );
        }

        Instant now =
            Instant.now();

        int updated;

        if (
            "APPROVED".equals(
                decision
            )
        ) {
            updated =
                jdbcTemplate.update(
                    """
                    UPDATE change_orders
                    SET status = 'APPROVED',
                        approved_by = ?,
                        approved_at = ?,
                        updated_at = ?,
                        version = version + 1
                    WHERE id = ?
                      AND organization_id = ?
                      AND project_id = ?
                      AND status =
                            'CLIENT_REVIEW'
                    """,
                    userId,
                    now,
                    now,
                    changeOrderId,
                    organizationId,
                    projectId
                );
        } else {
            updated =
                jdbcTemplate.update(
                    """
                    UPDATE change_orders
                    SET status = 'REJECTED',
                        approved_by = NULL,
                        approved_at = NULL,
                        updated_at = ?,
                        version = version + 1
                    WHERE id = ?
                      AND organization_id = ?
                      AND project_id = ?
                      AND status =
                            'CLIENT_REVIEW'
                    """,
                    now,
                    changeOrderId,
                    organizationId,
                    projectId
                );
        }

        if (updated != 1) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "CLIENT_CHANGE_ORDER_DECISION_RACE",
                "The change order was modified before the decision could be saved."
            );
        }

        String cleanComment =
            comment == null
                || comment.isBlank()
                ? null
                : comment.trim();

        jdbcTemplate.update(
            """
            INSERT INTO client_change_order_decisions (
                id,
                organization_id,
                project_id,
                change_order_id,
                decision,
                client_user_id,
                comment,
                decided_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            UUID.randomUUID(),
            organizationId,
            projectId,
            changeOrderId,
            decision,
            userId,
            cleanComment,
            now
        );

        return new ClientPortalDtos
            .DecisionView(
                changeOrderId,
                decision,
                userId,
                cleanComment,
                now
            );
    }
}
