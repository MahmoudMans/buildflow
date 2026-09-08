ALTER TABLE organization_memberships
    ADD CONSTRAINT uq_membership_id_organization
    UNIQUE (id, organization_id);


CREATE TABLE clients (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,

    type VARCHAR(30) NOT NULL,

    company_name VARCHAR(250),
    first_name VARCHAR(100),
    last_name VARCHAR(100),

    email VARCHAR(320),
    phone VARCHAR(100),

    tax_number VARCHAR(100),
    registration_number VARCHAR(100),

    billing_address TEXT,
    site_address TEXT,
    notes TEXT,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_clients_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uq_clients_id_organization
        UNIQUE (id, organization_id),

    CONSTRAINT ck_clients_type
        CHECK (
            type IN (
                'INDIVIDUAL',
                'COMPANY'
            )
        ),

    CONSTRAINT ck_clients_status
        CHECK (
            status IN (
                'ACTIVE',
                'INACTIVE',
                'ARCHIVED'
            )
        ),

    CONSTRAINT ck_clients_identity
        CHECK (
            (
                type = 'COMPANY'
                AND company_name IS NOT NULL
            )
            OR
            (
                type = 'INDIVIDUAL'
                AND first_name IS NOT NULL
                AND last_name IS NOT NULL
            )
        )
);

CREATE INDEX idx_clients_organization
    ON clients(organization_id);

CREATE INDEX idx_clients_organization_status
    ON clients(
        organization_id,
        status
    );

CREATE INDEX idx_clients_email
    ON clients(
        organization_id,
        email
    );


CREATE TABLE client_contacts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    job_title VARCHAR(150),

    email VARCHAR(320),
    phone VARCHAR(100),

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_client_contacts_client
        FOREIGN KEY (
            client_id,
            organization_id
        )
        REFERENCES clients(
            id,
            organization_id
        )
        ON DELETE CASCADE
);

CREATE INDEX idx_client_contacts_client
    ON client_contacts(
        organization_id,
        client_id
    );

CREATE UNIQUE INDEX uq_client_primary_contact
    ON client_contacts(client_id)
    WHERE is_primary = TRUE;


CREATE TABLE organization_number_sequences (
    organization_id UUID NOT NULL,
    number_type VARCHAR(50) NOT NULL,
    calendar_year INTEGER NOT NULL,
    next_value BIGINT NOT NULL,

    PRIMARY KEY (
        organization_id,
        number_type,
        calendar_year
    ),

    CONSTRAINT fk_number_sequences_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT ck_number_sequence_positive
        CHECK (next_value > 0)
);


CREATE TABLE projects (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,

    project_number VARCHAR(50) NOT NULL,

    name VARCHAR(250) NOT NULL,
    description TEXT,

    project_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,

    site_address VARCHAR(300),
    city VARCHAR(150),
    postal_code VARCHAR(50),
    country VARCHAR(150),

    start_date DATE,
    planned_end_date DATE,
    actual_end_date DATE,

    currency VARCHAR(3) NOT NULL,

    contract_value NUMERIC(19,4) NOT NULL DEFAULT 0,
    original_budget NUMERIC(19,4) NOT NULL DEFAULT 0,

    project_manager_membership_id UUID,

    progress_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_projects_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_projects_client
        FOREIGN KEY (
            client_id,
            organization_id
        )
        REFERENCES clients(
            id,
            organization_id
        ),

    CONSTRAINT fk_projects_manager
        FOREIGN KEY (
            project_manager_membership_id,
            organization_id
        )
        REFERENCES organization_memberships(
            id,
            organization_id
        ),

    CONSTRAINT uq_projects_id_organization
        UNIQUE (id, organization_id),

    CONSTRAINT uq_projects_number
        UNIQUE (
            organization_id,
            project_number
        ),

    CONSTRAINT ck_projects_type
        CHECK (
            project_type IN (
                'NEW_CONSTRUCTION',
                'RENOVATION',
                'INTERIOR',
                'ELECTRICAL',
                'PLUMBING',
                'ROOFING',
                'LANDSCAPING',
                'MAINTENANCE',
                'OTHER'
            )
        ),

    CONSTRAINT ck_projects_status
        CHECK (
            status IN (
                'DRAFT',
                'PLANNING',
                'ACTIVE',
                'ON_HOLD',
                'COMPLETED',
                'CANCELLED',
                'ARCHIVED'
            )
        ),

    CONSTRAINT ck_projects_priority
        CHECK (
            priority IN (
                'LOW',
                'MEDIUM',
                'HIGH',
                'CRITICAL'
            )
        ),

    CONSTRAINT ck_projects_progress
        CHECK (
            progress_percentage >= 0
            AND progress_percentage <= 100
        ),

    CONSTRAINT ck_projects_contract_value
        CHECK (contract_value >= 0),

    CONSTRAINT ck_projects_original_budget
        CHECK (original_budget >= 0),

    CONSTRAINT ck_projects_dates
        CHECK (
            start_date IS NULL
            OR planned_end_date IS NULL
            OR planned_end_date >= start_date
        )
);

CREATE INDEX idx_projects_organization
    ON projects(organization_id);

CREATE INDEX idx_projects_organization_status
    ON projects(
        organization_id,
        status
    );

CREATE INDEX idx_projects_client
    ON projects(
        organization_id,
        client_id
    );

CREATE INDEX idx_projects_manager
    ON projects(
        organization_id,
        project_manager_membership_id
    );

CREATE INDEX idx_projects_created
    ON projects(
        organization_id,
        created_at DESC
    );


CREATE TABLE project_members (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    membership_id UUID NOT NULL,

    project_role VARCHAR(50) NOT NULL,

    joined_at TIMESTAMPTZ NOT NULL,
    removed_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_project_members_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        )
        ON DELETE CASCADE,

    CONSTRAINT fk_project_members_membership
        FOREIGN KEY (
            membership_id,
            organization_id
        )
        REFERENCES organization_memberships(
            id,
            organization_id
        ),

    CONSTRAINT ck_project_members_role
        CHECK (
            project_role IN (
                'PROJECT_MANAGER',
                'SITE_MANAGER',
                'ENGINEER',
                'FOREMAN',
                'WORKER',
                'ACCOUNTANT',
                'VIEWER'
            )
        )
);

CREATE INDEX idx_project_members_project
    ON project_members(
        organization_id,
        project_id
    );

CREATE INDEX idx_project_members_membership
    ON project_members(
        organization_id,
        membership_id
    );

CREATE UNIQUE INDEX uq_active_project_member
    ON project_members(
        project_id,
        membership_id
    )
    WHERE removed_at IS NULL;
