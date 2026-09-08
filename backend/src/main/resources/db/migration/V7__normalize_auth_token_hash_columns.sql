ALTER TABLE auth_sessions
    ALTER COLUMN access_token_hash
    TYPE VARCHAR(64)
    USING BTRIM(access_token_hash)::VARCHAR(64);

ALTER TABLE auth_sessions
    ALTER COLUMN refresh_token_hash
    TYPE VARCHAR(64)
    USING BTRIM(refresh_token_hash)::VARCHAR(64);

ALTER TABLE email_verification_tokens
    ALTER COLUMN token_hash
    TYPE VARCHAR(64)
    USING BTRIM(token_hash)::VARCHAR(64);

ALTER TABLE password_reset_tokens
    ALTER COLUMN token_hash
    TYPE VARCHAR(64)
    USING BTRIM(token_hash)::VARCHAR(64);

ALTER TABLE organization_invitations
    ALTER COLUMN token_hash
    TYPE VARCHAR(64)
    USING BTRIM(token_hash)::VARCHAR(64);
