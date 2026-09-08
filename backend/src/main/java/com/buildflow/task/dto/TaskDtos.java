package com.buildflow.task.dto;

import com.buildflow.task.entity.Task;
import com.buildflow.task.entity.TaskPriority;
import com.buildflow.task.entity.TaskStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class TaskDtos {

    private TaskDtos() {
    }

    public record CreateTaskRequest(

        UUID milestoneId,

        UUID parentTaskId,

        @NotBlank
        @Size(max = 300)
        String title,

        @Size(max = 5000)
        String description,

        TaskPriority priority,

        LocalDate startDate,

        LocalDate dueDate,

        @DecimalMin("0.00")
        BigDecimal estimatedHours,

        @Min(1)
        @Max(10)
        Integer weight

    ) {
    }

    public record UpdateTaskRequest(

        UUID milestoneId,

        Boolean clearMilestone,

        @Size(
            min = 1,
            max = 300
        )
        String title,

        @Size(max = 5000)
        String description,

        TaskPriority priority,

        LocalDate startDate,

        LocalDate dueDate,

        @DecimalMin("0.00")
        BigDecimal estimatedHours,

        @Min(1)
        @Max(10)
        Integer weight

    ) {
    }

    public record UpdateProgressRequest(

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal progressPercentage

    ) {
    }

    public record AddAssignmentRequest(

        @NotNull
        UUID membershipId

    ) {
    }

    public record AddCommentRequest(

        @NotBlank
        @Size(max = 5000)
        String content

    ) {
    }

    public record UpdateCommentRequest(

        @NotBlank
        @Size(max = 5000)
        String content

    ) {
    }

    public record AddWorkLogRequest(

        @NotNull
        LocalDate workDate,

        @Min(1)
        @Max(1440)
        int minutesWorked,

        @Size(max = 5000)
        String description

    ) {
    }

    public record AssignmentView(
        UUID membershipId,
        UUID userId,
        String firstName,
        String lastName,
        String email,
        Instant assignedAt
    ) {
    }

    public record TaskSummary(
        UUID id,
        UUID milestoneId,
        UUID parentTaskId,
        String title,
        TaskStatus status,
        TaskPriority priority,
        LocalDate startDate,
        LocalDate dueDate,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        BigDecimal progressPercentage,
        int weight,
        List<AssignmentView> assignments
    ) {
        public static TaskSummary from(
            Task task,
            BigDecimal actualHours,
            List<AssignmentView> assignments
        ) {
            return new TaskSummary(
                task.getId(),
                task.getMilestoneId(),
                task.getParentTaskId(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getStartDate(),
                task.getDueDate(),
                task.getEstimatedHours(),
                actualHours,
                task.getProgressPercentage(),
                task.getWeight(),
                assignments
            );
        }
    }

    public record TaskView(
        UUID id,
        UUID projectId,
        UUID milestoneId,
        UUID parentTaskId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate startDate,
        LocalDate dueDate,
        BigDecimal estimatedHours,
        BigDecimal actualHours,
        BigDecimal progressPercentage,
        int weight,
        UUID createdBy,
        Instant createdAt,
        Instant completedAt,
        List<AssignmentView> assignments
    ) {
        public static TaskView from(
            Task task,
            BigDecimal actualHours,
            List<AssignmentView> assignments
        ) {
            return new TaskView(
                task.getId(),
                task.getProjectId(),
                task.getMilestoneId(),
                task.getParentTaskId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getStartDate(),
                task.getDueDate(),
                task.getEstimatedHours(),
                actualHours,
                task.getProgressPercentage(),
                task.getWeight(),
                task.getCreatedBy(),
                task.getCreatedAt(),
                task.getCompletedAt(),
                assignments
            );
        }
    }

    public record CommentView(
        UUID id,
        UUID authorMembershipId,
        UUID authorUserId,
        String authorName,
        String content,
        Instant createdAt,
        Instant editedAt,
        boolean edited
    ) {
    }

    public record WorkLogView(
        UUID id,
        UUID membershipId,
        UUID userId,
        String memberName,
        LocalDate workDate,
        int minutesWorked,
        String description,
        Instant createdAt
    ) {
    }
}
