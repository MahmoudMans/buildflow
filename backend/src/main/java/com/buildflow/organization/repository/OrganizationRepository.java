package com.buildflow.organization.repository;

import com.buildflow.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationRepository
    extends JpaRepository<Organization, UUID> {
}
