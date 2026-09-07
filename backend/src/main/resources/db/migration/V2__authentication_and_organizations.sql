CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    email_verified_at TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_app_users_status
        CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_app_users_email
    ON app_users (LOWER(email));

CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    legal_name VARCHAR(250),
    registration_number VARCHAR(100),
    tax_number VARCHAR(100),
    email VARCHAR(320),
    phone VARCHAR(100),
    address_line_1 VARCHAR(250),
    address_line_2 VARCHAR(250),
    city VARCHAR(150),
    postal_code VARCHAR(50),
    country VARCHAR(150),
    currency VARCHAR(3) NOT NULL,
    timezone VARCHAR(100) NOT NULL,
    logo_url VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE organization_memberships (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    deactivated_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_membership_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id),

    CONSTRAINT uq_membership_organization_user
        UNIQUE (organization_id, user_id),

    CONSTRAINT ck_membership_role
        CHECK (
            role IN (
                'OWNER',
                'ADMIN',
                'PROJECT_MANAGER',
                'SITE_MANAGER',
                'WORKER',
                'ACCOUNTANT',
                'CLIENT'
            )
        ),

    CONSTRAINT ck_membership_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_memberships_user
    ON organization_memberships(user_id);

CREATE INDEX idx_memberships_organization
    ON organization_memberships(organization_id);

CREATE INDEX idx_memberships_organization_status
    ON organization_memberships(organization_id, status);

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    access_token_hash CHAR(64) NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL,
    access_expires_at TIMESTAMPTZ NOT NULL,
    refresh_expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    ip_address VARCHAR(100),
    user_agent VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_auth_session_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_auth_session_access_hash
    ON auth_sessions(access_token_hash);

CREATE UNIQUE INDEX uq_auth_session_refresh_hash
    ON auth_sessions(refresh_token_hash);

CREATE INDEX idx_auth_sessions_user
    ON auth_sessions(user_id);

CREATE INDEX idx_auth_sessions_access_expiry
    ON auth_sessions(access_expires_at);

CREATE INDEX idx_auth_sessions_refresh_expiry
    ON auth_sessions(refresh_expires_at);

CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_email_verification_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_email_verification_token_hash
    ON email_verification_tokens(token_hash);

CREATE INDEX idx_email_verification_user
    ON email_verification_tokens(user_id);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_password_reset_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_password_reset_token_hash
    ON password_reset_tokens(token_hash);

CREATE INDEX idx_password_reset_user
    ON password_reset_tokens(user_id);

CREATE TABLE organization_invitations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    email VARCHAR(320) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    invited_by UUID NOT NULL,
    accepted_by UUID,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_invitation_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_invitation_invited_by
        FOREIGN KEY (invited_by)
        REFERENCES app_users(id),

    CONSTRAINT fk_invitation_accepted_by
        FOREIGN KEY (accepted_by)
        REFERENCES app_users(id),

    CONSTRAINT ck_invitation_role
        CHECK (
            role IN (
                'OWNER',
                'ADMIN',
                'PROJECT_MANAGER',
                'SITE_MANAGER',
                'WORKER',
                'ACCOUNTANT',
                'CLIENT'
            )
        ),

    CONSTRAINT ck_invitation_status
        CHECK (
            status IN (
                'PENDING',
                'ACCEPTED',
                'EXPIRED',
                'REVOKED'
            )
        )
);

CREATE UNIQUE INDEX uq_invitation_token_hash
    ON organization_invitations(token_hash);

CREATE UNIQUE INDEX uq_pending_organization_invitation
    ON organization_invitations (
        organization_id,
        LOWER(email)
    )
    WHERE status = 'PENDING';

CREATE INDEX idx_invitations_organization
    ON organization_invitations(organization_id);

CREATE INDEX idx_invitations_email
    ON organization_invitations(LOWER(email));

CREATE INDEX idx_invitations_expiry
    ON organization_invitations(expires_at);
