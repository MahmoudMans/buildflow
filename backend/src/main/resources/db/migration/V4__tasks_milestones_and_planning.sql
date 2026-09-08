CREATE TABLE milestones (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,

    name VARCHAR(250) NOT NULL,
    description TEXT,

    planned_date DATE,
    completed_date DATE,

    status VARCHAR(30) NOT NULL,

    progress_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,

    client_visible BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_milestones_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        )
        ON DELETE CASCADE,

    CONSTRAINT uq_milestones_scope
        UNIQUE (
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT ck_milestone_status
        CHECK (
            status IN (
                'UPCOMING',
                'IN_PROGRESS',
                'COMPLETED',
                'MISSED',
                'CANCELLED'
            )
        ),

    CONSTRAINT ck_milestone_progress
        CHECK (
            progress_percentage >= 0
            AND progress_percentage <= 100
        )
);

CREATE INDEX idx_milestones_project
    ON milestones(
        organization_id,
        project_id
    );

CREATE INDEX idx_milestones_status
    ON milestones(
        organization_id,
        project_id,
        status
    );

CREATE INDEX idx_milestones_date
    ON milestones(
        organization_id,
        planned_date
    );


CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,

    milestone_id UUID,
    parent_task_id UUID,

    title VARCHAR(300) NOT NULL,
    description TEXT,

    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,

    start_date DATE,
    due_date DATE,

    estimated_hours NUMERIC(10,2),

    progress_percentage NUMERIC(5,2) NOT NULL DEFAULT 0,

    weight SMALLINT NOT NULL DEFAULT 1,

    created_by UUID NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_tasks_project
        FOREIGN KEY (
            project_id,
            organization_id
        )
        REFERENCES projects(
            id,
            organization_id
        )
        ON DELETE CASCADE,

    CONSTRAINT fk_tasks_milestone
        FOREIGN KEY (
            milestone_id,
            organization_id,
            project_id
        )
        REFERENCES milestones(
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT fk_tasks_created_by
        FOREIGN KEY (created_by)
        REFERENCES app_users(id),

    CONSTRAINT uq_tasks_scope
        UNIQUE (
            id,
            organization_id,
            project_id
        ),

    CONSTRAINT ck_task_status
        CHECK (
            status IN (
                'TODO',
                'IN_PROGRESS',
                'BLOCKED',
                'REVIEW',
                'DONE',
                'CANCELLED'
            )
        ),

    CONSTRAINT ck_task_priority
        CHECK (
            priority IN (
                'LOW',
                'MEDIUM',
                'HIGH',
                'CRITICAL'
            )
        ),

    CONSTRAINT ck_task_progress
        CHECK (
            progress_percentage >= 0
            AND progress_percentage <= 100
        ),

    CONSTRAINT ck_task_weight
        CHECK (
            weight >= 1
            AND weight <= 10
        ),

    CONSTRAINT ck_task_estimated_hours
        CHECK (
            estimated_hours IS NULL
            OR estimated_hours >= 0
        ),

    CONSTRAINT ck_task_dates
        CHECK (
            start_date IS NULL
            OR due_date IS NULL
            OR due_date >= start_date
        )
);

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_parent
        FOREIGN KEY (
            parent_task_id,
            organization_id,
            project_id
        )
        REFERENCES tasks(
            id,
            organization_id,
            project_id
        );

CREATE INDEX idx_tasks_project
    ON tasks(
        organization_id,
        project_id
    );

CREATE INDEX idx_tasks_project_status
    ON tasks(
        organization_id,
        project_id,
        status
    );

CREATE INDEX idx_tasks_project_due_date
    ON tasks(
        organization_id,
        project_id,
        due_date
    );

CREATE INDEX idx_tasks_parent
    ON tasks(
        organization_id,
        project_id,
        parent_task_id
    );

CREATE INDEX idx_tasks_milestone
    ON tasks(
        organization_id,
        project_id,
        milestone_id
    );


CREATE TABLE task_assignments (
    task_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    membership_id UUID NOT NULL,

    assigned_at TIMESTAMPTZ NOT NULL,

    PRIMARY KEY (
        task_id,
        membership_id
    ),

    CONSTRAINT fk_task_assignments_task
        FOREIGN KEY (
            task_id,
            organization_id,
            project_id
        )
        REFERENCES tasks(
            id,
            organization_id,
            project_id
        )
        ON DELETE CASCADE,

    CONSTRAINT fk_task_assignments_membership
        FOREIGN KEY (
            membership_id,
            organization_id
        )
        REFERENCES organization_memberships(
            id,
            organization_id
        )
);

CREATE INDEX idx_task_assignments_membership
    ON task_assignments(
        organization_id,
        project_id,
        membership_id
    );


CREATE TABLE task_comments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,

    author_membership_id UUID NOT NULL,

    content TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    edited_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_task_comments_task
        FOREIGN KEY (
            task_id,
            organization_id,
            project_id
        )
        REFERENCES tasks(
            id,
            organization_id,
            project_id
        )
        ON DELETE CASCADE,

    CONSTRAINT fk_task_comments_author
        FOREIGN KEY (
            author_membership_id,
            organization_id
        )
        REFERENCES organization_memberships(
            id,
            organization_id
        )
);

CREATE INDEX idx_task_comments_task
    ON task_comments(
        organization_id,
        project_id,
        task_id,
        created_at
    );


CREATE TABLE task_work_logs (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,

    membership_id UUID NOT NULL,

    work_date DATE NOT NULL,

    minutes_worked INTEGER NOT NULL,

    description TEXT,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_task_work_logs_task
        FOREIGN KEY (
            task_id,
            organization_id,
            project_id
        )
        REFERENCES tasks(
            id,
            organization_id,
            project_id
        )
        ON DELETE CASCADE,

    CONSTRAINT fk_task_work_logs_membership
        FOREIGN KEY (
            membership_id,
            organization_id
        )
        REFERENCES organization_memberships(
            id,
            organization_id
        ),

    CONSTRAINT ck_task_work_log_minutes
        CHECK (
            minutes_worked > 0
            AND minutes_worked <= 1440
        )
);

CREATE INDEX idx_task_work_logs_task
    ON task_work_logs(
        organization_id,
        project_id,
        task_id,
        work_date
    );

CREATE INDEX idx_task_work_logs_member
    ON task_work_logs(
        organization_id,
        membership_id,
        work_date
    );
