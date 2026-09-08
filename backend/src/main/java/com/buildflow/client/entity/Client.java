package com.buildflow.client.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    private UUID id;

    @Column(
        name = "organization_id",
        nullable = false
    )
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientType type;

    @Column(
        name = "company_name",
        length = 250
    )
    private String companyName;

    @Column(
        name = "first_name",
        length = 100
    )
    private String firstName;

    @Column(
        name = "last_name",
        length = 100
    )
    private String lastName;

    @Column(length = 320)
    private String email;

    @Column(length = 100)
    private String phone;

    @Column(
        name = "tax_number",
        length = 100
    )
    private String taxNumber;

    @Column(
        name = "registration_number",
        length = 100
    )
    private String registrationNumber;

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "site_address")
    private String siteAddress;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientStatus status;

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

    protected Client() {
    }

    public static Client create(
        UUID organizationId,
        ClientType type,
        String companyName,
        String firstName,
        String lastName,
        String email,
        String phone,
        String taxNumber,
        String registrationNumber,
        String billingAddress,
        String siteAddress,
        String notes,
        Instant now
    ) {
        Client client = new Client();

        client.id = UUID.randomUUID();
        client.organizationId =
            organizationId;
        client.type = type;
        client.companyName =
            companyName;
        client.firstName = firstName;
        client.lastName = lastName;
        client.email = email;
        client.phone = phone;
        client.taxNumber = taxNumber;
        client.registrationNumber =
            registrationNumber;
        client.billingAddress =
            billingAddress;
        client.siteAddress =
            siteAddress;
        client.notes = notes;
        client.status =
            ClientStatus.ACTIVE;
        client.createdAt = now;
        client.updatedAt = now;

        return client;
    }

    public void update(
        ClientType type,
        String companyName,
        String firstName,
        String lastName,
        String email,
        String phone,
        String taxNumber,
        String registrationNumber,
        String billingAddress,
        String siteAddress,
        String notes,
        ClientStatus status,
        Instant now
    ) {
        if (type != null) {
            this.type = type;
        }

        if (companyName != null) {
            this.companyName =
                companyName;
        }

        if (firstName != null) {
            this.firstName =
                firstName;
        }

        if (lastName != null) {
            this.lastName =
                lastName;
        }

        if (email != null) {
            this.email = email;
        }

        if (phone != null) {
            this.phone = phone;
        }

        if (taxNumber != null) {
            this.taxNumber =
                taxNumber;
        }

        if (registrationNumber != null) {
            this.registrationNumber =
                registrationNumber;
        }

        if (billingAddress != null) {
            this.billingAddress =
                billingAddress;
        }

        if (siteAddress != null) {
            this.siteAddress =
                siteAddress;
        }

        if (notes != null) {
            this.notes = notes;
        }

        if (
            status != null
                && status != ClientStatus.ARCHIVED
        ) {
            this.status = status;
        }

        this.updatedAt = now;
    }

    public void archive(
        Instant now
    ) {
        this.status =
            ClientStatus.ARCHIVED;

        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public ClientType getType() {
        return type;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public String getSiteAddress() {
        return siteAddress;
    }

    public String getNotes() {
        return notes;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDisplayName() {
        if (
            type == ClientType.COMPANY
        ) {
            return companyName;
        }

        return firstName
            + " "
            + lastName;
    }
}
