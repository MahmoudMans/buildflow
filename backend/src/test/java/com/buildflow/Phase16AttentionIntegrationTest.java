package com.buildflow;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.dashboard.dto.AttentionDtos;
import com.buildflow.dashboard.dto.ReceivablesDtos;
import com.buildflow.dashboard.service.AttentionService;
import com.buildflow.dashboard.service.ReceivablesService;
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
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase16AttentionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer(
            "postgres:17-alpine"
        );

    private static final BigDecimal INVOICE_A =
        new BigDecimal("500.00");

    private static final BigDecimal INVOICE_B =
        new BigDecimal("250.00");

    @Autowired
    AuthService authService;

    @Autowired
    OrganizationService organizationService;

    @Autowired
    ClientService clientService;

    @Autowired
    ProjectService projectService;

    @Autowired
    AttentionService attentionService;

    @Autowired
    ReceivablesService receivablesService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    /**
     * One organization, two projects.
     *
     * Project A is ACTIVE and carries: one overdue task, one open CRITICAL
     * incident, one overdue invoice (500.00) and one change order in client
     * review, plus six rows that must NOT be reported: a future task, a DONE
     * task, a PAID invoice, a not-yet-due invoice, an APPROVED change order
     * and a RESOLVED incident.
     *
     * Project B stays in its initial status and carries: one overdue
     * (blocked) task, one open incident, one partially paid overdue
     * invoice with 250.00 outstanding.
     */
    private record Fixture(
        UUID organizationId,
        UUID ownerId,
        UUID projectA,
        UUID projectB,
        Map<String, UUID> users,
        Set<UUID> excluded
    ) {
    }

    @Test
    void ownerSeesEveryKindOfAttentionItemMostUrgentFirst() {

        Fixture f = seed();

        assertThat(visibleProjects(f, f.ownerId()))
            .contains(f.projectA(), f.projectB());

        AttentionDtos.AttentionSummary summary =
            attentionService.summary(
                f.organizationId(),
                f.ownerId()
            );

        assertThat(summary.financeVisible()).isTrue();
        assertThat(summary.overdueTasks()).isEqualTo(2L);
        assertThat(summary.openIncidents()).isEqualTo(2L);
        assertThat(summary.overdueInvoices()).isEqualTo(2L);
        assertThat(summary.overdueInvoiceAmount())
            .isEqualByComparingTo(INVOICE_A.add(INVOICE_B));
        assertThat(summary.changeOrdersAwaitingClient())
            .isEqualTo(1L);
        assertThat(summary.projectsAtRisk())
            .as(diagnostic(f))
            .isEqualTo(1L);

        assertThat(summary.items()).hasSize(8);

        assertThat(summary.items())
            .extracting(AttentionDtos.AttentionItem::kind)
            .contains(
                "PROJECT_AT_RISK",
                "OVERDUE_TASK",
                "OPEN_INCIDENT",
                "OVERDUE_INVOICE",
                "CHANGE_ORDER_REVIEW"
            );

        assertThat(summary.items())
            .anySatisfy(
                item -> {
                    assertThat(item.kind())
                        .isEqualTo("PROJECT_AT_RISK");
                    assertThat(item.projectId())
                        .isEqualTo(f.projectA());
                    assertThat(item.severity())
                        .isEqualTo("CRITICAL");
                }
            );

        assertThat(summary.items())
            .anySatisfy(
                item -> {
                    assertThat(item.kind())
                        .isEqualTo("OVERDUE_INVOICE");
                    assertThat(item.projectId())
                        .isEqualTo(f.projectA());
                    assertThat(item.amount())
                        .isEqualByComparingTo(INVOICE_A);
                }
            );

        assertThat(summary.items())
            .extracting(AttentionDtos.AttentionItem::entityId)
            .doesNotContainAnyElementsOf(f.excluded());

        List<Integer> ranks =
            summary.items()
                .stream()
                .map(item -> severityRank(item.severity()))
                .toList();

        assertThat(ranks).isSorted();
    }

    @Test
    void invoicesAreOnlyVisibleToFinanceRoles() {

        Fixture f = seed();

        for (String role : List.of("ADMIN", "ACCOUNTANT")) {

            assertThat(
                attentionService
                    .summary(
                        f.organizationId(),
                        f.users().get(role)
                    )
                    .financeVisible()
            )
                .as(role)
                .isTrue();
        }

        for (
            String role
            : List.of("PROJECT_MANAGER", "SITE_MANAGER")
        ) {

            AttentionDtos.AttentionSummary summary =
                attentionService.summary(
                    f.organizationId(),
                    f.users().get(role)
                );

            assertThat(summary.financeVisible())
                .as(role)
                .isFalse();

            assertThat(summary.overdueInvoices())
                .as(role)
                .isZero();

            assertThat(summary.overdueInvoiceAmount())
                .as(role)
                .isEqualByComparingTo(BigDecimal.ZERO);

            assertThat(summary.items())
                .as(role)
                .noneMatch(
                    item ->
                        "OVERDUE_INVOICE".equals(item.kind())
                );
        }
    }

    @Test
    void everyRoleOnlySeesItemsFromProjectsItCanAccess() {

        Fixture f = seed();

        // Global viewers see every project, field roles only the projects
        // they were added to, and clients see no internal project at all.
        assertThat(visibleProjects(f, f.ownerId()))
            .containsExactlyInAnyOrder(f.projectA(), f.projectB());

        assertThat(visibleProjects(f, f.users().get("ADMIN")))
            .containsExactlyInAnyOrder(f.projectA(), f.projectB());

        assertThat(visibleProjects(f, f.users().get("ACCOUNTANT")))
            .containsExactlyInAnyOrder(f.projectA(), f.projectB());

        assertThat(visibleProjects(f, f.users().get("PROJECT_MANAGER")))
            .containsExactly(f.projectA());

        assertThat(visibleProjects(f, f.users().get("SITE_MANAGER")))
            .containsExactly(f.projectA());

        assertThat(visibleProjects(f, f.users().get("CLIENT")))
            .isEmpty();

        assertScopedToVisibleProjects(
            f,
            "OWNER",
            f.ownerId(),
            true
        );

        assertScopedToVisibleProjects(
            f,
            "ADMIN",
            f.users().get("ADMIN"),
            true
        );

        assertScopedToVisibleProjects(
            f,
            "ACCOUNTANT",
            f.users().get("ACCOUNTANT"),
            true
        );

        assertScopedToVisibleProjects(
            f,
            "PROJECT_MANAGER",
            f.users().get("PROJECT_MANAGER"),
            false
        );

        assertScopedToVisibleProjects(
            f,
            "SITE_MANAGER",
            f.users().get("SITE_MANAGER"),
            false
        );

    }

    @Test
    void eachKindIsCappedWhileCountsStayExact() {

        Fixture f = seed();

        LocalDate today = LocalDate.now();

        for (int index = 0; index < 8; index++) {

            task(
                f.organizationId(),
                f.ownerId(),
                f.projectA(),
                "Extra overdue task " + index,
                "TODO",
                "LOW",
                today.minusDays(2 + index)
            );
        }

        AttentionDtos.AttentionSummary summary =
            attentionService.summary(
                f.organizationId(),
                f.ownerId()
            );

        assertThat(summary.overdueTasks()).isEqualTo(10L);

        assertThat(summary.items())
            .filteredOn(
                item -> "OVERDUE_TASK".equals(item.kind())
            )
            .hasSize(6);
    }

    @Test
    void clientRoleCannotReadTheInternalAttentionFeed() {

        Fixture f = seed();

        assertThatThrownBy(
            () ->
                attentionService.summary(
                    f.organizationId(),
                    f.users().get("CLIENT")
                )
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(exception.getCode())
                    .isEqualTo("INTERNAL_DASHBOARD_FORBIDDEN");
            }
        );
    }

    @Test
    void organizationsDoNotLeakIntoEachOther() {

        Fixture f = seed();

        String suffix =
            UUID.randomUUID().toString().substring(0, 8);

        UUID outsiderId = register("outsider-" + suffix);

        UUID otherOrganizationId =
            createOrganization(
                outsiderId,
                "Other Construction " + suffix
            );

        entityManager.flush();

        AttentionDtos.AttentionSummary own =
            attentionService.summary(
                otherOrganizationId,
                outsiderId
            );

        assertThat(own.items()).isEmpty();
        assertThat(own.overdueTasks()).isZero();
        assertThat(own.openIncidents()).isZero();
        assertThat(own.overdueInvoices()).isZero();
        assertThat(own.changeOrdersAwaitingClient()).isZero();
        assertThat(own.projectsAtRisk()).isZero();

        assertThatThrownBy(
            () ->
                attentionService.summary(
                    f.organizationId(),
                    outsiderId
                )
        ).isInstanceOf(RuntimeException.class);
    }

    @Test
    void receivablesAgeOutstandingInvoicesAndRankProjects() {

        Fixture f = seed();

        ReceivablesDtos.ReceivablesSummary summary =
            receivablesService.summary(
                f.organizationId(),
                f.ownerId()
            );

        assertThat(summary.available()).isTrue();
        assertThat(summary.totalOutstanding())
            .isEqualByComparingTo("950.00");
        assertThat(summary.totalOverdue())
            .isEqualByComparingTo("750.00");
        assertThat(summary.openInvoices()).isEqualTo(3L);
        assertThat(summary.overdueInvoices()).isEqualTo(2L);
        assertThat(summary.overduePercentage())
            .isEqualByComparingTo("78.9");
        assertThat(summary.laterAmount())
            .isEqualByComparingTo(BigDecimal.ZERO);

        List<ReceivablesDtos.AgingBucket> aging = summary.aging();

        assertThat(aging)
            .extracting(ReceivablesDtos.AgingBucket::key)
            .containsExactly(
                "CURRENT",
                "DAYS_1_30",
                "DAYS_31_60",
                "DAYS_61_90",
                "DAYS_90_PLUS"
            );

        assertThat(aging)
            .extracting(ReceivablesDtos.AgingBucket::invoices)
            .containsExactly(1L, 1L, 1L, 0L, 0L);

        // Not yet due: INV-ATT-A3 (200.00, due in 15 days)
        assertThat(aging.get(0).amount())
            .isEqualByComparingTo("200.00");

        // 5 days overdue: INV-ATT-B1 (250.00 outstanding)
        assertThat(aging.get(1).amount())
            .isEqualByComparingTo("250.00");

        // 40 days overdue: INV-ATT-A1 (500.00)
        assertThat(aging.get(2).amount())
            .isEqualByComparingTo("500.00");

        assertThat(aging.get(3).amount())
            .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(aging.get(4).amount())
            .isEqualByComparingTo(BigDecimal.ZERO);

        assertThat(summary.expected()).hasSize(6);

        assertThat(summary.expected().get(0).month())
            .isEqualTo(YearMonth.now().toString());

        String dueMonth =
            YearMonth
                .from(LocalDate.now().plusDays(15))
                .toString();

        assertThat(summary.expected())
            .filteredOn(item -> item.month().equals(dueMonth))
            .singleElement()
            .satisfies(
                item -> {
                    assertThat(item.invoices()).isEqualTo(1L);
                    assertThat(item.amount())
                        .isEqualByComparingTo("200.00");
                }
            );

        BigDecimal expectedTotal =
            summary.expected()
                .stream()
                .map(ReceivablesDtos.ExpectedCollection::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Overdue and paid invoices are not part of the forecast.
        assertThat(expectedTotal).isEqualByComparingTo("200.00");

        assertThat(summary.projects())
            .extracting(ReceivablesDtos.ProjectReceivable::projectId)
            .containsExactly(f.projectA(), f.projectB());

        ReceivablesDtos.ProjectReceivable first =
            summary.projects().get(0);

        assertThat(first.openInvoices()).isEqualTo(2L);
        assertThat(first.outstanding()).isEqualByComparingTo("700.00");
        assertThat(first.overdue()).isEqualByComparingTo("500.00");
        assertThat(first.oldestOverdueDays()).isEqualTo(40L);

        ReceivablesDtos.ProjectReceivable second =
            summary.projects().get(1);

        assertThat(second.openInvoices()).isEqualTo(1L);
        assertThat(second.outstanding()).isEqualByComparingTo("250.00");
        assertThat(second.overdue()).isEqualByComparingTo("250.00");
        assertThat(second.oldestOverdueDays()).isEqualTo(5L);
    }

    @Test
    void receivablesAreHiddenFromRolesOutsideFinance() {

        Fixture f = seed();

        for (String role : List.of("ADMIN", "ACCOUNTANT")) {

            ReceivablesDtos.ReceivablesSummary summary =
                receivablesService.summary(
                    f.organizationId(),
                    f.users().get(role)
                );

            assertThat(summary.available())
                .as(role)
                .isTrue();

            assertThat(summary.totalOutstanding())
                .as(role)
                .isEqualByComparingTo("950.00");
        }

        for (
            String role
            : List.of("PROJECT_MANAGER", "SITE_MANAGER")
        ) {

            ReceivablesDtos.ReceivablesSummary summary =
                receivablesService.summary(
                    f.organizationId(),
                    f.users().get(role)
                );

            assertThat(summary.available()).as(role).isFalse();
            assertThat(summary.totalOutstanding())
                .as(role)
                .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(summary.openInvoices()).as(role).isZero();
            assertThat(summary.aging()).as(role).isEmpty();
            assertThat(summary.expected()).as(role).isEmpty();
            assertThat(summary.projects()).as(role).isEmpty();
        }

        assertThatThrownBy(
            () ->
                receivablesService.summary(
                    f.organizationId(),
                    f.users().get("CLIENT")
                )
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN)
        );
    }

    @Test
    void receivablesDoNotLeakIntoOtherOrganizations() {

        Fixture f = seed();

        String suffix =
            UUID.randomUUID().toString().substring(0, 8);

        UUID outsiderId = register("outsider-" + suffix);

        UUID otherOrganizationId =
            createOrganization(
                outsiderId,
                "Other Construction " + suffix
            );

        entityManager.flush();

        ReceivablesDtos.ReceivablesSummary own =
            receivablesService.summary(
                otherOrganizationId,
                outsiderId
            );

        assertThat(own.available()).isTrue();
        assertThat(own.totalOutstanding())
            .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(own.openInvoices()).isZero();
        assertThat(own.projects()).isEmpty();

        assertThatThrownBy(
            () ->
                receivablesService.summary(
                    f.organizationId(),
                    outsiderId
                )
        ).isInstanceOf(RuntimeException.class);
    }

    // ------------------------------------------------------------------
    // Assertions
    // ------------------------------------------------------------------

    private void assertScopedToVisibleProjects(
        Fixture f,
        String label,
        UUID userId,
        boolean financeExpected
    ) {

        Set<UUID> visible = visibleProjects(f, userId);

        boolean seesA = visible.contains(f.projectA());
        boolean seesB = visible.contains(f.projectB());

        long tasks = (seesA ? 1 : 0) + (seesB ? 1 : 0);
        long incidents = (seesA ? 1 : 0) + (seesB ? 1 : 0);
        long invoices = (seesA ? 1 : 0) + (seesB ? 1 : 0);
        long changeOrders = seesA ? 1 : 0;

        BigDecimal amount = BigDecimal.ZERO;

        if (seesA) {
            amount = amount.add(INVOICE_A);
        }

        if (seesB) {
            amount = amount.add(INVOICE_B);
        }

        AttentionDtos.AttentionSummary summary =
            attentionService.summary(
                f.organizationId(),
                userId
            );

        assertThat(summary.financeVisible())
            .as(label + " financeVisible")
            .isEqualTo(financeExpected);

        assertThat(summary.overdueTasks())
            .as(label + " overdueTasks")
            .isEqualTo(tasks);

        assertThat(summary.openIncidents())
            .as(label + " openIncidents")
            .isEqualTo(incidents);

        assertThat(summary.overdueInvoices())
            .as(label + " overdueInvoices")
            .isEqualTo(financeExpected ? invoices : 0L);

        assertThat(summary.overdueInvoiceAmount())
            .as(label + " overdueInvoiceAmount")
            .isEqualByComparingTo(
                financeExpected ? amount : BigDecimal.ZERO
            );

        assertThat(summary.changeOrdersAwaitingClient())
            .as(label + " changeOrdersAwaitingClient")
            .isEqualTo(changeOrders);

        assertThat(summary.projectsAtRisk())
            .as(label + " projectsAtRisk; " + diagnostic(f))
            .isEqualTo(seesA ? 1L : 0L);

        assertThat(summary.items())
            .as(label + " items stay inside visible projects")
            .allSatisfy(
                item ->
                    assertThat(visible)
                        .contains(item.projectId())
            );

        if (!financeExpected) {

            assertThat(summary.items())
                .as(label + " has no invoice items")
                .noneMatch(
                    item ->
                        "OVERDUE_INVOICE".equals(item.kind())
                );
        }
    }

    private Set<UUID> visibleProjects(Fixture f, UUID userId) {

        return projectService
            .list(f.organizationId(), userId)
            .stream()
            .map(ProjectDtos.ProjectSummary::id)
            .collect(Collectors.toSet());
    }

    private static int severityRank(String severity) {

        if ("CRITICAL".equals(severity)) {
            return 0;
        }

        if ("WARNING".equals(severity)) {
            return 1;
        }

        return 2;
    }

    private String diagnostic(Fixture f) {

        String status =
            jdbcTemplate.queryForObject(
                "SELECT status FROM projects WHERE id = ?",
                String.class,
                f.projectA()
            );

        Long critical =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM site_incidents
                WHERE project_id = ?
                  AND severity = 'CRITICAL'
                  AND status NOT IN ('RESOLVED', 'CLOSED')
                """,
                Long.class,
                f.projectA()
            );

        return "project A status="
            + status
            + ", open critical incidents="
            + critical;
    }

    // ------------------------------------------------------------------
    // Fixture
    // ------------------------------------------------------------------

    private Fixture seed() {

        String suffix =
            UUID.randomUUID().toString().substring(0, 8);

        UUID ownerId = register("owner-" + suffix);

        UUID organizationId =
            createOrganization(
                ownerId,
                "Attention Construction " + suffix
            );

        ClientDtos.ClientView client =
            clientService.create(
                organizationId,
                ownerId,
                new ClientDtos
                    .CreateClientRequest(
                        ClientType.COMPANY,
                        "Attention Client",
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
                "Attention Project A",
                ProjectPriority.HIGH
            );

        UUID projectB =
            createProject(
                organizationId,
                ownerId,
                client.id(),
                "Attention Project B",
                ProjectPriority.MEDIUM
            );

        entityManager.flush();

        Map<String, UUID> users = new LinkedHashMap<>();
        Map<String, UUID> memberships = new LinkedHashMap<>();

        for (
            String role
            : List.of(
                "ADMIN",
                "ACCOUNTANT",
                "PROJECT_MANAGER",
                "SITE_MANAGER",
                "CLIENT"
            )
        ) {

            UUID userId =
                register(
                    role
                        .toLowerCase(Locale.ROOT)
                        .replace('_', '-')
                        + "-"
                        + suffix
                );

            memberships.put(
                role,
                addMembership(
                    organizationId,
                    userId,
                    role
                )
            );

            users.put(role, userId);
        }

        entityManager.flush();

        projectService.addMember(
            organizationId,
            ownerId,
            projectA,
            new ProjectDtos.AddProjectMemberRequest(
                memberships.get("PROJECT_MANAGER"),
                ProjectRole.PROJECT_MANAGER
            )
        );

        projectService.addMember(
            organizationId,
            ownerId,
            projectA,
            new ProjectDtos.AddProjectMemberRequest(
                memberships.get("SITE_MANAGER"),
                ProjectRole.SITE_MANAGER
            )
        );

        entityManager.flush();

        // Activate last: adding a project manager makes Hibernate write the
        // project row again, which would overwrite a status that was changed
        // earlier through plain SQL.
        int activated =
            jdbcTemplate.update(
                """
                UPDATE projects
                SET status = 'ACTIVE'
                WHERE id = ?
                  AND organization_id = ?
                """,
                projectA,
                organizationId
            );

        assertThat(activated).isEqualTo(1);

        entityManager.clear();

        LocalDate today = LocalDate.now();

        Set<UUID> excluded = new HashSet<>();

        // ----- Project A -----

        task(
            organizationId,
            ownerId,
            projectA,
            "Pour foundation slab",
            "TODO",
            "HIGH",
            today.minusDays(10)
        );

        excluded.add(
            task(
                organizationId,
                ownerId,
                projectA,
                "Order site signage",
                "TODO",
                "MEDIUM",
                today.plusDays(10)
            )
        );

        excluded.add(
            task(
                organizationId,
                ownerId,
                projectA,
                "Finished late",
                "DONE",
                "HIGH",
                today.minusDays(20)
            )
        );

        invoice(
            organizationId,
            projectA,
            client.id(),
            "INV-ATT-A1",
            "ISSUED",
            today.minusDays(40),
            INVOICE_A,
            BigDecimal.ZERO
        );

        excluded.add(
            invoice(
                organizationId,
                projectA,
                client.id(),
                "INV-ATT-A2",
                "PAID",
                today.minusDays(40),
                new BigDecimal("300.00"),
                new BigDecimal("300.00")
            )
        );

        excluded.add(
            invoice(
                organizationId,
                projectA,
                client.id(),
                "INV-ATT-A3",
                "ISSUED",
                today.plusDays(15),
                new BigDecimal("200.00"),
                BigDecimal.ZERO
            )
        );

        changeOrder(
            organizationId,
            ownerId,
            projectA,
            "CO-ATT-A1",
            "CLIENT_REVIEW"
        );

        excluded.add(
            changeOrder(
                organizationId,
                ownerId,
                projectA,
                "CO-ATT-A2",
                "APPROVED"
            )
        );

        incident(
            organizationId,
            ownerId,
            projectA,
            "INC-ATT-A1",
            "CRITICAL",
            "OPEN"
        );

        excluded.add(
            incident(
                organizationId,
                ownerId,
                projectA,
                "INC-ATT-A2",
                "HIGH",
                "RESOLVED"
            )
        );

        // ----- Project B -----

        task(
            organizationId,
            ownerId,
            projectB,
            "Order rebar",
            "BLOCKED",
            "CRITICAL",
            today.minusDays(3)
        );

        invoice(
            organizationId,
            projectB,
            client.id(),
            "INV-ATT-B1",
            "PARTIALLY_PAID",
            today.minusDays(5),
            new BigDecimal("400.00"),
            new BigDecimal("150.00")
        );

        incident(
            organizationId,
            ownerId,
            projectB,
            "INC-ATT-B1",
            "MEDIUM",
            "INVESTIGATING"
        );

        return new Fixture(
            organizationId,
            ownerId,
            projectA,
            projectB,
            users,
            excluded
        );
    }

    private UUID register(String label) {

        AuthService.AuthResult result =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Attention",
                    label,
                    label + "@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.1",
                "phase16-test"
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

    private UUID task(
        UUID organizationId,
        UUID createdBy,
        UUID projectId,
        String title,
        String status,
        String priority,
        LocalDate dueDate
    ) {

        UUID id = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", id);
        values.put("organization_id", organizationId);
        values.put("project_id", projectId);
        values.put("title", title);
        values.put("status", status);
        values.put("priority", priority);
        values.put("due_date", dueDate);
        values.put("created_by", createdBy);
        values.put("created_at", now);
        values.put("updated_at", now);

        insert("tasks", values, createdBy, true);

        return id;
    }

    private UUID invoice(
        UUID organizationId,
        UUID projectId,
        UUID clientId,
        String number,
        String status,
        LocalDate dueDate,
        BigDecimal total,
        BigDecimal paid
    ) {

        UUID id = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", id);
        values.put("organization_id", organizationId);
        values.put("project_id", projectId);
        values.put("client_id", clientId);
        values.put("invoice_number", number);
        values.put("status", status);
        values.put("issue_date", dueDate.minusDays(20));
        values.put("due_date", dueDate);
        values.put("subtotal", total);
        values.put("tax_amount", BigDecimal.ZERO);
        values.put("total", total);
        values.put("amount_paid", paid);
        values.put("amount_outstanding", total.subtract(paid));
        values.put("created_at", now);
        values.put("updated_at", now);

        insert("invoices", values, null, true);

        return id;
    }

    private UUID changeOrder(
        UUID organizationId,
        UUID requestedBy,
        UUID projectId,
        String number,
        String status
    ) {

        UUID id = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", id);
        values.put("organization_id", organizationId);
        values.put("project_id", projectId);
        values.put("change_order_number", number);
        values.put("title", "Change " + number);
        values.put("status", status);
        values.put("requested_by", requestedBy);
        values.put("submitted_at", now.minusDays(10));
        values.put("client_review_at", now.minusDays(9));
        values.put("created_at", now);
        values.put("updated_at", now);

        insert("change_orders", values, requestedBy, true);

        return id;
    }

    private UUID incident(
        UUID organizationId,
        UUID reportedBy,
        UUID projectId,
        String number,
        String severity,
        String status
    ) {

        UUID id = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", id);
        values.put("organization_id", organizationId);
        values.put("project_id", projectId);
        values.put("incident_number", number);
        values.put("type", "SAFETY");
        values.put("severity", severity);
        values.put("title", "Incident " + number);
        values.put("description", "Seeded by the attention test");
        values.put("occurred_at", now.minusDays(2));
        values.put("reported_at", now.minusDays(1));
        values.put("status", status);
        values.put("reported_by", reportedBy);
        values.put("created_at", now);
        values.put("updated_at", now);

        insert("site_incidents", values, reportedBy, true);

        return id;
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
