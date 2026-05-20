-- Phase 3: Command audit log table.
-- Append-only; records are written after each successful provider firm command.
CREATE TABLE COMMAND_AUDIT_LOG (
    GUID                UUID        NOT NULL DEFAULT gen_random_uuid(),
    VERSION             BIGINT,
    CREATED_BY          VARCHAR(255),
    CREATED_TIMESTAMP   TIMESTAMPTZ,
    LAST_UPDATED_BY     VARCHAR(255),
    LAST_UPDATED_TIMESTAMP TIMESTAMPTZ,

    PROVIDER_FIRM_GUID  UUID        NOT NULL,
    FIRM_NUMBER         VARCHAR(50) NOT NULL,
    COMMAND_TYPE        VARCHAR(100) NOT NULL,
    OCCURRED_AT         TIMESTAMPTZ NOT NULL,
    CHANGED_FIELDS      VARCHAR(500),

    CONSTRAINT pk_command_audit_log PRIMARY KEY (GUID)
);

CREATE INDEX idx_command_audit_log_provider_firm_guid
    ON COMMAND_AUDIT_LOG (PROVIDER_FIRM_GUID);

CREATE INDEX idx_command_audit_log_firm_number
    ON COMMAND_AUDIT_LOG (FIRM_NUMBER);

