package com.buildflow.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(
        name = "legal_name",
        length = 250
    )
    private String legalName;

    @Column(
        name = "registration_number",
        length = 100
    )
    private String registrationNumber;

    @Column(
        name = "tax_number",
        length = 100
    )
    private String taxNumber;

    @Column(length = 320)
    private String email;

    @Column(length = 100)
    private String phone;

    @Column(
        name = "address_line_1",
        length = 250
    )
    private String addressLine1;

    @Column(
        name = "address_line_2",
        length = 250
    )
    private String addressLine2;

    @Column(length = 150)
    private String city;

    @Column(
        name = "postal_code",
        length = 50
    )
    private String postalCode;

    @Column(length = 150)
    private String country;

    @Column(
        nullable = false,
        length = 3
    )
    private String currency;

    @Column(
        nullable = false,
        length = 100
    )
    private String timezone;

    @Column(
        name = "logo_url",
        length = 1000
    )
    private String logoUrl;

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

    protected Organization() {
    }

    public static Organization create(
        String name,
        String legalName,
        String registrationNumber,
        String taxNumber,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String postalCode,
        String country,
        String currency,
        String timezone,
        String logoUrl,
        Instant now
    ) {
        Organization organization =
            new Organization();

        organization.id = UUID.randomUUID();
        organization.name = name;
        organization.legalName = legalName;
        organization.registrationNumber =
            registrationNumber;
        organization.taxNumber = taxNumber;
        organization.email = email;
        organization.phone = phone;
        organization.addressLine1 =
            addressLine1;
        organization.addressLine2 =
            addressLine2;
        organization.city = city;
        organization.postalCode =
            postalCode;
        organization.country = country;
        organization.currency = currency;
        organization.timezone = timezone;
        organization.logoUrl = logoUrl;
        organization.createdAt = now;
        organization.updatedAt = now;

        return organization;
    }

    public void update(
        String name,
        String legalName,
        String registrationNumber,
        String taxNumber,
        String email,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String postalCode,
        String country,
        String currency,
        String timezone,
        String logoUrl,
        Instant now
    ) {
        if (name != null) {
            this.name = name;
        }

        if (legalName != null) {
            this.legalName = legalName;
        }

        if (registrationNumber != null) {
            this.registrationNumber =
                registrationNumber;
        }

        if (taxNumber != null) {
            this.taxNumber = taxNumber;
        }

        if (email != null) {
            this.email = email;
        }

        if (phone != null) {
            this.phone = phone;
        }

        if (addressLine1 != null) {
            this.addressLine1 =
                addressLine1;
        }

        if (addressLine2 != null) {
            this.addressLine2 =
                addressLine2;
        }

        if (city != null) {
            this.city = city;
        }

        if (postalCode != null) {
            this.postalCode =
                postalCode;
        }

        if (country != null) {
            this.country = country;
        }

        if (currency != null) {
            this.currency = currency;
        }

        if (timezone != null) {
            this.timezone = timezone;
        }

        if (logoUrl != null) {
            this.logoUrl = logoUrl;
        }

        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
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

    public String getCurrency() {
        return currency;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
