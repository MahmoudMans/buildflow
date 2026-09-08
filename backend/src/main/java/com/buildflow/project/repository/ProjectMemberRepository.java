package com.buildflow.project.repository;

import com.buildflow.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository
    extends JpaRepository<
        ProjectMember,
        UUID
    > {

    List<ProjectMember>
        findByProjectIdAndOrganizationIdAndRemovedAtIsNullOrderByJoinedAtAsc(
            UUID projectId,
            UUID organizationId
        );

    Optional<ProjectMember>
        findByIdAndProjectIdAndOrganizationId(
            UUID id,
            UUID projectId,
            UUID organizationId
        );

    Optional<ProjectMember>
        findByProjectIdAndOrganizationIdAndMembershipIdAndRemovedAtIsNull(
            UUID projectId,
            UUID organizationId,
            UUID membershipId
        );

    boolean
        existsByProjectIdAndOrganizationIdAndMembershipIdAndRemovedAtIsNull(
            UUID projectId,
            UUID organizationId,
            UUID membershipId
        );

    List<ProjectMember>
        findByOrganizationIdAndMembershipIdAndRemovedAtIsNull(
            UUID organizationId,
            UUID membershipId
        );
}
