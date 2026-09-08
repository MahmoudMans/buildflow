package com.buildflow.project.entity;

import com.buildflow.shared.error.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

    private static final Map<
        ProjectStatus,
        Set<ProjectStatus>
    > ALLOWED_TRANSITIONS =
        Map.of(
            ProjectStatus.DRAFT,
            Set.of(
                ProjectStatus.PLANNING,
                ProjectStatus.CANCELLED
            ),

            ProjectStatus.PLANNING,
            Set.of(
                ProjectStatus.ACTIVE,
                ProjectStatus.ON_HOLD,
                ProjectStatus.CANCELLED
            ),

            ProjectStatus.ACTIVE,
            Set.of(
                ProjectStatus.ON_HOLD,
                ProjectStatus.COMPLETED,
                ProjectStatus.CANCELLED
            ),

            ProjectStatus.ON_HOLD,
            Set.of(
                ProjectStatus.ACTIVE,
                ProjectStatus.CANCELLED
            ),

            ProjectStatus.COMPLETED,
            Set.of(
                ProjectStatus.ARCHIVED
            ),

            ProjectStatus.CANCELLED,
            Set.of(),

            ProjectStatus.ARCHIVED,
            Set.of()
        );

    @Id
    private UUID id;

    @Column(
        name = "organization_id",
        nullable = false
    )
    private UUID organizationId;

    @Column(
        name = "client_id",
        nullable = false
    )
    private UUID clientId;

    @Column(
        name = "project_number",
        nullable = false,
        length = 50
    )
    private String projectNumber;

    @Column(
        nullable = false,
        length = 250
    )
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "project_type",
        nullable = false
    )
    private ProjectType projectType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectPriority priority;

    @Column(
        name = "site_address",
        length = 300
    )
    private String siteAddress;

    @Column(length = 150)
    private String city;

    @Column(
        name = "postal_code",
        length = 50
    )
    private String postalCode;

    @Column(length = 150)
    private String country;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(
        nullable = false,
        length = 3
    )
    private String currency;

    @Column(
        name = "contract_value",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal contractValue;

    @Column(
        name = "original_budget",
        nullable = false,
        precision = 19,
        scale = 4
    )
    private BigDecimal originalBudget;

    @Column(
        name = "project_manager_membership_id"
    )
    private UUID projectManagerMembershipId;

    @Column(
        name = "progress_percentage",
        nullable = false,
        precision = 5,
        scale = 2
    )
    private BigDecimal progressPercentage;

    @Column(
        name = "created_at",
        nullable = false
    )
    private Instant createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Project() {
    }

    public static Project create(
        UUID organizationId,
        UUID clientId,
        String projectNumber,
        String name,
        String description,
        ProjectType projectType,
        ProjectPriority priority,
        String siteAddress,
        String city,
        String postalCode,
        String country,
        LocalDate startDate,
        LocalDate plannedEndDate,
        String currency,
        UUID projectManagerMembershipId,
        Instant now
    ) {
        Project project =
            new Project();

        project.id =
            UUID.randomUUID();

        project.organizationId =
            organizationId;

        project.clientId =
            clientId;

        project.projectNumber =
            projectNumber;

        project.name =
            name;

        project.description =
            description;

        project.projectType =
            projectType;

        project.status =
            ProjectStatus.DRAFT;

        project.priority =
            priority;

        project.siteAddress =
            siteAddress;

        project.city =
            city;

        project.postalCode =
            postalCode;

        project.country =
            country;

        project.startDate =
            startDate;

        project.plannedEndDate =
            plannedEndDate;

        project.currency =
            currency;

        project.projectManagerMembershipId =
            projectManagerMembershipId;

        project.contractValue =
            BigDecimal.ZERO;

        project.originalBudget =
            BigDecimal.ZERO;

        project.progressPercentage =
            BigDecimal.ZERO;

        project.createdAt =
            now;

        project.updatedAt =
            now;

        return project;
    }

    public void update(
        UUID clientId,
        String name,
        String description,
        ProjectType projectType,
        ProjectPriority priority,
        String siteAddress,
        String city,
        String postalCode,
        String country,
        LocalDate startDate,
        LocalDate plannedEndDate,
        String currency,
        UUID projectManagerMembershipId,
        boolean managerSupplied,
        Instant now
    ) {
        requireEditable();

        if (clientId != null) {
            requirePlanningEditable(
                "The project client"
            );

            this.clientId =
                clientId;
        }

        if (name != null) {
            this.name =
                name;
        }

        if (description != null) {
            this.description =
                description;
        }

        if (projectType != null) {
            requirePlanningEditable(
                "The project type"
            );

            this.projectType =
                projectType;
        }

        if (priority != null) {
            this.priority =
                priority;
        }

        if (siteAddress != null) {
            this.siteAddress =
                siteAddress;
        }

        if (city != null) {
            this.city =
                city;
        }

        if (postalCode != null) {
            this.postalCode =
                postalCode;
        }

        if (country != null) {
            this.country =
                country;
        }

        if (startDate != null) {
            this.startDate =
                startDate;
        }

        if (plannedEndDate != null) {
            this.plannedEndDate =
                plannedEndDate;
        }

        if (currency != null) {
            requirePlanningEditable(
                "The project currency"
            );

            this.currency =
                currency;
        }

        if (managerSupplied) {
            this.projectManagerMembershipId =
                projectManagerMembershipId;
        }

        validateDates();

        this.updatedAt =
            now;
    }

    public void transition(
        ProjectStatus target,
        LocalDate today,
        Instant now
    ) {
        Set<ProjectStatus> allowed =
            ALLOWED_TRANSITIONS.get(
                status
            );

        if (
            allowed == null
            || !allowed.contains(target)
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "INVALID_PROJECT_TRANSITION",
                "Project status cannot transition from "
                    + status
                    + " to "
                    + target
                    + "."
            );
        }

        status =
            target;

        if (
            target
                == ProjectStatus.COMPLETED
        ) {
            actualEndDate =
                today;
        }

        if (
            target
                == ProjectStatus.ARCHIVED
        ) {
            archivedAt =
                now;
        }

        updatedAt =
            now;
    }

    public void setProjectManagerMembershipId(
        UUID membershipId,
        Instant now
    ) {
        this.projectManagerMembershipId =
            membershipId;

        this.updatedAt =
            now;
    }

    private void requireEditable() {
        if (
            status == ProjectStatus.COMPLETED
            || status == ProjectStatus.CANCELLED
            || status == ProjectStatus.ARCHIVED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PROJECT_NOT_EDITABLE",
                "This project can no longer be edited."
            );
        }
    }

    private void requirePlanningEditable(
        String field
    ) {
        if (
            status != ProjectStatus.DRAFT
            && status != ProjectStatus.PLANNING
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PROJECT_FIELD_LOCKED",
                field
                    + " cannot be changed after project activation."
            );
        }
    }

    private void validateDates() {
        if (
            startDate != null
            && plannedEndDate != null
            && plannedEndDate.isBefore(
                startDate
            )
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PROJECT_DATES",
                "The planned end date cannot be before the start date."
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getProjectNumber() {
        return projectNumber;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public ProjectPriority getPriority() {
        return priority;
    }

    public String getSiteAddress() {
        return siteAddress;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getContractValue() {
        return contractValue;
    }

    public BigDecimal getOriginalBudget() {
        return originalBudget;
    }

    public UUID getProjectManagerMembershipId() {
        return projectManagerMembershipId;
    }

    public BigDecimal getProgressPercentage() {
        return progressPercentage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
