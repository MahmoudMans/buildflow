package com.buildflow.task.controller;

import com.buildflow.security.BuildFlowPrincipal;
import com.buildflow.task.dto.TaskDtos;
import com.buildflow.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
    "/api/v1/projects/{projectId}/tasks"
)
public class ProjectTaskController {

    private final TaskService
        taskService;

    public ProjectTaskController(
        TaskService taskService
    ) {
        this.taskService =
            taskService;
    }

    @GetMapping
    public List<
        TaskDtos.TaskSummary
    > list(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return taskService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping
    public ResponseEntity<
        TaskDtos.TaskView
    > create(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        TaskDtos.CreateTaskRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                taskService.create(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }
}
