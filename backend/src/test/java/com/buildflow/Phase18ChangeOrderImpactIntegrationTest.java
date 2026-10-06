package com.buildflow;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.changeorder.dto.ChangeOrderImpactDtos;
import com.buildflow.changeorder.service.ChangeOrderImpactService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectRole;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;
import com.buildflow.shared.error.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase18ChangeOrderImpactIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer(
            "postgres:17-alpine"
        );

    @Autowired
    AuthService authService;

    @Autowired
    OrganizationService organizationService;

    @Autowired
    ClientService clientService;

    @Autowired
    ProjectService projectService;

    @Autowired
    ChangeOrderImpactService impactService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    /**
     * Project A: contract value 100,000, budget 80,000, planned end date
     * 30 June 2027, with these change orders:
     *
     *   approved   cost  5,000  price  8,000   3 days  (already included)
     *   submitted  cost 10,000  price 15,000  14 days
     *   draft      cost  3,000  price  2,000   0 days  (loses money)
     *   rejected   cost 99,999  price 99,999  30 days  (ignored)
     *
     * Nothing has been spent or committed, so the forecast cost starts at 0.
     * Project B has its own pending change order that must never leak into
     * the totals of project A.
     */
    private record Fixture(
        UUID organizationId,
        UUID ownerId,
        UUID projectA,
        UUID projectB,
        UUID approved,
        UUID submitted,
        UUID draft,
        UUID rejected,
        Map<String, UUID> users
    ) {
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    void submittedChangeOrderShowsBudgetContractMarginAndSchedule() {

        Fixture f = seed();

        ChangeOrderImpactDtos.Impact impact =
            impact(f, f.ownerId(), f.submitted());

        assertThat(impact.changeOrderId()).isEqualTo(f.submitted());
        assertThat(impact.projectId()).isEqualTo(f.projectA());
        assertThat(impact.status().name()).isEqualTo("SUBMITTED");
        assertThat(impact.pending()).isTrue();
        assertThat(impact.alreadyApproved()).isFalse();

        assertThat(impact.costImpact()).isEqualByComparingTo("10000");
        assertThat(impact.clientPriceImpact())
            .isEqualByComparingTo("15000");
        assertThat(impact.scheduleImpactDays()).isEqualTo(14);
        assertThat(impact.changeOrderProfit())
            .isEqualByComparingTo("5000");
        assertThat(impact.changeOrderMarginPercentage())
            .isEqualByComparingTo("33.3333");

        // Today: original figures plus the approved change order.
        ChangeOrderImpactDtos.Position now = impact.current();

        assertThat(now.budget()).isEqualByComparingTo("85000");
        assertThat(now.contractValue()).isEqualByComparingTo("108000");
        assertThat(now.plannedProfit()).isEqualByComparingTo("23000");
        assertThat(now.plannedMarginPercentage())
            .isEqualByComparingTo("21.2963");
        assertThat(now.forecastCost()).isEqualByComparingTo("0");
        assertThat(now.forecastProfit()).isEqualByComparingTo("108000");
        assertThat(now.budgetHeadroom()).isEqualByComparingTo("85000");
        assertThat(now.plannedEndDate())
            .isEqualTo(LocalDate.of(2027, 6, 30));

        // After approving it.
        ChangeOrderImpactDtos.Position then = impact.afterApproval();

        assertThat(then.budget()).isEqualByComparingTo("95000");
        assertThat(then.contractValue()).isEqualByComparingTo("123000");
        assertThat(then.plannedProfit()).isEqualByComparingTo("28000");
        assertThat(then.plannedMarginPercentage())
            .isEqualByComparingTo("22.7642");
        assertThat(then.forecastCost()).isEqualByComparingTo("10000");
        assertThat(then.forecastProfit()).isEqualByComparingTo("113000");
        assertThat(then.budgetHeadroom()).isEqualByComparingTo("85000");
        assertThat(then.plannedEndDate())
            .isEqualTo(LocalDate.of(2027, 7, 14));

        assertThat(impact.warnings())
            .containsExactly("SCHEDULE_DELAY");

        // Submitted and draft only: not the approved, rejected or other
        // project's change orders.
        assertThat(impact.pipeline().pendingCount()).isEqualTo(2);
        assertThat(impact.pipeline().pendingCostImpact())
            .isEqualByComparingTo("13000");
        assertThat(impact.pipeline().pendingClientPriceImpact())
            .isEqualByComparingTo("17000");
    }

    @Test
    void changeOrderThatLosesMoneyIsFlagged() {

        Fixture f = seed();

        ChangeOrderImpactDtos.Impact impact =
            impact(f, f.ownerId(), f.draft());

        assertThat(impact.pending()).isTrue();
        assertThat(impact.changeOrderProfit())
            .isEqualByComparingTo("-1000");
        assertThat(impact.changeOrderMarginPercentage())
            .isEqualByComparingTo("-50");

        assertThat(impact.afterApproval().plannedMarginPercentage())
            .isEqualByComparingTo("20");

        // Price below cost, and the planned margin falls by more than a
        // percentage point (21.30% to 20.00%).
        assertThat(impact.warnings())
            .containsExactly(
                "CHANGE_ORDER_LOSES_MONEY",
                "MARGIN_DROPS"
            );

        assertThat(impact.afterApproval().plannedEndDate())
            .isEqualTo(LocalDate.of(2027, 6, 30));
    }

    @Test
    void approvedAndRejectedChangeOrdersChangeNothing() {

        Fixture f = seed();

        ChangeOrderImpactDtos.Impact approved =
            impact(f, f.ownerId(), f.approved());

        assertThat(approved.pending()).isFalse();
        assertThat(approved.alreadyApproved()).isTrue();
        assertThat(approved.afterApproval())
            .isEqualTo(approved.current());
        assertThat(approved.current().budget())
            .isEqualByComparingTo("85000");
        assertThat(approved.current().contractValue())
            .isEqualByComparingTo("108000");
        assertThat(approved.warnings()).isEmpty();

        ChangeOrderImpactDtos.Impact rejected =
            impact(f, f.ownerId(), f.rejected());

        assertThat(rejected.pending()).isFalse();
        assertThat(rejected.alreadyApproved()).isFalse();
        assertThat(rejected.afterApproval())
            .isEqualTo(rejected.current());
        assertThat(rejected.warnings()).isEmpty();
    }

    @Test
    void administratorAndAccountantSeeTheSameNumbers() {

        Fixture f = seed();

        ChangeOrderImpactDtos.Impact owner =
            impact(f, f.ownerId(), f.submitted());

        for (String role : List.of("ADMIN", "ACCOUNTANT")) {

            ChangeOrderImpactDtos.Impact other =
                impact(f, f.users().get(role), f.submitted());

            assertThat(other)
                .as(role)
                .isEqualTo(owner);
        }
    }

    @Test
    void clientsAndPeopleOutsideTheProjectAreRefused() {

        Fixture f = seed();

        for (String role : List.of("CLIENT", "OUTSIDE_MANAGER")) {

            assertThatThrownBy(
                () ->
                    impact(f, f.users().get(role), f.submitted())
            ).as(role).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                    assertThat(exception.getStatus())
                        .isIn(
                            HttpStatus.FORBIDDEN,
                            HttpStatus.NOT_FOUND
                        )
            );
        }
    }

    @Test
    void unknownChangeOrderIsNotFound() {

        Fixture f = seed();

        assertThatThrownBy(
            () -> impact(f, f.ownerId(), UUID.randomUUID())
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(exception.getCode())
                    .isEqualTo("CHANGE_ORDER_NOT_FOUND");
            }
        );
    }

    @Test
    void anotherOrganizationCannotSeeTheChangeOrder() {

        Fixture f = seed();

        String suffix =
            UUID.randomUUID().toString().substring(0, 8);

        UUID strangerId = register("stranger-" + suffix);

        UUID strangerOrganization =
            createOrganization(
                strangerId,
                "Other Construction " + suffix
            );

        assertThatThrownBy(
            () ->
                impactService.impact(
                    strangerOrganization,
                    strangerId,
                    f.submitted()
                )
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(exception.getCode())
                    .isEqualTo("CHANGE_ORDER_NOT_FOUND");
            }
        );
    }

    // ------------------------------------------------------------------
    // Fixture
    // ------------------------------------------------------------------

    private ChangeOrderImpactDtos.Impact impact(
        Fixture f,
        UUID userId,
        UUID changeOrderId
    ) {
        return impactService.impact(
            f.organizationId(),
            userId,
            changeOrderId
        );
    }

    private Fixture seed() {

        String suffix =
            UUID.randomUUID().toString().substring(0, 8);

        UUID ownerId = register("owner-" + suffix);

        UUID organizationId =
            createOrganization(
                ownerId,
                "Impact Construction " + suffix
            );

        ClientDtos.ClientView client =
            clientService.create(
                organizationId,
                ownerId,
                new ClientDtos
                    .CreateClientRequest(
                        ClientType.COMPANY,
                        "Impact Developments",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    )
            );

        UUID projectA =
            createProject(
                organizationId,
                ownerId,
                client.id(),
                "Impact Tower Fitout",
                ProjectPriority.HIGH
            );

        UUID projectB =
            createProject(
                organizationId,
                ownerId,
                client.id(),
                "Impact Bridge Repairs",
                ProjectPriority.MEDIUM
            );

        entityManager.flush();

        Map<String, UUID> users = new LinkedHashMap<>();
        Map<String, UUID> memberships = new LinkedHashMap<>();

        // label -> organization role
        List<String[]> people =
            List.of(
                new String[] {"ADMIN", "ADMIN"},
                new String[] {"ACCOUNTANT", "ACCOUNTANT"},
                new String[] {"PROJECT_MANAGER", "PROJECT_MANAGER"},
                new String[] {"OUTSIDE_MANAGER", "PROJECT_MANAGER"},
                new String[] {"CLIENT", "CLIENT"}
            );

        for (String[] person : people) {

            String label = person[0];

            UUID userId =
                register(
                    label
                        .toLowerCase(Locale.ROOT)
                        .replace('_', '-')
                        + "-"
                        + suffix
                );

            memberships.put(
                label,
                addMembership(
                    organizationId,
                    userId,
                    person[1]
                )
            );

            users.put(label, userId);
        }

        entityManager.flush();

        // Adding a project manager rewrites the project row, so the money
        // columns are set afterwards and the persistence context cleared.
        projectService.addMember(
            organizationId,
            ownerId,
            projectA,
            new ProjectDtos.AddProjectMemberRequest(
                memberships.get("PROJECT_MANAGER"),
                ProjectRole.PROJECT_MANAGER
            )
        );

        entityManager.flush();

        setProjectMoney(
            organizationId,
            projectA,
            "100000",
            "80000",
            LocalDate.of(2027, 6, 30)
        );

        setProjectMoney(
            organizationId,
            projectB,
            "50000",
            "40000",
            LocalDate.of(2027, 3, 31)
        );

        entityManager.clear();

        UUID approved =
            changeOrder(
                organizationId, ownerId, projectA,
                "APPROVED", "5000", "8000", 3
            );

        UUID submitted =
            changeOrder(
                organizationId, ownerId, projectA,
                "SUBMITTED", "10000", "15000", 14
            );

        UUID draft =
            changeOrder(
                organizationId, ownerId, projectA,
                "DRAFT", "3000", "2000", 0
            );

        UUID rejected =
            changeOrder(
                organizationId, ownerId, projectA,
                "REJECTED", "99999", "99999", 30
            );

        changeOrder(
            organizationId, ownerId, projectB,
            "SUBMITTED", "777", "888", 5
        );

        return new Fixture(
            organizationId,
            ownerId,
            projectA,
            projectB,
            approved,
            submitted,
            draft,
            rejected,
            users
        );
    }

    private void setProjectMoney(
        UUID organizationId,
        UUID projectId,
        String contractValue,
        String originalBudget,
        LocalDate plannedEnd
    ) {
        int updated =
            jdbcTemplate.update(
                """
                UPDATE projects
                SET contract_value = ?,
                    original_budget = ?,
                    planned_end_date = ?
                WHERE id = ?
                  AND organization_id = ?
                """,
                new BigDecimal(contractValue),
                new BigDecimal(originalBudget),
                plannedEnd,
                projectId,
                organizationId
            );

        assertThat(updated).isEqualTo(1);
    }

    private UUID changeOrder(
        UUID organizationId,
        UUID requestedBy,
        UUID projectId,
        String status,
        String costImpact,
        String clientPriceImpact,
        int scheduleImpactDays
    ) {

        UUID id = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", id);
        values.put("organization_id", organizationId);
        values.put("project_id", projectId);
        values.put(
            "change_order_number",
            "CO-T18-" + id.toString().substring(0, 8)
        );
        values.put("title", status + " change order");
        values.put("cost_impact", new BigDecimal(costImpact));
        values.put(
            "client_price_impact",
            new BigDecimal(clientPriceImpact)
        );
        values.put("schedule_impact_days", scheduleImpactDays);
        values.put("status", status);
        values.put("requested_by", requestedBy);
        values.put("created_at", now);
        values.put("updated_at", now);

        if ("APPROVED".equals(status)) {
            values.put("approved_by", requestedBy);
            values.put("approved_at", now);
        }

        insert("change_orders", values, requestedBy, true);

        return id;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private UUID register(String label) {

        AuthService.AuthResult result =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Impact",
                    label,
                    label + "@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.1",
                "phase18-test"
            );

        entityManager.flush();

        return result.user().getId();
    }

    private UUID createOrganization(UUID ownerId, String name) {

        OrganizationDtos.OrganizationView organization =
            organizationService.create(
                ownerId,
                new OrganizationDtos
                    .CreateOrganizationRequest(
                        name,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Paris",
                        null,
                        "France",
                        "EUR",
                        "Europe/Paris",
                        null
                    )
            );

        entityManager.flush();

        return organization.id();
    }

    private UUID createProject(
        UUID organizationId,
        UUID ownerId,
        UUID clientId,
        String name,
        ProjectPriority priority
    ) {

        ProjectDtos.ProjectView project =
            projectService.create(
                organizationId,
                ownerId,
                new ProjectDtos
                    .CreateProjectRequest(
                        clientId,
                        name,
                        null,
                        ProjectType.RENOVATION,
                        priority,
                        null,
                        "Paris",
                        null,
                        "France",
                        null,
                        null,
                        "EUR",
                        null
                    )
            );

        return project.id();
    }

    private UUID addMembership(
        UUID organizationId,
        UUID userId,
        String role
    ) {

        UUID membershipId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", membershipId);
        values.put("organization_id", organizationId);
        values.put("user_id", userId);
        values.put("role", role);
        values.put("status", "ACTIVE");
        values.put("created_at", now);
        values.put("updated_at", now);
        values.put("joined_at", now);

        // Lenient: optional columns that do not exist are skipped.
        insert("organization_memberships", values, userId, false);

        return membershipId;
    }


    /**
     * Inserts one row using only the columns we know about, then fills any
     * other NOT NULL column that has no default so the fixture keeps working
     * if a migration adds a required column later.
     */
    private void insert(
        String table,
        Map<String, Object> values,
        UUID fallbackUser,
        boolean strict
    ) {

        List<Map<String, Object>> columns =
            jdbcTemplate.queryForList(
                """
                SELECT column_name,
                       data_type,
                       is_nullable,
                       column_default
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = ?
                ORDER BY ordinal_position
                """,
                table
            );

        assertThat(columns)
            .as("table %s exists", table)
            .isNotEmpty();

        Map<String, Object> row = new LinkedHashMap<>();

        for (Map<String, Object> column : columns) {

            String name = (String) column.get("column_name");

            if (values.containsKey(name)) {
                row.put(name, values.get(name));
                continue;
            }

            boolean required =
                "NO".equals(column.get("is_nullable"))
                    && column.get("column_default") == null;

            if (required) {
                row.put(
                    name,
                    filler(
                        (String) column.get("data_type"),
                        name,
                        fallbackUser
                    )
                );
            }
        }

        if (strict) {
            assertThat(row.keySet())
                .as("columns of %s", table)
                .containsAll(values.keySet());
        }

        String sql =
            "INSERT INTO "
                + table
                + " ("
                + String.join(", ", row.keySet())
                + ") VALUES ("
                + String.join(
                    ", ",
                    Collections.nCopies(row.size(), "?")
                )
                + ")";

        jdbcTemplate.update(sql, row.values().toArray());
    }

    private static Object filler(
        String dataType,
        String column,
        UUID fallbackUser
    ) {

        return switch (dataType) {
            case "uuid" ->
                fallbackUser != null
                    && (column.endsWith("_by")
                        || column.endsWith("_user_id"))
                    ? fallbackUser
                    : UUID.randomUUID();
            case "timestamp with time zone" ->
                OffsetDateTime.now();
            case "timestamp without time zone" ->
                LocalDateTime.now();
            case "date" ->
                LocalDate.now();
            case "boolean" ->
                Boolean.FALSE;
            case "smallint", "integer", "bigint", "numeric" ->
                0;
            default ->
                "x";
        };
    }
}
