CREATE TABLE quotations (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    client_id UUID NOT NULL,

    quote_number VARCHAR(50) NOT NULL,

    status VARCHAR(30) NOT NULL,

    contract_quote BOOLEAN NOT NULL DEFAULT FALSE,

    issue_date DATE NOT NULL,
    valid_until DATE,

    subtotal NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total NUMERIC(19,4) NOT NULL DEFAULT 0,

    notes TEXT,
    terms TEXT,

    created_by UUID NOT NULL,

    sent_at TIMESTAMPTZ,
    viewed_at TIMESTAMPTZ,

    accepted_at TIMESTAMPTZ,
    accepted_by UUID,

    rejected_at TIMESTAMPTZ,

    expired_at TIMESTAMPTZ,

    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_quotations_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_quotations_client
        FOREIGN KEY (
            client_id,
            organization_id
        )
        REFERENCES clients(
            id,
            organization_id
        ),

    CONSTRAINT fk_quotations_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_users(id),

    CONSTRAINT fk_quotations_accepted_by
        FOREIGN KEY (accepted_by)
        REFERENCES app_users(id),

    CONSTRAINT uq_quotations_scope
        UNIQUE (
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT uq_quotation_number
        UNIQUE (
            organization_id,
            quote_number
        ),

    CONSTRAINT ck_quotation_status
        CHECK (
            status IN (
                'DRAFT',
                'SENT',
                'VIEWED',
                'ACCEPTED',
                'REJECTED',
                'EXPIRED',
                'CANCELLED'
            )
        ),

    CONSTRAINT ck_quotation_dates
        CHECK (
            valid_until IS NULL
            OR valid_until >= issue_date
        ),

    CONSTRAINT ck_quotation_subtotal
        CHECK (subtotal >= 0),

    CONSTRAINT ck_quotation_tax
        CHECK (tax_amount >= 0),

    CONSTRAINT ck_quotation_total
        CHECK (total >= 0)
);

CREATE INDEX idx_quotations_project
    ON quotations(
        organization_id,
        project_id,
        created_at DESC
    );

CREATE INDEX idx_quotations_client
    ON quotations(
        organization_id,
        client_id
    );

CREATE INDEX idx_quotations_status
    ON quotations(
        organization_id,
        project_id,
        status
    );


CREATE TABLE quotation_items (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    quotation_id UUID NOT NULL,

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

    CONSTRAINT fk_quotation_items_quotation
        FOREIGN KEY (
            quotation_id,
            organization_id,
            project_id
        )
        REFERENCES quotations(
            id,
            organization_id,
            project_id
        )
        ON DELETE CASCADE,

    CONSTRAINT uq_quotation_items_scope
        UNIQUE (
            id,
            organization_id,
            project_id,
            quotation_id
        ),

    CONSTRAINT ck_quotation_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT ck_quotation_item_unit_price
        CHECK (unit_price >= 0),

    CONSTRAINT ck_quotation_item_tax_rate
        CHECK (
            tax_rate >= 0
            AND tax_rate <= 100
        ),

    CONSTRAINT ck_quotation_item_subtotal
        CHECK (line_subtotal >= 0),

    CONSTRAINT ck_quotation_item_tax
        CHECK (tax_amount >= 0),

    CONSTRAINT ck_quotation_item_total
        CHECK (line_total >= 0)
);

CREATE INDEX idx_quotation_items_quotation
    ON quotation_items(
        organization_id,
        project_id,
        quotation_id
    );


CREATE TABLE invoices (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    client_id UUID NOT NULL,

    source_quote_id UUID,

    invoice_number VARCHAR(50) NOT NULL,

    status VARCHAR(30) NOT NULL,

    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,

    subtotal NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total NUMERIC(19,4) NOT NULL DEFAULT 0,

    amount_paid NUMERIC(19,4) NOT NULL DEFAULT 0,
    amount_outstanding NUMERIC(19,4) NOT NULL DEFAULT 0,

    notes TEXT,
    payment_instructions TEXT,

    issued_at TIMESTAMPTZ,

    voided_at TIMESTAMPTZ,
    void_reason TEXT,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_invoices_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_invoices_client
        FOREIGN KEY (
            client_id,
            organization_id
        )
        REFERENCES clients(
            id,
            organization_id
        ),

    CONSTRAINT fk_invoices_source_quote
        FOREIGN KEY (
            source_quote_id,
            organization_id,
            project_id
        )
        REFERENCES quotations(
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT uq_invoices_scope
        UNIQUE (
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT uq_invoice_number
        UNIQUE (
            organization_id,
            invoice_number
        ),

    CONSTRAINT ck_invoice_status
        CHECK (
            status IN (
                'DRAFT',
                'ISSUED',
                'PARTIALLY_PAID',
                'PAID',
                'OVERDUE',
                'VOID'
            )
        ),

    CONSTRAINT ck_invoice_dates
        CHECK (
            due_date >= issue_date
        ),

    CONSTRAINT ck_invoice_subtotal
        CHECK (subtotal >= 0),

    CONSTRAINT ck_invoice_tax
        CHECK (tax_amount >= 0),

    CONSTRAINT ck_invoice_total
        CHECK (total >= 0),

    CONSTRAINT ck_invoice_amount_paid
        CHECK (
            amount_paid >= 0
            AND amount_paid <= total
        ),

    CONSTRAINT ck_invoice_amount_outstanding
        CHECK (
            amount_outstanding >= 0
            AND amount_outstanding <= total
        )
);

CREATE INDEX idx_invoices_project
    ON invoices(
        organization_id,
        project_id,
        created_at DESC
    );

CREATE INDEX idx_invoices_client
    ON invoices(
        organization_id,
        client_id
    );

CREATE INDEX idx_invoices_status
    ON invoices(
        organization_id,
        project_id,
        status
    );

CREATE INDEX idx_invoices_due_date
    ON invoices(
        organization_id,
        status,
        due_date
    );


CREATE TABLE invoice_items (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    invoice_id UUID NOT NULL,

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

    CONSTRAINT fk_invoice_items_invoice
        FOREIGN KEY (
            invoice_id,
            organization_id,
            project_id
        )
        REFERENCES invoices(
            id,
            organization_id,
            project_id
        )
        ON DELETE CASCADE,

    CONSTRAINT uq_invoice_items_scope
        UNIQUE (
            id,
            organization_id,
            project_id,
            invoice_id
        ),

    CONSTRAINT ck_invoice_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT ck_invoice_item_unit_price
        CHECK (unit_price >= 0),

    CONSTRAINT ck_invoice_item_tax_rate
        CHECK (
            tax_rate >= 0
            AND tax_rate <= 100
        ),

    CONSTRAINT ck_invoice_item_subtotal
        CHECK (line_subtotal >= 0),

    CONSTRAINT ck_invoice_item_tax
        CHECK (tax_amount >= 0),

    CONSTRAINT ck_invoice_item_total
        CHECK (line_total >= 0)
);

CREATE INDEX idx_invoice_items_invoice
    ON invoice_items(
        organization_id,
        project_id,
        invoice_id
    );


CREATE TABLE payments (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    invoice_id UUID NOT NULL,

    payment_date DATE NOT NULL,

    amount NUMERIC(19,4) NOT NULL,

    method VARCHAR(30) NOT NULL,

    reference VARCHAR(250),

    notes TEXT,

    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_payments_invoice
        FOREIGN KEY (
            invoice_id,
            organization_id,
            project_id
        )
        REFERENCES invoices(
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT fk_payments_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_users(id),

    CONSTRAINT uq_payments_scope
        UNIQUE (
            id,
            organization_id,
            project_id,
            invoice_id
        ),

    CONSTRAINT ck_payment_amount
        CHECK (amount > 0),

    CONSTRAINT ck_payment_method
        CHECK (
            method IN (
                'BANK_TRANSFER',
                'CASH',
                'CARD',
                'CHEQUE',
                'OTHER'
            )
        )
);

CREATE INDEX idx_payments_invoice
    ON payments(
        organization_id,
        project_id,
        invoice_id,
        payment_date DESC
    );

CREATE INDEX idx_payments_project
    ON payments(
        organization_id,
        project_id,
        payment_date DESC
    );
