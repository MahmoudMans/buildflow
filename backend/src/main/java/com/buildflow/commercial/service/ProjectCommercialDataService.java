package com.buildflow.commercial.service;

import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class ProjectCommercialDataService {

    private final JdbcTemplate jdbcTemplate;

    public ProjectCommercialDataService(
        JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public UUID clientId(
        UUID organizationId,
        UUID projectId
    ) {
        return jdbcTemplate.queryForObject(
            """
            SELECT client_id
            FROM projects
            WHERE id = ?
              AND organization_id = ?
            """,
            UUID.class,
            projectId,
            organizationId
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal contractValue(
        UUID organizationId,
        UUID projectId
    ) {
        BigDecimal value =
            jdbcTemplate.queryForObject(
                """
                SELECT contract_value
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                """,
                BigDecimal.class,
                projectId,
                organizationId
            );

        if (value == null) {
            return zero();
        }

        return money(value);
    }

    @Transactional
    public void establishInitialContractValue(
        UUID organizationId,
        UUID projectId,
        BigDecimal acceptedQuoteTotal
    ) {
        BigDecimal existing =
            jdbcTemplate.queryForObject(
                """
                SELECT contract_value
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                FOR UPDATE
                """,
                BigDecimal.class,
                projectId,
                organizationId
            );

        BigDecimal normalizedExisting =
            existing == null
                ? zero()
                : money(existing);

        BigDecimal normalizedQuote =
            money(
                acceptedQuoteTotal
            );

        if (
            normalizedExisting.signum() != 0
            && normalizedExisting.compareTo(
                normalizedQuote
            ) != 0
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PROJECT_CONTRACT_VALUE_ALREADY_ESTABLISHED",
                "The project contract value has already been established by another accepted contract quotation."
            );
        }

        if (
            normalizedExisting.signum() == 0
        ) {
            jdbcTemplate.update(
                """
                UPDATE projects
                SET contract_value = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                  AND organization_id = ?
                """,
                normalizedQuote,
                projectId,
                organizationId
            );
        }
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(4);
    }

    private BigDecimal money(
        BigDecimal value
    ) {
        return value.setScale(
            4,
            RoundingMode.HALF_UP
        );
    }
}
