package com.buildflow;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.procurement.entity.PurchaseOrderStatus;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectRole;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;
import com.buildflow.search.dto.SearchDtos;
import com.buildflow.search.service.SearchService;
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
class Phase17SearchIntegrationTest {

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
    SearchService searchService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    /**
     * One organization, two projects, everything named around "Harbour".
     *
     * Project A ("Harbour Tower Fitout") has three project members: a
     * project manager, a site manager and an engineer (organization role
     * SITE_MANAGER, project role ENGINEER). Project B ("Harbour Bridge
     * Repairs") has no members besides the owner.
     */
    private record Fixture(
        UUID organizationId,
        UUID ownerId,
        UUID projectA,
        UUID projectB,
        String numberA,
        UUID taskInspection,
        UUID taskLighting,
        UUID taskPercent,
        UUID taskUnderscore,
        UUID companyClient,
        UUID individualClient,
        UUID supplierHarbour,
        UUID invoiceA,
        UUID invoiceB,
        UUID purchaseOrderA,
        UUID purchaseOrderB,
        Map<String, UUID> users
    ) {
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    void ownerFindsEveryKindOfRecord() {

        Fixture f = seed();

        SearchDtos.SearchResponse result =
            search(f, f.ownerId(), "harbour");

        assertThat(result.query()).isEqualTo("harbour");

        // Both names start with the query, so they come back by name.
        assertThat(ids(result, "PROJECT"))
            .containsExactly(f.projectB(), f.projectA());

        // A title that starts with the query ranks before one that only
        // contains it, even though the second one is newer.
        assertThat(ids(result, "TASK"))
            .containsExactly(f.taskInspection(), f.taskLighting());

        assertThat(ids(result, "CLIENT"))
            .containsExactlyInAnyOrder(
                f.companyClient(),
                f.individualClient()
            );

        assertThat(ids(result, "SUPPLIER"))
            .containsExactly(f.supplierHarbour());

        // Matched through the supplier name, not the PO number.
        assertThat(ids(result, "PURCHASE_ORDER"))
            .containsExactly(f.purchaseOrderA());

        assertThat(ids(result, "INVOICE")).isEmpty();

        assertThat(result.hits()).hasSize(8);

        SearchDtos.SearchHit lighting =
            hit(result, f.taskLighting());

        assertThat(lighting.kind()).isEqualTo("TASK");
        assertThat(lighting.projectId()).isEqualTo(f.projectA());
        assertThat(lighting.title())
            .isEqualTo("Install harbour lighting");
        assertThat(lighting.subtitle())
            .startsWith("Harbour Tower Fitout");
        assertThat(lighting.status()).isEqualTo("TODO");

        SearchDtos.SearchHit individual =
            hit(result, f.individualClient());

        assertThat(individual.kind()).isEqualTo("CLIENT");
        assertThat(individual.projectId()).isNull();
        assertThat(individual.title()).isEqualTo("Hannah Harbourne");
        assertThat(individual.subtitle())
            .isEqualTo("hannah@harbourne.test");

        SearchDtos.SearchHit order =
            hit(result, f.purchaseOrderA());

        assertThat(order.projectId()).isEqualTo(f.projectA());
        assertThat(order.title()).isEqualTo("PO-HBR-001");
        assertThat(order.subtitle())
            .contains("Harbour Steel Supply")
            .contains("Harbour Tower Fitout");

        SearchDtos.SearchHit project =
            hit(result, f.projectA());

        assertThat(project.projectId()).isEqualTo(f.projectA());
        assertThat(project.subtitle())
            .contains(f.numberA())
            .contains("Harbour Developments");
    }

    @Test
    void invoicesPurchaseOrdersAndProjectNumbersAreFoundByNumber() {

        Fixture f = seed();

        SearchDtos.SearchResponse invoices =
            search(f, f.ownerId(), "inv-hbr");

        assertThat(invoices.hits()).hasSize(2);
        assertThat(ids(invoices, "INVOICE"))
            .containsExactlyInAnyOrder(f.invoiceA(), f.invoiceB());

        SearchDtos.SearchHit invoice = hit(invoices, f.invoiceA());

        assertThat(invoice.projectId()).isEqualTo(f.projectA());
        assertThat(invoice.title()).isEqualTo("INV-HBR-001");
        assertThat(invoice.status()).isEqualTo("ISSUED");
        assertThat(invoice.subtitle())
            .startsWith("Harbour Tower Fitout")
            .contains("due");

        SearchDtos.SearchResponse orders =
            search(f, f.ownerId(), "po-hbr");

        assertThat(orders.hits()).hasSize(2);
        assertThat(ids(orders, "PURCHASE_ORDER"))
            .containsExactlyInAnyOrder(
                f.purchaseOrderA(),
                f.purchaseOrderB()
            );

        SearchDtos.SearchResponse byNumber =
            search(f, f.ownerId(), f.numberA());

        assertThat(ids(byNumber, "PROJECT"))
            .containsExactly(f.projectA());
    }

    @Test
    void matchingIgnoresCaseAndSurroundingWhitespace() {

        Fixture f = seed();

        SearchDtos.SearchResponse plain =
            search(f, f.ownerId(), "harbour");

        SearchDtos.SearchResponse noisy =
            search(f, f.ownerId(), "   HaRbOuR  ");

        assertThat(noisy.query()).isEqualTo("HaRbOuR");

        assertThat(noisy.hits())
            .extracting(SearchDtos.SearchHit::id)
            .containsExactlyElementsOf(
                plain.hits()
                    .stream()
                    .map(SearchDtos.SearchHit::id)
                    .toList()
            );
    }

    @Test
    void queriesShorterThanTwoCharactersReturnNothing() {

        Fixture f = seed();

        for (String query : new String[] {null, "", " ", "h", " h "}) {

            SearchDtos.SearchResponse result =
                search(f, f.ownerId(), query);

            assertThat(result.hits())
                .as("query [%s]", query)
                .isEmpty();
        }
    }

    @Test
    void wildcardCharactersAreMatchedLiterally() {

        Fixture f = seed();

        SearchDtos.SearchResponse percent =
            search(f, f.ownerId(), "100%");

        assertThat(percent.hits()).hasSize(1);
        assertThat(ids(percent, "TASK"))
            .containsExactly(f.taskPercent());

        SearchDtos.SearchResponse underscore =
            search(f, f.ownerId(), "rebar_12");

        assertThat(underscore.hits()).hasSize(1);
        assertThat(ids(underscore, "TASK"))
            .containsExactly(f.taskUnderscore());

        assertThat(search(f, f.ownerId(), "%%").hits()).isEmpty();
        assertThat(search(f, f.ownerId(), "__").hits()).isEmpty();
        assertThat(search(f, f.ownerId(), "ha!").hits()).isEmpty();
    }

    @Test
    void eachRoleOnlyFindsWhatItIsAllowedToSee() {

        Fixture f = seed();

        // ----- Administrator and accountant: everything the owner has.

        for (String role : List.of("ADMIN", "ACCOUNTANT")) {

            UUID user = f.users().get(role);

            SearchDtos.SearchResponse result =
                search(f, user, "harbour");

            assertThat(ids(result, "PROJECT"))
                .as(role)
                .containsExactly(f.projectB(), f.projectA());
            assertThat(ids(result, "TASK"))
                .as(role)
                .containsExactly(f.taskInspection(), f.taskLighting());
            assertThat(ids(result, "CLIENT")).as(role).hasSize(2);
            assertThat(ids(result, "SUPPLIER")).as(role).hasSize(1);
            assertThat(ids(result, "PURCHASE_ORDER"))
                .as(role)
                .containsExactly(f.purchaseOrderA());

            assertThat(ids(search(f, user, "inv-hbr"), "INVOICE"))
                .as(role)
                .containsExactlyInAnyOrder(
                    f.invoiceA(),
                    f.invoiceB()
                );

            assertThat(ids(search(f, user, "po-hbr"), "PURCHASE_ORDER"))
                .as(role)
                .containsExactlyInAnyOrder(
                    f.purchaseOrderA(),
                    f.purchaseOrderB()
                );
        }

        // ----- Project manager of A: own project, clients and suppliers,
        //       purchase orders of A, no invoices.

        UUID manager = f.users().get("PROJECT_MANAGER");

        SearchDtos.SearchResponse managerResult =
            search(f, manager, "harbour");

        assertThat(ids(managerResult, "PROJECT"))
            .containsExactly(f.projectA());
        assertThat(ids(managerResult, "TASK"))
            .containsExactly(f.taskLighting());
        assertThat(ids(managerResult, "CLIENT")).hasSize(2);
        assertThat(ids(managerResult, "SUPPLIER")).hasSize(1);
        assertThat(ids(managerResult, "PURCHASE_ORDER"))
            .containsExactly(f.purchaseOrderA());

        assertThat(search(f, manager, "inv-hbr").hits()).isEmpty();

        assertThat(
            ids(search(f, manager, "po-hbr"), "PURCHASE_ORDER")
        ).containsExactly(f.purchaseOrderA());

        // ----- Site manager of A: no clients, suppliers yes.

        UUID siteManager = f.users().get("SITE_MANAGER");

        SearchDtos.SearchResponse siteResult =
            search(f, siteManager, "harbour");

        assertThat(ids(siteResult, "PROJECT"))
            .containsExactly(f.projectA());
        assertThat(ids(siteResult, "TASK"))
            .containsExactly(f.taskLighting());
        assertThat(ids(siteResult, "CLIENT")).isEmpty();
        assertThat(ids(siteResult, "SUPPLIER")).hasSize(1);
        assertThat(ids(siteResult, "PURCHASE_ORDER"))
            .containsExactly(f.purchaseOrderA());

        assertThat(search(f, siteManager, "inv-hbr").hits()).isEmpty();

        // ----- Engineer on A: can see the project and its tasks but
        //       procurement is limited to project/site managers.

        UUID engineer = f.users().get("ENGINEER");

        SearchDtos.SearchResponse engineerResult =
            search(f, engineer, "harbour");

        assertThat(ids(engineerResult, "PROJECT"))
            .containsExactly(f.projectA());
        assertThat(ids(engineerResult, "TASK"))
            .containsExactly(f.taskLighting());
        assertThat(ids(engineerResult, "CLIENT")).isEmpty();
        assertThat(ids(engineerResult, "PURCHASE_ORDER")).isEmpty();
        assertThat(search(f, engineer, "po-hbr").hits()).isEmpty();
        assertThat(search(f, engineer, "inv-hbr").hits()).isEmpty();
    }

    @Test
    void clientRoleCannotUseTheInternalSearch() {

        Fixture f = seed();

        assertThatThrownBy(
            () -> search(f, f.users().get("CLIENT"), "harbour")
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
                assertThat(exception.getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(exception.getCode())
                    .isEqualTo("INTERNAL_SEARCH_FORBIDDEN");
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

        UUID outsiderSupplier =
            supplier(
                otherOrganizationId,
                outsiderId,
                "Harbour Outsider Supply",
                null,
                null
            );

        UUID outsiderClient =
            companyClient(
                otherOrganizationId,
                outsiderId,
                "Harbour Outsider Client"
            );

        SearchDtos.SearchResponse own =
            searchService.search(
                otherOrganizationId,
                outsiderId,
                "harbour"
            );

        assertThat(own.hits())
            .extracting(SearchDtos.SearchHit::id)
            .containsExactlyInAnyOrder(outsiderSupplier, outsiderClient);

        assertThat(
            search(f, f.ownerId(), "outsider").hits()
        ).isEmpty();

        assertThatThrownBy(
            () ->
                searchService.search(
                    f.organizationId(),
                    outsiderId,
                    "harbour"
                )
        ).isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getStatus())
                    .isEqualTo(HttpStatus.NOT_FOUND)
        );
    }

    @Test
    void eachKindIsCappedAtFiveResults() {

        Fixture f = seed();

        for (int i = 1; i <= 7; i++) {

            task(
                f.organizationId(),
                f.ownerId(),
                f.projectA(),
                "Cap task " + i,
                "TODO",
                "MEDIUM",
                LocalDate.now().plusDays(i)
            );

            supplier(
                f.organizationId(),
                f.ownerId(),
                "Capsupplier " + i,
                null,
                null
            );
        }

        SearchDtos.SearchResponse tasks =
            search(f, f.ownerId(), "cap task");

        assertThat(ids(tasks, "TASK")).hasSize(5);

        SearchDtos.SearchResponse suppliers =
            search(f, f.ownerId(), "capsupplier");

        assertThat(ids(suppliers, "SUPPLIER")).hasSize(5);
    }

    // ------------------------------------------------------------------
    // Assertion helpers
    // ------------------------------------------------------------------

    private SearchDtos.SearchResponse search(
        Fixture f,
        UUID userId,
        String query
    ) {
        return searchService.search(
            f.organizationId(),
            userId,
            query
        );
    }

    private static List<UUID> ids(
        SearchDtos.SearchResponse response,
        String kind
    ) {
        return response.hits()
            .stream()
            .filter(hit -> kind.equals(hit.kind()))
            .map(SearchDtos.SearchHit::id)
            .toList();
    }

    private static SearchDtos.SearchHit hit(
        SearchDtos.SearchResponse response,
        UUID id
    ) {
        return response.hits()
            .stream()
            .filter(hit -> id.equals(hit.id()))
            .findFirst()
            .orElseThrow(
                () ->
                    new AssertionError(
                        "No hit with id "
                            + id
                            + " in "
                            + response.hits()
                    )
            );
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
                "Search Construction " + suffix
            );

        ClientDtos.ClientView client =
            clientService.create(
                organizationId,
                ownerId,
                new ClientDtos
                    .CreateClientRequest(
                        ClientType.COMPANY,
                        "Harbour Developments",
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
                "Harbour Tower Fitout",
                ProjectPriority.HIGH
            );

        UUID projectB =
            createProject(
                organizationId,
                ownerId,
                client.id(),
                "Harbour Bridge Repairs",
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
                new String[] {"SITE_MANAGER", "SITE_MANAGER"},
                new String[] {"ENGINEER", "SITE_MANAGER"},
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

        projectService.addMember(
            organizationId,
            ownerId,
            projectA,
            new ProjectDtos.AddProjectMemberRequest(
                memberships.get("ENGINEER"),
                ProjectRole.SITE_MANAGER
            )
        );

        entityManager.flush();

        int demoted =
            jdbcTemplate.update(
                """
                UPDATE project_members
                SET project_role = 'ENGINEER'
                WHERE organization_id = ?
                  AND project_id = ?
                  AND membership_id = ?
                """,
                organizationId,
                projectA,
                memberships.get("ENGINEER")
            );

        assertThat(demoted).isEqualTo(1);

        entityManager.clear();

        String numberA =
            projectService.list(organizationId, ownerId)
                .stream()
                .filter(project -> project.id().equals(projectA))
                .findFirst()
                .orElseThrow()
                .projectNumber();

        LocalDate today = LocalDate.now();

        // Inserted first and therefore older: it still ranks first when
        // searching "harbour" because its title starts with the query.
        UUID taskInspection =
            task(
                organizationId,
                ownerId,
                projectB,
                "Harbour piles inspection",
                "TODO",
                "HIGH",
                today.plusDays(5)
            );

        UUID taskLighting =
            task(
                organizationId,
                ownerId,
                projectA,
                "Install harbour lighting",
                "TODO",
                "MEDIUM",
                today.plusDays(6)
            );

        UUID taskPercent =
            task(
                organizationId,
                ownerId,
                projectA,
                "Seal 100% waterproof",
                "TODO",
                "LOW",
                today.plusDays(7)
            );

        task(
            organizationId,
            ownerId,
            projectA,
            "Seal 1000 joints",
            "TODO",
            "LOW",
            today.plusDays(7)
        );

        UUID taskUnderscore =
            task(
                organizationId,
                ownerId,
                projectA,
                "Order rebar_12mm",
                "TODO",
                "LOW",
                today.plusDays(8)
            );

        task(
            organizationId,
            ownerId,
            projectA,
            "Order rebarX12mm",
            "TODO",
            "LOW",
            today.plusDays(8)
        );

        UUID individualClient =
            individualClient(
                organizationId,
                ownerId,
                "Hannah",
                "Harbourne",
                "hannah@harbourne.test"
            );

        companyClient(organizationId, ownerId, "Zenith Builders");

        UUID supplierHarbour =
            supplier(
                organizationId,
                ownerId,
                "Harbour Steel Supply",
                "Omar Haddad",
                "omar@harbour-steel.test"
            );

        UUID supplierDelta =
            supplier(
                organizationId,
                ownerId,
                "Delta Concrete",
                "Dina Delta",
                null
            );

        UUID invoiceA =
            invoice(
                organizationId,
                projectA,
                client.id(),
                "INV-HBR-001",
                "ISSUED",
                today.plusDays(10),
                new BigDecimal("500.00"),
                BigDecimal.ZERO
            );

        UUID invoiceB =
            invoice(
                organizationId,
                projectB,
                client.id(),
                "INV-HBR-002",
                "ISSUED",
                today.plusDays(20),
                new BigDecimal("250.00"),
                BigDecimal.ZERO
            );

        UUID purchaseOrderA =
            purchaseOrder(
                organizationId,
                ownerId,
                projectA,
                supplierHarbour,
                "PO-HBR-001"
            );

        UUID purchaseOrderB =
            purchaseOrder(
                organizationId,
                ownerId,
                projectB,
                supplierDelta,
                "PO-HBR-002"
            );

        entityManager.flush();

        return new Fixture(
            organizationId,
            ownerId,
            projectA,
            projectB,
            numberA,
            taskInspection,
            taskLighting,
            taskPercent,
            taskUnderscore,
            client.id(),
            individualClient,
            supplierHarbour,
            invoiceA,
            invoiceB,
            purchaseOrderA,
            purchaseOrderB,
            users
        );
    }

    private UUID companyClient(
        UUID organizationId,
        UUID createdBy,
        String companyName
    ) {

        UUID id = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", id);
        values.put("organization_id", organizationId);
        values.put("type", "COMPANY");
        values.put("company_name", companyName);
        values.put("status", "ACTIVE");
        values.put("created_at", now);
        values.put("updated_at", now);

        insert("clients", values, createdBy, true);

        return id;
    }

    private UUID individualClient(
        UUID organizationId,
        UUID createdBy,
        String firstName,
        String lastName,
        String email
    ) {

        UUID id = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", id);
        values.put("organization_id", organizationId);
        values.put("type", "INDIVIDUAL");
        values.put("first_name", firstName);
        values.put("last_name", lastName);
        values.put("email", email);
        values.put("status", "ACTIVE");
        values.put("created_at", now);
        values.put("updated_at", now);

        insert("clients", values, createdBy, true);

        return id;
    }

    private UUID supplier(
        UUID organizationId,
        UUID createdBy,
        String name,
        String contactName,
        String email
    ) {

        UUID id = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", id);
        values.put("organization_id", organizationId);
        values.put("name", name);

        if (contactName != null) {
            values.put("contact_name", contactName);
        }

        if (email != null) {
            values.put("email", email);
        }

        values.put("status", "ACTIVE");
        values.put("created_at", now);
        values.put("updated_at", now);

        insert("suppliers", values, createdBy, true);

        return id;
    }

    private UUID purchaseOrder(
        UUID organizationId,
        UUID createdBy,
        UUID projectId,
        UUID supplierId,
        String number
    ) {

        UUID id = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        Map<String, Object> values = new LinkedHashMap<>();

        values.put("id", id);
        values.put("organization_id", organizationId);
        values.put("project_id", projectId);
        values.put("supplier_id", supplierId);
        values.put("purchase_order_number", number);
        values.put("status", PurchaseOrderStatus.values()[0].name());
        values.put("issue_date", LocalDate.now());
        values.put("subtotal", BigDecimal.ZERO);
        values.put("tax_amount", BigDecimal.ZERO);
        values.put("total", BigDecimal.ZERO);
        values.put("created_by", createdBy);
        values.put("created_at", now);
        values.put("updated_at", now);

        insert("purchase_orders", values, createdBy, true);

        return id;
    }

    private UUID register(String label) {

        AuthService.AuthResult result =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Search",
                    label,
                    label + "@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.1",
                "phase17-test"
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
