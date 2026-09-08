package com.buildflow.task.repository;

import com.buildflow.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository
    extends JpaRepository<Task, UUID> {

    Optional<Task>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    Optional<Task>
        findByIdAndOrganizationIdAndProjectId(
            UUID id,
            UUID organizationId,
            UUID projectId
        );

    List<Task>
        findByProjectIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID projectId,
            UUID organizationId
        );

    List<Task>
        findByProjectIdAndOrganizationIdAndParentTaskIdOrderByCreatedAtAsc(
            UUID projectId,
            UUID organizationId,
            UUID parentTaskId
        );

    long countByProjectIdAndOrganizationIdAndParentTaskId(
        UUID projectId,
        UUID organizationId,
        UUID parentTaskId
    );
}
