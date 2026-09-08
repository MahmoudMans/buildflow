package com.buildflow.task.repository;

import com.buildflow.task.entity.TaskAssignment;
import com.buildflow.task.entity.TaskAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskAssignmentRepository
    extends JpaRepository<
        TaskAssignment,
        TaskAssignmentId
    > {

    List<TaskAssignment>
        findByTaskIdOrderByAssignedAtAsc(
            UUID taskId
        );

    boolean existsByTaskIdAndMembershipId(
        UUID taskId,
        UUID membershipId
    );

    void deleteByTaskIdAndMembershipId(
        UUID taskId,
        UUID membershipId
    );
}
