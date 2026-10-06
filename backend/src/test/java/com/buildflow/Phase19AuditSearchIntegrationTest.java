package com.buildflow;

import com.buildflow.audit.dto.AuditSearchDtos;
import com.buildflow.audit.service.AuditLogSearchService;
import com.buildflow.audit.service.AuditLogService;
import com.buildflow.audit.service.AuditSearchCriteria;
import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase19AuditSearchIntegrationTest {

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
    AuditLogSearchService searchService;

    @Autowired
    AuditLogService auditLogService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    /**
     * The organization works in Paris time. Entries 1 to 8 belong to it and
     * were all written in March 2026; entry 9 belongs to another
     * organization. The services used to build the fixture write a few
     * entries of their own, dated today, so every search below is limited to
     * March 2026 and only sees the entries listed here.
     *
     *   1  09:00  owner  riverside  TASK_CREATED          1 Mar
     *   2  10:00  admin  riverside  CHANGE_ORDER_CREATED  5 Mar
     *   3  23:30  owner  harbor     SITE_REPORT_CREATED   10 Mar UTC, 11 Mar in Paris
     *   4  22:59  owner  harbor     SITE_REPORT_CREATED   11 Mar UTC, 11 Mar in Paris
     *   5  23:00  admin  harbor     INCIDENT_RESOLVED     11 Mar UTC, 12 Mar in Paris
     *   6  08:00  nobody no project USER_REGISTERED       12 Mar (has secrets)
     *   7  12:00  owner  riverside  TASK_UPDATED          15 Mar
     *   8  12:00  owner  riverside  TASK_UPDATED          15 Mar, same instant as 7
     *   9  09:00  other organization's owner              2 Mar
     */
    private record Fixture(
        UUID organizationId,
        UUID ownerId,
        String ownerLabel,
        UUID adminId,
        UUID riverside,
        UUID harbor,
        UUID otherOrganizationId,
        UUID otherOwnerId,
        UUID otherProject,
        Map<String, UUID> users
    ) {
    }

    private static UUID row(int number) {
        return UUID.fromString(
            String.format("19190000-0000-0000-0000-%012d", number)
        );
    }

    private static List<UUID> rows(int... numbers) {

        List<UUID> ids = new java.util.ArrayList<>();

        for (int number : numbers) {
            ids.add(row(number));
        }

        return ids;
    }

    private static List<UUID> ids(AuditSearchDtos.SearchPage page) {
        return page.items()
            .stream()
            .map(AuditSearchDtos.Entry::id)
            .toList();
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    void ownerSeesTheNewestEntriesFirstWithNamesFilledIn() {

        Fixture f = seed();

        AuditSearchDtos.SearchPage page =
            search(f, f.ownerId(), march(null, null, null, null, null, null, null));

        assertThat(page.totalElements()).isEqualTo(8L);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.page()).isEqualTo(0);
        assertThat(page.size()).isEqualTo(50);

        // Entries 7 and 8 share an instant, so the larger id comes first.
        assertThat(ids(page)).isEqualTo(rows(8, 7, 6, 5, 4, 3, 2, 1));

        AuditSearchDtos.Entry first = entry(page, 1);

        assertThat(first.action()).isEqualTo("TASK_CREATED");
        assertThat(first.entityType()).isEqualTo("TASK");
        assertThat(first.actorUserId()).isEqualTo(f.ownerId());
        assertThat(first.actorName()).isEqualTo(nameOf(f.ownerId()));
        assertThat(first.actorEmail()).isEqualTo(emailOf(f.ownerId()));
        assertThat(first.projectId()).isEqualTo(f.riverside());
        assertThat(first.projectName()).isEqualTo("Audit Riverside Lofts");
        assertThat(first.projectNumber()).isNotNull();
        assertThat(first.ipAddress()).isEqualTo("10.0.0.1");
        assertThat(first.createdAt())
            .isEqualTo(OffsetDateTime.parse("2026-03-01T09:00:00Z").toInstant());
        assertThat(first.newValues())
            .isEqualTo("TaskView[id=7, title=Pour slab, status=TODO]");

        AuditSearchDtos.Entry nobody = entry(page, 6);

        assertThat(nobody.actorUserId()).isNull();
        assertThat(nobody.actorName()).isNull();
        assertThat(nobody.actorEmail()).isNull();
        assertThat(nobody.projectId()).isNull();
        assertThat(nobody.projectName()).isNull();
    }

    @Test
    void anAdminMaySearchToo() {

        Fixture f = seed();

        AuditSearchDtos.SearchPage page =
            search(f, f.adminId(), march(null, null, null, null, null, null, null));

        assertThat(page.totalElements()).isEqualTo(8L);
    }

    @Test
    void resultsAreCutIntoPagesAndCounted() {

        Fixture f = seed();

        AuditSearchDtos.SearchPage second =
            search(f, f.ownerId(), march(null, null, null, null, null, 1, 3));

        assertThat(ids(second)).isEqualTo(rows(5, 4, 3));
        assertThat(second.page()).isEqualTo(1);
        assertThat(second.size()).isEqualTo(3);
        assertThat(second.totalElements()).isEqualTo(8L);
        assertThat(second.totalPages()).isEqualTo(3);

        AuditSearchDtos.SearchPage last =
            search(f, f.ownerId(), march(null, null, null, null, null, 2, 3));

        assertThat(ids(last)).isEqualTo(rows(2, 1));

        AuditSearchDtos.SearchPage beyond =
            search(f, f.ownerId(), march(null, null, null, null, null, 9, 3));

        assertThat(beyond.items()).isEmpty();
        assertThat(beyond.totalElements()).isEqualTo(8L);
    }

    @Test
    void aPageSizeAboveTheLimitIsCutToTheLimit() {

        Fixture f = seed();

        AuditSearchDtos.SearchPage page =
            search(f, f.ownerId(), march(null, null, null, null, null, null, 1000));

        assertThat(page.size()).isEqualTo(100);
        assertThat(page.items().size()).isEqualTo(8);
    }

    @Test
    void entriesCanBeFilteredByProjectPersonActionAndKind() {

        Fixture f = seed();

        assertThat(
            ids(
                search(
                    f,
                    f.ownerId(),
                    AuditSearchCriteria.of(
                        f.riverside(), null, null, null,
                        MARCH_FIRST, MARCH_LAST, null, null, null
                    )
                )
            )
        ).as("project").isEqualTo(rows(8, 7, 2, 1));

        assertThat(
            ids(
                search(
                    f,
                    f.ownerId(),
                    AuditSearchCriteria.of(
                        null, f.adminId(), null, null,
                        MARCH_FIRST, MARCH_LAST, null, null, null
                    )
                )
            )
        ).as("person").isEqualTo(rows(5, 2));

        assertThat(
            ids(
                search(
                    f,
                    f.ownerId(),
                    AuditSearchCriteria.of(
                        null, null, null, "TASK_UPDATED",
                        MARCH_FIRST, MARCH_LAST, null, null, null
                    )
                )
            )
        ).as("action").isEqualTo(rows(8, 7));

        assertThat(
            ids(
                search(
                    f,
                    f.ownerId(),
                    AuditSearchCriteria.of(
                        null, null, "SITE_REPORT", null,
                        MARCH_FIRST, MARCH_LAST, null, null, null
                    )
                )
            )
        ).as("kind").isEqualTo(rows(4, 3));

        assertThat(
            ids(
                search(
                    f,
                    f.ownerId(),
                    AuditSearchCriteria.of(
                        f.harbor(), f.ownerId(), null, null,
                        MARCH_FIRST, MARCH_LAST, null, null, null
                    )
                )
            )
        ).as("project and person together").isEqualTo(rows(4, 3));
    }

    @Test
    void daysAreReadInTheOrganizationsTimeZone() {

        Fixture f = seed();

        // 11 March in Paris runs from 10 March 23:00 UTC to 11 March 23:00
        // UTC, so entry 3 is in and entry 5 (exactly 23:00 UTC) is out.
        assertThat(
            ids(
                search(
                    f,
                    f.ownerId(),
                    AuditSearchCriteria.of(
                        null, null, null, null,
                        LocalDate.of(2026, 3, 11),
                        LocalDate.of(2026, 3, 11),
                        null, null, null
                    )
                )
            )
        ).as("11 March").isEqualTo(rows(4, 3));

        assertThat(
            ids(
                search(
                    f,
                    f.ownerId(),
                    AuditSearchCriteria.of(
                        null, null, null, null,
                        LocalDate.of(2026, 3, 12),
                        LocalDate.of(2026, 3, 12),
                        null, null, null
                    )
                )
            )
        ).as("12 March").isEqualTo(rows(6, 5));
    }

    @Test
    void openEndedRangesWork() {

        Fixture f = seed();

        assertThat(
            ids(
                search(
                    f,
                    f.ownerId(),
                    AuditSearchCriteria.of(
                        null, null, null, null,
                        LocalDate.of(2026, 3, 12), MARCH_LAST,
                        null, null, null
                    )
                )
            )
        ).as("from 12 March").isEqualTo(rows(8, 7, 6, 5));

        assertThat(
            ids(
                search(
                    f,
                    f.ownerId(),
                    AuditSearchCriteria.of(
                        null, null, null, null,
                        MARCH_FIRST, LocalDate.of(2026, 3, 2),
                        null, null, null
                    )
                )
            )
        ).as("until 2 March").isEqualTo(rows(1));
    }

    @Test
    void textSearchMatchesPeopleActionsAndValues() {

        Fixture f = seed();

        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, "pour slab", null, null)))
        ).as("a value, any case").isEqualTo(rows(7, 1));

        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, f.ownerLabel(), null, null)))
        ).as("a person's name").isEqualTo(rows(8, 7, 4, 3, 1));

        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, f.ownerLabel() + "@example", null, null)))
        ).as("a person's email").isEqualTo(rows(8, 7, 4, 3, 1));

        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, "change_order", null, null)))
        ).as("an action").isEqualTo(rows(2));

        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, "zed@acme", null, null)))
        ).as("an entry without a person").isEqualTo(rows(6));

        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, "no such thing", null, null)))
        ).as("nothing").isEqualTo(rows());
    }

    @Test
    void percentAndUnderscoreInTheTextAreOrdinaryCharacters() {

        Fixture f = seed();

        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, "50%", null, null)))
        ).as("50% is not 50 followed by anything").isEqualTo(rows(3));

        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, "%", null, null)))
        ).as("a lone percent sign").isEqualTo(rows(3));

        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, "snak_", null, null)))
        ).as("underscore is not a wildcard").isEqualTo(rows());

        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, "snake_case", null, null)))
        ).as("a real underscore").isEqualTo(rows(5));
    }

    @Test
    void quotesInTheTextCannotBreakTheSearch() {

        Fixture f = seed();

        AuditSearchDtos.SearchPage page =
            search(
                f,
                f.ownerId(),
                march(null, null, null, null, "'; DROP TABLE audit_logs; --", null, null)
            );

        assertThat(page.items()).isEmpty();

        // The table is still there and still holds the fixture.
        assertThat(
            search(f, f.ownerId(), march(null, null, null, null, null, null, null))
                .totalElements()
        ).isEqualTo(8L);
    }

    @Test
    void passwordsAndTokensAreMaskedInTheResult() {

        Fixture f = seed();

        AuditSearchDtos.Entry entry =
            entry(
                search(f, f.ownerId(), march(null, null, null, null, null, null, null)),
                6
            );

        assertThat(entry.newValues()).contains("zed@acme.test");
        assertThat(entry.newValues()).contains("[redacted]");
        assertThat(entry.newValues()).doesNotContain("$2a$10$secret");
        assertThat(entry.newValues()).doesNotContain("abc123");
    }

    @Test
    void onlyOwnersAndAdministratorsMayRead() {

        Fixture f = seed();

        for (String role : List.of("PROJECT_MANAGER", "ACCOUNTANT", "CLIENT")) {

            UUID userId = f.users().get(role);

            assertThatThrownBy(
                () ->
                    search(f, userId, march(null, null, null, null, null, null, null))
            ).as("search as " + role).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> {
                    assertThat(exception.getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(exception.getCode())
                        .isEqualTo("AUDIT_LOG_FORBIDDEN");
                }
            );

            assertThatThrownBy(
                () -> searchService.facets(f.organizationId(), userId)
            ).as("facets as " + role).isInstanceOfSatisfying(
                BusinessException.class,
                exception ->
                    assertThat(exception.getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN)
            );
        }
    }

    @Test
    void anotherOrganizationsEntriesAreNeverVisible() {

        Fixture f = seed();

        // The other organization's owner sees only entry 9.
        AuditSearchDtos.SearchPage theirs =
            searchService.search(
                f.otherOrganizationId(),
                f.otherOwnerId(),
                march(null, null, null, null, null, null, null)
            );

        assertThat(ids(theirs)).isEqualTo(rows(9));
        assertThat(theirs.items().get(0).projectName())
            .isEqualTo("Audit Other Site");

        // Entry 9 says "Pour slab" too, but the first organization never
        // gets it back, even when asked for that project directly.
        assertThat(
            ids(search(f, f.ownerId(), march(null, null, null, null, "pour slab", null, null)))
        ).isEqualTo(rows(7, 1));

        assertThat(
            ids(
                search(
                    f,
                    f.ownerId(),
                    AuditSearchCriteria.of(
                        f.otherProject(), null, null, null,
                        MARCH_FIRST, MARCH_LAST, null, null, null
                    )
                )
            )
        ).isEqualTo(rows());

        // And being the owner of one organization gives no access to another.
        assertThatThrownBy(
            () ->
                searchService.search(
                    f.organizationId(),
                    f.otherOwnerId(),
                    march(null, null, null, null, null, null, null)
                )
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(exception.getCode())
                    .isEqualTo("ORGANIZATION_NOT_FOUND");
            }
        );
    }

    @Test
    void filterChoicesListOnlyWhatThisOrganizationHas() {

        Fixture f = seed();

        AuditSearchDtos.Facets facets =
            searchService.facets(f.organizationId(), f.ownerId());

        assertThat(facets.actions())
            .contains(
                "TASK_CREATED",
                "TASK_UPDATED",
                "CHANGE_ORDER_CREATED",
                "SITE_REPORT_CREATED",
                "INCIDENT_RESOLVED",
                "USER_REGISTERED"
            );

        assertThat(facets.entityTypes())
            .contains(
                "TASK",
                "CHANGE_ORDER",
                "SITE_REPORT",
                "INCIDENT",
                "AUTH"
            );

        List<UUID> actors =
            facets.actors()
                .stream()
                .map(AuditSearchDtos.ActorOption::userId)
                .toList();

        assertThat(actors).contains(f.ownerId(), f.adminId());
        assertThat(actors).doesNotContain(f.otherOwnerId());

        List<UUID> projects =
            facets.projects()
                .stream()
                .map(AuditSearchDtos.ProjectOption::projectId)
                .toList();

        assertThat(projects).contains(f.riverside(), f.harbor());
        assertThat(projects).doesNotContain(f.otherProject());

        AuditSearchDtos.ActorOption owner =
            facets.actors()
                .stream()
                .filter(actor -> actor.userId().equals(f.ownerId()))
                .findFirst()
                .orElseThrow();

        assertThat(owner.name()).isEqualTo(nameOf(f.ownerId()));
        assertThat(owner.email()).isEqualTo(emailOf(f.ownerId()));
    }

    @Test
    void theOlderAuditListStillWorks() {

        Fixture f = seed();

        List<UUID> older =
            auditLogService
                .list(f.organizationId(), f.ownerId(), null)
                .stream()
                .map(view -> view.id())
                .toList();

        assertThat(older).contains(row(1), row(8));
        assertThat(older).doesNotContain(row(9));
    }

    // ------------------------------------------------------------------
    // Fixture
    // ------------------------------------------------------------------

    private static final LocalDate MARCH_FIRST = LocalDate.of(2026, 3, 1);

    private static final LocalDate MARCH_LAST = LocalDate.of(2026, 3, 31);

    /** March 2026 only, plus whatever else the test asks for. */
    private static AuditSearchCriteria march(
        UUID project,
        UUID actor,
        String entityType,
        String action,
        String text,
        Integer page,
        Integer size
    ) {
        return AuditSearchCriteria.of(
            project,
            actor,
            entityType,
            action,
            MARCH_FIRST,
            MARCH_LAST,
            text,
            page,
            size
        );
    }

    private AuditSearchDtos.SearchPage search(
        Fixture f,
        UUID userId,
        AuditSearchCriteria criteria
    ) {
        return searchService.search(
            f.organizationId(),
            userId,
            criteria
        );
    }

    private String nameOf(UUID userId) {

        return jdbcTemplate.queryForObject(
            "SELECT first_name || ' ' || last_name FROM app_users WHERE id = ?",
            String.class,
            userId
        );
    }

    private String emailOf(UUID userId) {

        return jdbcTemplate.queryForObject(
            "SELECT email FROM app_users WHERE id = ?",
            String.class,
            userId
        );
    }

    private static AuditSearchDtos.Entry entry(
        AuditSearchDtos.SearchPage page,
        int number
    ) {
        return page.items()
            .stream()
            .filter(item -> item.id().equals(row(number)))
            .findFirst()
            .orElseThrow();
    }

    private Fixture seed() {

        String suffix =
            UUID.randomUUID().toString().substring(0, 8);

        String ownerLabel = "owner-" + suffix;

        UUID ownerId = register(ownerLabel);

        UUID organizationId =
            createOrganization(
                ownerId,
                "Audit Construction " + suffix
            );

        UUID clientId = createClient(organizationId, ownerId);

        UUID riverside =
            createProject(
                organizationId,
                ownerId,
                clientId,
                "Audit Riverside Lofts",
                ProjectPriority.HIGH
            );

        UUID harbor =
            createProject(
                organizationId,
                ownerId,
                clientId,
                "Audit Harbor Office",
                ProjectPriority.MEDIUM
            );

        entityManager.flush();

        Map<String, UUID> users = new LinkedHashMap<>();

        for (String role : List.of(
            "ADMIN",
            "PROJECT_MANAGER",
            "ACCOUNTANT",
            "CLIENT"
        )) {

            UUID userId =
                register(
                    role.toLowerCase(java.util.Locale.ROOT).replace('_', '-')
                        + "-"
                        + suffix
                );

            addMembership(organizationId, userId, role);

            users.put(role, userId);
        }

        entityManager.flush();

        UUID adminId = users.get("ADMIN");

        // A second organization with an entry of its own.
        UUID otherOwnerId = register("owner-b-" + suffix);

        UUID otherOrganizationId =
            createOrganization(
                otherOwnerId,
                "Audit Other Builders " + suffix
            );

        UUID otherClientId =
            createClient(otherOrganizationId, otherOwnerId);

        UUID otherProject =
            createProject(
                otherOrganizationId,
                otherOwnerId,
                otherClientId,
                "Audit Other Site",
                ProjectPriority.MEDIUM
            );

        entityManager.flush();

        audit(1, organizationId, riverside, ownerId, "TASK_CREATED", "TASK",
            "TaskView[id=7, title=Pour slab, status=TODO]",
            "10.0.0.1", "2026-03-01T09:00:00Z");

        audit(2, organizationId, riverside, adminId, "CHANGE_ORDER_CREATED", "CHANGE_ORDER",
            "ChangeOrderView[id=3, title=Extra rebar, costImpact=1200]",
            "10.0.0.2", "2026-03-05T10:00:00Z");

        audit(3, organizationId, harbor, ownerId, "SITE_REPORT_CREATED", "SITE_REPORT",
            "SiteReportView[id=9, summary=Weather 50% delay]",
            "10.0.0.1", "2026-03-10T23:30:00Z");

        audit(4, organizationId, harbor, ownerId, "SITE_REPORT_CREATED", "SITE_REPORT",
            "SiteReportView[id=10, summary=Delivered 500 bricks]",
            "10.0.0.1", "2026-03-11T22:59:59Z");

        audit(5, organizationId, harbor, adminId, "INCIDENT_RESOLVED", "INCIDENT",
            "IncidentView[id=4, note=snake_case_name]",
            "10.0.0.2", "2026-03-11T23:00:00Z");

        audit(6, organizationId, null, null, "USER_REGISTERED", "AUTH",
            "RegisterView[email=zed@acme.test, passwordHash=$2a$10$secret, token=abc123]",
            "10.0.0.9", "2026-03-12T08:00:00Z");

        audit(7, organizationId, riverside, ownerId, "TASK_UPDATED", "TASK",
            "TaskView[id=7, title=Pour slab, status=DONE]",
            "10.0.0.1", "2026-03-15T12:00:00Z");

        audit(8, organizationId, riverside, ownerId, "TASK_UPDATED", "TASK",
            "TaskView[id=8, title=Frame walls, status=DONE]",
            "10.0.0.1", "2026-03-15T12:00:00Z");

        audit(9, otherOrganizationId, otherProject, otherOwnerId, "TASK_CREATED", "TASK",
            "TaskView[id=1, title=Pour slab, status=TODO]",
            "10.9.9.9", "2026-03-02T09:00:00Z");

        entityManager.flush();
        entityManager.clear();

        return new Fixture(
            organizationId,
            ownerId,
            ownerLabel,
            adminId,
            riverside,
            harbor,
            otherOrganizationId,
            otherOwnerId,
            otherProject,
            users
        );
    }

    private void audit(
        int number,
        UUID organizationId,
        UUID projectId,
        UUID actorId,
        String action,
        String entityType,
        String newValues,
        String ipAddress,
        String createdAt
    ) {

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", row(number));
        values.put("organization_id", organizationId);

        if (projectId != null) {
            values.put("project_id", projectId);
        }

        if (actorId != null) {
            values.put("actor_user_id", actorId);
        }

        values.put("action", action);
        values.put("entity_type", entityType);
        values.put("entity_id", UUID.randomUUID());
        values.put("new_values", newValues);
        values.put("ip_address", ipAddress);
        values.put("created_at", OffsetDateTime.parse(createdAt));

        insert("audit_logs", values, actorId, true);
    }

    private UUID createClient(UUID organizationId, UUID ownerId) {

        return clientService.create(
            organizationId,
            ownerId,
            new ClientDtos
                .CreateClientRequest(
                    ClientType.COMPANY,
                    "Audit Developments",
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
        ).id();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private UUID register(String label) {

        AuthService.AuthResult result =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Audit",
                    label,
                    label + "@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.1",
                "phase19-test"
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
