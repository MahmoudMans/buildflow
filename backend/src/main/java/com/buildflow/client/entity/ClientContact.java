package com.buildflow.client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_contacts")
public class ClientContact {

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
        name = "first_name",
        nullable = false,
        length = 100
    )
    private String firstName;

    @Column(
        name = "last_name",
        nullable = false,
        length = 100
    )
    private String lastName;

    @Column(
        name = "job_title",
        length = 150
    )
    private String jobTitle;

    @Column(length = 320)
    private String email;

    @Column(length = 100)
    private String phone;

    @Column(
        name = "is_primary",
        nullable = false
    )
    private boolean primary;

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

    protected ClientContact() {
    }

    public static ClientContact create(
        UUID organizationId,
        UUID clientId,
        String firstName,
        String lastName,
        String jobTitle,
        String email,
        String phone,
        boolean primary,
        Instant now
    ) {
        ClientContact contact =
            new ClientContact();

        contact.id =
            UUID.randomUUID();

        contact.organizationId =
            organizationId;

        contact.clientId =
            clientId;

        contact.firstName =
            firstName;

        contact.lastName =
            lastName;

        contact.jobTitle =
            jobTitle;

        contact.email =
            email;

        contact.phone =
            phone;

        contact.primary =
            primary;

        contact.createdAt =
            now;

        contact.updatedAt =
            now;

        return contact;
    }

    public void update(
        String firstName,
        String lastName,
        String jobTitle,
        String email,
        String phone,
        Boolean primary,
        Instant now
    ) {
        if (firstName != null) {
            this.firstName =
                firstName;
        }

        if (lastName != null) {
            this.lastName =
                lastName;
        }

        if (jobTitle != null) {
            this.jobTitle =
                jobTitle;
        }

        if (email != null) {
            this.email =
                email;
        }

        if (phone != null) {
            this.phone =
                phone;
        }

        if (primary != null) {
            this.primary =
                primary;
        }

        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
