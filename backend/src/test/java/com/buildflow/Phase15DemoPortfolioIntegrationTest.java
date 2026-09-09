package com.buildflow;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(
    properties = {
        "buildflow.demo.enabled=true",
        "buildflow.demo.password=BuildFlowDemo2026!"
    }
)
class Phase15DemoPortfolioIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer(
            "postgres:17-alpine"
        );

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createsCompletePortfolioDemoDataset() {

        String userTable =
            firstExistingTable(
                List.of(
                    "users",
                    "app_users",
                    "user_accounts"
                )
            );

        assertThat(
            userTable
        ).isNotNull();

        Integer demoUsers =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM %s
                WHERE email IN (
                    'owner@buildflow.demo',
                    'pm@buildflow.demo',
                    'site@buildflow.demo',
                    'accountant@buildflow.demo',
                    'client@buildflow.demo'
                )
                """.formatted(
                    userTable
                ),
                Integer.class
            );

        assertThat(
            demoUsers
        ).isEqualTo(
            5
        );

        assertDemoAuthenticationState(
            userTable
        );

        Integer organizationCount =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM organizations
                WHERE name =
                    'NorthBuild Construction'
                """,
                Integer.class
            );

        assertThat(
            organizationCount
        ).isEqualTo(
            1
        );

        Integer projectCount =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM projects project
                JOIN organizations organization
                  ON organization.id =
                        project.organization_id
                WHERE organization.name =
                    'NorthBuild Construction'
                  AND project.name IN (
                    'Rivoli Hotel Renovation',
                    'Belleville Office Fit-Out',
                    'Neuilly Residence Extension'
                  )
                """,
                Integer.class
            );

        assertThat(
            projectCount
        ).isEqualTo(
            3
        );

        String membershipTable =
            firstExistingTable(
                List.of(
                    "organization_memberships",
                    "memberships"
                )
            );

        assertThat(
            membershipTable
        ).isNotNull();

        List<String> roles =
            jdbcTemplate.queryForList(
                """
                SELECT DISTINCT role
                FROM %s membership
                JOIN organizations organization
                  ON organization.id =
                        membership.organization_id
                WHERE organization.name =
                    'NorthBuild Construction'
                """.formatted(
                    membershipTable
                ),
                String.class
            );

        assertThat(
            roles
        )
            .contains(
                "OWNER",
                "PROJECT_MANAGER",
                "SITE_MANAGER",
                "ACCOUNTANT",
                "CLIENT"
            );
    }

    private void assertDemoAuthenticationState(
        String userTable
    ) {
        List<String> availableColumns =
            jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                ORDER BY ordinal_position
                """,
                String.class,
                userTable
            );

        String emailFilter =
            """
            email IN (
                'owner@buildflow.demo',
                'pm@buildflow.demo',
                'site@buildflow.demo',
                'accountant@buildflow.demo',
                'client@buildflow.demo'
            )
            """;

        for (
            String column
            : List.of(
                "email_verified",
                "is_email_verified",
                "verified",
                "is_verified",
                "email_confirmed",
                "enabled",
                "is_enabled",
                "active",
                "is_active"
            )
        ) {
            if (
                !availableColumns.contains(
                    column
                )
            ) {
                continue;
            }

            Integer trueCount =
                jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM %s
                    WHERE %s
                      AND %s = TRUE
                    """.formatted(
                        userTable,
                        emailFilter,
                        column
                    ),
                    Integer.class
                );

            assertThat(
                trueCount
            )
                .as(
                    column
                        + " must be true for all demo accounts"
                )
                .isEqualTo(
                    5
                );
        }

        if (
            availableColumns.contains(
                "status"
            )
        ) {
            Integer activeCount =
                jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM %s
                    WHERE %s
                      AND status = 'ACTIVE'
                    """.formatted(
                        userTable,
                        emailFilter
                    ),
                    Integer.class
                );

            assertThat(
                activeCount
            )
                .as(
                    "demo accounts must be ACTIVE"
                )
                .isEqualTo(
                    5
                );
        }

        if (
            availableColumns.contains(
                "failed_login_attempts"
            )
        ) {
            Integer unlockedCount =
                jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM %s
                    WHERE %s
                      AND failed_login_attempts = 0
                    """.formatted(
                        userTable,
                        emailFilter
                    ),
                    Integer.class
                );

            assertThat(
                unlockedCount
            )
                .as(
                    "demo accounts must have no failed-login debt"
                )
                .isEqualTo(
                    5
                );
        }

        if (
            availableColumns.contains(
                "locked_until"
            )
        ) {
            Integer unlockedCount =
                jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM %s
                    WHERE %s
                      AND locked_until IS NULL
                    """.formatted(
                        userTable,
                        emailFilter
                    ),
                    Integer.class
                );

            assertThat(
                unlockedCount
            )
                .as(
                    "demo accounts must not be locked"
                )
                .isEqualTo(
                    5
                );
        }
    }

    private String firstExistingTable(
        List<String> candidates
    ) {
        for (
            String candidate
            : candidates
        ) {
            Integer count =
                jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name = ?
                    """,
                    Integer.class,
                    candidate
                );

            if (
                count != null
                && count > 0
            ) {
                return candidate;
            }
        }

        return null;
    }
}
