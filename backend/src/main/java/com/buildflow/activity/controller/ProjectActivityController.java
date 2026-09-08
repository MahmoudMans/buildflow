package com.buildflow.activity.controller;

import com.buildflow.activity.dto.ActivityDtos;
import com.buildflow.activity.service.ProjectActivityService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
    "/api/v1/projects/{projectId}/activity"
)
public class ProjectActivityController {

    private final ProjectActivityService
        activityService;

    public ProjectActivityController(
        ProjectActivityService
            activityService
    ) {
        this.activityService =
            activityService;
    }

    @GetMapping
    public List<ActivityDtos.View> list(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID projectId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return activityService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }
}
