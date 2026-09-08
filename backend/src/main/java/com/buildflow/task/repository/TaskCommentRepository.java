package com.buildflow.task.repository;

import com.buildflow.task.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskCommentRepository
    extends JpaRepository<
        TaskComment,
        UUID
    > {

    List<TaskComment>
        findByTaskIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID taskId,
            UUID organizationId
        );

    Optional<TaskComment>
        findByIdAndTaskIdAndOrganizationId(
            UUID id,
            UUID taskId,
            UUID organizationId
        );
}
