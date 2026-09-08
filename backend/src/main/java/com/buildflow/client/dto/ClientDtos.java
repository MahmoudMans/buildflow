package com.buildflow.client.dto;

import com.buildflow.client.entity.Client;
import com.buildflow.client.entity.ClientContact;
import com.buildflow.client.entity.ClientStatus;
import com.buildflow.client.entity.ClientType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ClientDtos {

    private ClientDtos() {
    }

    public record CreateClientRequest(

        @NotNull
        ClientType type,

        @Size(max = 250)
        String companyName,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Email
        @Size(max = 320)
        String email,

        @Size(max = 100)
        String phone,

        @Size(max = 100)
        String taxNumber,

        @Size(max = 100)
        String registrationNumber,

        @Size(max = 2000)
        String billingAddress,

        @Size(max = 2000)
        String siteAddress,

        @Size(max = 5000)
        String notes

    ) {
    }

    public record UpdateClientRequest(

        ClientType type,

        @Size(max = 250)
        String companyName,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Email
        @Size(max = 320)
        String email,

        @Size(max = 100)
        String phone,

        @Size(max = 100)
        String taxNumber,

        @Size(max = 100)
        String registrationNumber,

        @Size(max = 2000)
        String billingAddress,

        @Size(max = 2000)
        String siteAddress,

        @Size(max = 5000)
        String notes,

        ClientStatus status

    ) {
    }

    public record CreateContactRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @Size(max = 150)
        String jobTitle,

        @Email
        @Size(max = 320)
        String email,

        @Size(max = 100)
        String phone,

        boolean primary

    ) {
    }

    public record UpdateContactRequest(

        @Size(
            min = 1,
            max = 100
        )
        String firstName,

        @Size(
            min = 1,
            max = 100
        )
        String lastName,

        @Size(max = 150)
        String jobTitle,

        @Email
        @Size(max = 320)
        String email,

        @Size(max = 100)
        String phone,

        Boolean primary

    ) {
    }

    public record ClientSummary(
        UUID id,
        ClientType type,
        String displayName,
        String email,
        String phone,
        ClientStatus status,
        Instant createdAt
    ) {
        public static ClientSummary from(
            Client client
        ) {
            return new ClientSummary(
                client.getId(),
                client.getType(),
                client.getDisplayName(),
                client.getEmail(),
                client.getPhone(),
                client.getStatus(),
                client.getCreatedAt()
            );
        }
    }

    public record ContactView(
        UUID id,
        String firstName,
        String lastName,
        String jobTitle,
        String email,
        String phone,
        boolean primary,
        Instant createdAt
    ) {
        public static ContactView from(
            ClientContact contact
        ) {
            return new ContactView(
                contact.getId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getJobTitle(),
                contact.getEmail(),
                contact.getPhone(),
                contact.isPrimary(),
                contact.getCreatedAt()
            );
        }
    }

    public record ClientView(
        UUID id,
        ClientType type,
        String companyName,
        String firstName,
        String lastName,
        String displayName,
        String email,
        String phone,
        String taxNumber,
        String registrationNumber,
        String billingAddress,
        String siteAddress,
        String notes,
        ClientStatus status,
        Instant createdAt,
        List<ContactView> contacts
    ) {
        public static ClientView from(
            Client client,
            List<ClientContact> contacts
        ) {
            return new ClientView(
                client.getId(),
                client.getType(),
                client.getCompanyName(),
                client.getFirstName(),
                client.getLastName(),
                client.getDisplayName(),
                client.getEmail(),
                client.getPhone(),
                client.getTaxNumber(),
                client.getRegistrationNumber(),
                client.getBillingAddress(),
                client.getSiteAddress(),
                client.getNotes(),
                client.getStatus(),
                client.getCreatedAt(),
                contacts
                    .stream()
                    .map(
                        ContactView::from
                    )
                    .toList()
            );
        }
    }
}
