package com.buildflow.organization.service;

import com.buildflow.organization.entity.MembershipStatus;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.repository.OrganizationMembershipRepository;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class OrganizationAccessService {

    private static final Set<OrganizationRole>
        MANAGEMENT_ROLES =
            Set.of(
                OrganizationRole.OWNER,
                OrganizationRole.ADMIN
            );

    private final
        OrganizationMembershipRepository
            membershipRepository;

    public OrganizationAccessService(
        OrganizationMembershipRepository
            membershipRepository
    ) {
        this.membershipRepository =
            membershipRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireMembership(
        UUID organizationId,
        UUID userId
    ) {
        return membershipRepository
            .findByOrganizationIdAndUserIdAndStatus(
                organizationId,
                userId,
                MembershipStatus.ACTIVE
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "ORGANIZATION_NOT_FOUND",
                        "The organization does not exist or is not accessible."
                    )
            );
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireManagement(
        UUID organizationId,
        UUID userId
    ) {
        return requireAnyRole(
            organizationId,
            userId,
            MANAGEMENT_ROLES
        );
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireAnyRole(
        UUID organizationId,
        UUID userId,
        Set<OrganizationRole> allowedRoles
    ) {
        OrganizationMembership membership =
            requireMembership(
                organizationId,
                userId
            );

        if (
            !allowedRoles.contains(
                membership.getRole()
            )
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "ORGANIZATION_PERMISSION_DENIED",
                "You do not have permission to perform this operation."
            );
        }

        return membership;
    }
}
