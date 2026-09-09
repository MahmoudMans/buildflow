package com.buildflow.report.service;

import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.project.service.ProjectAccessService;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class ReportAccessService {

    private static final Set<OrganizationRole>
        PROJECT_REPORT_ROLES =
            Set.of(
                OrganizationRole.OWNER,
                OrganizationRole.ADMIN,
                OrganizationRole.PROJECT_MANAGER,
                OrganizationRole.SITE_MANAGER,
                OrganizationRole.ACCOUNTANT
            );

    private static final Set<OrganizationRole>
        FINANCIAL_REPORT_ROLES =
            Set.of(
                OrganizationRole.OWNER,
                OrganizationRole.ADMIN,
                OrganizationRole.PROJECT_MANAGER,
                OrganizationRole.ACCOUNTANT
            );

    private static final Set<OrganizationRole>
        ORGANIZATION_EXPORT_ROLES =
            Set.of(
                OrganizationRole.OWNER,
                OrganizationRole.ADMIN,
                OrganizationRole.ACCOUNTANT
            );

    private static final Set<OrganizationRole>
        ORGANIZATION_WIDE_PROJECT_ROLES =
            Set.of(
                OrganizationRole.OWNER,
                OrganizationRole.ADMIN,
                OrganizationRole.ACCOUNTANT
            );

    private final OrganizationAccessService
        organizationAccessService;

    private final ProjectAccessService
        projectAccessService;

    private final JdbcTemplate
        jdbcTemplate;

    public ReportAccessService(
        OrganizationAccessService
            organizationAccessService,
        ProjectAccessService
            projectAccessService,
        JdbcTemplate jdbcTemplate
    ) {
        this.organizationAccessService =
            organizationAccessService;

        this.projectAccessService =
            projectAccessService;

        this.jdbcTemplate =
            jdbcTemplate;
    }

    public OrganizationRole
        requireProjectReport(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        OrganizationRole role =
            requireRole(
                organizationId,
                userId,
                PROJECT_REPORT_ROLES,
                "PROJECT_REPORT_FORBIDDEN"
            );

        requireProjectAccess(
            organizationId,
            userId,
            projectId,
            role
        );

        return role;
    }

    public OrganizationRole
        requireFinancialProject(
            UUID organizationId,
            UUID userId,
            UUID projectId
        ) {

        OrganizationRole role =
            requireRole(
                organizationId,
                userId,
                FINANCIAL_REPORT_ROLES,
                "FINANCIAL_REPORT_FORBIDDEN"
            );

        requireProjectAccess(
            organizationId,
            userId,
            projectId,
            role
        );

        return role;
    }

    public void requireCommercialProject(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        requireFinancialProject(
            organizationId,
            userId,
            projectId
        );
    }

    public void requireSiteReport(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        requireProjectReport(
            organizationId,
            userId,
            projectId
        );
    }

    public void requireOrganizationExport(
        UUID organizationId,
        UUID userId
    ) {
        requireRole(
            organizationId,
            userId,
            ORGANIZATION_EXPORT_ROLES,
            "ORGANIZATION_EXPORT_FORBIDDEN"
        );
    }

    private OrganizationRole requireRole(
        UUID organizationId,
        UUID userId,
        Set<OrganizationRole> allowedRoles,
        String forbiddenCode
    ) {
        OrganizationRole role =
            organizationAccessService
                .requireMembership(
                    organizationId,
                    userId
                )
                .getRole();

        if (
            !allowedRoles.contains(
                role
            )
        ) {
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                forbiddenCode,
                "You are not authorized to generate this report."
            );
        }

        return role;
    }

    private void requireProjectAccess(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        OrganizationRole role
    ) {
        if (
            ORGANIZATION_WIDE_PROJECT_ROLES
                .contains(
                    role
                )
        ) {
            requireTenantProject(
                organizationId,
                projectId
            );

            return;
        }

        projectAccessService
            .requireVisible(
                organizationId,
                projectId,
                userId
            );
    }

    private void requireTenantProject(
        UUID organizationId,
        UUID projectId
    ) {
        Long count =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM projects
                WHERE id = ?
                  AND organization_id = ?
                """,
                Long.class,
                projectId,
                organizationId
            );

        if (
            count == null
            || count.longValue() != 1L
        ) {
            throw new BusinessException(
                HttpStatus.NOT_FOUND,
                "PROJECT_NOT_FOUND",
                "The project does not exist."
            );
        }
    }
}
