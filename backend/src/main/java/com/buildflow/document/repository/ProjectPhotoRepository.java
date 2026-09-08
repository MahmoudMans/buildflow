package com.buildflow.document.repository;

import com.buildflow.document.entity.ProjectPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectPhotoRepository
    extends JpaRepository<
        ProjectPhoto,
        UUID
    > {

    List<ProjectPhoto>
        findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID projectId,
            UUID organizationId
        );

    List<ProjectPhoto>
        findByProjectIdAndOrganizationIdAndClientVisibleTrueOrderByCreatedAtDesc(
            UUID projectId,
            UUID organizationId
        );
}
