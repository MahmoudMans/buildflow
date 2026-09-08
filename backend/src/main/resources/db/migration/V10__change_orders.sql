CREATE TABLE change_orders (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,

    change_order_number VARCHAR(50) NOT NULL,

    title VARCHAR(250) NOT NULL,
    description TEXT,
    reason TEXT,

    cost_impact NUMERIC(19,4) NOT NULL DEFAULT 0,
    client_price_impact NUMERIC(19,4) NOT NULL DEFAULT 0,
    schedule_impact_days INTEGER NOT NULL DEFAULT 0,

    status VARCHAR(30) NOT NULL,

    requested_by UUID NOT NULL,

    submitted_at TIMESTAMPTZ,

    client_review_at TIMESTAMPTZ,

    approved_by UUID,
    approved_at TIMESTAMPTZ,

    rejected_by UUID,
    rejected_at TIMESTAMPTZ,

    cancelled_by UUID,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_change_orders_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_change_orders_requested_by
        FOREIGN KEY (requested_by)
        REFERENCES app_users(id),

    CONSTRAINT fk_change_orders_approved_by
        FOREIGN KEY (approved_by)
        REFERENCES app_users(id),

    CONSTRAINT fk_change_orders_rejected_by
        FOREIGN KEY (rejected_by)
        REFERENCES app_users(id),

    CONSTRAINT fk_change_orders_cancelled_by
        FOREIGN KEY (cancelled_by)
        REFERENCES app_users(id),

    CONSTRAINT uq_change_orders_scope
        UNIQUE (
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT uq_change_order_number
        UNIQUE (
            organization_id,
            change_order_number
        ),

    CONSTRAINT ck_change_order_status
        CHECK (
            status IN (
                'DRAFT',
                'SUBMITTED',
                'CLIENT_REVIEW',
                'APPROVED',
                'REJECTED',
                'CANCELLED'
            )
        )
);

CREATE INDEX idx_change_orders_project
    ON change_orders(
        organization_id,
        project_id,
        created_at DESC
    );

CREATE INDEX idx_change_orders_status
    ON change_orders(
        organization_id,
        project_id,
        status
    );
