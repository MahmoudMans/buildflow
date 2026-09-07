CREATE TABLE buildflow_schema_metadata (
    id SMALLINT PRIMARY KEY,
    application_name VARCHAR(100) NOT NULL,
    foundation_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_buildflow_schema_metadata_singleton CHECK (id = 1)
);

INSERT INTO buildflow_schema_metadata (
    id,
    application_name,
    foundation_version
)
VALUES (
    1,
    'BuildFlow',
    'phase-0'
);
