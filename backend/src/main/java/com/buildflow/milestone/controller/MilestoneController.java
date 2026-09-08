package com.buildflow.milestone.controller;

import com.buildflow.milestone.dto.MilestoneDtos;
import com.buildflow.milestone.entity.MilestoneStatus;
import com.buildflow.milestone.service.MilestoneService;
import com.buildflow.security.BuildFlowPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping(
    "/api/v1/projects/{projectId}/milestones"
)
public class MilestoneController {

    private final MilestoneService
        milestoneService;

    public MilestoneController(
        MilestoneService milestoneService
    ) {
        this.milestoneService =
            milestoneService;
    }

    @GetMapping
    public List<
        MilestoneDtos.MilestoneView
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
        return milestoneService.list(
            organizationId,
            principal.userId(),
            projectId
        );
    }

    @PostMapping
    public ResponseEntity<
        MilestoneDtos.MilestoneView
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
        MilestoneDtos
            .CreateMilestoneRequest
            request
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                milestoneService.create(
                    organizationId,
                    principal.userId(),
                    projectId,
                    request
                )
            );
    }

    @PatchMapping("/{milestoneId}")
    public MilestoneDtos.MilestoneView
        update(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID milestoneId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            MilestoneDtos
                .UpdateMilestoneRequest
                request
        ) {
        return milestoneService.update(
            organizationId,
            principal.userId(),
            projectId,
            milestoneId,
            request
        );
    }

    @PostMapping(
        "/{milestoneId}/start"
    )
    public MilestoneDtos.MilestoneView
        start(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID milestoneId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {
        return transition(
            organizationId,
            principal,
            projectId,
            milestoneId,
            MilestoneStatus.IN_PROGRESS
        );
    }

    @PostMapping(
        "/{milestoneId}/complete"
    )
    public MilestoneDtos.MilestoneView
        complete(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID milestoneId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {
        return transition(
            organizationId,
            principal,
            projectId,
            milestoneId,
            MilestoneStatus.COMPLETED
        );
    }

    @PostMapping(
        "/{milestoneId}/miss"
    )
    public MilestoneDtos.MilestoneView
        miss(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID milestoneId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {
        return transition(
            organizationId,
            principal,
            projectId,
            milestoneId,
            MilestoneStatus.MISSED
        );
    }

    @PostMapping(
        "/{milestoneId}/cancel"
    )
    public MilestoneDtos.MilestoneView
        cancel(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID projectId,
            @PathVariable
            UUID milestoneId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {
        return transition(
            organizationId,
            principal,
            projectId,
            milestoneId,
            MilestoneStatus.CANCELLED
        );
    }

    private MilestoneDtos.MilestoneView
        transition(
            UUID organizationId,
            BuildFlowPrincipal principal,
            UUID projectId,
            UUID milestoneId,
            MilestoneStatus status
        ) {
        return milestoneService.transition(
            organizationId,
            principal.userId(),
            projectId,
            milestoneId,
            status
        );
    }
}
