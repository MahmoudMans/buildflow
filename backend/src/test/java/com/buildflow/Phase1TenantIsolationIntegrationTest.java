package com.buildflow;

import com.buildflow.auth.entity.AppUser;
import com.buildflow.auth.repository.AppUserRepository;
import com.buildflow.organization.dto.OrganizationDtos;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.organization.service.OrganizationService;
import com.buildflow.shared.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class Phase1TenantIsolationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer(
            "postgres:17-alpine"
        );

    @Autowired
    AppUserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    OrganizationService
        organizationService;

    @Autowired
    OrganizationAccessService
        accessService;

    @Test
    void userFromAnotherTenantCannotAccessOrganization() {

        Instant now = Instant.now();

        AppUser userA =
            AppUser.create(
                "owner-a@example.test",
                passwordEncoder.encode(
                    "StrongPassword123!"
                ),
                "Owner",
                "A",
                now
            );

        AppUser userB =
            AppUser.create(
                "owner-b@example.test",
                passwordEncoder.encode(
                    "StrongPassword456!"
                ),
                "Owner",
                "B",
                now
            );

        userRepository.save(userA);
        userRepository.save(userB);

        OrganizationDtos.OrganizationView
            organization =
                organizationService.create(
                    userA.getId(),
                    new OrganizationDtos
                        .CreateOrganizationRequest(
                            "Tenant A",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "EUR",
                            "Europe/Paris",
                            null
                        )
                );

        assertThat(
            accessService
                .requireMembership(
                    organization.id(),
                    userA.getId()
                )
                .getUser()
                .getId()
        )
            .isEqualTo(
                userA.getId()
            );

        assertThatThrownBy(
            () ->
                accessService
                    .requireMembership(
                        organization.id(),
                        userB.getId()
                    )
        )
            .isInstanceOf(
                BusinessException.class
            )
            .hasMessageContaining(
                "does not exist or is not accessible"
            );
    }
}
