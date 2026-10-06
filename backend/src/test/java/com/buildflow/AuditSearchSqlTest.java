package com.buildflow;

import com.buildflow.audit.service.AuditSearchCriteria;
import com.buildflow.audit.service.AuditSearchSql;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSearchSqlTest {

    private static final UUID ORGANIZATION = UUID.randomUUID();

    private static AuditSearchCriteria none() {
        return AuditSearchCriteria.of(
            null, null, null, null, null, null, null, null, null
        );
    }

    private static AuditSearchCriteria everything(String text) {
        return AuditSearchCriteria.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "CHANGE_ORDER",
            "CHANGE_ORDER_CREATED",
            LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31),
            text,
            2,
            20
        );
    }

    private static AuditSearchSql.Filter filterFor(
        AuditSearchCriteria criteria
    ) {
        return AuditSearchSql.filter(
            ORGANIZATION,
            criteria,
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-04-01T00:00:00Z")
        );
    }

    private static List<String> sorted(Set<String> names) {

        List<String> list = new ArrayList<>(names);

        Collections.sort(list);

        return list;
    }

    private static Set<String> namedParameters(String sql) {

        Set<String> names = new LinkedHashSet<>();

        Matcher matcher = Pattern.compile(":([A-Za-z]+)").matcher(sql);

        while (matcher.find()) {
            names.add(matcher.group(1));
        }

        return names;
    }

    @Test
    void withoutFiltersOnlyTheOrganizationIsChecked() {

        AuditSearchSql.Filter filter =
            AuditSearchSql.filter(ORGANIZATION, none(), null, null);

        assertThat(filter.where())
            .isEqualTo("a.organization_id = :organizationId");
        assertThat(new ArrayList<String>(filter.parameters().keySet()))
            .containsExactly("organizationId");
        assertThat(filter.parameters().get("organizationId"))
            .isEqualTo(ORGANIZATION);
    }

    @Test
    void everyFilterAddsItsOwnCondition() {

        AuditSearchCriteria criteria = everything("slab");

        AuditSearchSql.Filter filter = filterFor(criteria);

        assertThat(filter.where())
            .contains("a.project_id = :projectId");
        assertThat(filter.where())
            .contains("a.actor_user_id = :actorUserId");
        assertThat(filter.where())
            .contains("a.entity_type = :entityType");
        assertThat(filter.where())
            .contains("a.action = :action");
        assertThat(filter.where())
            .contains("a.created_at >= :fromInstant");
        assertThat(filter.where())
            .contains("a.created_at < :toInstant");
        assertThat(filter.where())
            .contains("a.new_values ILIKE :text ESCAPE '!'");

        assertThat(filter.parameters().get("projectId"))
            .isEqualTo(criteria.projectId());
        assertThat(filter.parameters().get("actorUserId"))
            .isEqualTo(criteria.actorUserId());
        assertThat(filter.parameters().get("entityType"))
            .isEqualTo("CHANGE_ORDER");
        assertThat(filter.parameters().get("action"))
            .isEqualTo("CHANGE_ORDER_CREATED");
        assertThat(filter.parameters().get("fromInstant"))
            .isEqualTo(Timestamp.from(Instant.parse("2026-03-01T00:00:00Z")));
        assertThat(filter.parameters().get("toInstant"))
            .isEqualTo(Timestamp.from(Instant.parse("2026-04-01T00:00:00Z")));
        assertThat(filter.parameters().get("text"))
            .isEqualTo("%slab%");
    }

    @Test
    void theEscapeCharacterMatchesTheOneInTheCriteria() {

        assertThat(String.valueOf(AuditSearchCriteria.LIKE_ESCAPE))
            .isEqualTo("!");
    }

    @Test
    void whatSomeoneTypedNeverBecomesPartOfTheSql() {

        String attack = "'; DROP TABLE audit_logs; --";

        AuditSearchSql.Filter filter = filterFor(everything(attack));

        assertThat(filter.where()).doesNotContain("DROP");
        assertThat(filter.where()).doesNotContain("--");
        assertThat(filter.where()).doesNotContain(attack);
        assertThat(filter.parameters().get("text"))
            .isEqualTo("%'; DROP TABLE audit!_logs; --%");
    }

    @Test
    void everyNamedParameterHasAValueAndEveryValueIsUsed() {

        AuditSearchSql.Filter filter = filterFor(everything("slab"));

        Set<String> given = new LinkedHashSet<>(filter.parameters().keySet());

        // The select also needs the two paging values.
        Set<String> givenForSelect = new LinkedHashSet<>(given);

        givenForSelect.add("limit");
        givenForSelect.add("offset");

        assertThat(
            sorted(
                namedParameters(AuditSearchSql.selectSql(filter.where()))
            )
        ).isEqualTo(sorted(givenForSelect));

        assertThat(
            sorted(
                namedParameters(AuditSearchSql.countSql(filter.where()))
            )
        ).isEqualTo(sorted(given));
    }

    @Test
    void theSelectIsNewestFirstAndPaged() {

        String sql = AuditSearchSql.selectSql("a.organization_id = :organizationId");

        assertThat(sql)
            .contains("WHERE a.organization_id = :organizationId");
        assertThat(sql)
            .contains("ORDER BY a.created_at DESC, a.id DESC");
        assertThat(sql)
            .contains("LIMIT :limit OFFSET :offset");
        assertThat(sql)
            .contains("LEFT JOIN app_users u ON u.id = a.actor_user_id");
    }

    @Test
    void theCountHasNoPaging() {

        String sql = AuditSearchSql.countSql("a.organization_id = :organizationId");

        assertThat(sql).contains("SELECT count(*)");
        assertThat(sql).doesNotContain("LIMIT");
        assertThat(sql).doesNotContain("ORDER BY");
    }

    @Test
    void theFacetQueriesAreLimitedToTheOrganization() {

        for (String sql : List.of(
            AuditSearchSql.ACTIONS,
            AuditSearchSql.ENTITY_TYPES,
            AuditSearchSql.ACTORS,
            AuditSearchSql.PROJECTS
        )) {
            assertThat(sql).contains(":organizationId");
            assertThat(sql).contains("LIMIT 500");
        }
    }
}
