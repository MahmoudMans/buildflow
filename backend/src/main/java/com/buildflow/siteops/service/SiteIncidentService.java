package com.buildflow.siteops.service;

import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.shared.error.BusinessException;
import com.buildflow.siteops.dto.SiteOperationDtos;
import com.buildflow.siteops.entity.SiteIncident;
import com.buildflow.siteops.repository.SiteIncidentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class SiteIncidentService {

    private final SiteIncidentRepository
        incidentRepository;

    private final SiteOperationsAccessService
        accessService;

    private final OrganizationAccessService
        organizationAccessService;

    private final IncidentNumberService
        numberService;

    public SiteIncidentService(
        SiteIncidentRepository
            incidentRepository,
        SiteOperationsAccessService
            accessService,
        OrganizationAccessService
            organizationAccessService,
        IncidentNumberService
            numberService
    ) {
        this.incidentRepository =
            incidentRepository;

        this.accessService =
            accessService;

        this.organizationAccessService =
            organizationAccessService;

        this.numberService =
            numberService;
    }

    @Transactional(readOnly = true)
    public List<SiteOperationDtos.IncidentView>
        list(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        accessService.requireView(
            organizationId,
            projectId,
            userId
        );

        return incidentRepository
            .findByProjectIdAndOrganizationIdOrderByReportedAtDesc(
                projectId,
                organizationId
            )
            .stream()
            .map(this::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public SiteOperationDtos.IncidentView get(
        UUID organizationId,
        UUID userId,
        UUID incidentId
    ) {
        SiteIncident incident =
            requireIncident(
                organizationId,
                incidentId
            );

        accessService.requireView(
            organizationId,
            incident.getProjectId(),
            userId
        );

        return toView(incident);
    }

    @Transactional
    public SiteOperationDtos.IncidentView create(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        SiteOperationDtos.CreateIncidentRequest
            request
    ) {
        accessService.requireManage(
            organizationId,
            projectId,
            userId
        );

        validateAssignee(
            organizationId,
            request.assignedTo()
        );

        Instant now =
            Instant.now();

        SiteIncident incident =
            SiteIncident.create(
                organizationId,
                projectId,
                numberService.next(
                    organizationId,
                    LocalDate.ofInstant(
                        now,
                        ZoneOffset.UTC
                    ).getYear()
                ),
                request.type(),
                request.severity(),
                request.title(),
                request.description(),
                request.occurredAt(),
                userId,
                request.assignedTo(),
                request.clientVisible(),
                now
            );

        incident =
            incidentRepository.save(
                incident
            );

        return toView(incident);
    }

    @Transactional
    public SiteOperationDtos.IncidentView update(
        UUID organizationId,
        UUID userId,
        UUID incidentId,
        SiteOperationDtos.UpdateIncidentRequest
            request
    ) {
        SiteIncident incident =
            requireIncident(
                organizationId,
                incidentId
            );

        accessService.requireManage(
            organizationId,
            incident.getProjectId(),
            userId
        );

        validateAssignee(
            organizationId,
            request.assignedTo()
        );

        incident.update(
            request.type(),
            request.severity(),
            request.title(),
            request.description(),
            request.assignedTo(),
            request.clientVisible(),
            Instant.now()
        );

        return toView(incident);
    }

    @Transactional
    public SiteOperationDtos.IncidentView investigate(
        UUID organizationId,
        UUID userId,
        UUID incidentId
    ) {
        SiteIncident incident =
            managed(
                organizationId,
                userId,
                incidentId
            );

        incident.investigate(
            Instant.now()
        );

        return toView(incident);
    }

    @Transactional
    public SiteOperationDtos.IncidentView
        requireAction(
            UUID organizationId,
            UUID userId,
            UUID incidentId
        ) {

        SiteIncident incident =
            managed(
                organizationId,
                userId,
                incidentId
            );

        incident.requireAction(
            Instant.now()
        );

        return toView(incident);
    }

    @Transactional
    public SiteOperationDtos.IncidentView resolve(
        UUID organizationId,
        UUID userId,
        UUID incidentId,
        SiteOperationDtos.ResolveIncidentRequest
            request
    ) {
        SiteIncident incident =
            managed(
                organizationId,
                userId,
                incidentId
            );

        incident.resolve(
            request.resolution(),
            Instant.now()
        );

        return toView(incident);
    }

    @Transactional
    public SiteOperationDtos.IncidentView close(
        UUID organizationId,
        UUID userId,
        UUID incidentId
    ) {
        SiteIncident incident =
            managed(
                organizationId,
                userId,
                incidentId
            );

        incident.close(
            Instant.now()
        );

        return toView(incident);
    }

    private SiteIncident managed(
        UUID organizationId,
        UUID userId,
        UUID incidentId
    ) {
        SiteIncident incident =
            requireIncident(
                organizationId,
                incidentId
            );

        accessService.requireManage(
            organizationId,
            incident.getProjectId(),
            userId
        );

        return incident;
    }

    private SiteIncident requireIncident(
        UUID organizationId,
        UUID incidentId
    ) {
        return incidentRepository
            .findByIdAndOrganizationId(
                incidentId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "SITE_INCIDENT_NOT_FOUND",
                        "The requested site incident does not exist."
                    )
            );
    }

    private void validateAssignee(
        UUID organizationId,
        UUID assignedTo
    ) {
        if (assignedTo == null) {
            return;
        }

        organizationAccessService
            .requireMembership(
                organizationId,
                assignedTo
            );
    }

    private SiteOperationDtos.IncidentView toView(
        SiteIncident incident
    ) {
        return new SiteOperationDtos.IncidentView(
            incident.getId(),
            incident.getProjectId(),
            incident.getIncidentNumber(),
            incident.getType(),
            incident.getSeverity(),
            incident.getTitle(),
            incident.getDescription(),
            incident.getOccurredAt(),
            incident.getReportedAt(),
            incident.getStatus(),
            incident.getReportedBy(),
            incident.getAssignedTo(),
            incident.getResolution(),
            incident.getResolvedAt(),
            incident.isClientVisible(),
            incident.getCreatedAt(),
            incident.getUpdatedAt()
        );
    }
}
