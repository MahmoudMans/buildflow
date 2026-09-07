package com.buildflow.organization.service;

import com.buildflow.auth.entity.AppUser;
import com.buildflow.auth.repository.AppUserRepository;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.entity.MembershipStatus;
import com.buildflow.organization.entity.Organization;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.repository.OrganizationMembershipRepository;
import com.buildflow.organization.repository.OrganizationRepository;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class OrganizationService {

    private static final Set<
        String
    > SUPPORTED_CURRENCIES =
        Set.of(
            "EUR",
            "USD",
            "GBP",
            "TND",
            "MAD",
            "AED"
        );

    private final OrganizationRepository
        organizationRepository;

    private final
        OrganizationMembershipRepository
            membershipRepository;

    private final AppUserRepository
        userRepository;

    private final OrganizationAccessService
        accessService;

    public OrganizationService(
        OrganizationRepository
            organizationRepository,
        OrganizationMembershipRepository
            membershipRepository,
        AppUserRepository userRepository,
        OrganizationAccessService
            accessService
    ) {
        this.organizationRepository =
            organizationRepository;
        this.membershipRepository =
            membershipRepository;
        this.userRepository =
            userRepository;
        this.accessService =
            accessService;
    }

    @Transactional
    public OrganizationDtos.OrganizationView
        create(
            UUID userId,
            OrganizationDtos
                .CreateOrganizationRequest
                request
        ) {

        AppUser user =
            userRepository
                .findById(userId)
                .orElseThrow(
                    () ->
                        new BusinessException(
                            HttpStatus.UNAUTHORIZED,
                            "USER_NOT_AVAILABLE",
                            "The user account is unavailable."
                        )
                );

        String currency =
            normalizeCurrency(
                request.currency()
            );

        String timezone =
            normalizeTimezone(
                request.timezone()
            );

        Instant now = Instant.now();

        Organization organization =
            Organization.create(
                cleanRequired(
                    request.name()
                ),
                cleanOptional(
                    request.legalName()
                ),
                cleanOptional(
                    request.registrationNumber()
                ),
                cleanOptional(
                    request.taxNumber()
                ),
                normalizeOptionalEmail(
                    request.email()
                ),
                cleanOptional(
                    request.phone()
                ),
                cleanOptional(
                    request.addressLine1()
                ),
                cleanOptional(
                    request.addressLine2()
                ),
                cleanOptional(
                    request.city()
                ),
                cleanOptional(
                    request.postalCode()
                ),
                cleanOptional(
                    request.country()
                ),
                currency,
                timezone,
                cleanOptional(
                    request.logoUrl()
                ),
                now
            );

        organizationRepository.save(
            organization
        );

        OrganizationMembership owner =
            OrganizationMembership.create(
                organization,
                user,
                OrganizationRole.OWNER,
                now
            );

        membershipRepository.save(
            owner
        );

        return OrganizationDtos
            .OrganizationView
            .from(organization);
    }

    @Transactional(readOnly = true)
    public List<
        OrganizationDtos.OrganizationSummary
    > listForUser(
        UUID userId
    ) {
        return membershipRepository
            .findByUserIdAndStatusOrderByJoinedAtAsc(
                userId,
                MembershipStatus.ACTIVE
            )
            .stream()
            .map(
                OrganizationDtos
                    .OrganizationSummary
                    ::from
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationDtos.OrganizationView
        get(
            UUID organizationId,
            UUID userId
        ) {

        OrganizationMembership membership =
            accessService
                .requireMembership(
                    organizationId,
                    userId
                );

        return OrganizationDtos
            .OrganizationView
            .from(
                membership.getOrganization()
            );
    }

    @Transactional
    public OrganizationDtos.OrganizationView
        update(
            UUID organizationId,
            UUID userId,
            OrganizationDtos
                .UpdateOrganizationRequest
                request
        ) {

        OrganizationMembership membership =
            accessService
                .requireManagement(
                    organizationId,
                    userId
                );

        Organization organization =
            membership.getOrganization();

        String currency = null;

        if (
            request.currency()
                != null
        ) {
            currency =
                normalizeCurrency(
                    request.currency()
                );
        }

        String timezone = null;

        if (
            request.timezone()
                != null
        ) {
            timezone =
                normalizeTimezone(
                    request.timezone()
                );
        }

        organization.update(
            cleanOptionalPreserveNull(
                request.name()
            ),
            cleanOptionalPreserveNull(
                request.legalName()
            ),
            cleanOptionalPreserveNull(
                request.registrationNumber()
            ),
            cleanOptionalPreserveNull(
                request.taxNumber()
            ),
            normalizeOptionalEmailPreserveNull(
                request.email()
            ),
            cleanOptionalPreserveNull(
                request.phone()
            ),
            cleanOptionalPreserveNull(
                request.addressLine1()
            ),
            cleanOptionalPreserveNull(
                request.addressLine2()
            ),
            cleanOptionalPreserveNull(
                request.city()
            ),
            cleanOptionalPreserveNull(
                request.postalCode()
            ),
            cleanOptionalPreserveNull(
                request.country()
            ),
            currency,
            timezone,
            cleanOptionalPreserveNull(
                request.logoUrl()
            ),
            Instant.now()
        );

        return OrganizationDtos
            .OrganizationView
            .from(organization);
    }

    @Transactional(readOnly = true)
    public List<
        OrganizationDtos.MemberView
    > listMembers(
        UUID organizationId,
        UUID userId
    ) {
        accessService.requireMembership(
            organizationId,
            userId
        );

        return membershipRepository
            .findByOrganizationIdOrderByJoinedAtAsc(
                organizationId
            )
            .stream()
            .map(
                OrganizationDtos
                    .MemberView
                    ::from
            )
            .toList();
    }

    @Transactional
    public OrganizationDtos.MemberView
        changeRole(
            UUID organizationId,
            UUID membershipId,
            UUID actorUserId,
            OrganizationRole newRole
        ) {

        accessService.requireManagement(
            organizationId,
            actorUserId
        );

        if (
            newRole
                == OrganizationRole.OWNER
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "OWNER_ROLE_REQUIRES_TRANSFER",
                "Ownership must be changed through an ownership-transfer workflow."
            );
        }

        OrganizationMembership membership =
            membershipRepository
                .findByIdAndOrganizationId(
                    membershipId,
                    organizationId
                )
                .orElseThrow(
                    this::memberNotFound
                );

        if (
            membership.getRole()
                == OrganizationRole.OWNER
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "OWNER_ROLE_IMMUTABLE",
                "The owner role cannot be modified through member management."
            );
        }

        membership.changeRole(
            newRole
        );

        return OrganizationDtos
            .MemberView
            .from(membership);
    }

    @Transactional
    public OrganizationDtos.MemberView
        deactivateMember(
            UUID organizationId,
            UUID membershipId,
            UUID actorUserId
        ) {

        accessService.requireManagement(
            organizationId,
            actorUserId
        );

        OrganizationMembership membership =
            membershipRepository
                .findByIdAndOrganizationId(
                    membershipId,
                    organizationId
                )
                .orElseThrow(
                    this::memberNotFound
                );

        if (
            membership.getRole()
                == OrganizationRole.OWNER
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "OWNER_CANNOT_BE_DEACTIVATED",
                "The organization owner cannot be deactivated."
            );
        }

        if (
            membership
                .getUser()
                .getId()
                .equals(actorUserId)
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "SELF_DEACTIVATION_NOT_ALLOWED",
                "You cannot deactivate your own membership from this operation."
            );
        }

        membership.deactivate(
            Instant.now()
        );

        return OrganizationDtos
            .MemberView
            .from(membership);
    }

    @Transactional
    public OrganizationDtos.MemberView
        activateMember(
            UUID organizationId,
            UUID membershipId,
            UUID actorUserId
        ) {

        accessService.requireManagement(
            organizationId,
            actorUserId
        );

        OrganizationMembership membership =
            membershipRepository
                .findByIdAndOrganizationId(
                    membershipId,
                    organizationId
                )
                .orElseThrow(
                    this::memberNotFound
                );

        membership.activate();

        return OrganizationDtos
            .MemberView
            .from(membership);
    }

    private String normalizeCurrency(
        String value
    ) {
        String currency =
            value == null
                || value.isBlank()
                ? "EUR"
                : value
                    .trim()
                    .toUpperCase(
                        Locale.ROOT
                    );

        if (
            !SUPPORTED_CURRENCIES
                .contains(currency)
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "UNSUPPORTED_CURRENCY",
                "The selected currency is not supported."
            );
        }

        try {
            Currency.getInstance(currency);
        } catch (
            IllegalArgumentException exception
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_CURRENCY",
                "The selected currency is invalid."
            );
        }

        return currency;
    }

    private String normalizeTimezone(
        String value
    ) {
        String timezone =
            value == null
                || value.isBlank()
                ? "UTC"
                : value.trim();

        try {
            ZoneId.of(timezone);
        } catch (
            ZoneRulesException exception
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVALID_TIMEZONE",
                "The selected timezone is invalid."
            );
        }

        return timezone;
    }

    private BusinessException
        memberNotFound() {

        return new BusinessException(
            HttpStatus.NOT_FOUND,
            "MEMBER_NOT_FOUND",
            "The organization member does not exist."
        );
    }

    private String cleanRequired(
        String value
    ) {
        return value.trim();
    }

    private String cleanOptional(
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

    private String cleanOptionalPreserveNull(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private String normalizeOptionalEmail(
        String value
    ) {
        String cleaned =
            cleanOptional(value);

        if (cleaned == null) {
            return null;
        }

        return cleaned.toLowerCase(
            Locale.ROOT
        );
    }

    private String normalizeOptionalEmailPreserveNull(
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
}
