package com.buildflow.project.controller;

import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectStatus;
import com.buildflow.project.service.ProjectService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService
        projectService;

    public ProjectController(
        ProjectService projectService
    ) {
        this.projectService =
            projectService;
    }

    @GetMapping
    public List<
        ProjectDtos.ProjectSummary
    > list(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return projectService.list(
            organizationId,
            principal.userId()
        );
    }

    @PostMapping
    public ResponseEntity<
        ProjectDtos.ProjectView
    > create(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal,
        @Valid
        @RequestBody
        ProjectDtos.CreateProjectRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                projectService.create(
                    organizationId,
                    principal.userId(),
                    request
                )
            );
    }

    @GetMapping("/{projectId}")
    public ProjectDtos.ProjectView get(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return projectService.get(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PatchMapping("/{projectId}")
    public ProjectDtos.ProjectView update(
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
        ProjectDtos.UpdateProjectRequest
            request
    ) {
        return projectService.update(
            organizationId,
            principal.userId(),
            projectId,
            request
        );
    }

    @PostMapping("/{projectId}/plan")
    public ProjectDtos.ProjectView plan(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            projectId,
            principal,
            ProjectStatus.PLANNING
        );
    }

    @PostMapping("/{projectId}/activate")
    public ProjectDtos.ProjectView activate(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            projectId,
            principal,
            ProjectStatus.ACTIVE
        );
    }

    @PostMapping("/{projectId}/hold")
    public ProjectDtos.ProjectView hold(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            projectId,
            principal,
            ProjectStatus.ON_HOLD
        );
    }

    @PostMapping("/{projectId}/resume")
    public ProjectDtos.ProjectView resume(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            projectId,
            principal,
            ProjectStatus.ACTIVE
        );
    }

    @PostMapping("/{projectId}/complete")
    public ProjectDtos.ProjectView complete(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            projectId,
            principal,
            ProjectStatus.COMPLETED
        );
    }

    @PostMapping("/{projectId}/cancel")
    public ProjectDtos.ProjectView cancel(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            projectId,
            principal,
            ProjectStatus.CANCELLED
        );
    }

    @PostMapping("/{projectId}/archive")
    public ProjectDtos.ProjectView archive(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return transition(
            organizationId,
            projectId,
            principal,
            ProjectStatus.ARCHIVED
        );
    }

    @GetMapping("/{projectId}/members")
    public List<
        ProjectDtos.ProjectMemberView
    > members(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return projectService.listMembers(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<
        ProjectDtos.ProjectMemberView
    > addMember(
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
        ProjectDtos.AddProjectMemberRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                projectService.addMember(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }

    @PatchMapping(
        "/{projectId}/members/{projectMemberId}"
    )
    public ProjectDtos.ProjectMemberView
        changeMemberRole(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID projectMemberId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            ProjectDtos
                .ChangeProjectRoleRequest
                request
        ) {

        return projectService
            .changeMemberRole(
                organizationId,
                principal.userId(),
                projectId,
                projectMemberId,
                request
            );
    }

    @DeleteMapping(
        "/{projectId}/members/{projectMemberId}"
    )
    public ResponseEntity<Void>
        removeMember(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID projectMemberId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        projectService.removeMember(
            organizationId,
            principal.userId(),
            projectId,
            projectMemberId
        );

        return ResponseEntity
            .noContent()
            .build();
    }

    private ProjectDtos.ProjectView
        transition(
            UUID organizationId,
            UUID projectId,
            BuildFlowPrincipal principal,
            ProjectStatus target
        ) {

        return projectService
            .transition(
                organizationId,
                principal.userId(),
                projectId,
                target
            );
    }
}
