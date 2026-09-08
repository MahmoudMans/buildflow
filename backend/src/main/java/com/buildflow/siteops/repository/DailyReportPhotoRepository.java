package com.buildflow.siteops.repository;

import com.buildflow.siteops.entity.DailyReportPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyReportPhotoRepository
    extends JpaRepository<
        DailyReportPhoto,
        UUID
    > {

    List<DailyReportPhoto>
        findByReportIdAndOrganizationIdOrderByCreatedAtAsc(
            UUID reportId,
            UUID organizationId
        );

    Optional<DailyReportPhoto>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );
}
