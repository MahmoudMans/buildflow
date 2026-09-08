package com.buildflow.client.service;

import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.Client;
import com.buildflow.client.entity.ClientContact;
import com.buildflow.client.entity.ClientStatus;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.repository.ClientContactRepository;
import com.buildflow.client.repository.ClientRepository;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ClientService {

    private static final Set<
        OrganizationRole
    > CLIENT_ROLES =
        Set.of(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.PROJECT_MANAGER,
            OrganizationRole.ACCOUNTANT
        );

    private final ClientRepository
        clientRepository;

    private final ClientContactRepository
        contactRepository;

    private final OrganizationAccessService
        accessService;

    public ClientService(
        ClientRepository clientRepository,
        ClientContactRepository contactRepository,
        OrganizationAccessService accessService
    ) {
        this.clientRepository =
            clientRepository;

        this.contactRepository =
            contactRepository;

        this.accessService =
            accessService;
    }

    @Transactional(readOnly = true)
    public List<
        ClientDtos.ClientSummary
    > list(
        UUID organizationId,
        UUID userId
    ) {
        requireClientPermission(
            organizationId,
            userId
        );

        return clientRepository
            .findByOrganizationIdOrderByCreatedAtDesc(
                organizationId
            )
            .stream()
            .map(
                ClientDtos
                    .ClientSummary
                    ::from
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public ClientDtos.ClientView get(
        UUID organizationId,
        UUID userId,
        UUID clientId
    ) {
        requireClientPermission(
            organizationId,
            userId
        );

        Client client =
            requireClient(
                organizationId,
                clientId
            );

        List<ClientContact> contacts =
            contactRepository
                .findByClientIdAndOrganizationIdOrderByCreatedAtAsc(
                    clientId,
                    organizationId
                );

        return ClientDtos
            .ClientView
            .from(
                client,
                contacts
            );
    }

    @Transactional
    public ClientDtos.ClientView create(
        UUID organizationId,
        UUID userId,
        ClientDtos.CreateClientRequest
            request
    ) {
        requireClientPermission(
            organizationId,
            userId
        );

        validateIdentity(
            request.type(),
            request.companyName(),
            request.firstName(),
            request.lastName()
        );

        Instant now =
            Instant.now();

        Client client =
            Client.create(
                organizationId,
                request.type(),
                clean(
                    request.companyName()
                ),
                clean(
                    request.firstName()
                ),
                clean(
                    request.lastName()
                ),
                normalizeEmail(
                    request.email()
                ),
                clean(
                    request.phone()
                ),
                clean(
                    request.taxNumber()
                ),
                clean(
                    request.registrationNumber()
                ),
                clean(
                    request.billingAddress()
                ),
                clean(
                    request.siteAddress()
                ),
                clean(
                    request.notes()
                ),
                now
            );

        clientRepository.save(
            client
        );

        return ClientDtos
            .ClientView
            .from(
                client,
                List.of()
            );
    }

    @Transactional
    public ClientDtos.ClientView update(
        UUID organizationId,
        UUID userId,
        UUID clientId,
        ClientDtos.UpdateClientRequest
            request
    ) {
        requireClientPermission(
            organizationId,
            userId
        );

        Client client =
            requireClient(
                organizationId,
                clientId
            );

        if (
            client.getStatus()
                == ClientStatus.ARCHIVED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "CLIENT_ARCHIVED",
                "An archived client cannot be modified."
            );
        }

        ClientType resultingType =
            request.type() == null
                ? client.getType()
                : request.type();

        String resultingCompany =
            request.companyName() == null
                ? client.getCompanyName()
                : clean(
                    request.companyName()
                );

        String resultingFirstName =
            request.firstName() == null
                ? client.getFirstName()
                : clean(
                    request.firstName()
                );

        String resultingLastName =
            request.lastName() == null
                ? client.getLastName()
                : clean(
                    request.lastName()
                );

        validateIdentity(
            resultingType,
            resultingCompany,
            resultingFirstName,
            resultingLastName
        );

        client.update(
            request.type(),
            cleanPreserveNull(
                request.companyName()
            ),
            cleanPreserveNull(
                request.firstName()
            ),
            cleanPreserveNull(
                request.lastName()
            ),
            normalizeEmailPreserveNull(
                request.email()
            ),
            cleanPreserveNull(
                request.phone()
            ),
            cleanPreserveNull(
                request.taxNumber()
            ),
            cleanPreserveNull(
                request.registrationNumber()
            ),
            cleanPreserveNull(
                request.billingAddress()
            ),
            cleanPreserveNull(
                request.siteAddress()
            ),
            cleanPreserveNull(
                request.notes()
            ),
            request.status(),
            Instant.now()
        );

        return get(
            organizationId,
            userId,
            clientId
        );
    }

    @Transactional
    public void archive(
        UUID organizationId,
        UUID userId,
        UUID clientId
    ) {
        requireClientPermission(
            organizationId,
            userId
        );

        Client client =
            requireClient(
                organizationId,
                clientId
            );

        client.archive(
            Instant.now()
        );
    }

    @Transactional
    public ClientDtos.ContactView
        addContact(
            UUID organizationId,
            UUID userId,
            UUID clientId,
            ClientDtos.CreateContactRequest
                request
        ) {
        requireClientPermission(
            organizationId,
            userId
        );

        Client client =
            requireClient(
                organizationId,
                clientId
            );

        if (
            client.getStatus()
                == ClientStatus.ARCHIVED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "CLIENT_ARCHIVED",
                "Contacts cannot be added to an archived client."
            );
        }

        boolean firstContact =
            contactRepository
                .countByClientIdAndOrganizationId(
                    clientId,
                    organizationId
                )
                == 0;

        boolean primary =
            request.primary()
                || firstContact;

        if (primary) {
            contactRepository
                .clearPrimaryContact(
                    clientId,
                    organizationId
                );

            contactRepository.flush();
        }

        ClientContact contact =
            ClientContact.create(
                organizationId,
                clientId,
                cleanRequired(
                    request.firstName()
                ),
                cleanRequired(
                    request.lastName()
                ),
                clean(
                    request.jobTitle()
                ),
                normalizeEmail(
                    request.email()
                ),
                clean(
                    request.phone()
                ),
                primary,
                Instant.now()
            );

        contactRepository.save(
            contact
        );

        return ClientDtos
            .ContactView
            .from(contact);
    }

    @Transactional
    public ClientDtos.ContactView
        updateContact(
            UUID organizationId,
            UUID userId,
            UUID contactId,
            ClientDtos.UpdateContactRequest
                request
        ) {
        requireClientPermission(
            organizationId,
            userId
        );

        ClientContact contact =
            contactRepository
                .findByIdAndOrganizationId(
                    contactId,
                    organizationId
                )
                .orElseThrow(
                    this::contactNotFound
                );

        requireClient(
            organizationId,
            contact.getClientId()
        );

        if (
            Boolean.TRUE.equals(
                request.primary()
            )
        ) {
            contactRepository
                .clearPrimaryContact(
                    contact.getClientId(),
                    organizationId
                );

            contactRepository.flush();
        }

        contact.update(
            cleanPreserveNull(
                request.firstName()
            ),
            cleanPreserveNull(
                request.lastName()
            ),
            cleanPreserveNull(
                request.jobTitle()
            ),
            normalizeEmailPreserveNull(
                request.email()
            ),
            cleanPreserveNull(
                request.phone()
            ),
            request.primary(),
            Instant.now()
        );

        return ClientDtos
            .ContactView
            .from(contact);
    }

    @Transactional
    public void deleteContact(
        UUID organizationId,
        UUID userId,
        UUID contactId
    ) {
        requireClientPermission(
            organizationId,
            userId
        );

        ClientContact contact =
            contactRepository
                .findByIdAndOrganizationId(
                    contactId,
                    organizationId
                )
                .orElseThrow(
                    this::contactNotFound
                );

        contactRepository.delete(
            contact
        );
    }

    @Transactional(readOnly = true)
    public Client requireClient(
        UUID organizationId,
        UUID clientId
    ) {
        return clientRepository
            .findByIdAndOrganizationId(
                clientId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "CLIENT_NOT_FOUND",
                        "The client does not exist or is not accessible."
                    )
            );
    }

    private void requireClientPermission(
        UUID organizationId,
        UUID userId
    ) {
        accessService.requireAnyRole(
            organizationId,
            userId,
            CLIENT_ROLES
        );
    }

    private void validateIdentity(
        ClientType type,
        String companyName,
        String firstName,
        String lastName
    ) {
        if (
            type == ClientType.COMPANY
            && isBlank(companyName)
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "COMPANY_NAME_REQUIRED",
                "A company client requires a company name."
            );
        }

        if (
            type == ClientType.INDIVIDUAL
            && (
                isBlank(firstName)
                || isBlank(lastName)
            )
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "CLIENT_NAME_REQUIRED",
                "An individual client requires a first and last name."
            );
        }
    }

    private boolean isBlank(
        String value
    ) {
        return value == null
            || value.isBlank();
    }

    private String cleanRequired(
        String value
    ) {
        return value.trim();
    }

    private String clean(
        String value
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private String cleanPreserveNull(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private String normalizeEmail(
        String value
    ) {
        String cleaned =
            clean(value);

        if (cleaned == null) {
            return null;
        }

        return cleaned.toLowerCase(
            Locale.ROOT
        );
    }

    private String normalizeEmailPreserveNull(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return value
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }

    private BusinessException
        contactNotFound() {

        return new BusinessException(
            HttpStatus.NOT_FOUND,
            "CLIENT_CONTACT_NOT_FOUND",
            "The client contact does not exist."
        );
    }
}
