package com.buildflow;

import com.buildflow.audit.service.AuditSearchCriteria;
import com.buildflow.shared.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditSearchCriteriaTest {

    private static AuditSearchCriteria of(
        String text,
        Integer page,
        Integer size
    ) {
        return AuditSearchCriteria.of(
            null,
            null,
            null,
            null,
            null,
            null,
            text,
            page,
            size
        );
    }

    private static AuditSearchCriteria between(
        LocalDate from,
        LocalDate to
    ) {
        return AuditSearchCriteria.of(
            null,
            null,
            null,
            null,
            from,
            to,
            null,
            null,
            null
        );
    }

    @Test
    void noFiltersGivesTheFirstPageOfFifty() {

        AuditSearchCriteria criteria = of(null, null, null);

        assertThat(criteria.page()).isEqualTo(0);
        assertThat(criteria.size()).isEqualTo(50);
        assertThat(criteria.offset()).isEqualTo(0);
        assertThat(criteria.text()).isNull();
        assertThat(criteria.likePattern()).isNull();
        assertThat(criteria.fromInstant(ZoneId.of("UTC"))).isNull();
        assertThat(criteria.toExclusiveInstant(ZoneId.of("UTC"))).isNull();
    }

    @Test
    void pageSizeStaysBetweenOneAndOneHundred() {

        assertThat(of(null, null, 0).size()).isEqualTo(1);
        assertThat(of(null, null, -5).size()).isEqualTo(1);
        assertThat(of(null, null, 25).size()).isEqualTo(25);
        assertThat(of(null, null, 100).size()).isEqualTo(100);
        assertThat(of(null, null, 1000).size()).isEqualTo(100);
    }

    @Test
    void pageNumberStaysBetweenZeroAndTheLimit() {

        assertThat(of(null, -3, null).page()).isEqualTo(0);
        assertThat(of(null, 7, null).page()).isEqualTo(7);
        assertThat(of(null, 999999, null).page()).isEqualTo(5000);
    }

    @Test
    void offsetIsPageTimesSize() {

        assertThat(of(null, 3, 20).offset()).isEqualTo(60);
    }

    @Test
    void textIsTrimmedAndBlankTextMeansNoText() {

        assertThat(of("  slab  ", null, null).text()).isEqualTo("slab");
        assertThat(of("   ", null, null).text()).isNull();
        assertThat(of("", null, null).text()).isNull();
    }

    @Test
    void veryLongTextIsCut() {

        assertThat(of("x".repeat(500), null, null).text().length())
            .isEqualTo(100);
    }

    @Test
    void codesAreTrimmedAndBlankCodesMeanNoFilter() {

        AuditSearchCriteria criteria =
            AuditSearchCriteria.of(
                null,
                null,
                "  CHANGE_ORDER ",
                " ",
                null,
                null,
                null,
                null,
                null
            );

        assertThat(criteria.entityType()).isEqualTo("CHANGE_ORDER");
        assertThat(criteria.action()).isNull();
    }

    @Test
    void idsAreKept() {

        UUID project = UUID.randomUUID();
        UUID actor = UUID.randomUUID();

        AuditSearchCriteria criteria =
            AuditSearchCriteria.of(
                project,
                actor,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );

        assertThat(criteria.projectId()).isEqualTo(project);
        assertThat(criteria.actorUserId()).isEqualTo(actor);
    }

    @Test
    void likeWildcardsInTheTextAreEscaped() {

        assertThat(of("50%_off!", null, null).likePattern())
            .isEqualTo("%50!%!_off!!%");
    }

    @Test
    void aStartAfterTheEndIsRefused() {

        assertThatThrownBy(
            () ->
                between(
                    LocalDate.of(2026, 3, 12),
                    LocalDate.of(2026, 3, 11)
                )
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(exception.getCode())
                    .isEqualTo("AUDIT_INVALID_RANGE");
            }
        );
    }

    @Test
    void oneDayRangeIsAllowed() {

        AuditSearchCriteria criteria =
            between(
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 3, 11)
            );

        assertThat(criteria.from()).isEqualTo(LocalDate.of(2026, 3, 11));
        assertThat(criteria.to()).isEqualTo(LocalDate.of(2026, 3, 11));
    }

    @Test
    void daysAreReadInTheOrganizationTimeZone() {

        ZoneId paris = ZoneId.of("Europe/Paris");

        AuditSearchCriteria criteria =
            between(
                LocalDate.of(2026, 3, 11),
                LocalDate.of(2026, 3, 11)
            );

        assertThat(criteria.fromInstant(paris))
            .isEqualTo(Instant.parse("2026-03-10T23:00:00Z"));
        assertThat(criteria.toExclusiveInstant(paris))
            .isEqualTo(Instant.parse("2026-03-11T23:00:00Z"));
    }

    @Test
    void theDayTheClocksChangeIsTwentyThreeHoursLong() {

        ZoneId paris = ZoneId.of("Europe/Paris");

        AuditSearchCriteria criteria =
            between(
                LocalDate.of(2026, 3, 29),
                LocalDate.of(2026, 3, 29)
            );

        assertThat(criteria.fromInstant(paris))
            .isEqualTo(Instant.parse("2026-03-28T23:00:00Z"));
        assertThat(criteria.toExclusiveInstant(paris))
            .isEqualTo(Instant.parse("2026-03-29T22:00:00Z"));
    }

    @Test
    void anOpenEndedRangeHasOnlyOneBound() {

        ZoneId utc = ZoneId.of("UTC");

        AuditSearchCriteria onlyFrom =
            between(LocalDate.of(2026, 3, 11), null);

        assertThat(onlyFrom.fromInstant(utc))
            .isEqualTo(Instant.parse("2026-03-11T00:00:00Z"));
        assertThat(onlyFrom.toExclusiveInstant(utc)).isNull();

        AuditSearchCriteria onlyTo =
            between(null, LocalDate.of(2026, 3, 11));

        assertThat(onlyTo.fromInstant(utc)).isNull();
        assertThat(onlyTo.toExclusiveInstant(utc))
            .isEqualTo(Instant.parse("2026-03-12T00:00:00Z"));
    }

    @Test
    void datesAreReadFromIsoText() {

        assertThat(AuditSearchCriteria.parseDate(null)).isNull();
        assertThat(AuditSearchCriteria.parseDate("")).isNull();
        assertThat(AuditSearchCriteria.parseDate("   ")).isNull();
        assertThat(AuditSearchCriteria.parseDate("2026-03-31"))
            .isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(AuditSearchCriteria.parseDate(" 2026-03-31 "))
            .isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void textThatIsNotADateIsRefused() {

        for (String bad : new String[] {
            "31/03/2026",
            "yesterday",
            "2026-02-30",
            "2026-13-01"
        }) {
            assertThatThrownBy(
                () -> AuditSearchCriteria.parseDate(bad)
            ).as(bad).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> {
                    assertThat(exception.getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode())
                        .isEqualTo("AUDIT_INVALID_DATE");
                }
            );
        }
    }
}
