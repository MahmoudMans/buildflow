package com.buildflow.siteops.repository;

import com.buildflow.siteops.entity.DailySiteReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailySiteReportRepository
    extends JpaRepository<
        DailySiteReport,
        UUID
    > {

    List<DailySiteReport>
        findByProjectIdAndOrganizationIdOrderByReportDateDesc(
            UUID projectId,
            UUID organizationId
        );

    Optional<DailySiteReport>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    boolean
        existsByProjectIdAndOrganizationIdAndReportDate(
            UUID projectId,
            UUID organizationId,
            LocalDate reportDate
        );
}
