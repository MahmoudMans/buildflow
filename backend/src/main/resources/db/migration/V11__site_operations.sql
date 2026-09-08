CREATE TABLE daily_site_reports (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,

    report_date DATE NOT NULL,

    weather_text VARCHAR(500),
    workers_on_site INTEGER NOT NULL DEFAULT 0,

    summary TEXT NOT NULL,
    work_completed TEXT,
    delays TEXT,
    issues TEXT,
    next_steps TEXT,

    created_by UUID NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uq_daily_site_report_scope
        UNIQUE (
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT fk_daily_site_report_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_daily_site_report_creator
        FOREIGN KEY (created_by)
        REFERENCES app_users(id),

    CONSTRAINT uq_daily_site_report_date
        UNIQUE (
            organization_id,
            project_id,
            report_date
        ),

    CONSTRAINT ck_daily_site_report_workers
        CHECK (
            workers_on_site >= 0
        )
);

CREATE INDEX idx_daily_site_reports_project_date
    ON daily_site_reports(
        organization_id,
        project_id,
        report_date DESC
    );


CREATE TABLE daily_report_photos (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    report_id UUID NOT NULL,

    storage_key UUID NOT NULL,

    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,

    caption VARCHAR(1000),
    category VARCHAR(40) NOT NULL,

    client_visible BOOLEAN NOT NULL DEFAULT FALSE,

    content BYTEA NOT NULL,

    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_daily_report_photo_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_daily_report_photo_report
        FOREIGN KEY (
            report_id,
            organization_id,
            project_id
        )
        REFERENCES daily_site_reports(
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT fk_daily_report_photo_creator
        FOREIGN KEY (created_by)
        REFERENCES app_users(id),

    CONSTRAINT uq_daily_report_photo_scope
        UNIQUE (
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT uq_daily_report_photo_storage_key
        UNIQUE (
            organization_id,
            storage_key
        ),

    CONSTRAINT ck_daily_report_photo_category
        CHECK (
            category IN (
                'BEFORE',
                'PROGRESS',
                'AFTER',
                'DELIVERY',
                'DAMAGE',
                'SAFETY_ISSUE',
                'OTHER'
            )
        ),

    CONSTRAINT ck_daily_report_photo_size
        CHECK (
            size_bytes > 0
            AND size_bytes <= 5242880
        )
);

CREATE INDEX idx_daily_report_photos_report
    ON daily_report_photos(
        organization_id,
        report_id,
        created_at
    );


CREATE TABLE site_incidents (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,

    incident_number VARCHAR(50) NOT NULL,

    type VARCHAR(40) NOT NULL,
    severity VARCHAR(20) NOT NULL,

    title VARCHAR(250) NOT NULL,
    description TEXT NOT NULL,

    occurred_at TIMESTAMPTZ NOT NULL,
    reported_at TIMESTAMPTZ NOT NULL,

    status VARCHAR(40) NOT NULL,

    reported_by UUID NOT NULL,
    assigned_to UUID,

    resolution TEXT,
    resolved_at TIMESTAMPTZ,

    client_visible BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_site_incident_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_site_incident_reporter
        FOREIGN KEY (reported_by)
        REFERENCES app_users(id),

    CONSTRAINT fk_site_incident_assignee
        FOREIGN KEY (assigned_to)
        REFERENCES app_users(id),

    CONSTRAINT uq_site_incident_number
        UNIQUE (
            organization_id,
            incident_number
        ),

    CONSTRAINT ck_site_incident_type
        CHECK (
            type IN (
                'SAFETY',
                'MATERIAL_DAMAGE',
                'SITE_ISSUE',
                'QUALITY',
                'DELAY',
                'EQUIPMENT'
            )
        ),

    CONSTRAINT ck_site_incident_severity
        CHECK (
            severity IN (
                'LOW',
                'MEDIUM',
                'HIGH',
                'CRITICAL'
            )
        ),

    CONSTRAINT ck_site_incident_status
        CHECK (
            status IN (
                'OPEN',
                'INVESTIGATING',
                'ACTION_REQUIRED',
                'RESOLVED',
                'CLOSED'
            )
        )
);

CREATE INDEX idx_site_incidents_project_status
    ON site_incidents(
        organization_id,
        project_id,
        status,
        reported_at DESC
    );


CREATE TABLE project_progress_updates (
    id UUID PRIMARY KEY,

    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,

    progress_percentage_snapshot NUMERIC(5,2) NOT NULL,

    summary TEXT NOT NULL,
    blockers TEXT,
    next_steps TEXT,

    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_progress_update_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        ),

    CONSTRAINT fk_progress_update_creator
        FOREIGN KEY (created_by)
        REFERENCES app_users(id),

    CONSTRAINT ck_progress_update_percentage
        CHECK (
            progress_percentage_snapshot >= 0
            AND progress_percentage_snapshot <= 100
        )
);

CREATE INDEX idx_progress_updates_project
    ON project_progress_updates(
        organization_id,
        project_id,
        created_at DESC
    );
