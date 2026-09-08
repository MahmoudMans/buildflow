package com.buildflow.supplier.dto;

import com.buildflow.supplier.entity.Supplier;
import com.buildflow.supplier.entity.SupplierStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record CreateSupplierRequest(

        @NotBlank
        @Size(max = 250)
        String name,

        @Size(max = 200)
        String contactName,

        @Email
        @Size(max = 320)
        String email,

        @Size(max = 100)
        String phone,

        @Size(max = 5000)
        String address,

        @Size(max = 100)
        String taxNumber,

        @Size(max = 250)
        String paymentTerms,

        @Size(max = 5000)
        String notes

    ) {
    }

    public record UpdateSupplierRequest(

        @Size(
            min = 1,
            max = 250
        )
        String name,

        @Size(max = 200)
        String contactName,

        @Email
        @Size(max = 320)
        String email,

        @Size(max = 100)
        String phone,

        @Size(max = 5000)
        String address,

        @Size(max = 100)
        String taxNumber,

        @Size(max = 250)
        String paymentTerms,

        @Size(max = 5000)
        String notes,

        SupplierStatus status

    ) {
    }

    public record SupplierView(
        UUID id,
        String name,
        String contactName,
        String email,
        String phone,
        String address,
        String taxNumber,
        String paymentTerms,
        String notes,
        SupplierStatus status,
        Instant createdAt
    ) {
        public static SupplierView from(
            Supplier supplier
        ) {
            return new SupplierView(
                supplier.getId(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getTaxNumber(),
                supplier.getPaymentTerms(),
                supplier.getNotes(),
                supplier.getStatus(),
                supplier.getCreatedAt()
            );
        }
    }
}
