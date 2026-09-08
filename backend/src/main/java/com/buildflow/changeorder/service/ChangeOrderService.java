package com.buildflow.changeorder.service;

import com.buildflow.changeorder.dto.ChangeOrderDtos;
import com.buildflow.changeorder.entity.ChangeOrder;
import com.buildflow.changeorder.entity.ChangeOrderStatus;
import com.buildflow.changeorder.repository.ChangeOrderRepository;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class ChangeOrderService {

    private final ChangeOrderRepository
        changeOrderRepository;

    private final ChangeOrderAccessService
        accessService;

    private final ChangeOrderNumberService
        numberService;

    public ChangeOrderService(
        ChangeOrderRepository
            changeOrderRepository,
        ChangeOrderAccessService
            accessService,
        ChangeOrderNumberService
            numberService
    ) {
        this.changeOrderRepository =
            changeOrderRepository;

        this.accessService =
            accessService;

        this.numberService =
            numberService;
    }

    @Transactional(readOnly = true)
    public List<ChangeOrderDtos.Summary> list(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        OrganizationMembership membership =
            accessService.requireView(
                organizationId,
                projectId,
                userId
            );

        boolean clientView =
            membership.getRole()
                == OrganizationRole.CLIENT;

        return changeOrderRepository
            .findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
                projectId,
                organizationId
            )
            .stream()
            .filter(
                changeOrder ->
                    !clientView
                    || clientVisibleStatus(
                        changeOrder.getStatus()
                    )
            )
            .map(
                changeOrder ->
                    toSummary(
                        changeOrder,
                        clientView
                    )
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public ChangeOrderDtos.View get(
        UUID organizationId,
        UUID userId,
        UUID changeOrderId
    ) {
        ChangeOrder changeOrder =
            requireChangeOrder(
                organizationId,
                changeOrderId
            );

        OrganizationMembership membership =
            accessService.requireView(
                organizationId,
                changeOrder.getProjectId(),
                userId
            );

        boolean clientView =
            membership.getRole()
                == OrganizationRole.CLIENT;

        if (
            clientView
            && !clientVisibleStatus(
                changeOrder.getStatus()
            )
        ) {
            throw new BusinessException(
                HttpStatus.NOT_FOUND,
                "CHANGE_ORDER_NOT_FOUND",
                "The change order does not exist or is not accessible."
            );
        }

        return toView(
            changeOrder,
            clientView
        );
    }

    @Transactional
    public ChangeOrderDtos.View create(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        ChangeOrderDtos.CreateRequest request
    ) {
        OrganizationMembership actor =
            accessService.requireManage(
                organizationId,
                projectId,
                userId
            );

        LocalDate today =
            LocalDate.now(
                ZoneId.of(
                    actor
                        .getOrganization()
                        .getTimezone()
                )
            );

        ChangeOrder changeOrder =
            ChangeOrder.create(
                organizationId,
                projectId,
                numberService.next(
                    organizationId,
                    today.getYear()
                ),
                request.title(),
                request.description(),
                request.reason(),
                request.costImpact(),
                request.clientPriceImpact(),
                request.scheduleImpactDays(),
                userId,
                Instant.now()
            );

        changeOrder =
            changeOrderRepository.save(
                changeOrder
            );

        return toView(
            changeOrder,
            false
        );
    }

    @Transactional
    public ChangeOrderDtos.View update(
        UUID organizationId,
        UUID userId,
        UUID changeOrderId,
        ChangeOrderDtos.UpdateRequest request
    ) {
        ChangeOrder changeOrder =
            requireChangeOrder(
                organizationId,
                changeOrderId
            );

        accessService.requireManage(
            organizationId,
            changeOrder.getProjectId(),
            userId
        );

        changeOrder.updateDraft(
            request.title(),
            request.description(),
            request.reason(),
            request.costImpact(),
            request.clientPriceImpact(),
            request.scheduleImpactDays(),
            Instant.now()
        );

        return toView(
            changeOrder,
            false
        );
    }

    @Transactional
    public ChangeOrderDtos.View submit(
        UUID organizationId,
        UUID userId,
        UUID changeOrderId
    ) {
        ChangeOrder changeOrder =
            requireChangeOrder(
                organizationId,
                changeOrderId
            );

        accessService.requireManage(
            organizationId,
            changeOrder.getProjectId(),
            userId
        );

        changeOrder.submit(
            Instant.now()
        );

        return toView(
            changeOrder,
            false
        );
    }

    @Transactional
    public ChangeOrderDtos.View
        sendToClientReview(
            UUID organizationId,
            UUID userId,
            UUID changeOrderId
        ) {

        ChangeOrder changeOrder =
            requireChangeOrder(
                organizationId,
                changeOrderId
            );

        accessService.requireManage(
            organizationId,
            changeOrder.getProjectId(),
            userId
        );

        changeOrder.sendToClientReview(
            Instant.now()
        );

        return toView(
            changeOrder,
            false
        );
    }

    @Transactional
    public ChangeOrderDtos.View approve(
        UUID organizationId,
        UUID userId,
        UUID changeOrderId
    ) {
        ChangeOrder changeOrder =
            requireChangeOrder(
                organizationId,
                changeOrderId
            );

        OrganizationMembership actor =
            accessService.requireDecision(
                organizationId,
                changeOrder.getProjectId(),
                userId
            );

        changeOrder.approve(
            userId,
            Instant.now()
        );

        boolean clientView =
            actor.getRole()
                == OrganizationRole.CLIENT;

        return toView(
            changeOrder,
            clientView
        );
    }

    @Transactional
    public ChangeOrderDtos.View reject(
        UUID organizationId,
        UUID userId,
        UUID changeOrderId
    ) {
        ChangeOrder changeOrder =
            requireChangeOrder(
                organizationId,
                changeOrderId
            );

        OrganizationMembership actor =
            accessService.requireDecision(
                organizationId,
                changeOrder.getProjectId(),
                userId
            );

        changeOrder.reject(
            userId,
            Instant.now()
        );

        boolean clientView =
            actor.getRole()
                == OrganizationRole.CLIENT;

        return toView(
            changeOrder,
            clientView
        );
    }

    @Transactional
    public ChangeOrderDtos.View cancel(
        UUID organizationId,
        UUID userId,
        UUID changeOrderId,
        ChangeOrderDtos.CancelRequest request
    ) {
        ChangeOrder changeOrder =
            requireChangeOrder(
                organizationId,
                changeOrderId
            );

        accessService.requireManage(
            organizationId,
            changeOrder.getProjectId(),
            userId
        );

        changeOrder.cancel(
            userId,
            request.reason(),
            Instant.now()
        );

        return toView(
            changeOrder,
            false
        );
    }

    @Transactional(readOnly = true)
    public ChangeOrder requireChangeOrder(
        UUID organizationId,
        UUID changeOrderId
    ) {
        return changeOrderRepository
            .findByIdAndOrganizationId(
                changeOrderId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "CHANGE_ORDER_NOT_FOUND",
                        "The change order does not exist or is not accessible."
                    )
            );
    }

    private boolean clientVisibleStatus(
        ChangeOrderStatus status
    ) {
        return status
            == ChangeOrderStatus.CLIENT_REVIEW
            || status
                == ChangeOrderStatus.APPROVED
            || status
                == ChangeOrderStatus.REJECTED;
    }

    private ChangeOrderDtos.Summary
        toSummary(
            ChangeOrder changeOrder,
            boolean clientView
        ) {

        return new ChangeOrderDtos
            .Summary(
                changeOrder.getId(),
                changeOrder
                    .getChangeOrderNumber(),
                changeOrder.getTitle(),
                changeOrder.getStatus(),
                clientView
                    ? null
                    : changeOrder
                        .getCostImpact(),
                changeOrder
                    .getClientPriceImpact(),
                changeOrder
                    .getScheduleImpactDays(),
                changeOrder
                    .getCreatedAt()
            );
    }

    private ChangeOrderDtos.View toView(
        ChangeOrder changeOrder,
        boolean clientView
    ) {
        return new ChangeOrderDtos.View(
            changeOrder.getId(),
            changeOrder.getProjectId(),
            changeOrder
                .getChangeOrderNumber(),
            changeOrder.getTitle(),
            changeOrder.getDescription(),
            changeOrder.getReason(),
            clientView
                ? null
                : changeOrder
                    .getCostImpact(),
            changeOrder
                .getClientPriceImpact(),
            changeOrder
                .getScheduleImpactDays(),
            changeOrder.getStatus(),
            changeOrder.getRequestedBy(),
            changeOrder.getSubmittedAt(),
            changeOrder.getClientReviewAt(),
            changeOrder.getApprovedBy(),
            changeOrder.getApprovedAt(),
            changeOrder.getRejectedBy(),
            changeOrder.getRejectedAt(),
            changeOrder.getCancelledBy(),
            changeOrder.getCancelledAt(),
            changeOrder
                .getCancellationReason(),
            changeOrder.getCreatedAt(),
            changeOrder.getUpdatedAt()
        );
    }
}
