package com.buildflow.milestone.dto;

import com.buildflow.milestone.entity.Milestone;
import com.buildflow.milestone.entity.MilestoneStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class MilestoneDtos {

    private MilestoneDtos() {
    }

    public record CreateMilestoneRequest(

        @NotBlank
        @Size(max = 250)
        String name,

        @Size(max = 5000)
        String description,

        LocalDate plannedDate,

        boolean clientVisible

    ) {
    }

    public record UpdateMilestoneRequest(

        @Size(
            min = 1,
            max = 250
        )
        String name,

        @Size(max = 5000)
        String description,

        LocalDate plannedDate,

        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal progressPercentage,

        Boolean clientVisible

    ) {
    }

    public record MilestoneView(
        UUID id,
        UUID projectId,
        String name,
        String description,
        LocalDate plannedDate,
        LocalDate completedDate,
        MilestoneStatus status,
        BigDecimal progressPercentage,
        boolean clientVisible,
        Instant createdAt
    ) {
        public static MilestoneView from(
            Milestone milestone
        ) {
            return new MilestoneView(
                milestone.getId(),
                milestone.getProjectId(),
                milestone.getName(),
                milestone.getDescription(),
                milestone.getPlannedDate(),
                milestone.getCompletedDate(),
                milestone.getStatus(),
                milestone.getProgressPercentage(),
                milestone.isClientVisible(),
                milestone.getCreatedAt()
            );
        }
    }
}
