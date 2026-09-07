package com.buildflow.organization.repository;

import com.buildflow.organization.entity.MembershipStatus;
import com.buildflow.organization.entity.OrganizationMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository
    extends JpaRepository<
        OrganizationMembership,
        UUID
    > {

    Optional<OrganizationMembership>
        findByOrganizationIdAndUserId(
            UUID organizationId,
            UUID userId
        );

    Optional<OrganizationMembership>
        findByOrganizationIdAndUserIdAndStatus(
            UUID organizationId,
            UUID userId,
            MembershipStatus status
        );

    Optional<OrganizationMembership>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    List<OrganizationMembership>
        findByUserIdAndStatusOrderByJoinedAtAsc(
            UUID userId,
            MembershipStatus status
        );

    List<OrganizationMembership>
        findByOrganizationIdOrderByJoinedAtAsc(
            UUID organizationId
        );

    @Query("""
        select case when count(m) > 0
                    then true
                    else false
               end
        from OrganizationMembership m
        where m.organization.id = :organizationId
          and lower(m.user.email) = lower(:email)
        """)
    boolean existsMembershipForEmail(
        @Param("organizationId")
        UUID organizationId,
        @Param("email")
        String email
    );
}
