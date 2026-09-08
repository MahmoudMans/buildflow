package com.buildflow.siteops.repository;

import com.buildflow.siteops.entity.SiteIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteIncidentRepository
    extends JpaRepository<
        SiteIncident,
        UUID
    > {

    List<SiteIncident>
        findByProjectIdAndOrganizationIdOrderByReportedAtDesc(
            UUID projectId,
            UUID organizationId
        );

    Optional<SiteIncident>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );
}
