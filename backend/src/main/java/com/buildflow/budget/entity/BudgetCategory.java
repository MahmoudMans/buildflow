package com.buildflow.budget.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "budget_categories")
public class BudgetCategory {

    @Id
    private UUID id;

    @Column(
        name = "organization_id",
        nullable = false
    )
    private UUID organizationId;

    @Column(
        name = "project_id",
        nullable = false
    )
    private UUID projectId;

    @Column(
        nullable = false,
        length = 200
    )
    private String name;

    private String description;

    @Column(
        name = "sort_order",
        nullable = false
    )
    private int sortOrder;

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

    @Version
    @Column(nullable = false)
    private long version;

    protected BudgetCategory() {
    }

    public static BudgetCategory create(
        UUID organizationId,
        UUID projectId,
        String name,
        String description,
        int sortOrder,
        Instant now
    ) {
        BudgetCategory category =
            new BudgetCategory();

        category.id =
            UUID.randomUUID();

        category.organizationId =
            organizationId;

        category.projectId =
            projectId;

        category.name =
            name;

        category.description =
            description;

        category.sortOrder =
            sortOrder;

        category.createdAt =
            now;

        category.updatedAt =
            now;

        return category;
    }

    public void update(
        String name,
        String description,
        Integer sortOrder,
        Instant now
    ) {
        if (name != null) {
            this.name =
                name;
        }

        if (description != null) {
            this.description =
                description;
        }

        if (sortOrder != null) {
            this.sortOrder =
                sortOrder;
        }

        this.updatedAt =
            now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
