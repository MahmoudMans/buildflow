package com.buildflow.demo;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;

import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;

import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationService;

import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;

import java.math.BigDecimal;

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

@Component
@ConditionalOnProperty(
    name = "buildflow.demo.enabled",
    havingValue = "true"
)
public class DemoPortfolioSeeder
    implements ApplicationRunner {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            DemoPortfolioSeeder.class
        );

    private static final Pattern IDENTIFIER =
        Pattern.compile(
            "[a-z0-9_]+"
        );

    private static final String ORGANIZATION_NAME =
        "NorthBuild Construction";

    private static final List<String> DEMO_EMAILS =
        List.of(
            "owner@buildflow.demo",
            "pm@buildflow.demo",
            "site@buildflow.demo",
            "accountant@buildflow.demo",
            "client@buildflow.demo"
        );

    private final AuthService authService;
    private final OrganizationService organizationService;
    private final ClientService clientService;
    private final ProjectService projectService;
    private final JdbcTemplate jdbcTemplate;

    private final String password;

    public DemoPortfolioSeeder(
        AuthService authService,
        OrganizationService organizationService,
        ClientService clientService,
        ProjectService projectService,
        JdbcTemplate jdbcTemplate,
        @Value(
            "${buildflow.demo.password:BuildFlowDemo2026!}"
        )
        String password
    ) {
        this.authService =
            authService;

        this.organizationService =
            organizationService;

        this.clientService =
            clientService;

        this.projectService =
            projectService;

        this.jdbcTemplate =
            jdbcTemplate;

        this.password =
            password;
    }

    @Override
    public void run(
        ApplicationArguments arguments
    ) {
        String userTable =
            firstExistingTable(
                List.of(
                    "users",
                    "app_users",
                    "user_accounts"
                )
            );

        if (userTable == null) {
            throw new IllegalStateException(
                "Could not locate the BuildFlow user table."
            );
        }

        UUID existingOwner =
            userIdByEmail(
                userTable,
                "owner@buildflow.demo"
            );

        if (existingOwner != null) {
            normalizeDemoAuthenticationState(
                userTable
            );

            assertCompleteDemoUserSet(
                userTable
            );

            LOGGER.info(
                "BuildFlow demo portfolio data already exists and authentication state is normalized."
            );

            return;
        }

        LOGGER.info(
            "Creating BuildFlow Phase 15 demo portfolio data."
        );

        DemoUser owner =
            register(
                "Olivia",
                "Martin",
                "owner@buildflow.demo"
            );

        DemoUser manager =
            register(
                "Paul",
                "Moreau",
                "pm@buildflow.demo"
            );

        DemoUser siteManager =
            register(
                "Sofia",
                "Bernard",
                "site@buildflow.demo"
            );

        DemoUser accountant =
            register(
                "Alice",
                "Roux",
                "accountant@buildflow.demo"
            );

        DemoUser clientUser =
            register(
                "Claire",
                "Dubois",
                "client@buildflow.demo"
            );

        normalizeDemoAuthenticationState(
            userTable
        );

        assertCompleteDemoUserSet(
            userTable
        );

        OrganizationDtos
            .CreateOrganizationRequest
            organizationRequest =
                record(
                    OrganizationDtos
                        .CreateOrganizationRequest
                        .class,
                    recordValues(
                        "name",
                        ORGANIZATION_NAME,
                        "legalName",
                        "NorthBuild Construction SAS",
                        "registrationNumber",
                        "DEMO-RCS-2026",
                        "taxNumber",
                        "FRDEMO2026",
                        "email",
                        "contact@northbuild.demo",
                        "phone",
                        "+33 1 80 00 20 26",
                        "addressLine1",
                        "18 Avenue de l'Opera",
                        "city",
                        "Paris",
                        "postalCode",
                        "75001",
                        "country",
                        "France",
                        "currency",
                        "EUR",
                        "timezone",
                        "Europe/Paris"
                    )
                );

        OrganizationDtos.OrganizationView
            organization =
                organizationService.create(
                    owner.id(),
                    organizationRequest
                );

        UUID organizationId =
            organization.id();

        UUID ownerMembershipId =
            membershipId(
                organizationId,
                owner.id()
            );

        ensureOrganizationMembership(
            organizationId,
            manager,
            OrganizationRole
                .PROJECT_MANAGER,
            owner.id()
        );

        UUID siteMembershipId =
            ensureOrganizationMembership(
                organizationId,
                siteManager,
                OrganizationRole
                    .SITE_MANAGER,
                owner.id()
            );

        ensureOrganizationMembership(
            organizationId,
            accountant,
            OrganizationRole
                .ACCOUNTANT,
            owner.id()
        );

        UUID clientMembershipId =
            ensureOrganizationMembership(
                organizationId,
                clientUser,
                OrganizationRole
                    .CLIENT,
                owner.id()
            );

        UUID managerMembershipId =
            membershipId(
                organizationId,
                manager.id()
            );

        if (ownerMembershipId == null) {
            throw new IllegalStateException(
                "Demo owner membership was not created."
            );
        }

        ClientDtos.ClientView hotelClient =
            createClient(
                organizationId,
                owner.id(),
                "Rivoli Hospitality Group",
                "finance@rivoli.demo"
            );

        ClientDtos.ClientView officeClient =
            createClient(
                organizationId,
                owner.id(),
                "Belleville Workspace SAS",
                "operations@belleville.demo"
            );

        ClientDtos.ClientView residenceClient =
            createClient(
                organizationId,
                owner.id(),
                "Claire Dubois",
                "client@buildflow.demo"
            );

        ProjectDtos.ProjectView hotelProject =
            createProject(
                organizationId,
                owner.id(),
                hotelClient.id(),
                "Rivoli Hotel Renovation",
                "Full occupied-hotel renovation including guest rooms, reception, MEP upgrades and phased handover.",
                ProjectType.RENOVATION,
                ProjectPriority.HIGH,
                "28 Rue de Rivoli",
                "Paris",
                "75004",
                new BigDecimal(
                    "1850000.00"
                )
            );

        ProjectDtos.ProjectView officeProject =
            createProject(
                organizationId,
                owner.id(),
                officeClient.id(),
                "Belleville Office Fit-Out",
                "Fast-track commercial office fit-out with partitions, electrical works, lighting and finishes.",
                ProjectType.INTERIOR,
                ProjectPriority.MEDIUM,
                "72 Rue de Belleville",
                "Paris",
                "75020",
                new BigDecimal(
                    "640000.00"
                )
            );

        ProjectDtos.ProjectView residenceProject =
            createProject(
                organizationId,
                owner.id(),
                residenceClient.id(),
                "Neuilly Residence Extension",
                "Residential extension and renovation with structural opening, roofing, plumbing and landscaping packages.",
                ProjectType.RENOVATION,
                ProjectPriority.HIGH,
                "14 Avenue Sainte-Foy",
                "Neuilly-sur-Seine",
                "92200",
                new BigDecimal(
                    "920000.00"
                )
            );

        List<UUID> projectIds =
            List.of(
                hotelProject.id(),
                officeProject.id(),
                residenceProject.id()
            );

        for (
            UUID projectId
            : projectIds
        ) {
            ensureProjectMembership(
                organizationId,
                projectId,
                manager.id(),
                managerMembershipId,
                "PROJECT_MANAGER"
            );

            ensureProjectMembership(
                organizationId,
                projectId,
                siteManager.id(),
                siteMembershipId,
                "SITE_MANAGER"
            );
        }

        linkClientUserIfSupported(
            organizationId,
            residenceClient.id(),
            clientUser.id(),
            clientMembershipId
        );

        LOGGER.info(
            "BuildFlow demo seed complete: organization={}, projects={}, users={}",
            organizationId,
            projectIds.size(),
            5
        );
    }

    private DemoUser register(
        String firstName,
        String lastName,
        String email
    ) {
        AuthDtos.RegisterRequest request =
            record(
                AuthDtos.RegisterRequest.class,
                Map.of(
                    "firstName",
                    firstName,
                    "lastName",
                    lastName,
                    "email",
                    email,
                    "password",
                    password
                )
            );

        AuthService.AuthResult result =
            authService.register(
                request,
                "127.0.0.1",
                "buildflow-phase15-demo"
            );

        return new DemoUser(
            result.user().getId(),
            email
        );
    }

    private ClientDtos.ClientView createClient(
        UUID organizationId,
        UUID actorUserId,
        String displayName,
        String email
    ) {
        Map<String, Object> values =
            new LinkedHashMap<>();

        values.put(
            "type",
            ClientType.COMPANY
        );

        values.put(
            "name",
            displayName
        );

        values.put(
            "companyName",
            displayName
        );

        values.put(
            "displayName",
            displayName
        );

        values.put(
            "firstName",
            displayName
        );

        values.put(
            "email",
            email
        );

        values.put(
            "phone",
            "+33 1 80 00 20 26"
        );

        values.put(
            "billingAddress",
            "Paris, France"
        );

        values.put(
            "siteAddress",
            "Paris, France"
        );

        values.put(
            "notes",
            "BuildFlow portfolio demonstration client."
        );

        ClientDtos.CreateClientRequest request =
            record(
                ClientDtos
                    .CreateClientRequest
                    .class,
                values
            );

        return clientService.create(
            organizationId,
            actorUserId,
            request
        );
    }

    private ProjectDtos.ProjectView createProject(
        UUID organizationId,
        UUID actorUserId,
        UUID clientId,
        String name,
        String description,
        ProjectType type,
        ProjectPriority priority,
        String siteAddress,
        String city,
        String postalCode,
        BigDecimal contractValue
    ) {
        LocalDate start =
            LocalDate.now()
                .minusMonths(2);

        LocalDate plannedEnd =
            LocalDate.now()
                .plusMonths(8);

        Map<String, Object> values =
            new LinkedHashMap<>();

        values.put(
            "clientId",
            clientId
        );

        values.put(
            "name",
            name
        );

        values.put(
            "description",
            description
        );

        values.put(
            "projectType",
            type
        );

        values.put(
            "type",
            type
        );

        values.put(
            "priority",
            priority
        );

        values.put(
            "siteAddress",
            siteAddress
        );

        values.put(
            "address",
            siteAddress
        );

        values.put(
            "city",
            city
        );

        values.put(
            "postalCode",
            postalCode
        );

        values.put(
            "country",
            "France"
        );

        values.put(
            "startDate",
            start
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
            contractValue
        );

        ProjectDtos.CreateProjectRequest request =
            record(
                ProjectDtos
                    .CreateProjectRequest
                    .class,
                values
            );

        return projectService.create(
            organizationId,
            actorUserId,
            request
        );
    }

    private UUID ensureOrganizationMembership(
        UUID organizationId,
        DemoUser user,
        OrganizationRole role,
        UUID ownerId
    ) {
        UUID existing =
            membershipId(
                organizationId,
                user.id()
            );

        if (existing != null) {
            return existing;
        }

        String table =
            firstExistingTable(
                List.of(
                    "organization_memberships",
                    "memberships"
                )
            );

        if (table == null) {
            throw new IllegalStateException(
                "Could not locate organization membership table."
            );
        }

        Map<String, Object> values =
            new LinkedHashMap<>();

        values.put(
            "id",
            UUID.randomUUID()
        );

        values.put(
            "organization_id",
            organizationId
        );

        values.put(
            "user_id",
            user.id()
        );

        values.put(
            "role",
            role.name()
        );

        values.put(
            "organization_role",
            role.name()
        );

        values.put(
            "membership_role",
            role.name()
        );

        values.put(
            "status",
            "ACTIVE"
        );

        values.put(
            "email",
            user.email()
        );

        values.put(
            "invited_by_user_id",
            ownerId
        );

        values.put(
            "invited_by",
            ownerId
        );

        values.put(
            "created_at",
            Timestamp.from(
                Instant.now()
            )
        );

        values.put(
            "updated_at",
            Timestamp.from(
                Instant.now()
            )
        );

        values.put(
            "joined_at",
            Timestamp.from(
                Instant.now()
            )
        );

        values.put(
            "accepted_at",
            Timestamp.from(
                Instant.now()
            )
        );

        values.put(
            "is_active",
            true
        );

        values.put(
            "version",
            0L
        );

        insertRequiredAware(
            table,
            values
        );

        UUID membershipId =
            membershipId(
                organizationId,
                user.id()
            );

        if (membershipId == null) {
            throw new IllegalStateException(
                "Demo organization membership insert did not persist."
            );
        }

        return membershipId;
    }

    private UUID membershipId(
        UUID organizationId,
        UUID userId
    ) {
        String table =
            firstExistingTable(
                List.of(
                    "organization_memberships",
                    "memberships"
                )
            );

        if (table == null) {
            return null;
        }

        Set<String> columns =
            columns(table);

        if (
            !columns.contains(
                "organization_id"
            )
            || !columns.contains(
                "user_id"
            )
            || !columns.contains(
                "id"
            )
        ) {
            return null;
        }

        List<UUID> ids =
            jdbcTemplate.queryForList(
                """
                SELECT id
                FROM %s
                WHERE organization_id = ?
                  AND user_id = ?
                LIMIT 1
                """.formatted(
                    identifier(table)
                ),
                UUID.class,
                organizationId,
                userId
            );

        return ids.isEmpty()
            ? null
            : ids.getFirst();
    }

    private void ensureProjectMembership(
        UUID organizationId,
        UUID projectId,
        UUID userId,
        UUID membershipId,
        String projectRole
    ) {
        String table =
            firstExistingTable(
                List.of(
                    "project_members",
                    "project_memberships"
                )
            );

        if (table == null) {
            LOGGER.warn(
                "Project membership table not found; role demonstration will use organization membership only."
            );

            return;
        }

        Set<String> columns =
            columns(table);

        String userColumn =
            columns.contains(
                "user_id"
            )
                ? "user_id"
                : columns.contains(
                    "membership_id"
                )
                    ? "membership_id"
                    : null;

        if (
            !columns.contains(
                "project_id"
            )
            || userColumn == null
        ) {
            LOGGER.warn(
                "Project membership table does not expose expected project/user boundary."
            );

            return;
        }

        Object userBoundary =
            "membership_id"
                .equals(
                    userColumn
                )
                ? membershipId
                : userId;

        Integer count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM %s
                WHERE project_id = ?
                  AND %s = ?
                """.formatted(
                    identifier(table),
                    identifier(userColumn)
                ),
                Integer.class,
                projectId,
                userBoundary
            );

        if (
            count != null
            && count > 0
        ) {
            return;
        }

        Map<String, Object> values =
            new LinkedHashMap<>();

        values.put(
            "id",
            UUID.randomUUID()
        );

        values.put(
            "organization_id",
            organizationId
        );

        values.put(
            "project_id",
            projectId
        );

        values.put(
            "user_id",
            userId
        );

        values.put(
            "membership_id",
            membershipId
        );

        values.put(
            "project_role",
            projectRole
        );

        values.put(
            "project_member_role",
            projectRole
        );

        values.put(
            "role",
            projectRole
        );

        values.put(
            "status",
            "ACTIVE"
        );

        values.put(
            "joined_at",
            Timestamp.from(
                Instant.now()
            )
        );

        values.put(
            "created_at",
            Timestamp.from(
                Instant.now()
            )
        );

        values.put(
            "updated_at",
            Timestamp.from(
                Instant.now()
            )
        );

        values.put(
            "version",
            0L
        );

        insertRequiredAware(
            table,
            values
        );
    }

    private void linkClientUserIfSupported(
        UUID organizationId,
        UUID clientId,
        UUID userId,
        UUID membershipId
    ) {
        List<String> candidates =
            List.of(
                "client_users",
                "client_user_access",
                "client_members",
                "client_access"
            );

        String table =
            firstExistingTable(
                candidates
            );

        if (table == null) {
            updateClientEmailIfSupported(
                clientId,
                "client@buildflow.demo"
            );

            return;
        }

        Set<String> columns =
            columns(table);

        if (
            !columns.contains(
                "client_id"
            )
        ) {
            return;
        }

        String identityColumn =
            columns.contains(
                "user_id"
            )
                ? "user_id"
                : columns.contains(
                    "membership_id"
                )
                    ? "membership_id"
                    : null;

        if (identityColumn == null) {
            return;
        }

        Object identity =
            "membership_id"
                .equals(
                    identityColumn
                )
                ? membershipId
                : userId;

        Integer count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM %s
                WHERE client_id = ?
                  AND %s = ?
                """.formatted(
                    identifier(table),
                    identifier(
                        identityColumn
                    )
                ),
                Integer.class,
                clientId,
                identity
            );

        if (
            count != null
            && count > 0
        ) {
            return;
        }

        Map<String, Object> values =
            new LinkedHashMap<>();

        values.put(
            "id",
            UUID.randomUUID()
        );

        values.put(
            "organization_id",
            organizationId
        );

        values.put(
            "client_id",
            clientId
        );

        values.put(
            "user_id",
            userId
        );

        values.put(
            "membership_id",
            membershipId
        );

        values.put(
            "status",
            "ACTIVE"
        );

        values.put(
            "created_at",
            Timestamp.from(
                Instant.now()
            )
        );

        values.put(
            "updated_at",
            Timestamp.from(
                Instant.now()
            )
        );

        values.put(
            "version",
            0L
        );

        insertRequiredAware(
            table,
            values
        );
    }

    private void updateClientEmailIfSupported(
        UUID clientId,
        String email
    ) {
        if (
            !tableExists(
                "clients"
            )
        ) {
            return;
        }

        Set<String> clientColumns =
            columns(
                "clients"
            );

        for (
            String candidate
            : List.of(
                "email",
                "contact_email"
            )
        ) {
            if (
                clientColumns.contains(
                    candidate
                )
            ) {
                jdbcTemplate.update(
                    """
                    UPDATE clients
                    SET %s = ?
                    WHERE id = ?
                    """.formatted(
                        identifier(
                            candidate
                        )
                    ),
                    email,
                    clientId
                );

                return;
            }
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
                "No values resolved for demo insert into "
                    + table
            );
        }

        List<String> columns =
            new ArrayList<>(
                values.keySet()
            );

        String columnSql =
            columns.stream()
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
            columns.stream()
                .map(
                    ignored -> "?"
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
            columns.stream()
                .map(
                    values::get
                )
                .toArray();

        jdbcTemplate.update(
            """
            INSERT INTO %s (%s)
            VALUES (%s)
            """.formatted(
                identifier(table),
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
            "id".equals(name)
            || name.endsWith(
                "_id"
            )
        ) {
            return null;
        }

        if (
            name.endsWith(
                "_at"
            )
            && type.contains(
                "timestamp"
            )
        ) {
            return Timestamp.from(
                Instant.now()
            );
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
                "boolean"
            )
        ) {
            return true;
        }

        return null;
    }

    private void normalizeDemoAuthenticationState(
        String userTable
    ) {
        Set<String> availableColumns =
            columns(
                userTable
            );

        List<String> assignments =
            new ArrayList<>();

        addDemoAssignment(
            assignments,
            availableColumns,
            "email_verified",
            "TRUE"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "is_email_verified",
            "TRUE"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "verified",
            "TRUE"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "is_verified",
            "TRUE"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "email_confirmed",
            "TRUE"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "enabled",
            "TRUE"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "is_enabled",
            "TRUE"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "active",
            "TRUE"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "is_active",
            "TRUE"
        );

        /*
         * A SQL string literal is intentionally used here instead
         * of a bind parameter so PostgreSQL can coerce ACTIVE to
         * either VARCHAR/TEXT or a native enum column.
         */
        addDemoAssignment(
            assignments,
            availableColumns,
            "status",
            "'ACTIVE'"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "failed_login_attempts",
            "0"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "login_attempts",
            "0"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "locked_until",
            "NULL"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "lock_until",
            "NULL"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "locked_at",
            "NULL"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "deactivated_at",
            "NULL"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "suspended_at",
            "NULL"
        );

        addDemoAssignment(
            assignments,
            availableColumns,
            "deleted_at",
            "NULL"
        );

        addDemoTimestampAssignment(
            assignments,
            availableColumns,
            "email_verified_at"
        );

        addDemoTimestampAssignment(
            assignments,
            availableColumns,
            "verified_at"
        );

        addDemoTimestampAssignment(
            assignments,
            availableColumns,
            "confirmed_at"
        );

        if (assignments.isEmpty()) {
            LOGGER.info(
                "No explicit account lifecycle columns require demo normalization in {}.",
                userTable
            );

            return;
        }

        String placeholders =
            String.join(
                ", ",
                java.util.Collections.nCopies(
                    DEMO_EMAILS.size(),
                    "?"
                )
            );

        String sql =
            """
            UPDATE %s
            SET %s
            WHERE LOWER(email) IN (%s)
            """.formatted(
                identifier(
                    userTable
                ),
                String.join(
                    ", ",
                    assignments
                ),
                placeholders
            );

        Object[] arguments =
            DEMO_EMAILS
                .stream()
                .map(
                    email ->
                        email.toLowerCase(
                            Locale.ROOT
                        )
                )
                .toArray();

        int updated =
            jdbcTemplate.update(
                sql,
                arguments
            );

        LOGGER.info(
            "Normalized authentication lifecycle state for {} BuildFlow demo users.",
            updated
        );
    }

    private void addDemoAssignment(
        List<String> assignments,
        Set<String> availableColumns,
        String column,
        String expression
    ) {
        if (
            availableColumns.contains(
                column
            )
        ) {
            assignments.add(
                identifier(
                    column
                )
                    + " = "
                    + expression
            );
        }
    }

    private void addDemoTimestampAssignment(
        List<String> assignments,
        Set<String> availableColumns,
        String column
    ) {
        if (
            availableColumns.contains(
                column
            )
        ) {
            assignments.add(
                identifier(
                    column
                )
                    + " = COALESCE("
                    + identifier(
                        column
                    )
                    + ", CURRENT_TIMESTAMP)"
            );
        }
    }

    private void assertCompleteDemoUserSet(
        String userTable
    ) {
        String placeholders =
            String.join(
                ", ",
                java.util.Collections.nCopies(
                    DEMO_EMAILS.size(),
                    "?"
                )
            );

        Object[] arguments =
            DEMO_EMAILS
                .stream()
                .map(
                    email ->
                        email.toLowerCase(
                            Locale.ROOT
                        )
                )
                .toArray();

        Integer count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM %s
                WHERE LOWER(email) IN (%s)
                """.formatted(
                    identifier(
                        userTable
                    ),
                    placeholders
                ),
                Integer.class,
                arguments
            );

        if (
            count == null
            || count
                != DEMO_EMAILS.size()
        ) {
            throw new IllegalStateException(
                "BuildFlow demo user set is incomplete: expected "
                    + DEMO_EMAILS.size()
                    + " accounts but found "
                    + count
                    + "."
            );
        }
    }

    private UUID userIdByEmail(
        String table,
        String email
    ) {
        Set<String> columns =
            columns(table);

        if (
            !columns.contains(
                "id"
            )
            || !columns.contains(
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
                    identifier(table)
                ),
                UUID.class,
                email
            );

        return ids.isEmpty()
            ? null
            : ids.getFirst();
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
                "Invalid SQL identifier."
            );
        }

        return "\""
            + value
            + "\"";
    }

    private Map<String, Object> recordValues(
        Object... entries
    ) {
        if (
            entries.length % 2
                != 0
        ) {
            throw new IllegalArgumentException(
                "Record values require key/value pairs."
            );
        }

        Map<String, Object> result =
            new LinkedHashMap<>();

        for (
            int index = 0;
            index < entries.length;
            index += 2
        ) {
            Object key =
                entries[index];

            if (
                !(key instanceof String)
            ) {
                throw new IllegalArgumentException(
                    "Record value keys must be strings."
                );
            }

            result.put(
                (String) key,
                entries[
                    index + 1
                ]
            );
        }

        return result;
    }

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
                        component
                            .getType(),
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
                "Could not build demo request record "
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

    private record DemoUser(
        UUID id,
        String email
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
