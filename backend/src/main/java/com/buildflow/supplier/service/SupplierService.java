package com.buildflow.supplier.service;

import com.buildflow.organization.entity.OrganizationRole;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.shared.error.BusinessException;
import com.buildflow.supplier.dto.SupplierDtos;
import com.buildflow.supplier.entity.Supplier;
import com.buildflow.supplier.entity.SupplierStatus;
import com.buildflow.supplier.repository.SupplierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class SupplierService {

    private static final Set<
        OrganizationRole
    > VIEW_ROLES =
        Set.of(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.PROJECT_MANAGER,
            OrganizationRole.SITE_MANAGER,
            OrganizationRole.ACCOUNTANT
        );

    private static final Set<
        OrganizationRole
    > EDIT_ROLES =
        Set.of(
            OrganizationRole.OWNER,
            OrganizationRole.ADMIN,
            OrganizationRole.PROJECT_MANAGER
        );

    private final SupplierRepository
        supplierRepository;

    private final OrganizationAccessService
        organizationAccessService;

    public SupplierService(
        SupplierRepository supplierRepository,
        OrganizationAccessService
            organizationAccessService
    ) {
        this.supplierRepository =
            supplierRepository;

        this.organizationAccessService =
            organizationAccessService;
    }

    @Transactional(readOnly = true)
    public List<
        SupplierDtos.SupplierView
    > list(
        UUID organizationId,
        UUID userId
    ) {
        requireView(
            organizationId,
            userId
        );

        return supplierRepository
            .findByOrganizationIdOrderByNameAsc(
                organizationId
            )
            .stream()
            .map(
                SupplierDtos
                    .SupplierView
                    ::from
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public SupplierDtos.SupplierView get(
        UUID organizationId,
        UUID userId,
        UUID supplierId
    ) {
        requireView(
            organizationId,
            userId
        );

        return SupplierDtos
            .SupplierView
            .from(
                requireSupplier(
                    organizationId,
                    supplierId
                )
            );
    }

    @Transactional
    public SupplierDtos.SupplierView create(
        UUID organizationId,
        UUID userId,
        SupplierDtos.CreateSupplierRequest
            request
    ) {
        requireEdit(
            organizationId,
            userId
        );

        Supplier supplier =
            Supplier.create(
                organizationId,
                request.name().trim(),
                clean(
                    request.contactName()
                ),
                normalizeEmail(
                    request.email()
                ),
                clean(
                    request.phone()
                ),
                clean(
                    request.address()
                ),
                clean(
                    request.taxNumber()
                ),
                clean(
                    request.paymentTerms()
                ),
                clean(
                    request.notes()
                ),
                Instant.now()
            );

        supplierRepository.save(
            supplier
        );

        return SupplierDtos
            .SupplierView
            .from(supplier);
    }

    @Transactional
    public SupplierDtos.SupplierView update(
        UUID organizationId,
        UUID userId,
        UUID supplierId,
        SupplierDtos.UpdateSupplierRequest
            request
    ) {
        requireEdit(
            organizationId,
            userId
        );

        Supplier supplier =
            requireSupplier(
                organizationId,
                supplierId
            );

        if (
            supplier.getStatus()
                == SupplierStatus.ARCHIVED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "SUPPLIER_ARCHIVED",
                "An archived supplier cannot be modified."
            );
        }

        supplier.update(
            cleanPreserveNull(
                request.name()
            ),
            cleanPreserveNull(
                request.contactName()
            ),
            normalizeEmailPreserveNull(
                request.email()
            ),
            cleanPreserveNull(
                request.phone()
            ),
            cleanPreserveNull(
                request.address()
            ),
            cleanPreserveNull(
                request.taxNumber()
            ),
            cleanPreserveNull(
                request.paymentTerms()
            ),
            cleanPreserveNull(
                request.notes()
            ),
            request.status(),
            Instant.now()
        );

        return SupplierDtos
            .SupplierView
            .from(supplier);
    }

    @Transactional
    public void archive(
        UUID organizationId,
        UUID userId,
        UUID supplierId
    ) {
        requireEdit(
            organizationId,
            userId
        );

        requireSupplier(
            organizationId,
            supplierId
        ).archive(
            Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public Supplier requireSupplier(
        UUID organizationId,
        UUID supplierId
    ) {
        return supplierRepository
            .findByIdAndOrganizationId(
                supplierId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "SUPPLIER_NOT_FOUND",
                        "The supplier does not exist or is not accessible."
                    )
            );
    }

    @Transactional(readOnly = true)
    public Supplier requireActiveSupplier(
        UUID organizationId,
        UUID supplierId
    ) {
        Supplier supplier =
            requireSupplier(
                organizationId,
                supplierId
            );

        if (
            supplier.getStatus()
                != SupplierStatus.ACTIVE
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "SUPPLIER_NOT_ACTIVE",
                "Only an active supplier can be used for new procurement."
            );
        }

        return supplier;
    }

    private void requireView(
        UUID organizationId,
        UUID userId
    ) {
        organizationAccessService
            .requireAnyRole(
                organizationId,
                userId,
                VIEW_ROLES
            );
    }

    private void requireEdit(
        UUID organizationId,
        UUID userId
    ) {
        organizationAccessService
            .requireAnyRole(
                organizationId,
                userId,
                EDIT_ROLES
            );
    }

    private String clean(
        String value
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private String cleanPreserveNull(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private String normalizeEmail(
        String value
    ) {
        String cleaned =
            clean(value);

        if (cleaned == null) {
            return null;
        }

        return cleaned.toLowerCase(
            Locale.ROOT
        );
    }

    private String normalizeEmailPreserveNull(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return value
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }
}
