package com.buildflow.task.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class TaskAssignmentId
    implements Serializable {

    private UUID taskId;
    private UUID membershipId;

    public TaskAssignmentId() {
    }

    public TaskAssignmentId(
        UUID taskId,
        UUID membershipId
    ) {
        this.taskId = taskId;
        this.membershipId =
            membershipId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    @Override
    public boolean equals(
        Object object
    ) {
        if (
            this == object
        ) {
            return true;
        }

        if (
            !(object
                instanceof TaskAssignmentId other)
        ) {
            return false;
        }

        return Objects.equals(
            taskId,
            other.taskId
        )
            && Objects.equals(
                membershipId,
                other.membershipId
            );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            taskId,
            membershipId
        );
    }
}
