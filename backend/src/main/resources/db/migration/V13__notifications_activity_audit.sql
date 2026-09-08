CREATE TABLE notifications (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    organization_id UUID NOT NULL,

    type VARCHAR(80) NOT NULL,

    title VARCHAR(250) NOT NULL,
    message TEXT NOT NULL,

    entity_type VARCHAR(100),
    entity_id UUID,

    source_key VARCHAR(300),

    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id),

    CONSTRAINT fk_notification_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
);

CREATE INDEX idx_notifications_user
    ON notifications(
        organization_id,
        user_id,
        created_at DESC
    );

CREATE INDEX idx_notifications_unread
    ON notifications(
        organization_id,
        user_id,
        read_at,
        created_at DESC
    );

CREATE UNIQUE INDEX uq_notification_source
    ON notifications(
        user_id,
        source_key
    )
    WHERE source_key IS NOT NULL;


CREATE TABLE project_activity (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,

    actor_user_id UUID,

    action VARCHAR(120) NOT NULL,

    message TEXT NOT NULL,

    entity_type VARCHAR(100),
    entity_id UUID,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_project_activity_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_project_activity_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES app_users(id)
);

CREATE INDEX idx_project_activity_project
    ON project_activity(
        organization_id,
        project_id,
        created_at DESC
    );


CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID,

    actor_user_id UUID,

    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,

    old_values TEXT,
    new_values TEXT,

    ip_address VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_audit_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_audit_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_audit_actor
        FOREIGN KEY (actor_user_id)
        REFERENCES app_users(id)
);

CREATE INDEX idx_audit_organization
    ON audit_logs(
        organization_id,
        created_at DESC
    );

CREATE INDEX idx_audit_project
    ON audit_logs(
        organization_id,
        project_id,
        created_at DESC
    );

CREATE INDEX idx_audit_entity
    ON audit_logs(
        organization_id,
        entity_type,
        entity_id,
        created_at DESC
    );
