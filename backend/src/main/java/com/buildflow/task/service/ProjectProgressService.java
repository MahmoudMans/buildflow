package com.buildflow.task.service;

import com.buildflow.project.repository.ProjectRepository;
import com.buildflow.task.entity.Task;
import com.buildflow.task.entity.TaskStatus;
import com.buildflow.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectProgressService {

    private final TaskRepository
        taskRepository;

    private final ProjectRepository
        projectRepository;

    public ProjectProgressService(
        TaskRepository taskRepository,
        ProjectRepository projectRepository
    ) {
        this.taskRepository =
            taskRepository;

        this.projectRepository =
            projectRepository;
    }

    @Transactional
    public BigDecimal recalculate(
        UUID organizationId,
        UUID projectId
    ) {
        List<Task> tasks =
            taskRepository
                .findByProjectIdAndOrganizationIdOrderByCreatedAtAsc(
                    projectId,
                    organizationId
                )
                .stream()
                .filter(
                    task ->
                        task.getStatus()
                            != TaskStatus.CANCELLED
                )
                .toList();

        BigDecimal progress;

        if (tasks.isEmpty()) {
            progress =
                BigDecimal.ZERO
                    .setScale(2);
        } else {
            BigDecimal weightedTotal =
                BigDecimal.ZERO;

            BigDecimal totalWeight =
                BigDecimal.ZERO;

            for (Task task : tasks) {
                BigDecimal weight =
                    BigDecimal.valueOf(
                        task.getWeight()
                    );

                weightedTotal =
                    weightedTotal.add(
                        task
                            .getProgressPercentage()
                            .multiply(weight)
                    );

                totalWeight =
                    totalWeight.add(
                        weight
                    );
            }

            progress =
                weightedTotal.divide(
                    totalWeight,
                    2,
                    RoundingMode.HALF_UP
                );
        }

        projectRepository
            .updateCalculatedProgress(
                projectId,
                organizationId,
                progress,
                Instant.now()
            );

        return progress;
    }
}
