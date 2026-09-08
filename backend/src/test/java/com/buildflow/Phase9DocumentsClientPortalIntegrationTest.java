package com.buildflow;

import com.buildflow.auth.dto.AuthDtos;
import com.buildflow.auth.service.AuthService;
import com.buildflow.client.dto.ClientDtos;
import com.buildflow.client.entity.ClientType;
import com.buildflow.client.service.ClientService;
import com.buildflow.document.dto.DocumentDtos;
import com.buildflow.document.entity.DocumentCategory;
import com.buildflow.document.entity.ProjectPhotoCategory;
import com.buildflow.document.service.ProjectDocumentService;
import com.buildflow.document.service.ProjectPhotoService;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.portal.dto.ClientPortalDtos;
import com.buildflow.portal.service.ClientPortalAccessService;
import com.buildflow.portal.service.ClientPortalProjectService;
import com.buildflow.project.dto.ProjectDtos;
import com.buildflow.project.entity.ProjectPriority;
import com.buildflow.project.entity.ProjectType;
import com.buildflow.project.service.ProjectService;
import com.buildflow.shared.error.BusinessException;
import com.buildflow.storage.ObjectStorageService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
@Import(
    Phase9DocumentsClientPortalIntegrationTest
        .StorageTestConfiguration.class
)
class Phase9DocumentsClientPortalIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer(
            "postgres:17-alpine"
        );

    @Autowired
    AuthService authService;

    @Autowired
    OrganizationService organizationService;

    @Autowired
    ClientService clientService;

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectDocumentService documentService;

    @Autowired
    ProjectPhotoService photoService;

    @Autowired
    ClientPortalAccessService
        portalAccessService;

    @Autowired
    ClientPortalProjectService
        portalProjectService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManager entityManager;

    @Test
    void clientPortalExposesOnlyAuthorizedProjectFiles()
        throws Exception {

        AuthService.AuthResult owner =
            authService.register(
                new AuthDtos.RegisterRequest(
                    "Portal",
                    "Owner",
                    "phase9@example.test",
                    "StrongPassword123!"
                ),
                "127.0.0.1",
                "test"
            );

        OrganizationDtos.OrganizationView
            organization =
                organizationService.create(
                    owner.user().getId(),
                    new OrganizationDtos
                        .CreateOrganizationRequest(
                            "Phase 9 Construction",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "Paris",
                            null,
                            "France",
                            "EUR",
                            "Europe/Paris",
                            null
                        )
                );

        ClientDtos.ClientView client =
            clientService.create(
                organization.id(),
                owner.user().getId(),
                new ClientDtos
                    .CreateClientRequest(
                        ClientType.COMPANY,
                        "Phase 9 Client",
                        null,
                        null,
                        "client@example.test",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    )
            );

        ProjectDtos.ProjectView project =
            projectService.create(
                organization.id(),
                owner.user().getId(),
                new ProjectDtos
                    .CreateProjectRequest(
                        client.id(),
                        "Secure Client Portal Project",
                        null,
                        ProjectType.RENOVATION,
                        ProjectPriority.HIGH,
                        null,
                        "Paris",
                        null,
                        "France",
                        null,
                        null,
                        "EUR",
                        null
                    )
            );

        byte[] internalBytes =
            "INTERNAL COST FILE"
                .getBytes();

        byte[] clientBytes =
            "CLIENT CONTRACT FILE"
                .getBytes();

        DocumentDtos.DocumentView
            internalDocument =
                documentService.upload(
                    organization.id(),
                    owner.user().getId(),
                    project.id(),
                    new DocumentDtos
                        .UploadDocumentRequest(
                            "internal.pdf",
                            "Internal estimating evidence",
                            DocumentCategory.TECHNICAL,
                            "application/pdf",
                            Base64
                                .getEncoder()
                                .encodeToString(
                                    internalBytes
                                ),
                            false
                        )
                );

        DocumentDtos.DocumentView
            clientDocument =
                documentService.upload(
                    organization.id(),
                    owner.user().getId(),
                    project.id(),
                    new DocumentDtos
                        .UploadDocumentRequest(
                            "contract.pdf",
                            "Client contract",
                            DocumentCategory.CONTRACT,
                            "application/pdf",
                            Base64
                                .getEncoder()
                                .encodeToString(
                                    clientBytes
                                ),
                            true
                        )
                );

        photoService.upload(
            organization.id(),
            owner.user().getId(),
            project.id(),
            new DocumentDtos
                .UploadPhotoRequest(
                    "progress.png",
                    "image/png",
                    Base64
                        .getEncoder()
                        .encodeToString(
                            new byte[] {
                                (byte) 0x89,
                                0x50,
                                0x4E,
                                0x47
                            }
                        ),
                    "Client progress",
                    null,
                    ProjectPhotoCategory.PROGRESS,
                    true
                )
        );

        portalAccessService.link(
            organization.id(),
            owner.user().getId(),
            client.id(),
            owner.user().getId()
        );

        entityManager.flush();

        int roleChanges =
            jdbcTemplate.update(
                """
                UPDATE organization_memberships
                SET role = 'CLIENT'
                WHERE organization_id = ?
                  AND user_id = ?
                """,
                organization.id(),
                owner.user().getId()
            );

        assertThat(
            roleChanges
        ).isEqualTo(1);

        entityManager.clear();

        assertThat(
            portalProjectService.projects(
                organization.id(),
                owner.user().getId()
            )
        )
            .extracting(
                ClientPortalDtos
                    .ProjectSummary::id
            )
            .containsExactly(
                project.id()
            );

        java.util.List<
            DocumentDtos.DocumentView
        > clientDocuments =
            documentService.list(
                organization.id(),
                owner.user().getId(),
                project.id()
            );

        assertThat(
            clientDocuments
        )
            .allMatch(
                DocumentDtos
                    .DocumentView::clientVisible
            );

        assertThat(
            clientDocuments
        )
            .extracting(
                DocumentDtos
                    .DocumentView::id
            )
            .contains(
                clientDocument.id()
            )
            .doesNotContain(
                internalDocument.id()
            );

        assertThat(
            clientDocuments
        )
            .extracting(
                DocumentDtos
                    .DocumentView::category
            )
            .contains(
                DocumentCategory.CONTRACT,
                DocumentCategory.PHOTO
            );

        assertThat(
            photoService.list(
                organization.id(),
                owner.user().getId(),
                project.id()
            )
        ).hasSize(1);

        assertThatThrownBy(
            () ->
                documentService.content(
                    organization.id(),
                    owner.user().getId(),
                    internalDocument.id()
                )
        )
            .isInstanceOf(
                BusinessException.class
            );

        assertThat(
            documentService.content(
                organization.id(),
                owner.user().getId(),
                clientDocument.id()
            ).content()
        ).containsExactly(
            clientBytes
        );

        ClientPortalDtos.ProjectView
            portalView =
                portalProjectService.project(
                    organization.id(),
                    owner.user().getId(),
                    project.id()
                );

        assertThat(
            java.util.stream.Stream.of(
                ClientPortalDtos.ProjectView.class,
                ClientPortalDtos.ProjectSummary.class,
                ClientPortalDtos.MilestoneView.class,
                ClientPortalDtos.ProgressUpdateView.class,
                ClientPortalDtos.ChangeOrderView.class,
                ClientPortalDtos.QuoteView.class,
                ClientPortalDtos.InvoiceView.class
            )
                .flatMap(
                    type ->
                        java.util.Arrays.stream(
                            type.getRecordComponents()
                        )
                )
                .map(
                    java.lang.reflect.RecordComponent::getName
                )
                .toList()
        )
            .doesNotContain(
                "originalBudget",
                "revisedBudget",
                "forecastProfit",
                "forecastCost",
                "costImpact",
                "supplierPrice",
                "supplierPrices",
                "workerSalary",
                "workerSalaries",
                "internalComments"
            );

        assertThat(
            portalView.project().id()
        ).isEqualTo(
            project.id()
        );
    }

    @TestConfiguration
    static class StorageTestConfiguration {

        @Bean
        @Primary
        ObjectStorageService
            objectStorageService() {

            return new InMemoryStorage();
        }
    }

    static final class InMemoryStorage
        implements ObjectStorageService {

        private final Map<
            String,
            StoredObject
        > objects =
            new ConcurrentHashMap<>();

        @Override
        public void put(
            String objectKey,
            String mimeType,
            byte[] content
        ) {
            objects.put(
                objectKey,
                new StoredObject(
                    content.clone(),
                    mimeType
                )
            );
        }

        @Override
        public StoredObject get(
            String objectKey
        ) {
            StoredObject value =
                objects.get(
                    objectKey
                );

            if (value == null) {
                throw new IllegalStateException(
                    "Object not found."
                );
            }

            return new StoredObject(
                value.content().clone(),
                value.mimeType()
            );
        }

        @Override
        public void delete(
            String objectKey
        ) {
            objects.remove(
                objectKey
            );
        }
    }
}
