package com.buildflow.notification.controller;

import com.buildflow.notification.dto.NotificationDtos;
import com.buildflow.notification.service.NotificationService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService
        notificationService;

    public NotificationController(
        NotificationService
            notificationService
    ) {
        this.notificationService =
            notificationService;
    }

    @GetMapping
    public List<NotificationDtos.View> list(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return notificationService.list(
            organizationId,
            principal.userId()
        );
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCount
        unreadCount(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return notificationService
            .unreadCount(
                organizationId,
                principal.userId()
            );
    }

    @PostMapping("/{notificationId}/read")
    public void markRead(
        @RequestHeader(
            "X-Organization-Id"
        )
        UUID organizationId,
        @PathVariable
        UUID notificationId,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        notificationService.markRead(
            organizationId,
            principal.userId(),
            notificationId
        );
    }

    @PostMapping("/read-all")
    public NotificationDtos.UnreadCount
        markAllRead(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        notificationService.markAllRead(
            organizationId,
            principal.userId()
        );

        return notificationService
            .unreadCount(
                organizationId,
                principal.userId()
            );
    }
}
