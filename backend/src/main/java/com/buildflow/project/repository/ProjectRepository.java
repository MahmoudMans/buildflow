package com.buildflow.project.repository;

import com.buildflow.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository
    extends JpaRepository<Project, UUID> {

    Optional<Project>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    List<Project>
        findByOrganizationIdOrderByCreatedAtDesc(
            UUID organizationId
        );

    List<Project>
        findByOrganizationIdAndIdInOrderByCreatedAtDesc(
            UUID organizationId,
            Collection<UUID> ids
        );

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Query("""
        update Project project
        set project.progressPercentage = :progress,
            project.updatedAt = :updatedAt
        where project.id = :projectId
          and project.organizationId = :organizationId
        """)
    int updateCalculatedProgress(
        @Param("projectId")
        UUID projectId,
        @Param("organizationId")
        UUID organizationId,
        @Param("progress")
        BigDecimal progress,
        @Param("updatedAt")
        Instant updatedAt
    );
}
