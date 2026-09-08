package com.buildflow.milestone.repository;

import com.buildflow.milestone.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MilestoneRepository
    extends JpaRepository<
        Milestone,
        UUID
    > {

    List<Milestone>
        findByProjectIdAndOrganizationIdOrderByPlannedDateAscCreatedAtAsc(
            UUID projectId,
            UUID organizationId
        );

    Optional<Milestone>
        findByIdAndProjectIdAndOrganizationId(
            UUID id,
            UUID projectId,
            UUID organizationId
        );
}
