package com.buildflow.commercial.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CommercialNumberService {

    private final JdbcTemplate jdbcTemplate;

    public CommercialNumberService(
        JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public String nextQuoteNumber(
        UUID organizationId,
        int year
    ) {
        return nextNumber(
            organizationId,
            year,
            "QUOTE",
            "QTE"
        );
    }

    @Transactional
    public String nextInvoiceNumber(
        UUID organizationId,
        int year
    ) {
        return nextNumber(
            organizationId,
            year,
            "INVOICE",
            "INV"
        );
    }

    private String nextNumber(
        UUID organizationId,
        int year,
        String type,
        String prefix
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO organization_number_sequences (
                organization_id,
                number_type,
                calendar_year,
                next_value
            )
            VALUES (?, ?, ?, 1)
            ON CONFLICT (
                organization_id,
                number_type,
                calendar_year
            )
            DO NOTHING
            """,
            organizationId,
            type,
            year
        );

        Long value =
            jdbcTemplate.queryForObject(
                """
                UPDATE organization_number_sequences
                SET next_value = next_value + 1
                WHERE organization_id = ?
                  AND number_type = ?
                  AND calendar_year = ?
                RETURNING next_value - 1
                """,
                Long.class,
                organizationId,
                type,
                year
            );

        if (value == null) {
            throw new IllegalStateException(
                "Could not allocate commercial document number."
            );
        }

        return String.format(
            "%s-%d-%04d",
            prefix,
            year,
            value
        );
    }
}
