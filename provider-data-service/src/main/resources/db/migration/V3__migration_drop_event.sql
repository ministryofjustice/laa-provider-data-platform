-- Records individual entities dropped during data migration.
-- One row per (dropped record, reason): a record dropped for N reasons produces N rows.
-- Rows for a given PHASE_NAME are replaced on each run; stale rows are deleted before
-- new events are inserted.
--
-- REF_DATA holds the structured fields of the dropped record as JSONB, keyed by the field
-- names of the DropRef subtype (e.g. firmNumber, officeCode, sortCode).  The schema of
-- REF_DATA is intentionally open so that new fields can be added without a DDL migration.
-- REF_DESCRIPTION holds the human-readable summary produced by DropRef.describe().

CREATE TABLE MIGRATION_DROP_EVENT (
    GUID             UUID         NOT NULL,
    PHASE_NAME       VARCHAR(100) NOT NULL,
    REASON           VARCHAR(255) NOT NULL,
    DROPPED_AT       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    REF_DESCRIPTION  TEXT         NOT NULL,
    REF_DATA         JSONB        NOT NULL,
    PRIMARY KEY (GUID)
);

CREATE INDEX IX_MIGRATION_DROP_PHASE_REASON
    ON MIGRATION_DROP_EVENT (PHASE_NAME, REASON);

CREATE INDEX IX_MIGRATION_DROP_REF_DATA
    ON MIGRATION_DROP_EVENT USING GIN (REF_DATA);
