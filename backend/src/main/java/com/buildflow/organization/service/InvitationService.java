package com.buildflow.organization.service;

import com.buildflow.auth.entity.AppUser;
import com.buildflow.auth.repository.AppUserRepository;
import com.buildflow.auth.service.ApplicationMailService;
import com.buildflow.auth.service.TokenCodec;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.entity.InvitationStatus;
import com.buildflow.organization.entity.OrganizationInvitation;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.repository.OrganizationInvitationRepository;
import com.buildflow.organization.repository.OrganizationMembershipRepository;
import com.buildflow.security.SecurityProperties;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvitationService {

    private final
        OrganizationInvitationRepository
            invitationRepository;

    private final
        OrganizationMembershipRepository
            membershipRepository;

    private final AppUserRepository
        userRepository;

    private final OrganizationAccessService
        accessService;

    private final TokenCodec tokenCodec;
    private final SecurityProperties properties;
    private final ApplicationMailService
        mailService;

    public InvitationService(
        OrganizationInvitationRepository
            invitationRepository,
        OrganizationMembershipRepository
            membershipRepository,
        AppUserRepository userRepository,
        OrganizationAccessService
            accessService,
        TokenCodec tokenCodec,
        SecurityProperties properties,
        ApplicationMailService mailService
    ) {
        this.invitationRepository =
            invitationRepository;
        this.membershipRepository =
            membershipRepository;
        this.userRepository =
            userRepository;
        this.accessService =
            accessService;
        this.tokenCodec = tokenCodec;
        this.properties = properties;
        this.mailService = mailService;
    }

    @Transactional
    public OrganizationDtos.InvitationView
        invite(
            UUID organizationId,
            UUID actorUserId,
            OrganizationDtos
                .InviteMemberRequest request
        ) {

        OrganizationMembership actor =
            accessService
                .requireManagement(
                    organizationId,
                    actorUserId
                );

        if (
            request.role()
                == OrganizationRole.OWNER
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "OWNER_INVITATION_NOT_ALLOWED",
                "The owner role cannot be assigned through an invitation."
            );
        }

        String email =
            normalizeEmail(
                request.email()
            );

        if (
            membershipRepository
                .existsMembershipForEmail(
                    organizationId,
                    email
                )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "MEMBERSHIP_ALREADY_EXISTS",
                "This user already belongs to the organization."
            );
        }

        Instant now = Instant.now();

        invitationRepository
            .findByOrganizationIdAndEmailIgnoreCaseAndStatus(
                organizationId,
                email,
                InvitationStatus.PENDING
            )
            .ifPresent(
                existing -> {
                    if (
                        existing
                            .getExpiresAt()
                            .isAfter(now)
                    ) {
                        throw new BusinessException(
                            HttpStatus.CONFLICT,
                            "INVITATION_ALREADY_PENDING",
                            "A pending invitation already exists for this email address."
                        );
                    }

                    existing.expire(now);
                    invitationRepository.flush();
                }
            );

        String rawToken =
            tokenCodec.generate();

        OrganizationInvitation invitation =
            OrganizationInvitation.create(
                actor.getOrganization(),
                email,
                request.role(),
                tokenCodec.hash(
                    rawToken
                ),
                actor.getUser(),
                now.plus(
                    properties.invitationTtl()
                ),
                now
            );

        invitationRepository.save(
            invitation
        );

        mailService.sendInvitation(
            email,
            actor
                .getOrganization()
                .getName(),
            rawToken
        );

        return OrganizationDtos
            .InvitationView
            .from(invitation);
    }

    @Transactional
    public List<
        OrganizationDtos.InvitationView
    > list(
        UUID organizationId,
        UUID actorUserId
    ) {
        accessService.requireManagement(
            organizationId,
            actorUserId
        );

        Instant now = Instant.now();

        List<OrganizationInvitation>
            invitations =
                invitationRepository
                    .findByOrganizationIdOrderByCreatedAtDesc(
                        organizationId
                    );

        invitations.forEach(
            invitation -> {
                if (
                    invitation.getStatus()
                        == InvitationStatus.PENDING
                        && !invitation
                            .getExpiresAt()
                            .isAfter(now)
                ) {
                    invitation.expire(
                        now
                    );
                }
            }
        );

        return invitations
            .stream()
            .map(
                OrganizationDtos
                    .InvitationView
                    ::from
            )
            .toList();
    }

    @Transactional
    public OrganizationDtos.InvitationView
        resend(
            UUID organizationId,
            UUID invitationId,
            UUID actorUserId
        ) {
        accessService.requireManagement(
            organizationId,
            actorUserId
        );

        OrganizationInvitation invitation =
            invitationRepository
                .findByIdAndOrganizationId(
                    invitationId,
                    organizationId
                )
                .orElseThrow(
                    this::invitationNotFound
                );

        if (
            invitation.getStatus()
                == InvitationStatus.ACCEPTED
                || invitation.getStatus()
                    == InvitationStatus.REVOKED
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVITATION_NOT_RESENDABLE",
                "This invitation cannot be resent."
            );
        }

        Instant now = Instant.now();

        String rawToken =
            tokenCodec.generate();

        invitation.resend(
            tokenCodec.hash(
                rawToken
            ),
            now.plus(
                properties.invitationTtl()
            ),
            now
        );

        mailService.sendInvitation(
            invitation.getEmail(),
            invitation
                .getOrganization()
                .getName(),
            rawToken
        );

        return OrganizationDtos
            .InvitationView
            .from(invitation);
    }

    @Transactional
    public void revoke(
        UUID organizationId,
        UUID invitationId,
        UUID actorUserId
    ) {
        accessService.requireManagement(
            organizationId,
            actorUserId
        );

        OrganizationInvitation invitation =
            invitationRepository
                .findByIdAndOrganizationId(
                    invitationId,
                    organizationId
                )
                .orElseThrow(
                    this::invitationNotFound
                );

        if (
            invitation.getStatus()
                != InvitationStatus.PENDING
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "INVITATION_NOT_PENDING",
                "Only a pending invitation can be revoked."
            );
        }

        invitation.revoke(
            Instant.now()
        );
    }

    @Transactional
    public OrganizationDtos.OrganizationSummary
        accept(
            String rawToken,
            UUID userId
        ) {
        Instant now = Instant.now();

        OrganizationInvitation invitation =
            invitationRepository
                .findByTokenHash(
                    tokenCodec.hash(
                        rawToken
                    )
                )
                .orElseThrow(
                    this::invalidInvitation
                );

        if (
            invitation.getStatus()
                != InvitationStatus.PENDING
        ) {
            throw invalidInvitation();
        }

        if (
            !invitation
                .getExpiresAt()
                .isAfter(now)
        ) {
            invitation.expire(now);

            throw invalidInvitation();
        }

        AppUser user =
            userRepository
                .findById(userId)
                .orElseThrow(
                    this::invalidInvitation
                );

        if (
            !user.getEmail()
                .equalsIgnoreCase(
                    invitation.getEmail()
                )
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "INVITATION_EMAIL_MISMATCH",
                "This invitation belongs to another email address."
            );
        }

        if (
            membershipRepository
                .findByOrganizationIdAndUserId(
                    invitation
                        .getOrganization()
                        .getId(),
                    userId
                )
                .isPresent()
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "MEMBERSHIP_ALREADY_EXISTS",
                "You already belong to this organization."
            );
        }

        OrganizationMembership membership =
            OrganizationMembership.create(
                invitation
                    .getOrganization(),
                user,
                invitation.getRole(),
                now
            );

        membershipRepository.save(
            membership
        );

        invitation.accept(
            user,
            now
        );

        return OrganizationDtos
            .OrganizationSummary
            .from(membership);
    }

    private String normalizeEmail(
        String value
    ) {
        return value
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }

    private BusinessException
        invitationNotFound() {

        return new BusinessException(
            HttpStatus.NOT_FOUND,
            "INVITATION_NOT_FOUND",
            "The invitation does not exist."
        );
    }

    private BusinessException
        invalidInvitation() {

        return new BusinessException(
            HttpStatus.BAD_REQUEST,
            "INVALID_INVITATION",
            "The invitation is invalid or expired."
        );
    }
}
