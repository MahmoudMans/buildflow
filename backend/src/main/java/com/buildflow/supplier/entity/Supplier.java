package com.buildflow.supplier.entity;

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
@Table(name = "suppliers")
public class Supplier {

    @Id
    private UUID id;

    @Column(
        name = "organization_id",
        nullable = false
    )
    private UUID organizationId;

    @Column(
        nullable = false,
        length = 250
    )
    private String name;

    @Column(
        name = "contact_name",
        length = 200
    )
    private String contactName;

    @Column(length = 320)
    private String email;

    @Column(length = 100)
    private String phone;

    private String address;

    @Column(
        name = "tax_number",
        length = 100
    )
    private String taxNumber;

    @Column(
        name = "payment_terms",
        length = 250
    )
    private String paymentTerms;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupplierStatus status;

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

    protected Supplier() {
    }

    public static Supplier create(
        UUID organizationId,
        String name,
        String contactName,
        String email,
        String phone,
        String address,
        String taxNumber,
        String paymentTerms,
        String notes,
        Instant now
    ) {
        Supplier supplier =
            new Supplier();

        supplier.id =
            UUID.randomUUID();

        supplier.organizationId =
            organizationId;

        supplier.name =
            name;

        supplier.contactName =
            contactName;

        supplier.email =
            email;

        supplier.phone =
            phone;

        supplier.address =
            address;

        supplier.taxNumber =
            taxNumber;

        supplier.paymentTerms =
            paymentTerms;

        supplier.notes =
            notes;

        supplier.status =
            SupplierStatus.ACTIVE;

        supplier.createdAt =
            now;

        supplier.updatedAt =
            now;

        return supplier;
    }

    public void update(
        String name,
        String contactName,
        String email,
        String phone,
        String address,
        String taxNumber,
        String paymentTerms,
        String notes,
        SupplierStatus status,
        Instant now
    ) {
        if (name != null) {
            this.name =
                name;
        }

        if (contactName != null) {
            this.contactName =
                contactName;
        }

        if (email != null) {
            this.email =
                email;
        }

        if (phone != null) {
            this.phone =
                phone;
        }

        if (address != null) {
            this.address =
                address;
        }

        if (taxNumber != null) {
            this.taxNumber =
                taxNumber;
        }

        if (paymentTerms != null) {
            this.paymentTerms =
                paymentTerms;
        }

        if (notes != null) {
            this.notes =
                notes;
        }

        if (
            status != null
            && status != SupplierStatus.ARCHIVED
        ) {
            this.status =
                status;
        }

        this.updatedAt =
            now;
    }

    public void archive(
        Instant now
    ) {
        this.status =
            SupplierStatus.ARCHIVED;

        this.updatedAt =
            now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public String getContactName() {
        return contactName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public String getNotes() {
        return notes;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
