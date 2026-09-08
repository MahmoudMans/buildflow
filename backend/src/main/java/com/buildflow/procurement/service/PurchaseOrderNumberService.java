package com.buildflow.procurement.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PurchaseOrderNumberService {

    private final JdbcTemplate
        jdbcTemplate;

    public PurchaseOrderNumberService(
        JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate =
            jdbcTemplate;
    }

    @Transactional
    public String nextNumber(
        UUID organizationId,
        int year
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO organization_number_sequences (
                organization_id,
                number_type,
                calendar_year,
                next_value
            )
            VALUES (?, 'PURCHASE_ORDER', ?, 1)
            ON CONFLICT (
                organization_id,
                number_type,
                calendar_year
            )
            DO NOTHING
            """,
            organizationId,
            year
        );

        Long number =
            jdbcTemplate.queryForObject(
                """
                UPDATE organization_number_sequences
                SET next_value = next_value + 1
                WHERE organization_id = ?
                  AND number_type = 'PURCHASE_ORDER'
                  AND calendar_year = ?
                RETURNING next_value - 1
                """,
                Long.class,
                organizationId,
                year
            );

        if (number == null) {
            throw new IllegalStateException(
                "Could not allocate purchase order number."
            );
        }

        return String.format(
            "PO-%d-%04d",
            year,
            number
        );
    }
}
