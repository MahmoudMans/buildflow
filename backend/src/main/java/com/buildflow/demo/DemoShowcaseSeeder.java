package com.buildflow.demo;

import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;

import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.nio.charset.StandardCharsets;

import java.sql.Timestamp;

import java.time.Instant;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Order(2)
@Component
@DependsOn(
    "demoPortfolioSeeder"
)
@ConditionalOnProperty(
    name = "buildflow.demo.enabled",
    havingValue = "true"
)
public class DemoShowcaseSeeder
    implements ApplicationRunner {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            DemoShowcaseSeeder.class
        );

    private static final Pattern IDENTIFIER =
        Pattern.compile(
            "[a-z0-9_]+"
        );

    private static final String ORGANIZATION_NAME =
        "NorthBuild Construction";

    private final ClientService clientService;
    private final ProjectService projectService;
    private final JdbcTemplate jdbcTemplate;

    private final Map<String, Integer>
        inserted =
            new LinkedHashMap<>();

    private final Map<String, Integer>
        existing =
            new LinkedHashMap<>();

    private final Map<String, Integer>
        skipped =
            new LinkedHashMap<>();

    private final Set<String>
        disabledTables =
            new java.util.HashSet<>();

    public DemoShowcaseSeeder(
        ClientService clientService,
        ProjectService projectService,
        JdbcTemplate jdbcTemplate
    ) {
        this.clientService =
            clientService;

        this.projectService =
            projectService;

        this.jdbcTemplate =
            jdbcTemplate;
    }

    @Override
    public void run(
        ApplicationArguments arguments
    ) {
        UUID organizationId =
            organizationId();

        UUID ownerId =
            userIdByEmail(
                "owner@buildflow.demo"
            );

        UUID managerId =
            userIdByEmail(
                "pm@buildflow.demo"
            );

        UUID siteManagerId =
            userIdByEmail(
                "site@buildflow.demo"
            );

        UUID accountantId =
            userIdByEmail(
                "accountant@buildflow.demo"
            );

        if (
            organizationId == null
            || ownerId == null
            || managerId == null
            || siteManagerId == null
            || accountantId == null
        ) {
            throw new IllegalStateException(
                "The base BuildFlow demo seed must exist before the showcase seed."
            );
        }

        LOGGER.info(
            "Creating/enriching BuildFlow showcase dataset for organization={}",
            organizationId
        );

        Map<String, UUID> clients =
            seedClients(
                organizationId,
                ownerId
            );

        Map<String, UUID> projects =
            seedProjects(
                organizationId,
                ownerId,
                managerId,
                clients
            );

        seedClientContacts(
            organizationId,
            ownerId,
            clients
        );

        List<UUID> suppliers =
            seedSuppliers(
                organizationId,
                ownerId
            );

        int projectIndex = 0;

        for (
            ProjectSeed project
            : projectSeeds()
        ) {
            UUID projectId =
                projects.get(
                    project.name()
                );

            UUID clientId =
                clients.get(
                    project.clientName()
                );

            if (
                projectId == null
                || clientId == null
            ) {
                LOGGER.warn(
                    "Skipping showcase bundle for project={} because required IDs were unavailable.",
                    project.name()
                );

                projectIndex++;
                continue;
            }

            List<UUID> milestoneIds =
                seedMilestones(
                    organizationId,
                    projectId,
                    ownerId,
                    project,
                    projectIndex
                );

            seedTasks(
                organizationId,
                projectId,
                ownerId,
                managerId,
                siteManagerId,
                milestoneIds,
                project,
                projectIndex
            );

            List<UUID> budgetCategories =
                seedBudget(
                    organizationId,
                    projectId,
                    ownerId,
                    project,
                    projectIndex
                );

            if (
                !"DRAFT".equals(
                    project.status()
                )
            ) {
                List<UUID> purchaseOrders =
                    seedProcurement(
                        organizationId,
                        projectId,
                        ownerId,
                        suppliers,
                        project,
                        projectIndex
                    );

                seedExpenses(
                    organizationId,
                    projectId,
                    ownerId,
                    accountantId,
                    suppliers,
                    purchaseOrders,
                    budgetCategories,
                    project,
                    projectIndex
                );

                seedQuotes(
                    organizationId,
                    projectId,
                    clientId,
                    ownerId,
                    project,
                    projectIndex
                );

                seedChangeOrders(
                    organizationId,
                    projectId,
                    ownerId,
                    project,
                    projectIndex
                );
            }

            if (
                Set.of(
                    "ACTIVE",
                    "ON_HOLD",
                    "COMPLETED"
                )
                .contains(
                    project.status()
                )
            ) {
                seedInvoicesAndPayments(
                    organizationId,
                    projectId,
                    clientId,
                    ownerId,
                    accountantId,
                    project,
                    projectIndex
                );

                seedSiteReports(
                    organizationId,
                    projectId,
                    siteManagerId,
                    project,
                    projectIndex
                );

                seedIncidents(
                    organizationId,
                    projectId,
                    siteManagerId,
                    managerId,
                    project,
                    projectIndex
                );
            }

            projectIndex++;
        }

        seedNotifications(
            organizationId,
            ownerId,
            managerId,
            siteManagerId,
            accountantId,
            projects
        );

        LOGGER.info(
            "BuildFlow showcase seed complete. inserted={}, existing={}, skipped={}",
            inserted,
            existing,
            skipped
        );
    }

    // =========================================================================
    // Clients
    // =========================================================================

    private Map<String, UUID> seedClients(
        UUID organizationId,
        UUID ownerId
    ) {
        Map<String, UUID> result =
            new LinkedHashMap<>();

        for (
            ClientSeed seed
            : clientSeeds()
        ) {
            UUID id =
                ensureClient(
                    organizationId,
                    ownerId,
                    seed
                );

            if (id != null) {
                result.put(
                    seed.name(),
                    id
                );
            }
        }

        return result;
    }

    private List<ClientSeed> clientSeeds() {
        return List.of(
            new ClientSeed(
                "Rivoli Hospitality Group",
                "finance@rivoli.demo",
                "+33 1 42 74 18 20",
                "Paris"
            ),
            new ClientSeed(
                "Belleville Workspace SAS",
                "operations@belleville.demo",
                "+33 1 43 66 20 18",
                "Paris"
            ),
            new ClientSeed(
                "Claire Dubois",
                "client@buildflow.demo",
                "+33 6 41 28 72 19",
                "Neuilly-sur-Seine"
            ),
            new ClientSeed(
                "Atelier Montmartre Hotels",
                "projects@montmartre-hotels.demo",
                "+33 1 46 06 44 17",
                "Paris"
            ),
            new ClientSeed(
                "Defense Corporate Offices",
                "facilities@defense-offices.demo",
                "+33 1 47 75 11 28",
                "Puteaux"
            ),
            new ClientSeed(
                "Boulogne Sante",
                "development@boulogne-sante.demo",
                "+33 1 46 03 16 80",
                "Boulogne-Billancourt"
            ),
            new ClientSeed(
                "Maison Versailles",
                "renovation@maison-versailles.demo",
                "+33 1 39 50 82 14",
                "Versailles"
            ),
            new ClientSeed(
                "Levallois Retail Partners",
                "stores@levallois-retail.demo",
                "+33 1 47 57 22 91",
                "Levallois-Perret"
            ),
            new ClientSeed(
                "Issy Riverside Offices",
                "workspace@issy-riverside.demo",
                "+33 1 40 93 71 52",
                "Issy-les-Moulineaux"
            ),
            new ClientSeed(
                "Seine Logistics",
                "property@seine-logistics.demo",
                "+33 1 48 20 61 43",
                "Saint-Denis"
            )
        );
    }

    private UUID ensureClient(
        UUID organizationId,
        UUID actorUserId,
        ClientSeed seed
    ) {
        UUID existingId =
            namedEntityId(
                "clients",
                organizationId,
                seed.name(),
                List.of(
                    "company_name",
                    "name",
                    "display_name"
                )
            );

        if (existingId != null) {
            count(
                existing,
                "clients"
            );

            return existingId;
        }

        Map<String, Object> values =
            new LinkedHashMap<>();

        values.put(
            "type",
            ClientType.COMPANY
        );

        values.put(
            "name",
            seed.name()
        );

        values.put(
            "companyName",
            seed.name()
        );

        values.put(
            "displayName",
            seed.name()
        );

        values.put(
            "firstName",
            seed.name()
        );

        values.put(
            "email",
            seed.email()
        );

        values.put(
            "phone",
            seed.phone()
        );

        values.put(
            "billingAddress",
            seed.city()
                + ", France"
        );

        values.put(
            "siteAddress",
            seed.city()
                + ", France"
        );

        values.put(
            "notes",
            "BuildFlow showcase client."
        );

        ClientDtos.CreateClientRequest
            request =
                record(
                    ClientDtos
                        .CreateClientRequest
                        .class,
                    values
                );

        ClientDtos.ClientView client =
            clientService.create(
                organizationId,
                actorUserId,
                request
            );

        count(
            inserted,
            "clients"
        );

        return client.id();
    }

    // =========================================================================
    // Projects
    // =========================================================================

    private Map<String, UUID> seedProjects(
        UUID organizationId,
        UUID ownerId,
        UUID managerId,
        Map<String, UUID> clients
    ) {
        Map<String, UUID> result =
            new LinkedHashMap<>();

        for (
            ProjectSeed seed
            : projectSeeds()
        ) {
            UUID clientId =
                clients.get(
                    seed.clientName()
                );

            if (clientId == null) {
                continue;
            }

            UUID projectId =
                ensureProject(
                    organizationId,
                    ownerId,
                    clientId,
                    seed
                );

            if (projectId == null) {
                continue;
            }

            configureProject(
                projectId,
                managerId,
                seed
            );

            result.put(
                seed.name(),
                projectId
            );
        }

        return result;
    }

    private List<ProjectSeed> projectSeeds() {
        return List.of(
            new ProjectSeed(
                "Rivoli Hotel Renovation",
                "Rivoli Hospitality Group",
                "Full occupied-hotel renovation including guest rooms, reception, MEP upgrades and phased handover.",
                ProjectType.RENOVATION,
                ProjectPriority.HIGH,
                "28 Rue de Rivoli",
                "Paris",
                "75004",
                new BigDecimal(
                    "1850000.00"
                ),
                new BigDecimal(
                    "1420000.00"
                ),
                "ACTIVE",
                68,
                8,
                5
            ),
            new ProjectSeed(
                "Belleville Office Fit-Out",
                "Belleville Workspace SAS",
                "Fast-track commercial fit-out with partitions, lighting, electrical works and collaborative workspaces.",
                ProjectType.INTERIOR,
                ProjectPriority.MEDIUM,
                "72 Rue de Belleville",
                "Paris",
                "75020",
                new BigDecimal(
                    "640000.00"
                ),
                new BigDecimal(
                    "495000.00"
                ),
                "ACTIVE",
                81,
                5,
                2
            ),
            new ProjectSeed(
                "Neuilly Residence Extension",
                "Claire Dubois",
                "Residential extension and renovation with structural opening, roofing, plumbing and landscaping packages.",
                ProjectType.RENOVATION,
                ProjectPriority.HIGH,
                "14 Avenue Sainte-Foy",
                "Neuilly-sur-Seine",
                "92200",
                new BigDecimal(
                    "920000.00"
                ),
                new BigDecimal(
                    "710000.00"
                ),
                "PLANNING",
                18,
                1,
                9
            ),
            new ProjectSeed(
                "Montmartre Boutique Hotel Conversion",
                "Atelier Montmartre Hotels",
                "Conversion of a historic residential building into a 34-room boutique hotel with premium finishes.",
                ProjectType.RENOVATION,
                ProjectPriority.HIGH,
                "19 Rue Lepic",
                "Paris",
                "75018",
                new BigDecimal(
                    "2780000.00"
                ),
                new BigDecimal(
                    "2140000.00"
                ),
                "ACTIVE",
                46,
                6,
                8
            ),
            new ProjectSeed(
                "La Defense HQ Refurbishment",
                "Defense Corporate Offices",
                "Multi-floor headquarters refurbishment with phased occupation, meeting suites and MEP modernization.",
                ProjectType.INTERIOR,
                ProjectPriority.HIGH,
                "12 Esplanade du General de Gaulle",
                "Puteaux",
                "92800",
                new BigDecimal(
                    "3420000.00"
                ),
                new BigDecimal(
                    "2650000.00"
                ),
                "ACTIVE",
                57,
                7,
                6
            ),
            new ProjectSeed(
                "Boulogne Medical Clinic Fit-Out",
                "Boulogne Sante",
                "Clinical interior fit-out including treatment rooms, specialist electrical installations and accessibility works.",
                ProjectType.INTERIOR,
                ProjectPriority.HIGH,
                "38 Route de la Reine",
                "Boulogne-Billancourt",
                "92100",
                new BigDecimal(
                    "1280000.00"
                ),
                new BigDecimal(
                    "970000.00"
                ),
                "ACTIVE",
                84,
                7,
                2
            ),
            new ProjectSeed(
                "Versailles Townhouse Restoration",
                "Maison Versailles",
                "Completed restoration of a historic townhouse including masonry, roof, joinery and interior finishes.",
                ProjectType.RENOVATION,
                ProjectPriority.MEDIUM,
                "8 Rue des Reservoirs",
                "Versailles",
                "78000",
                new BigDecimal(
                    "760000.00"
                ),
                new BigDecimal(
                    "595000.00"
                ),
                "COMPLETED",
                100,
                14,
                -1
            ),
            new ProjectSeed(
                "Levallois Retail Flagship",
                "Levallois Retail Partners",
                "New flagship retail interior with custom joinery, lighting, storefront and point-of-sale areas.",
                ProjectType.INTERIOR,
                ProjectPriority.MEDIUM,
                "41 Rue du President Wilson",
                "Levallois-Perret",
                "92300",
                new BigDecimal(
                    "510000.00"
                ),
                new BigDecimal(
                    "395000.00"
                ),
                "DRAFT",
                0,
                0,
                6
            ),
            new ProjectSeed(
                "Issy Riverside Office Renovation",
                "Issy Riverside Offices",
                "Office renovation with acoustic upgrades, flexible workspaces, electrical modernization and new finishes.",
                ProjectType.RENOVATION,
                ProjectPriority.MEDIUM,
                "6 Quai de la Bataille de Stalingrad",
                "Issy-les-Moulineaux",
                "92130",
                new BigDecimal(
                    "1160000.00"
                ),
                new BigDecimal(
                    "905000.00"
                ),
                "ON_HOLD",
                34,
                5,
                7
            ),
            new ProjectSeed(
                "Saint-Denis Logistics Office Upgrade",
                "Seine Logistics",
                "Operational office modernization within an active logistics site including MEP, partitions and safety upgrades.",
                ProjectType.MAINTENANCE,
                ProjectPriority.MEDIUM,
                "55 Avenue du President Wilson",
                "Saint-Denis",
                "93200",
                new BigDecimal(
                    "430000.00"
                ),
                new BigDecimal(
                    "335000.00"
                ),
                "PLANNING",
                12,
                1,
                5
            )
        );
    }

    private UUID ensureProject(
        UUID organizationId,
        UUID actorUserId,
        UUID clientId,
        ProjectSeed seed
    ) {
        UUID existingId =
            namedEntityId(
                "projects",
                organizationId,
                seed.name(),
                List.of(
                    "name"
                )
            );

        if (existingId != null) {
            count(
                existing,
                "projects"
            );

            return existingId;
        }

        LocalDate startDate =
            LocalDate.now()
                .minusMonths(
                    seed.startMonthsAgo()
                );

        LocalDate plannedEnd =
            seed.endMonthsAhead() >= 0
                ? LocalDate.now()
                    .plusMonths(
                        seed.endMonthsAhead()
                    )
                : LocalDate.now()
                    .minusMonths(
                        Math.abs(
                            seed.endMonthsAhead()
                        )
                    );

        Map<String, Object> values =
            new LinkedHashMap<>();

        values.put(
            "clientId",
            clientId
        );

        values.put(
            "name",
            seed.name()
        );

        values.put(
            "description",
            seed.description()
        );

        values.put(
            "projectType",
            seed.type()
        );

        values.put(
            "type",
            seed.type()
        );

        values.put(
            "priority",
            seed.priority()
        );

        values.put(
            "siteAddress",
            seed.address()
        );

        values.put(
            "address",
            seed.address()
        );

        values.put(
            "city",
            seed.city()
        );

        values.put(
            "postalCode",
            seed.postalCode()
        );

        values.put(
            "country",
            "France"
        );

        values.put(
            "startDate",
            startDate
        );

        values.put(
            "plannedEndDate",
            plannedEnd
        );

        values.put(
            "expectedCompletion",
            plannedEnd
        );

        values.put(
            "currency",
            "EUR"
        );

        values.put(
            "contractValue",
            seed.contractValue()
        );

        ProjectDtos.CreateProjectRequest
            request =
                record(
                    ProjectDtos
                        .CreateProjectRequest
                        .class,
                    values
                );

        ProjectDtos.ProjectView project =
            projectService.create(
                organizationId,
                actorUserId,
                request
            );

        count(
            inserted,
            "projects"
        );

        return project.id();
    }

    private void configureProject(
        UUID projectId,
        UUID managerId,
        ProjectSeed seed
    ) {
        Map<String, Object> values =
            new LinkedHashMap<>();

        LocalDate start =
            LocalDate.now()
                .minusMonths(
                    seed.startMonthsAgo()
                );

        LocalDate plannedEnd =
            seed.endMonthsAhead() >= 0
                ? LocalDate.now()
                    .plusMonths(
                        seed.endMonthsAhead()
                    )
                : LocalDate.now()
                    .minusMonths(
                        Math.abs(
                            seed.endMonthsAhead()
                        )
                    );

        values.put(
            "status",
            seed.status()
        );

        values.put(
            "progress_percentage",
            seed.progress()
        );

        values.put(
            "progress",
            seed.progress()
        );

        values.put(
            "start_date",
            start
        );

        values.put(
            "planned_end_date",
            plannedEnd
        );

        values.put(
            "expected_completion",
            plannedEnd
        );

        values.put(
            "original_budget",
            seed.originalBudget()
        );

        values.put(
            "contract_value",
            seed.contractValue()
        );

        values.put(
            "project_manager_id",
            managerId
        );

        if (
            "COMPLETED".equals(
                seed.status()
            )
        ) {
            values.put(
                "actual_end_date",
                LocalDate.now()
                    .minusWeeks(3)
            );
        }

        values.put(
            "updated_at",
            now()
        );

        updateSupportedById(
            "projects",
            projectId,
            values
        );
    }

    // =========================================================================
    // Client contacts
    // =========================================================================

    private void seedClientContacts(
        UUID organizationId,
        UUID ownerId,
        Map<String, UUID> clients
    ) {
        int index = 0;

        for (
            Map.Entry<String, UUID> entry
            : clients.entrySet()
        ) {
            String name =
                switch (
                    index % 5
                ) {
                    case 0 ->
                        "Sophie Laurent";

                    case 1 ->
                        "Thomas Bernard";

                    case 2 ->
                        "Camille Robert";

                    case 3 ->
                        "Julien Martin";

                    default ->
                        "Emma Petit";
                };

            String[] names =
                name.split(
                    " ",
                    2
                );

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "client_id",
                entry.getValue()
            );

            values.put(
                "first_name",
                names[0]
            );

            values.put(
                "last_name",
                names[1]
            );

            values.put(
                "job_title",
                index % 2 == 0
                    ? "Project Director"
                    : "Finance Manager"
            );

            values.put(
                "email",
                "contact"
                    + (index + 1)
                    + "@client.demo"
            );

            values.put(
                "phone",
                "+33 1 80 00 "
                    + String.format(
                        Locale.ROOT,
                        "%02d %02d",
                        30 + index,
                        40 + index
                    )
            );

            values.put(
                "is_primary",
                true
            );

            values.put(
                "created_by",
                ownerId
            );

            seedRow(
                List.of(
                    "client_contacts"
                ),
                "client-contact-"
                    + entry.getKey(),
                values
            );

            index++;
        }
    }

    // =========================================================================
    // Suppliers
    // =========================================================================

    private List<UUID> seedSuppliers(
        UUID organizationId,
        UUID ownerId
    ) {
        List<UUID> ids =
            new ArrayList<>();

        int index = 0;

        for (
            SupplierSeed seed
            : supplierSeeds()
        ) {
            UUID existingId =
                namedEntityId(
                    "suppliers",
                    organizationId,
                    seed.name(),
                    List.of(
                        "name"
                    )
                );

            if (existingId != null) {
                ids.add(
                    existingId
                );

                count(
                    existing,
                    "suppliers"
                );

                index++;
                continue;
            }

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "name",
                seed.name()
            );

            values.put(
                "contact_name",
                seed.contact()
            );

            values.put(
                "email",
                seed.email()
            );

            values.put(
                "phone",
                seed.phone()
            );

            values.put(
                "address",
                seed.address()
            );

            values.put(
                "tax_number",
                "FR-DEMO-"
                    + String.format(
                        Locale.ROOT,
                        "%04d",
                        3100 + index
                    )
            );

            values.put(
                "payment_terms",
                seed.paymentTerms()
            );

            values.put(
                "notes",
                "Preferred BuildFlow showcase supplier."
            );

            values.put(
                "status",
                seed.status()
            );

            values.put(
                "created_by",
                ownerId
            );

            UUID id =
                seedRow(
                    List.of(
                        "suppliers"
                    ),
                    "supplier-"
                        + seed.name(),
                    values
                );

            if (id != null) {
                ids.add(
                    id
                );
            }

            index++;
        }

        return ids;
    }

    private List<SupplierSeed> supplierSeeds() {
        return List.of(
            new SupplierSeed(
                "Ile-de-France Beton",
                "Nicolas Leroy",
                "sales@idf-beton.demo",
                "+33 1 60 21 18 40",
                "Saint-Ouen, France",
                "30 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "Lumiere Electricite SAS",
                "Manon Girard",
                "orders@lumiere-elec.demo",
                "+33 1 46 44 51 72",
                "Paris, France",
                "45 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "Plomberie Seine Services",
                "Antoine Muller",
                "contact@plomberie-seine.demo",
                "+33 1 48 02 11 83",
                "Nanterre, France",
                "30 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "Atelier Bois Paris",
                "Lucas Fontaine",
                "projects@atelier-bois.demo",
                "+33 1 43 20 62 15",
                "Paris, France",
                "30 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "Eco Isolation France",
                "Lea Chevalier",
                "commerce@eco-isolation.demo",
                "+33 1 49 41 73 26",
                "Montreuil, France",
                "45 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "Toitures Martin",
                "Hugo Martin",
                "devis@toitures-martin.demo",
                "+33 1 39 66 54 37",
                "Versailles, France",
                "30 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "MetalPro Structures",
                "Camille Andre",
                "sales@metalpro.demo",
                "+33 1 47 88 92 14",
                "Gennevilliers, France",
                "45 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "Sol et Pierre",
                "Arthur Dupont",
                "orders@sol-pierre.demo",
                "+33 1 42 39 81 65",
                "Paris, France",
                "30 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "Vert Horizon Paysage",
                "Chloe Lambert",
                "planning@vert-horizon.demo",
                "+33 1 46 18 72 90",
                "Rueil-Malmaison, France",
                "30 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "Ascenseurs Metropole",
                "Louis Bonnet",
                "projects@ascenseurs.demo",
                "+33 1 40 84 57 61",
                "Clichy, France",
                "60 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "SecuriChantier",
                "Sarah Morel",
                "equipment@securichantier.demo",
                "+33 1 55 93 30 17",
                "Saint-Denis, France",
                "30 days",
                "ACTIVE"
            ),
            new SupplierSeed(
                "Peinture Ile-de-France",
                "Theo Simon",
                "contact@peinture-idf.demo",
                "+33 1 47 50 61 44",
                "Boulogne-Billancourt, France",
                "30 days",
                "INACTIVE"
            )
        );
    }

    // =========================================================================
    // Milestones
    // =========================================================================

    private List<UUID> seedMilestones(
        UUID organizationId,
        UUID projectId,
        UUID ownerId,
        ProjectSeed project,
        int projectIndex
    ) {
        List<String> names =
            List.of(
                "Site setup complete",
                "Structural works complete",
                "MEP rough-in complete",
                "Finishes complete",
                "Handover"
            );

        List<UUID> ids =
            new ArrayList<>();

        int[] thresholds = {
            15,
            35,
            55,
            80,
            100
        };

        for (
            int index = 0;
            index < names.size();
            index++
        ) {
            String status =
                project.progress()
                    >= thresholds[index]
                    ? "COMPLETED"
                    : project.progress()
                        + 18
                        >= thresholds[index]
                        ? "IN_PROGRESS"
                        : "UPCOMING";

            int progress =
                "COMPLETED".equals(
                    status
                )
                    ? 100
                    : "IN_PROGRESS".equals(
                        status
                    )
                        ? Math.max(
                            20,
                            Math.min(
                                90,
                                project.progress()
                            )
                        )
                        : 0;

            LocalDate planned =
                LocalDate.now()
                    .minusMonths(
                        project.startMonthsAgo()
                    )
                    .plusWeeks(
                        5L
                            * (
                                index
                                    + 1
                            )
                    );

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "project_id",
                projectId
            );

            values.put(
                "name",
                names.get(
                    index
                )
            );

            values.put(
                "description",
                "Showcase milestone for "
                    + project.name()
            );

            values.put(
                "planned_date",
                planned
            );

            if (
                "COMPLETED".equals(
                    status
                )
            ) {
                values.put(
                    "completed_date",
                    planned.minusDays(
                        index % 2 == 0
                            ? 2
                            : 0
                    )
                );
            }

            values.put(
                "status",
                status
            );

            values.put(
                "progress_percentage",
                progress
            );

            values.put(
                "client_visible",
                true
            );

            values.put(
                "created_by",
                ownerId
            );

            UUID id =
                seedRow(
                    List.of(
                        "milestones",
                        "project_milestones"
                    ),
                    "milestone-"
                        + projectIndex
                        + "-"
                        + index,
                    values
                );

            if (id != null) {
                ids.add(
                    id
                );
            }
        }

        return ids;
    }

    // =========================================================================
    // Tasks
    // =========================================================================

    private void seedTasks(
        UUID organizationId,
        UUID projectId,
        UUID ownerId,
        UUID managerId,
        UUID siteManagerId,
        List<UUID> milestones,
        ProjectSeed project,
        int projectIndex
    ) {
        List<String> titles =
            List.of(
                "Mobilize site and safety zones",
                "Complete demolition package",
                "Coordinate structural openings",
                "Install electrical first fix",
                "Install plumbing first fix",
                "Complete HVAC distribution",
                "Close partitions and ceilings",
                "Complete flooring and finishes",
                "Final quality inspection",
                "Prepare client handover"
            );

        int completedCount =
            Math.min(
                titles.size(),
                project.progress()
                    / 10
            );

        for (
            int index = 0;
            index < titles.size();
            index++
        ) {
            String status;

            int taskProgress;

            if (
                index < completedCount
            ) {
                status =
                    "DONE";

                taskProgress =
                    100;

            } else if (
                index == completedCount
                && project.progress() > 0
            ) {
                status =
                    project.progress() >= 75
                        ? "REVIEW"
                        : "IN_PROGRESS";

                taskProgress =
                    "REVIEW".equals(
                        status
                    )
                        ? Math.max(
                            90,
                            Math.min(
                                99,
                                project.progress()
                            )
                        )
                        : Math.max(
                            15,
                            Math.min(
                                95,
                                project.progress()
                            )
                        );

            } else if (
                index
                    == completedCount + 1
                && project.progress()
                    >= 30
                && projectIndex % 3 == 0
            ) {
                status =
                    "BLOCKED";

                taskProgress =
                    35;

            } else {
                status =
                    "TODO";

                taskProgress =
                    0;
            }

            LocalDate start =
                LocalDate.now()
                    .minusWeeks(
                        Math.max(
                            1,
                            8 - index
                        )
                    );

            LocalDate due =
                LocalDate.now()
                    .plusDays(
                        (
                            index
                                - completedCount
                        )
                            * 5L
                    );

            if (
                due.isBefore(
                    start
                )
            ) {
                due =
                    start.plusDays(
                        2
                    );
            }

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "project_id",
                projectId
            );

            if (
                !milestones.isEmpty()
            ) {
                values.put(
                    "milestone_id",
                    milestones.get(
                        Math.min(
                            milestones.size()
                                - 1,
                            index / 2
                        )
                    )
                );
            }

            values.put(
                "title",
                titles.get(
                    index
                )
            );

            values.put(
                "description",
                "Showcase task for "
                    + project.name()
            );

            values.put(
                "status",
                status
            );

            values.put(
                "priority",
                index % 4 == 0
                    ? "HIGH"
                    : "MEDIUM"
            );

            values.put(
                "start_date",
                start
            );

            values.put(
                "due_date",
                due
            );

            values.put(
                "estimated_hours",
                new BigDecimal(
                    16 + index * 5
                )
            );

            values.put(
                "actual_hours",
                new BigDecimal(
                    taskProgress == 100
                        ? 14 + index * 4
                        : taskProgress > 0
                            ? 8 + index * 2
                            : 0
                )
            );

            values.put(
                "progress_percentage",
                taskProgress
            );

            values.put(
                "weight",
                1 + (
                    index % 5
                )
            );

            values.put(
                "created_by",
                ownerId
            );

            values.put(
                "created_by_user_id",
                ownerId
            );

            UUID assignee =
                index % 2 == 0
                    ? managerId
                    : siteManagerId;

            values.put(
                "assigned_to",
                assignee
            );

            values.put(
                "assigned_to_user_id",
                assignee
            );

            values.put(
                "assignee_id",
                assignee
            );

            if (
                taskProgress == 100
            ) {
                values.put(
                    "completed_at",
                    Timestamp.from(
                        Instant.now()
                            .minusSeconds(
                                (
                                    12L
                                        + index
                                )
                                    * 86400L
                            )
                    )
                );
            }

            seedRow(
                List.of(
                    "tasks",
                    "project_tasks"
                ),
                "task-"
                    + projectIndex
                    + "-"
                    + index,
                values
            );
        }
    }

    // =========================================================================
    // Budget
    // =========================================================================

    private List<UUID> seedBudget(
        UUID organizationId,
        UUID projectId,
        UUID ownerId,
        ProjectSeed project,
        int projectIndex
    ) {
        List<String> categories =
            List.of(
                "Labor",
                "Materials",
                "Equipment",
                "Subcontractors",
                "Permits",
                "Contingency"
            );

        List<BigDecimal> shares =
            List.of(
                new BigDecimal(
                    "0.28"
                ),
                new BigDecimal(
                    "0.30"
                ),
                new BigDecimal(
                    "0.08"
                ),
                new BigDecimal(
                    "0.22"
                ),
                new BigDecimal(
                    "0.04"
                ),
                new BigDecimal(
                    "0.08"
                )
            );

        List<UUID> ids =
            new ArrayList<>();

        for (
            int index = 0;
            index < categories.size();
            index++
        ) {
            Map<String, Object> categoryValues =
                baseValues(
                    organizationId
                );

            categoryValues.put(
                "project_id",
                projectId
            );

            categoryValues.put(
                "name",
                categories.get(
                    index
                )
            );

            categoryValues.put(
                "description",
                categories.get(
                    index
                )
                    + " budget for "
                    + project.name()
            );

            categoryValues.put(
                "sort_order",
                index + 1
            );

            UUID categoryId =
                seedRow(
                    List.of(
                        "budget_categories",
                        "project_budget_categories"
                    ),
                    "budget-category-"
                        + projectIndex
                        + "-"
                        + index,
                    categoryValues
                );

            if (categoryId == null) {
                continue;
            }

            ids.add(
                categoryId
            );

            BigDecimal amount =
                project.originalBudget()
                    .multiply(
                        shares.get(
                            index
                        )
                    )
                    .setScale(
                        2,
                        RoundingMode.HALF_UP
                    );

            Map<String, Object> itemValues =
                baseValues(
                    organizationId
                );

            itemValues.put(
                "project_id",
                projectId
            );

            itemValues.put(
                "budget_category_id",
                categoryId
            );

            itemValues.put(
                "description",
                categories.get(
                    index
                )
                    + " allowance"
            );

            itemValues.put(
                "quantity",
                BigDecimal.ONE
            );

            itemValues.put(
                "unit_cost",
                amount
            );

            itemValues.put(
                "planned_amount",
                amount
            );

            itemValues.put(
                "notes",
                "Showcase baseline budget."
            );

            itemValues.put(
                "created_by",
                ownerId
            );

            seedRow(
                List.of(
                    "budget_items",
                    "project_budget_items"
                ),
                "budget-item-"
                    + projectIndex
                    + "-"
                    + index,
                itemValues
            );
        }

        return ids;
    }

    // =========================================================================
    // Procurement
    // =========================================================================

    private List<UUID> seedProcurement(
        UUID organizationId,
        UUID projectId,
        UUID ownerId,
        List<UUID> suppliers,
        ProjectSeed project,
        int projectIndex
    ) {
        List<UUID> purchaseOrders =
            new ArrayList<>();

        if (suppliers.isEmpty()) {
            return purchaseOrders;
        }

        int year =
            LocalDate.now()
                .getYear();

        for (
            int index = 0;
            index < 3;
            index++
        ) {
            UUID supplierId =
                suppliers.get(
                    (
                        projectIndex * 3
                            + index
                    )
                        % suppliers.size()
                );

            BigDecimal subtotal =
                project.originalBudget()
                    .multiply(
                        BigDecimal.valueOf(
                            0.035d
                                + index
                                    * 0.012d
                        )
                    )
                    .setScale(
                        2,
                        RoundingMode.HALF_UP
                    );

            BigDecimal tax =
                subtotal.multiply(
                    new BigDecimal(
                        "0.20"
                    )
                ).setScale(
                    2,
                    RoundingMode.HALF_UP
                );

            BigDecimal total =
                subtotal.add(
                    tax
                );

            String status =
                switch (index) {
                    case 0 ->
                        "APPROVED";

                    case 1 ->
                        "ORDERED";

                    default ->
                        project.progress() > 70
                            ? "RECEIVED"
                            : "PENDING_APPROVAL";
                };

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "project_id",
                projectId
            );

            values.put(
                "supplier_id",
                supplierId
            );

            values.put(
                "purchase_order_number",
                "PO-"
                    + year
                    + "-"
                    + String.format(
                        Locale.ROOT,
                        "%04d",
                        2100
                            + projectIndex * 10
                            + index
                    )
            );

            values.put(
                "status",
                status
            );

            values.put(
                "issue_date",
                LocalDate.now()
                    .minusWeeks(
                        6L - index
                    )
            );

            values.put(
                "expected_delivery_date",
                LocalDate.now()
                    .plusWeeks(
                        index + 1L
                    )
            );

            values.put(
                "subtotal",
                subtotal
            );

            values.put(
                "tax_amount",
                tax
            );

            values.put(
                "total",
                total
            );

            values.put(
                "notes",
                "Showcase procurement package."
            );

            values.put(
                "created_by",
                ownerId
            );

            if (
                Set.of(
                    "APPROVED",
                    "ORDERED",
                    "RECEIVED"
                ).contains(
                    status
                )
            ) {
                values.put(
                    "approved_by",
                    ownerId
                );

                values.put(
                    "approved_at",
                    Timestamp.from(
                        Instant.now()
                            .minusSeconds(
                                14L
                                    * 86400L
                            )
                    )
                );
            }

            UUID poId =
                seedRow(
                    List.of(
                        "purchase_orders"
                    ),
                    "po-"
                        + projectIndex
                        + "-"
                        + index,
                    values
                );

            if (poId == null) {
                continue;
            }

            purchaseOrders.add(
                poId
            );

            for (
                int itemIndex = 0;
                itemIndex < 2;
                itemIndex++
            ) {
                BigDecimal line =
                    subtotal.divide(
                        new BigDecimal(
                            "2"
                        ),
                        2,
                        RoundingMode.HALF_UP
                    );

                Map<String, Object> item =
                    baseValues(
                        organizationId
                    );

                item.put(
                    "purchase_order_id",
                    poId
                );

                item.put(
                    "project_id",
                    projectId
                );

                item.put(
                    "description",
                    itemIndex == 0
                        ? "Primary materials package"
                        : "Delivery and installation package"
                );

                item.put(
                    "quantity",
                    BigDecimal.ONE
                );

                item.put(
                    "unit",
                    "lot"
                );

                item.put(
                    "unit_price",
                    line
                );

                item.put(
                    "tax_rate",
                    new BigDecimal(
                        "20.00"
                    )
                );

                item.put(
                    "line_total",
                    line
                );

                seedRow(
                    List.of(
                        "purchase_order_items"
                    ),
                    "po-item-"
                        + projectIndex
                        + "-"
                        + index
                        + "-"
                        + itemIndex,
                    item
                );
            }
        }

        return purchaseOrders;
    }

    // =========================================================================
    // Expenses
    // =========================================================================

    private void seedExpenses(
        UUID organizationId,
        UUID projectId,
        UUID ownerId,
        UUID accountantId,
        List<UUID> suppliers,
        List<UUID> purchaseOrders,
        List<UUID> budgetCategories,
        ProjectSeed project,
        int projectIndex
    ) {
        int year =
            LocalDate.now()
                .getYear();

        for (
            int index = 0;
            index < 6;
            index++
        ) {
            BigDecimal net =
                project.originalBudget()
                    .multiply(
                        BigDecimal.valueOf(
                            0.008d
                                + index
                                    * 0.002d
                        )
                    )
                    .setScale(
                        2,
                        RoundingMode.HALF_UP
                    );

            BigDecimal tax =
                net.multiply(
                    new BigDecimal(
                        "0.20"
                    )
                ).setScale(
                    2,
                    RoundingMode.HALF_UP
                );

            String status =
                index < 4
                    ? "APPROVED"
                    : index == 4
                        ? "SUBMITTED"
                        : "DRAFT";

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "project_id",
                projectId
            );

            if (
                !budgetCategories.isEmpty()
            ) {
                values.put(
                    "budget_category_id",
                    budgetCategories.get(
                        index
                            % budgetCategories.size()
                    )
                );
            }

            if (
                !suppliers.isEmpty()
            ) {
                values.put(
                    "supplier_id",
                    suppliers.get(
                        (
                            projectIndex
                                + index
                        )
                            % suppliers.size()
                    )
                );
            }

            if (
                !purchaseOrders.isEmpty()
                && index < 3
            ) {
                values.put(
                    "purchase_order_id",
                    purchaseOrders.get(
                        index
                            % purchaseOrders.size()
                    )
                );
            }

            values.put(
                "expense_number",
                "EXP-"
                    + year
                    + "-"
                    + String.format(
                        Locale.ROOT,
                        "%04d",
                        3100
                            + projectIndex * 10
                            + index
                    )
            );

            values.put(
                "description",
                switch (index) {
                    case 0 ->
                        "Site labor package";

                    case 1 ->
                        "Electrical materials";

                    case 2 ->
                        "Plumbing materials";

                    case 3 ->
                        "Joinery subcontract";

                    case 4 ->
                        "Equipment rental";

                    default ->
                        "Site logistics";
                    }
            );

            values.put(
                "expense_date",
                LocalDate.now()
                    .minusDays(
                        35L - index * 4L
                    )
            );

            values.put(
                "net_amount",
                net
            );

            values.put(
                "tax_amount",
                tax
            );

            values.put(
                "total_amount",
                net.add(
                    tax
                )
            );

            values.put(
                "status",
                status
            );

            values.put(
                "created_by",
                accountantId
            );

            values.put(
                "created_by_user_id",
                accountantId
            );

            if (
                "APPROVED".equals(
                    status
                )
            ) {
                values.put(
                    "approved_by",
                    ownerId
                );

                values.put(
                    "approved_at",
                    Timestamp.from(
                        Instant.now()
                            .minusSeconds(
                                (
                                    25L
                                        - index
                                )
                                    * 86400L
                            )
                    )
                );
            }

            seedRow(
                List.of(
                    "expenses",
                    "project_expenses"
                ),
                "expense-"
                    + projectIndex
                    + "-"
                    + index,
                values
            );
        }
    }

    // =========================================================================
    // Quotes
    // =========================================================================

    private void seedQuotes(
        UUID organizationId,
        UUID projectId,
        UUID clientId,
        UUID ownerId,
        ProjectSeed project,
        int projectIndex
    ) {
        int year =
            LocalDate.now()
                .getYear();

        for (
            int index = 0;
            index < 2;
            index++
        ) {
            BigDecimal total =
                index == 0
                    ? project.contractValue()
                    : project.contractValue()
                        .multiply(
                            new BigDecimal(
                                "0.075"
                            )
                        )
                        .setScale(
                            2,
                            RoundingMode.HALF_UP
                        );

            BigDecimal subtotal =
                total.divide(
                    new BigDecimal(
                        "1.20"
                    ),
                    2,
                    RoundingMode.HALF_UP
                );

            BigDecimal tax =
                total.subtract(
                    subtotal
                );

            String status =
                index == 0
                    ? "ACCEPTED"
                    : "SENT";

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "project_id",
                projectId
            );

            values.put(
                "client_id",
                clientId
            );

            values.put(
                "quote_number",
                "QTE-"
                    + year
                    + "-"
                    + String.format(
                        Locale.ROOT,
                        "%04d",
                        4100
                            + projectIndex * 10
                            + index
                    )
            );

            values.put(
                "status",
                status
            );

            values.put(
                "issue_date",
                LocalDate.now()
                    .minusMonths(
                        4 - index
                    )
            );

            values.put(
                "valid_until",
                LocalDate.now()
                    .plusDays(
                        30L
                    )
            );

            values.put(
                "subtotal",
                subtotal
            );

            values.put(
                "tax_amount",
                tax
            );

            values.put(
                "total",
                total
            );

            values.put(
                "notes",
                index == 0
                    ? "Primary contract quotation."
                    : "Additional scope proposal."
            );

            values.put(
                "terms",
                "Valid for 30 days. Payment schedule per contract."
            );

            values.put(
                "contract_quote",
                index == 0
            );

            values.put(
                "created_by",
                ownerId
            );

            if (
                "ACCEPTED".equals(
                    status
                )
            ) {
                values.put(
                    "accepted_at",
                    Timestamp.from(
                        Instant.now()
                            .minusSeconds(
                                90L
                                    * 86400L
                            )
                    )
                );

                values.put(
                    "accepted_by",
                    ownerId
                );
            }

            UUID quoteId =
                seedRow(
                    List.of(
                        "quotes",
                        "quotations"
                    ),
                    "quote-"
                        + projectIndex
                        + "-"
                        + index,
                    values
                );

            if (quoteId == null) {
                continue;
            }

            for (
                int itemIndex = 0;
                itemIndex < 2;
                itemIndex++
            ) {
                BigDecimal line =
                    subtotal.divide(
                        new BigDecimal(
                            "2"
                        ),
                        2,
                        RoundingMode.HALF_UP
                    );

                Map<String, Object> item =
                    baseValues(
                        organizationId
                    );

                item.put(
                    "quote_id",
                    quoteId
                );

                item.put(
                    "quotation_id",
                    quoteId
                );

                item.put(
                    "project_id",
                    projectId
                );

                item.put(
                    "client_id",
                    clientId
                );

                item.put(
                    "description",
                    itemIndex == 0
                        ? "Construction works"
                        : "Project management and handover"
                );

                item.put(
                    "quantity",
                    BigDecimal.ONE
                );

                item.put(
                    "unit",
                    "lot"
                );

                item.put(
                    "unit_price",
                    line
                );

                item.put(
                    "tax_rate",
                    new BigDecimal(
                        "20.00"
                    )
                );

                item.put(
                    "line_total",
                    line
                );

                seedRow(
                    List.of(
                        "quote_items",
                        "quotation_items"
                    ),
                    "quote-item-"
                        + projectIndex
                        + "-"
                        + index
                        + "-"
                        + itemIndex,
                    item
                );
            }
        }
    }

    // =========================================================================
    // Change orders
    // =========================================================================

    private void seedChangeOrders(
        UUID organizationId,
        UUID projectId,
        UUID ownerId,
        ProjectSeed project,
        int projectIndex
    ) {
        int year =
            LocalDate.now()
                .getYear();

        for (
            int index = 0;
            index < 2;
            index++
        ) {
            boolean approved =
                index == 0;

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "project_id",
                projectId
            );

            values.put(
                "change_order_number",
                "CO-"
                    + year
                    + "-"
                    + String.format(
                        Locale.ROOT,
                        "%04d",
                        5100
                            + projectIndex * 10
                            + index
                    )
            );

            values.put(
                "title",
                index == 0
                    ? "Additional client-requested finishes"
                    : "Revised electrical scope"
            );

            values.put(
                "description",
                "Showcase change-order workflow."
            );

            values.put(
                "reason",
                index == 0
                    ? "Client design change"
                    : "Site coordination requirement"
            );

            values.put(
                "cost_impact",
                new BigDecimal(
                    index == 0
                        ? "8500.00"
                        : "4200.00"
                )
            );

            values.put(
                "client_price_impact",
                new BigDecimal(
                    index == 0
                        ? "12500.00"
                        : "6800.00"
                )
            );

            values.put(
                "schedule_impact_days",
                index == 0
                    ? 4
                    : 2
            );

            values.put(
                "status",
                approved
                    ? "APPROVED"
                    : "CLIENT_REVIEW"
            );

            values.put(
                "requested_by",
                ownerId
            );

            if (approved) {
                values.put(
                    "approved_by",
                    ownerId
                );

                values.put(
                    "approved_at",
                    Timestamp.from(
                        Instant.now()
                            .minusSeconds(
                                18L
                                    * 86400L
                            )
                    )
                );
            }

            seedRow(
                List.of(
                    "change_orders"
                ),
                "change-order-"
                    + projectIndex
                    + "-"
                    + index,
                values
            );
        }
    }

    // =========================================================================
    // Invoices and payments
    // =========================================================================

    private void seedInvoicesAndPayments(
        UUID organizationId,
        UUID projectId,
        UUID clientId,
        UUID ownerId,
        UUID accountantId,
        ProjectSeed project,
        int projectIndex
    ) {
        int year =
            LocalDate.now()
                .getYear();

        for (
            int index = 0;
            index < 2;
            index++
        ) {
            BigDecimal total =
                project.contractValue()
                    .multiply(
                        index == 0
                            ? new BigDecimal(
                                "0.30"
                            )
                            : new BigDecimal(
                                "0.22"
                            )
                    )
                    .setScale(
                        2,
                        RoundingMode.HALF_UP
                    );

            BigDecimal subtotal =
                total.divide(
                    new BigDecimal(
                        "1.20"
                    ),
                    2,
                    RoundingMode.HALF_UP
                );

            BigDecimal tax =
                total.subtract(
                    subtotal
                );

            String status;

            BigDecimal paid;

            if (
                index == 0
                && project.progress()
                    >= 55
            ) {
                status =
                    "PAID";

                paid =
                    total;

            } else if (
                project.progress()
                    >= 35
            ) {
                status =
                    "PARTIALLY_PAID";

                paid =
                    total.multiply(
                        new BigDecimal(
                            "0.55"
                        )
                    ).setScale(
                        2,
                        RoundingMode.HALF_UP
                    );

            } else {
                status =
                    "ISSUED";

                paid =
                    BigDecimal.ZERO;
            }

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "project_id",
                projectId
            );

            values.put(
                "client_id",
                clientId
            );

            values.put(
                "invoice_number",
                "INV-"
                    + year
                    + "-"
                    + String.format(
                        Locale.ROOT,
                        "%04d",
                        6100
                            + projectIndex * 10
                            + index
                    )
            );

            values.put(
                "status",
                status
            );

            values.put(
                "issue_date",
                LocalDate.now()
                    .minusDays(
                        55L
                            - index * 22L
                    )
            );

            values.put(
                "due_date",
                LocalDate.now()
                    .minusDays(
                        25L
                            - index * 22L
                    )
            );

            values.put(
                "subtotal",
                subtotal
            );

            values.put(
                "tax_amount",
                tax
            );

            values.put(
                "total",
                total
            );

            values.put(
                "amount_paid",
                paid
            );

            values.put(
                "amount_outstanding",
                total.subtract(
                    paid
                )
            );

            values.put(
                "notes",
                "Showcase progress invoice."
            );

            values.put(
                "payment_instructions",
                "Bank transfer within 30 days."
            );

            values.put(
                "created_by",
                accountantId
            );

            UUID invoiceId =
                seedRow(
                    List.of(
                        "invoices"
                    ),
                    "invoice-"
                        + projectIndex
                        + "-"
                        + index,
                    values
                );

            if (invoiceId == null) {
                continue;
            }

            for (
                int itemIndex = 0;
                itemIndex < 2;
                itemIndex++
            ) {
                BigDecimal line =
                    subtotal.divide(
                        new BigDecimal(
                            "2"
                        ),
                        2,
                        RoundingMode.HALF_UP
                    );

                Map<String, Object> item =
                    baseValues(
                        organizationId
                    );

                item.put(
                    "invoice_id",
                    invoiceId
                );

                item.put(
                    "project_id",
                    projectId
                );

                item.put(
                    "client_id",
                    clientId
                );

                item.put(
                    "description",
                    itemIndex == 0
                        ? "Progress construction works"
                        : "Site management and coordination"
                );

                item.put(
                    "quantity",
                    BigDecimal.ONE
                );

                item.put(
                    "unit",
                    "lot"
                );

                item.put(
                    "unit_price",
                    line
                );

                item.put(
                    "tax_rate",
                    new BigDecimal(
                        "20.00"
                    )
                );

                item.put(
                    "line_total",
                    line
                );

                seedRow(
                    List.of(
                        "invoice_items"
                    ),
                    "invoice-item-"
                        + projectIndex
                        + "-"
                        + index
                        + "-"
                        + itemIndex,
                    item
                );
            }

            if (
                paid.compareTo(
                    BigDecimal.ZERO
                ) > 0
            ) {
                Map<String, Object> payment =
                    baseValues(
                        organizationId
                    );

                payment.put(
                    "invoice_id",
                    invoiceId
                );

                payment.put(
                    "project_id",
                    projectId
                );

                payment.put(
                    "client_id",
                    clientId
                );

                payment.put(
                    "payment_date",
                    LocalDate.now()
                        .minusDays(
                            12L
                                + index
                        )
                );

                payment.put(
                    "amount",
                    paid
                );

                payment.put(
                    "method",
                    "BANK_TRANSFER"
                );

                payment.put(
                    "payment_method",
                    "BANK_TRANSFER"
                );

                payment.put(
                    "reference",
                    "DEMO-PAY-"
                        + projectIndex
                        + "-"
                        + index
                );

                payment.put(
                    "notes",
                    "Showcase payment."
                );

                payment.put(
                    "created_by",
                    accountantId
                );

                payment.put(
                    "created_by_user_id",
                    accountantId
                );

                seedRow(
                    List.of(
                        "payments"
                    ),
                    "payment-"
                        + projectIndex
                        + "-"
                        + index,
                    payment
                );
            }
        }
    }

    // =========================================================================
    // Site reports
    // =========================================================================

    private void seedSiteReports(
        UUID organizationId,
        UUID projectId,
        UUID siteManagerId,
        ProjectSeed project,
        int projectIndex
    ) {
        List<String> weather =
            List.of(
                "Clear, 17 C",
                "Cloudy, 14 C",
                "Light rain, 12 C",
                "Clear, 19 C"
            );

        for (
            int index = 0;
            index < 4;
            index++
        ) {
            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "project_id",
                projectId
            );

            values.put(
                "report_date",
                LocalDate.now()
                    .minusDays(
                        3L
                            + index * 4L
                    )
            );

            values.put(
                "weather_text",
                weather.get(
                    index
                )
            );

            values.put(
                "workers_on_site",
                12
                    + projectIndex
                    + index * 3
            );

            values.put(
                "summary",
                "Good progress across planned work fronts."
            );

            values.put(
                "work_completed",
                switch (index) {
                    case 0 ->
                        "Partitions and first-fix electrical progressed.";

                    case 1 ->
                        "Ceiling grids and MEP coordination completed in Zone B.";

                    case 2 ->
                        "Floor preparation and joinery installation advanced.";

                    default ->
                        "Quality checks completed and handover areas cleaned.";
                }
            );

            values.put(
                "delays",
                index == 2
                    ? "Minor supplier delivery delay."
                    : "No material delay."
            );

            values.put(
                "issues",
                index == 1
                    ? "One coordination item raised for review."
                    : "No critical issue."
            );

            values.put(
                "next_steps",
                "Continue planned sequence and close open quality items."
            );

            values.put(
                "created_by",
                siteManagerId
            );

            values.put(
                "created_by_user_id",
                siteManagerId
            );

            seedRow(
                List.of(
                    "daily_site_reports",
                    "site_reports"
                ),
                "site-report-"
                    + projectIndex
                    + "-"
                    + index,
                values
            );
        }
    }

    // =========================================================================
    // Incidents
    // =========================================================================

    private void seedIncidents(
        UUID organizationId,
        UUID projectId,
        UUID siteManagerId,
        UUID managerId,
        ProjectSeed project,
        int projectIndex
    ) {
        int year =
            LocalDate.now()
                .getYear();

        for (
            int index = 0;
            index < 2;
            index++
        ) {
            boolean resolved =
                index == 0;

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "project_id",
                projectId
            );

            values.put(
                "incident_number",
                "INC-"
                    + year
                    + "-"
                    + String.format(
                        Locale.ROOT,
                        "%04d",
                        7100
                            + projectIndex * 10
                            + index
                    )
            );

            values.put(
                "type",
                index == 0
                    ? "QUALITY"
                    : "SAFETY"
            );

            values.put(
                "severity",
                index == 0
                    ? "MEDIUM"
                    : "HIGH"
            );

            values.put(
                "title",
                index == 0
                    ? "Finish quality correction required"
                    : "Temporary access route obstruction"
            );

            values.put(
                "description",
                index == 0
                    ? "A completed finish did not meet the agreed quality standard."
                    : "Material delivery temporarily obstructed the designated access route."
            );

            values.put(
                "occurred_at",
                Timestamp.from(
                    Instant.now()
                        .minusSeconds(
                            (
                                16L
                                    - index * 7L
                            )
                                * 86400L
                        )
                )
            );

            values.put(
                "reported_at",
                Timestamp.from(
                    Instant.now()
                        .minusSeconds(
                            (
                                16L
                                    - index * 7L
                            )
                                * 86400L
                            - 1800L
                        )
                )
            );

            values.put(
                "status",
                resolved
                    ? "RESOLVED"
                    : "OPEN"
            );

            values.put(
                "reported_by",
                siteManagerId
            );

            values.put(
                "assigned_to",
                managerId
            );

            values.put(
                "client_visible",
                false
            );

            if (resolved) {
                values.put(
                    "resolution",
                    "Corrective work completed and inspected."
                );

                values.put(
                    "resolved_at",
                    Timestamp.from(
                        Instant.now()
                            .minusSeconds(
                                8L
                                    * 86400L
                            )
                    )
                );
            }

            seedRow(
                List.of(
                    "site_incidents",
                    "incidents"
                ),
                "incident-"
                    + projectIndex
                    + "-"
                    + index,
                values
            );
        }
    }

    // =========================================================================
    // Notifications
    // =========================================================================

    private void seedNotifications(
        UUID organizationId,
        UUID ownerId,
        UUID managerId,
        UUID siteManagerId,
        UUID accountantId,
        Map<String, UUID> projects
    ) {
        if (projects.isEmpty()) {
            return;
        }

        List<UUID> users =
            List.of(
                ownerId,
                managerId,
                siteManagerId,
                accountantId
            );

        List<Map.Entry<String, UUID>>
            projectEntries =
                new ArrayList<>(
                    projects.entrySet()
                );

        List<String> types =
            List.of(
                "TASK_ASSIGNED",
                "TASK_OVERDUE",
                "MILESTONE_DUE",
                "PURCHASE_ORDER_APPROVAL",
                "INVOICE_OVERDUE",
                "CHANGE_ORDER_APPROVED",
                "INCIDENT_REPORTED"
            );

        for (
            int index = 0;
            index < 28;
            index++
        ) {
            Map.Entry<String, UUID>
                project =
                    projectEntries.get(
                        index
                            % projectEntries.size()
                    );

            UUID userId =
                users.get(
                    index
                        % users.size()
                );

            String type =
                types.get(
                    index
                        % types.size()
                );

            Map<String, Object> values =
                baseValues(
                    organizationId
                );

            values.put(
                "user_id",
                userId
            );

            values.put(
                "type",
                type
            );

            values.put(
                "title",
                switch (
                    index % 7
                ) {
                    case 0 ->
                        "Task assignment updated";

                    case 1 ->
                        "Task requires attention";

                    case 2 ->
                        "Milestone due soon";

                    case 3 ->
                        "Purchase order needs review";

                    case 4 ->
                        "Invoice payment follow-up";

                    case 5 ->
                        "Change order approved";

                    default ->
                        "Site incident reported";
                }
            );

            values.put(
                "message",
                "Showcase notification for "
                    + project.getKey()
                    + "."
            );

            values.put(
                "entity_type",
                "PROJECT"
            );

            values.put(
                "entity_id",
                project.getValue()
            );

            if (
                index % 3 == 0
            ) {
                values.put(
                    "read_at",
                    Timestamp.from(
                        Instant.now()
                            .minusSeconds(
                                3600L
                            )
                    )
                );
            }

            values.put(
                "created_at",
                Timestamp.from(
                    Instant.now()
                        .minusSeconds(
                            index
                                * 7200L
                        )
                )
            );

            seedRow(
                List.of(
                    "notifications"
                ),
                "notification-"
                    + index,
                values
            );
        }
    }

    // =========================================================================
    // Generic schema-aware inserts
    // =========================================================================

    private UUID seedRow(
        List<String> tableCandidates,
        String seedKey,
        Map<String, Object> supplied
    ) {
        String table =
            firstExistingTable(
                tableCandidates
            );

        if (table == null) {
            count(
                skipped,
                String.join(
                    "/",
                    tableCandidates
                )
            );

            return null;
        }

        if (
            disabledTables.contains(
                table
            )
        ) {
            return null;
        }

        Set<String> tableColumns =
            columns(
                table
            );

        if (
            !tableColumns.contains(
                "id"
            )
        ) {
            disabledTables.add(
                table
            );

            LOGGER.warn(
                "Skipping showcase table={} because no id column exists for deterministic idempotency.",
                table
            );

            count(
                skipped,
                table
            );

            return null;
        }

        UUID id =
            deterministicUuid(
                seedKey
            );

        if (
            rowExistsById(
                table,
                id
            )
        ) {
            count(
                existing,
                table
            );

            return id;
        }

        Map<String, Object> values =
            new LinkedHashMap<>(
                supplied
            );

        values.put(
            "id",
            id
        );

        try {
            insertRequiredAware(
                table,
                values
            );

            count(
                inserted,
                table
            );

            return id;

        } catch (
            RuntimeException exception
        ) {
            disabledTables.add(
                table
            );

            count(
                skipped,
                table
            );

            LOGGER.warn(
                "Disabling showcase seed for table={} after schema/constraint mismatch: {}",
                table,
                exception.getMessage()
            );

            return null;
        }
    }

    private void insertRequiredAware(
        String table,
        Map<String, Object> suppliedValues
    ) {
        List<ColumnInfo> metadata =
            jdbcTemplate.query(
                """
                SELECT
                    column_name,
                    is_nullable,
                    column_default,
                    data_type
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                ORDER BY ordinal_position
                """,
                (
                    resultSet,
                    rowNumber
                ) ->
                    new ColumnInfo(
                        resultSet.getString(
                            "column_name"
                        ),
                        resultSet.getString(
                            "is_nullable"
                        ),
                        resultSet.getString(
                            "column_default"
                        ),
                        resultSet.getString(
                            "data_type"
                        )
                    ),
                table
            );

        Map<String, Object> values =
            new LinkedHashMap<>();

        for (
            ColumnInfo column
            : metadata
        ) {
            if (
                suppliedValues.containsKey(
                    column.name()
                )
            ) {
                Object value =
                    suppliedValues.get(
                        column.name()
                    );

                if (value != null) {
                    values.put(
                        column.name(),
                        value
                    );
                }

                continue;
            }

            if (
                "NO".equals(
                    column.nullable()
                )
                && column.defaultValue()
                    == null
            ) {
                Object fallback =
                    fallbackValue(
                        column
                    );

                if (fallback == null) {
                    throw new IllegalStateException(
                        "Cannot safely seed required column "
                            + table
                            + "."
                            + column.name()
                    );
                }

                values.put(
                    column.name(),
                    fallback
                );
            }
        }

        if (values.isEmpty()) {
            throw new IllegalStateException(
                "No values resolved for "
                    + table
            );
        }

        List<String> names =
            new ArrayList<>(
                values.keySet()
            );

        String columnSql =
            names.stream()
                .map(
                    this::identifier
                )
                .reduce(
                    (
                        left,
                        right
                    ) ->
                        left
                            + ", "
                            + right
                )
                .orElseThrow();

        String placeholders =
            names.stream()
                .map(
                    ignored ->
                        "?"
                )
                .reduce(
                    (
                        left,
                        right
                    ) ->
                        left
                            + ", "
                            + right
                )
                .orElseThrow();

        Object[] arguments =
            names.stream()
                .map(
                    values::get
                )
                .toArray();

        jdbcTemplate.update(
            """
            INSERT INTO %s (%s)
            VALUES (%s)
            """.formatted(
                identifier(
                    table
                ),
                columnSql,
                placeholders
            ),
            arguments
        );
    }

    private Object fallbackValue(
        ColumnInfo column
    ) {
        String name =
            column.name();

        String type =
            column.dataType()
                .toLowerCase(
                    Locale.ROOT
                );

        if (
            "id".equals(
                name
            )
            || name.endsWith(
                "_id"
            )
        ) {
            return null;
        }

        if (
            "currency".equals(
                name
            )
        ) {
            return "EUR";
        }

        if (
            name.endsWith(
                "_at"
            )
            && type.contains(
                "timestamp"
            )
        ) {
            return now();
        }

        if (
            type.equals(
                "date"
            )
        ) {
            return LocalDate.now();
        }

        if (
            "version".equals(
                name
            )
            || type.contains(
                "integer"
            )
            || type.contains(
                "bigint"
            )
        ) {
            return 0L;
        }

        if (
            type.contains(
                "numeric"
            )
            || type.contains(
                "decimal"
            )
        ) {
            return BigDecimal.ZERO;
        }

        if (
            type.contains(
                "boolean"
            )
        ) {
            return false;
        }

        return null;
    }

    // =========================================================================
    // Core lookup/update helpers
    // =========================================================================

    private UUID organizationId() {
        String table =
            firstExistingTable(
                List.of(
                    "organizations"
                )
            );

        if (table == null) {
            return null;
        }

        Set<String> cols =
            columns(
                table
            );

        if (
            !cols.contains(
                "id"
            )
            || !cols.contains(
                "name"
            )
        ) {
            return null;
        }

        List<UUID> ids =
            jdbcTemplate.queryForList(
                """
                SELECT id
                FROM organizations
                WHERE LOWER(name) =
                    LOWER(?)
                LIMIT 1
                """,
                UUID.class,
                ORGANIZATION_NAME
            );

        return ids.isEmpty()
            ? null
            : ids.getFirst();
    }

    private UUID userIdByEmail(
        String email
    ) {
        String table =
            firstExistingTable(
                List.of(
                    "users",
                    "app_users",
                    "user_accounts"
                )
            );

        if (table == null) {
            return null;
        }

        Set<String> cols =
            columns(
                table
            );

        if (
            !cols.contains(
                "id"
            )
            || !cols.contains(
                "email"
            )
        ) {
            return null;
        }

        List<UUID> ids =
            jdbcTemplate.queryForList(
                """
                SELECT id
                FROM %s
                WHERE LOWER(email) =
                    LOWER(?)
                LIMIT 1
                """.formatted(
                    identifier(
                        table
                    )
                ),
                UUID.class,
                email
            );

        return ids.isEmpty()
            ? null
            : ids.getFirst();
    }

    private UUID namedEntityId(
        String table,
        UUID organizationId,
        String name,
        List<String> candidateNameColumns
    ) {
        if (
            !tableExists(
                table
            )
        ) {
            return null;
        }

        Set<String> cols =
            columns(
                table
            );

        if (
            !cols.contains(
                "id"
            )
        ) {
            return null;
        }

        String nameColumn =
            candidateNameColumns
                .stream()
                .filter(
                    cols::contains
                )
                .findFirst()
                .orElse(
                    null
                );

        if (nameColumn == null) {
            return null;
        }

        boolean scoped =
            cols.contains(
                "organization_id"
            );

        String sql =
            scoped
                ? """
                  SELECT id
                  FROM %s
                  WHERE organization_id = ?
                    AND LOWER(%s) =
                        LOWER(?)
                  LIMIT 1
                  """.formatted(
                    identifier(
                        table
                    ),
                    identifier(
                        nameColumn
                    )
                )
                : """
                  SELECT id
                  FROM %s
                  WHERE LOWER(%s) =
                      LOWER(?)
                  LIMIT 1
                  """.formatted(
                    identifier(
                        table
                    ),
                    identifier(
                        nameColumn
                    )
                );

        List<UUID> ids =
            scoped
                ? jdbcTemplate
                    .queryForList(
                        sql,
                        UUID.class,
                        organizationId,
                        name
                    )
                : jdbcTemplate
                    .queryForList(
                        sql,
                        UUID.class,
                        name
                    );

        return ids.isEmpty()
            ? null
            : ids.getFirst();
    }

    private void updateSupportedById(
        String table,
        UUID id,
        Map<String, Object> supplied
    ) {
        if (
            !tableExists(
                table
            )
        ) {
            return;
        }

        Set<String> cols =
            columns(
                table
            );

        if (
            !cols.contains(
                "id"
            )
        ) {
            return;
        }

        Map<String, Object> values =
            new LinkedHashMap<>();

        for (
            Map.Entry<String, Object>
                entry
            : supplied.entrySet()
        ) {
            if (
                entry.getValue() != null
                && cols.contains(
                    entry.getKey()
                )
            ) {
                values.put(
                    entry.getKey(),
                    entry.getValue()
                );
            }
        }

        if (values.isEmpty()) {
            return;
        }

        String assignments =
            values.keySet()
                .stream()
                .map(
                    column ->
                        identifier(
                            column
                        )
                            + " = ?"
                )
                .reduce(
                    (
                        left,
                        right
                    ) ->
                        left
                            + ", "
                            + right
                )
                .orElseThrow();

        List<Object> args =
            new ArrayList<>(
                values.values()
            );

        args.add(
            id
        );

        jdbcTemplate.update(
            """
            UPDATE %s
            SET %s
            WHERE id = ?
            """.formatted(
                identifier(
                    table
                ),
                assignments
            ),
            args.toArray()
        );
    }

    private boolean rowExistsById(
        String table,
        UUID id
    ) {
        Integer count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM %s
                WHERE id = ?
                """.formatted(
                    identifier(
                        table
                    )
                ),
                Integer.class,
                id
            );

        return count != null
            && count > 0;
    }

    private String firstExistingTable(
        List<String> candidates
    ) {
        for (
            String candidate
            : candidates
        ) {
            if (
                tableExists(
                    candidate
                )
            ) {
                return candidate;
            }
        }

        return null;
    }

    private boolean tableExists(
        String table
    ) {
        Integer count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = ?
                """,
                Integer.class,
                table
            );

        return count != null
            && count > 0;
    }

    private Set<String> columns(
        String table
    ) {
        return Set.copyOf(
            jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                """,
                String.class,
                table
            )
        );
    }

    private String identifier(
        String value
    ) {
        if (
            value == null
            || !IDENTIFIER
                .matcher(
                    value
                )
                .matches()
        ) {
            throw new IllegalArgumentException(
                "Invalid SQL identifier: "
                    + value
            );
        }

        return "\""
            + value
            + "\"";
    }

    private Map<String, Object>
        baseValues(
            UUID organizationId
        ) {
        Map<String, Object> values =
            new LinkedHashMap<>();

        values.put(
            "organization_id",
            organizationId
        );

        values.put(
            "created_at",
            now()
        );

        values.put(
            "updated_at",
            now()
        );

        values.put(
            "version",
            0L
        );

        return values;
    }

    private Timestamp now() {
        return Timestamp.from(
            Instant.now()
        );
    }

    private UUID deterministicUuid(
        String key
    ) {
        return UUID.nameUUIDFromBytes(
            (
                "buildflow-showcase-v1:"
                    + key
            )
                .getBytes(
                    StandardCharsets.UTF_8
                )
        );
    }

    private void count(
        Map<String, Integer> target,
        String key
    ) {
        target.merge(
            key,
            1,
            Integer::sum
        );
    }

    // =========================================================================
    // Dynamic request-record builder
    // =========================================================================

    private <T> T record(
        Class<T> type,
        Map<String, Object> values
    ) {
        try {
            RecordComponent[] components =
                type.getRecordComponents();

            Class<?>[] types =
                new Class<?>[
                    components.length
                ];

            Object[] arguments =
                new Object[
                    components.length
                ];

            for (
                int index = 0;
                index
                    < components.length;
                index++
            ) {
                RecordComponent component =
                    components[index];

                types[index] =
                    component.getType();

                Object value =
                    values.get(
                        component.getName()
                    );

                arguments[index] =
                    coerce(
                        component.getType(),
                        value
                    );
            }

            Constructor<T> constructor =
                type.getDeclaredConstructor(
                    types
                );

            return constructor
                .newInstance(
                    arguments
                );

        } catch (
            ReflectiveOperationException
                exception
        ) {
            throw new IllegalStateException(
                "Could not build showcase request record "
                    + type.getName(),
                exception
            );
        }
    }

    private Object coerce(
        Class<?> type,
        Object value
    ) {
        if (value != null) {
            return value;
        }

        if (!type.isPrimitive()) {
            return null;
        }

        if (
            type
                == boolean.class
        ) {
            return false;
        }

        if (
            type
                == int.class
        ) {
            return 0;
        }

        if (
            type
                == long.class
        ) {
            return 0L;
        }

        if (
            type
                == double.class
        ) {
            return 0.0d;
        }

        if (
            type
                == float.class
        ) {
            return 0.0f;
        }

        if (
            type
                == short.class
        ) {
            return (short) 0;
        }

        if (
            type
                == byte.class
        ) {
            return (byte) 0;
        }

        if (
            type
                == char.class
        ) {
            return '\0';
        }

        return null;
    }

    // =========================================================================
    // Seed records
    // =========================================================================

    private record ClientSeed(
        String name,
        String email,
        String phone,
        String city
    ) {
    }

    private record SupplierSeed(
        String name,
        String contact,
        String email,
        String phone,
        String address,
        String paymentTerms,
        String status
    ) {
    }

    private record ProjectSeed(
        String name,
        String clientName,
        String description,
        ProjectType type,
        ProjectPriority priority,
        String address,
        String city,
        String postalCode,
        BigDecimal contractValue,
        BigDecimal originalBudget,
        String status,
        int progress,
        int startMonthsAgo,
        int endMonthsAhead
    ) {
    }

    private record ColumnInfo(
        String name,
        String nullable,
        String defaultValue,
        String dataType
    ) {
    }
}
