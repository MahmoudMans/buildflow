package com.buildflow.changeorder.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ChangeOrderNumberService {

    private final JdbcTemplate jdbcTemplate;

    public ChangeOrderNumberService(
        JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate =
            jdbcTemplate;
    }

    @Transactional
    public String next(
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
            VALUES (?, 'CHANGE_ORDER', ?, 1)
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

        Long value =
            jdbcTemplate.queryForObject(
                """
                UPDATE organization_number_sequences
                SET next_value = next_value + 1
                WHERE organization_id = ?
                  AND number_type = 'CHANGE_ORDER'
                  AND calendar_year = ?
                RETURNING next_value - 1
                """,
                Long.class,
                organizationId,
                year
            );

        if (value == null) {
            throw new IllegalStateException(
                "Could not allocate change-order number."
            );
        }

        return String.format(
            "CO-%d-%04d",
            year,
            value
        );
    }
}
