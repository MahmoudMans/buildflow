CREATE TABLE suppliers (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,

    name VARCHAR(250) NOT NULL,
    contact_name VARCHAR(200),

    email VARCHAR(320),
    phone VARCHAR(100),

    address TEXT,

    tax_number VARCHAR(100),
    payment_terms VARCHAR(250),

    notes TEXT,

    status VARCHAR(30) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_suppliers_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT uq_suppliers_scope
        UNIQUE (
            id,
            organization_id
        ),

    CONSTRAINT ck_supplier_status
        CHECK (
            status IN (
                'ACTIVE',
                'INACTIVE',
                'ARCHIVED'
            )
        )
);

CREATE INDEX idx_suppliers_organization
    ON suppliers(
        organization_id,
        status,
        name
    );


CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    supplier_id UUID NOT NULL,

    purchase_order_number VARCHAR(50) NOT NULL,

    status VARCHAR(30) NOT NULL,

    issue_date DATE NOT NULL,
    expected_delivery_date DATE,

    subtotal NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total NUMERIC(19,4) NOT NULL DEFAULT 0,

    notes TEXT,

    created_by UUID NOT NULL,

    approved_by UUID,
    approved_at TIMESTAMPTZ,

    ordered_at TIMESTAMPTZ,

    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_purchase_orders_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_purchase_orders_supplier
        FOREIGN KEY (
            supplier_id,
            organization_id
        )
        REFERENCES suppliers(
            id,
            organization_id
        ),

    CONSTRAINT fk_purchase_orders_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_users(id),

    CONSTRAINT fk_purchase_orders_approved_by
        FOREIGN KEY (approved_by)
        REFERENCES app_users(id),

    CONSTRAINT uq_purchase_orders_scope
        UNIQUE (
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT uq_purchase_order_number
        UNIQUE (
            organization_id,
            purchase_order_number
        ),

    CONSTRAINT ck_purchase_order_status
        CHECK (
            status IN (
                'DRAFT',
                'PENDING_APPROVAL',
                'APPROVED',
                'ORDERED',
                'PARTIALLY_RECEIVED',
                'RECEIVED',
                'CANCELLED'
            )
        ),

    CONSTRAINT ck_purchase_order_subtotal
        CHECK (subtotal >= 0),

    CONSTRAINT ck_purchase_order_tax
        CHECK (tax_amount >= 0),

    CONSTRAINT ck_purchase_order_total
        CHECK (total >= 0),

    CONSTRAINT ck_purchase_order_dates
        CHECK (
            expected_delivery_date IS NULL
            OR expected_delivery_date >= issue_date
        )
);

CREATE INDEX idx_purchase_orders_project
    ON purchase_orders(
        organization_id,
        project_id,
        created_at DESC
    );

CREATE INDEX idx_purchase_orders_status
    ON purchase_orders(
        organization_id,
        project_id,
        status
    );

CREATE INDEX idx_purchase_orders_supplier
    ON purchase_orders(
        organization_id,
        supplier_id
    );


CREATE TABLE purchase_order_items (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    purchase_order_id UUID NOT NULL,

    description VARCHAR(500) NOT NULL,

    quantity NUMERIC(19,4) NOT NULL,

    unit VARCHAR(50) NOT NULL,

    unit_price NUMERIC(19,4) NOT NULL,

    tax_rate NUMERIC(7,4) NOT NULL,

    line_subtotal NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL,
    line_total NUMERIC(19,4) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_purchase_order_items_order
        FOREIGN KEY (
            purchase_order_id,
            organization_id,
            project_id
        )
        REFERENCES purchase_orders(
            id,
            organization_id,
            project_id
        )
        ON DELETE CASCADE,

    CONSTRAINT uq_purchase_order_items_scope
        UNIQUE (
            id,
            organization_id,
            project_id,
            purchase_order_id
        ),

    CONSTRAINT ck_purchase_order_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT ck_purchase_order_item_unit_price
        CHECK (unit_price >= 0),

    CONSTRAINT ck_purchase_order_item_tax_rate
        CHECK (
            tax_rate >= 0
            AND tax_rate <= 100
        ),

    CONSTRAINT ck_purchase_order_item_subtotal
        CHECK (line_subtotal >= 0),

    CONSTRAINT ck_purchase_order_item_tax
        CHECK (tax_amount >= 0),

    CONSTRAINT ck_purchase_order_item_total
        CHECK (line_total >= 0)
);

CREATE INDEX idx_purchase_order_items_order
    ON purchase_order_items(
        organization_id,
        project_id,
        purchase_order_id
    );


CREATE TABLE goods_receipts (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    purchase_order_id UUID NOT NULL,

    received_date DATE NOT NULL,

    received_by_membership_id UUID NOT NULL,

    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_goods_receipts_order
        FOREIGN KEY (
            purchase_order_id,
            organization_id,
            project_id
        )
        REFERENCES purchase_orders(
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT fk_goods_receipts_member
        FOREIGN KEY (
            received_by_membership_id,
            organization_id
        )
        REFERENCES organization_memberships(
            id,
            organization_id
        ),

    CONSTRAINT uq_goods_receipts_scope
        UNIQUE (
            id,
            organization_id,
            project_id,
            purchase_order_id
        )
);

CREATE INDEX idx_goods_receipts_order
    ON goods_receipts(
        organization_id,
        project_id,
        purchase_order_id,
        received_date DESC
    );


CREATE TABLE goods_receipt_items (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    purchase_order_id UUID NOT NULL,
    goods_receipt_id UUID NOT NULL,
    purchase_order_item_id UUID NOT NULL,

    quantity_received NUMERIC(19,4) NOT NULL,

    CONSTRAINT fk_goods_receipt_items_receipt
        FOREIGN KEY (
            goods_receipt_id,
            organization_id,
            project_id,
            purchase_order_id
        )
        REFERENCES goods_receipts(
            id,
            organization_id,
            project_id,
            purchase_order_id
        )
        ON DELETE CASCADE,

    CONSTRAINT fk_goods_receipt_items_order_item
        FOREIGN KEY (
            purchase_order_item_id,
            organization_id,
            project_id,
            purchase_order_id
        )
        REFERENCES purchase_order_items(
            id,
            organization_id,
            project_id,
            purchase_order_id
        ),

    CONSTRAINT uq_goods_receipt_item
        UNIQUE (
            goods_receipt_id,
            purchase_order_item_id
        ),

    CONSTRAINT ck_goods_receipt_quantity
        CHECK (
            quantity_received > 0
        )
);

CREATE INDEX idx_goods_receipt_items_order_item
    ON goods_receipt_items(
        organization_id,
        project_id,
        purchase_order_id,
        purchase_order_item_id
    );


ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_supplier
        FOREIGN KEY (
            supplier_id,
            organization_id
        )
        REFERENCES suppliers(
            id,
            organization_id
        );

ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_purchase_order
        FOREIGN KEY (
            purchase_order_id,
            organization_id,
            project_id
        )
        REFERENCES purchase_orders(
            id,
            organization_id,
            project_id
        );
