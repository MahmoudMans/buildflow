package com.buildflow.document.repository;

import com.buildflow.document.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectDocumentRepository
    extends JpaRepository<
        ProjectDocument,
        UUID
    > {

    List<ProjectDocument>
        findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID projectId,
            UUID organizationId
        );

    List<ProjectDocument>
        findByProjectIdAndOrganizationIdAndClientVisibleTrueOrderByCreatedAtDesc(
            UUID projectId,
            UUID organizationId
        );

    Optional<ProjectDocument>
        findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
        );

    Optional<ProjectDocument>
        findTopByProjectIdAndOrganizationIdAndNameOrderByDocumentVersionDesc(
            UUID projectId,
            UUID organizationId,
            String name
        );
}
