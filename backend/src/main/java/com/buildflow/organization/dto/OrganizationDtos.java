package com.buildflow.organization.dto;

import com.buildflow.organization.entity.InvitationStatus;
import com.buildflow.organization.entity.MembershipStatus;
import com.buildflow.organization.entity.Organization;
import com.buildflow.organization.entity.OrganizationInvitation;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    public record CreateOrganizationRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 250)
        String legalName,

        @Size(max = 100)
        String registrationNumber,

        @Size(max = 100)
        String taxNumber,

        @Email
        @Size(max = 320)
        String email,

        @Size(max = 100)
        String phone,

        @Size(max = 250)
        String addressLine1,

        @Size(max = 250)
        String addressLine2,

        @Size(max = 150)
        String city,

        @Size(max = 50)
        String postalCode,

        @Size(max = 150)
        String country,

        @Size(max = 3)
        String currency,

        @Size(max = 100)
        String timezone,

        @Size(max = 1000)
        String logoUrl

    ) {
    }

    public record UpdateOrganizationRequest(

        @Size(
            min = 1,
            max = 200
        )
        String name,

        @Size(max = 250)
        String legalName,

        @Size(max = 100)
        String registrationNumber,

        @Size(max = 100)
        String taxNumber,

        @Email
        @Size(max = 320)
        String email,

        @Size(max = 100)
        String phone,

        @Size(max = 250)
        String addressLine1,

        @Size(max = 250)
        String addressLine2,

        @Size(max = 150)
        String city,

        @Size(max = 50)
        String postalCode,

        @Size(max = 150)
        String country,

        @Size(max = 3)
        String currency,

        @Size(max = 100)
        String timezone,

        @Size(max = 1000)
        String logoUrl

    ) {
    }

    public record InviteMemberRequest(

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotNull
        OrganizationRole role

    ) {
    }

    public record ChangeRoleRequest(

        @NotNull
        OrganizationRole role

    ) {
    }

    public record AcceptInvitationRequest(

        @NotBlank
        String token

    ) {
    }

    public record OrganizationView(
        UUID id,
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
        Instant createdAt
    ) {
        public static OrganizationView from(
            Organization organization
        ) {
            return new OrganizationView(
                organization.getId(),
                organization.getName(),
                organization.getLegalName(),
                organization
                    .getRegistrationNumber(),
                organization.getTaxNumber(),
                organization.getEmail(),
                organization.getPhone(),
                organization.getAddressLine1(),
                organization.getAddressLine2(),
                organization.getCity(),
                organization.getPostalCode(),
                organization.getCountry(),
                organization.getCurrency(),
                organization.getTimezone(),
                organization.getLogoUrl(),
                organization.getCreatedAt()
            );
        }
    }

    public record OrganizationSummary(
        UUID id,
        String name,
        String currency,
        String timezone,
        OrganizationRole role
    ) {
        public static OrganizationSummary from(
            OrganizationMembership membership
        ) {
            Organization organization =
                membership.getOrganization();

            return new OrganizationSummary(
                organization.getId(),
                organization.getName(),
                organization.getCurrency(),
                organization.getTimezone(),
                membership.getRole()
            );
        }
    }

    public record MemberView(
        UUID membershipId,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        OrganizationRole role,
        MembershipStatus status,
        Instant joinedAt
    ) {
        public static MemberView from(
            OrganizationMembership membership
        ) {
            return new MemberView(
                membership.getId(),
                membership
                    .getUser()
                    .getId(),
                membership
                    .getUser()
                    .getEmail(),
                membership
                    .getUser()
                    .getFirstName(),
                membership
                    .getUser()
                    .getLastName(),
                membership.getRole(),
                membership.getStatus(),
                membership.getJoinedAt()
            );
        }
    }

    public record InvitationView(
        UUID id,
        String email,
        OrganizationRole role,
        InvitationStatus status,
        Instant expiresAt,
        Instant createdAt
    ) {
        public static InvitationView from(
            OrganizationInvitation invitation
        ) {
            return new InvitationView(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
            );
        }
    }

    public record TenantContextView(
        OrganizationView organization,
        OrganizationRole role
    ) {
    }
}
