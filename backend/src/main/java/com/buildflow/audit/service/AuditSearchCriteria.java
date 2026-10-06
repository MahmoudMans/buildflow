package com.buildflow.audit.service;

import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * A validated audit search. Build it with {@link #of}, which trims the text,
 * limits the page size and rejects a start date after the end date.
 *
 * from and to are whole days in the organization's own time zone, and both
 * are inclusive.
 */
public record AuditSearchCriteria(
    UUID projectId,
    UUID actorUserId,
    String entityType,
    String action,
    LocalDate from,
    LocalDate to,
    String text,
    int page,
    int size
) {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 100;
    public static final int MAX_PAGE = 5_000;
    public static final int MAX_TEXT = 100;
    public static final int MAX_CODE = 200;

    /** Character that escapes % and _ in a LIKE pattern. */
    public static final char LIKE_ESCAPE = '!';

    public static AuditSearchCriteria of(
        UUID projectId,
        UUID actorUserId,
        String entityType,
        String action,
        LocalDate from,
        LocalDate to,
        String text,
        Integer page,
        Integer size
    ) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "AUDIT_INVALID_RANGE",
                "The start date must not be after the end date."
            );
        }

        return new AuditSearchCriteria(
            projectId,
            actorUserId,
            clean(entityType, MAX_CODE),
            clean(action, MAX_CODE),
            from,
            to,
            clean(text, MAX_TEXT),
            page == null
                ? 0
                : Math.min(Math.max(page, 0), MAX_PAGE),
            size == null
                ? DEFAULT_SIZE
                : Math.min(Math.max(size, 1), MAX_SIZE)
        );
    }

    /** Reads a date written like 2026-03-31. Blank means no date. */
    public static LocalDate parseDate(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "AUDIT_INVALID_DATE",
                "Dates must be written like 2026-03-31."
            );
        }
    }

    /** First instant of the from day, or null when there is no start. */
    public Instant fromInstant(ZoneId zone) {
        return from == null
            ? null
            : from.atStartOfDay(zone).toInstant();
    }

    /** First instant after the to day, or null when there is no end. */
    public Instant toExclusiveInstant(ZoneId zone) {
        return to == null
            ? null
            : to.plusDays(1).atStartOfDay(zone).toInstant();
    }

    public int offset() {
        return page * size;
    }

    /** A LIKE pattern that matches the text anywhere, or null. */
    public String likePattern() {
        return text == null
            ? null
            : "%" + escapeLike(text) + "%";
    }

    static String escapeLike(String value) {

        StringBuilder escaped = new StringBuilder(value.length() + 8);

        for (char c : value.toCharArray()) {
            if (c == LIKE_ESCAPE || c == '%' || c == '_') {
                escaped.append(LIKE_ESCAPE);
            }
            escaped.append(c);
        }

        return escaped.toString();
    }

    private static String clean(String value, int maxLength) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.length() > maxLength
            ? trimmed.substring(0, maxLength)
            : trimmed;
    }
}
