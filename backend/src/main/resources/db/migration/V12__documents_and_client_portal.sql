CREATE TABLE project_documents (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,

    name VARCHAR(255) NOT NULL,
    description TEXT,

    category VARCHAR(40) NOT NULL,

    storage_key UUID NOT NULL,

    mime_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,

    version INTEGER NOT NULL DEFAULT 1,

    client_visible BOOLEAN NOT NULL DEFAULT FALSE,

    uploaded_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_project_document_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_project_document_uploader
        FOREIGN KEY (uploaded_by)
        REFERENCES app_users(id),

    CONSTRAINT uq_project_document_storage
        UNIQUE (
            organization_id,
            storage_key
        ),

    CONSTRAINT uq_project_document_scope
        UNIQUE (
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT ck_project_document_category
        CHECK (
            category IN (
                'CONTRACT',
                'DRAWING',
                'PERMIT',
                'QUOTE',
                'INVOICE',
                'RECEIPT',
                'PHOTO',
                'REPORT',
                'INSURANCE',
                'SAFETY',
                'TECHNICAL',
                'OTHER'
            )
        ),

    CONSTRAINT ck_project_document_size
        CHECK (
            size_bytes > 0
            AND size_bytes <= 15728640
        ),

    CONSTRAINT ck_project_document_version
        CHECK (
            version >= 1
        )
);

CREATE INDEX idx_project_documents_project
    ON project_documents(
        organization_id,
        project_id,
        created_at DESC
    );

CREATE INDEX idx_project_documents_client
    ON project_documents(
        organization_id,
        project_id,
        client_visible,
        created_at DESC
    );


CREATE TABLE project_photos (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,

    file_id UUID NOT NULL,

    caption VARCHAR(1000),
    taken_date DATE,

    category VARCHAR(30) NOT NULL,

    client_visible BOOLEAN NOT NULL DEFAULT FALSE,

    uploaded_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_project_photo_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_project_photo_file
        FOREIGN KEY (
            file_id,
            organization_id,
            project_id
        )
        REFERENCES project_documents(
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT fk_project_photo_uploader
        FOREIGN KEY (uploaded_by)
        REFERENCES app_users(id),

    CONSTRAINT ck_project_photo_category
        CHECK (
            category IN (
                'BEFORE',
                'PROGRESS',
                'AFTER',
                'ISSUE',
                'DELIVERY',
                'OTHER'
            )
        )
);

CREATE INDEX idx_project_photos_project
    ON project_photos(
        organization_id,
        project_id,
        taken_date DESC,
        created_at DESC
    );

CREATE INDEX idx_project_photos_client
    ON project_photos(
        organization_id,
        project_id,
        client_visible,
        created_at DESC
    );


CREATE TABLE client_portal_accesses (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    client_id UUID NOT NULL,
    membership_id UUID NOT NULL,

    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_client_portal_access_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_client_portal_access_client
        FOREIGN KEY (client_id)
        REFERENCES clients(id),

    CONSTRAINT fk_client_portal_access_membership
        FOREIGN KEY (membership_id)
        REFERENCES organization_memberships(id),

    CONSTRAINT fk_client_portal_access_creator
        FOREIGN KEY (created_by)
        REFERENCES app_users(id),

    CONSTRAINT uq_client_portal_access
        UNIQUE (
            organization_id,
            client_id,
            membership_id
        )
);

CREATE INDEX idx_client_portal_access_membership
    ON client_portal_accesses(
        organization_id,
        membership_id
    );


CREATE TABLE client_change_order_decisions (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    change_order_id UUID NOT NULL,

    decision VARCHAR(20) NOT NULL,

    client_user_id UUID NOT NULL,

    comment VARCHAR(4000),

    decided_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_client_co_decision_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_client_co_decision_change_order
        FOREIGN KEY (change_order_id)
        REFERENCES change_orders(id),

    CONSTRAINT fk_client_co_decision_user
        FOREIGN KEY (client_user_id)
        REFERENCES app_users(id),

    CONSTRAINT uq_client_co_decision
        UNIQUE (
            organization_id,
            change_order_id
        ),

    CONSTRAINT ck_client_co_decision
        CHECK (
            decision IN (
                'APPROVED',
                'REJECTED'
            )
        )
);

CREATE INDEX idx_client_co_decisions_project
    ON client_change_order_decisions(
        organization_id,
        project_id,
        decided_at DESC
    );
