package com.buildflow.task.controller;

import com.buildflow.security.BuildFlowPrincipal;
import com.buildflow.task.dto.TaskDtos;
import com.buildflow.task.entity.TaskStatus;
import com.buildflow.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService
        taskService;

    public TaskController(
        TaskService taskService
    ) {
        this.taskService =
            taskService;
    }

    @GetMapping("/{taskId}")
    public TaskDtos.TaskView get(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return taskService.get(
            organizationId,
            principal.userId(),
            taskId
        );
    }

    @PatchMapping("/{taskId}")
    public TaskDtos.TaskView update(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        TaskDtos.UpdateTaskRequest
            request
    ) {
        return taskService.update(
            organizationId,
            principal.userId(),
            taskId,
            request
        );
    }

    @PatchMapping("/{taskId}/progress")
    public TaskDtos.TaskView progress(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        TaskDtos.UpdateProgressRequest
            request
    ) {
        return taskService
            .updateProgress(
                organizationId,
                principal.userId(),
                taskId,
                request
            );
    }

    @PostMapping("/{taskId}/start")
    public TaskDtos.TaskView start(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            principal,
            taskId,
            TaskStatus.IN_PROGRESS
        );
    }

    @PostMapping("/{taskId}/block")
    public TaskDtos.TaskView block(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            principal,
            taskId,
            TaskStatus.BLOCKED
        );
    }

    @PostMapping("/{taskId}/review")
    public TaskDtos.TaskView review(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            principal,
            taskId,
            TaskStatus.REVIEW
        );
    }

    @PostMapping("/{taskId}/complete")
    public TaskDtos.TaskView complete(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            principal,
            taskId,
            TaskStatus.DONE
        );
    }

    @PostMapping("/{taskId}/cancel")
    public TaskDtos.TaskView cancel(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            principal,
            taskId,
            TaskStatus.CANCELLED
        );
    }

    @PostMapping("/{taskId}/reopen")
    public TaskDtos.TaskView reopen(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        TaskDtos.TaskView current =
            taskService.get(
                organizationId,
                principal.userId(),
                taskId
            );

        TaskStatus target =
            current.status()
                == TaskStatus.CANCELLED
                ? TaskStatus.TODO
                : TaskStatus.IN_PROGRESS;

        return transition(
            organizationId,
            principal,
            taskId,
            target
        );
    }

    @PostMapping("/{taskId}/assignments")
    public TaskDtos.TaskView addAssignment(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        TaskDtos.AddAssignmentRequest
            request
    ) {
        return taskService
            .addAssignment(
                organizationId,
                principal.userId(),
                taskId,
                request
            );
    }

    @DeleteMapping(
        "/{taskId}/assignments/{membershipId}"
    )
    public TaskDtos.TaskView removeAssignment(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @PathVariable
        UUID membershipId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return taskService
            .removeAssignment(
                organizationId,
                principal.userId(),
                taskId,
                membershipId
            );
    }

    @GetMapping("/{taskId}/comments")
    public List<
        TaskDtos.CommentView
    > comments(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return taskService.comments(
            organizationId,
            principal.userId(),
            taskId
        );
    }

    @PostMapping("/{taskId}/comments")
    public TaskDtos.CommentView addComment(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        TaskDtos.AddCommentRequest
            request
    ) {
        return taskService.addComment(
            organizationId,
            principal.userId(),
            taskId,
            request
        );
    }

    @PatchMapping(
        "/{taskId}/comments/{commentId}"
    )
    public TaskDtos.CommentView
        updateComment(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID taskId,
            @PathVariable
            UUID commentId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            TaskDtos.UpdateCommentRequest
                request
        ) {
        return taskService
            .updateComment(
                organizationId,
                principal.userId(),
                taskId,
                commentId,
                request
            );
    }

    @GetMapping("/{taskId}/work-logs")
    public List<
        TaskDtos.WorkLogView
    > workLogs(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return taskService.workLogs(
            organizationId,
            principal.userId(),
            taskId
        );
    }

    @PostMapping("/{taskId}/work-logs")
    public TaskDtos.WorkLogView addWorkLog(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID taskId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        TaskDtos.AddWorkLogRequest
            request
    ) {
        return taskService.addWorkLog(
            organizationId,
            principal.userId(),
            taskId,
            request
        );
    }

    private TaskDtos.TaskView transition(
        UUID organizationId,
        BuildFlowPrincipal principal,
        UUID taskId,
        TaskStatus target
    ) {
        return taskService.transition(
            organizationId,
            principal.userId(),
            taskId,
            target
        );
    }
}
