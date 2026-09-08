package com.buildflow.task.repository;

import com.buildflow.task.entity.TaskWorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskWorkLogRepository
    extends JpaRepository<
        TaskWorkLog,
        UUID
    > {

    List<TaskWorkLog>
        findByTaskIdAndOrganizationIdOrderByWorkDateDescCreatedAtDesc(
            UUID taskId,
            UUID organizationId
        );

    @Query("""
        select coalesce(
            sum(log.minutesWorked),
            0
        )
        from TaskWorkLog log
        where log.taskId = :taskId
          and log.organizationId = :organizationId
        """)
    Long totalMinutesForTask(
        @Param("taskId")
        UUID taskId,
        @Param("organizationId")
        UUID organizationId
    );
}
