package com.buildflow.siteops.repository;

import com.buildflow.siteops.entity.ProjectProgressUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectProgressUpdateRepository
    extends JpaRepository<
        ProjectProgressUpdate,
        UUID
    > {

    List<ProjectProgressUpdate>
        findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID projectId,
            UUID organizationId
        );
}
