-- Adds a flag to mark the single default contract manager ("Mr Default") that is
-- assigned automatically when no contract manager GUID is supplied in a request (AC2).
-- A partial unique index enforces that at most one row can hold the flag at any time.

ALTER TABLE CONTRACT_MANAGER
    ADD COLUMN DEFAULT_CONTRACT_MANAGER BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX UK_CONTRACT_MANAGER_ONE_DEFAULT
    ON CONTRACT_MANAGER (DEFAULT_CONTRACT_MANAGER)
    WHERE DEFAULT_CONTRACT_MANAGER = TRUE;

-- Seed the system default contract manager used by AC2.
INSERT INTO CONTRACT_MANAGER (GUID, VERSION, CONTRACT_MANAGER_ID, FIRST_NAME, LAST_NAME, DEFAULT_CONTRACT_MANAGER)
VALUES (gen_random_uuid(), 0, 'MR-DEFAULT', 'Mr', 'Default', TRUE);

