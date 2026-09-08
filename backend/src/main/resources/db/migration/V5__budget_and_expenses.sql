ALTER TABLE projects
    ADD COLUMN budget_baseline_locked BOOLEAN NOT NULL DEFAULT FALSE;


CREATE TABLE budget_categories (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,

    name VARCHAR(200) NOT NULL,
    description TEXT,

    sort_order INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_budget_categories_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        )
        ON DELETE CASCADE,

    CONSTRAINT uq_budget_categories_scope
        UNIQUE (
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT ck_budget_category_sort_order
        CHECK (
            sort_order >= 0
        )
);

CREATE UNIQUE INDEX uq_budget_category_project_name
    ON budget_categories(
        project_id,
        LOWER(name)
    );

CREATE INDEX idx_budget_categories_project
    ON budget_categories(
        organization_id,
        project_id,
        sort_order
    );


CREATE TABLE budget_items (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    budget_category_id UUID NOT NULL,

    description VARCHAR(500) NOT NULL,

    quantity NUMERIC(19,4) NOT NULL,
    unit_cost NUMERIC(19,4) NOT NULL,
    planned_amount NUMERIC(19,4) NOT NULL,

    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_budget_items_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        )
        ON DELETE CASCADE,

    CONSTRAINT fk_budget_items_category
        FOREIGN KEY (
            budget_category_id,
            organization_id,
            project_id
        )
        REFERENCES budget_categories(
            id,
            organization_id,
            project_id
        )
        ON DELETE CASCADE,

    CONSTRAINT ck_budget_item_quantity
        CHECK (
            quantity >= 0
        ),

    CONSTRAINT ck_budget_item_unit_cost
        CHECK (
            unit_cost >= 0
        ),

    CONSTRAINT ck_budget_item_planned_amount
        CHECK (
            planned_amount >= 0
        )
);

CREATE INDEX idx_budget_items_category
    ON budget_items(
        organization_id,
        project_id,
        budget_category_id
    );


CREATE TABLE expenses (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    budget_category_id UUID NOT NULL,

    supplier_id UUID,
    purchase_order_id UUID,
    receipt_document_id UUID,

    expense_number VARCHAR(50) NOT NULL,

    description VARCHAR(500) NOT NULL,

    expense_date DATE NOT NULL,

    net_amount NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL,

    status VARCHAR(30) NOT NULL,

    created_by UUID NOT NULL,

    approved_by UUID,
    approved_at TIMESTAMPTZ,

    rejection_reason TEXT,

    void_reason TEXT,
    voided_by UUID,
    voided_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_expenses_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_expenses_budget_category
        FOREIGN KEY (
            budget_category_id,
            organization_id,
            project_id
        )
        REFERENCES budget_categories(
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT fk_expenses_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_users(id),

    CONSTRAINT fk_expenses_approved_by
        FOREIGN KEY (approved_by)
        REFERENCES app_users(id),

    CONSTRAINT fk_expenses_voided_by
        FOREIGN KEY (voided_by)
        REFERENCES app_users(id),

    CONSTRAINT uq_expense_number
        UNIQUE (
            organization_id,
            expense_number
        ),

    CONSTRAINT ck_expense_status
        CHECK (
            status IN (
                'DRAFT',
                'SUBMITTED',
                'APPROVED',
                'REJECTED',
                'VOID'
            )
        ),

    CONSTRAINT ck_expense_net_amount
        CHECK (
            net_amount >= 0
        ),

    CONSTRAINT ck_expense_tax_amount
        CHECK (
            tax_amount >= 0
        ),

    CONSTRAINT ck_expense_total_amount
        CHECK (
            total_amount >= 0
        )
);

CREATE INDEX idx_expenses_project
    ON expenses(
        organization_id,
        project_id,
        expense_date DESC
    );

CREATE INDEX idx_expenses_project_status
    ON expenses(
        organization_id,
        project_id,
        status
    );

CREATE INDEX idx_expenses_budget_category
    ON expenses(
        organization_id,
        project_id,
        budget_category_id
    );

CREATE INDEX idx_expenses_created_by
    ON expenses(
        organization_id,
        created_by
    );
