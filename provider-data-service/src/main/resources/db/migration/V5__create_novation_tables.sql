-- Create Novation and Novation Link tables.
--
-- A NOVATION represents a provider change event. A NOVATION_LINK represents one
-- predecessor/successor mapping within that event, allowing a novation to model
-- multiple predecessors with the same successor (and vice versa).
--
-- DECISION_* records the operation that moves a novation out of Proposed.
-- RESCINDED_* records a later rescission without overwriting the original decision.
-- DECISION_BY and RESCINDED_BY are populated from the authenticated user performing
-- the corresponding operation.
--
-- CONTRACT and SCHEDULE entities are not yet present in PDA-r2. Their GUID
-- columns are included now so that the novation model does not need to change
-- when those entities are introduced. Foreign keys for those columns should be
-- added by the migrations that introduce the corresponding tables.

CREATE TABLE NOVATION (
    GUID                     UUID         NOT NULL,
    VERSION                  BIGINT,
    CREATED_BY               VARCHAR(255),
    CREATED_TIMESTAMP        TIMESTAMP(6) WITH TIME ZONE,
    LAST_UPDATED_BY          VARCHAR(255),
    LAST_UPDATED_TIMESTAMP   TIMESTAMP(6) WITH TIME ZONE,
    NOVATION_TYPE            VARCHAR(255) NOT NULL,
    NOVATION_EFFECTIVE_DATE  DATE         NOT NULL,
    NOVATION_STATUS          VARCHAR(255),
    DECISION_DATE            DATE,
    DECISION_REASON          TEXT,
    DECISION_BY              VARCHAR(255),
    RESCINDED_DATE           DATE,
    RESCINDED_REASON         TEXT,
    RESCINDED_BY             VARCHAR(255),
    DRIVER_FOR_NOVATION      TEXT,
    NOTES                    TEXT,
    PRIMARY KEY (GUID),
    CONSTRAINT CK_NOVATION_STATUS
        CHECK (NOVATION_STATUS IS NULL OR NOVATION_STATUS IN (
            'Proposed',
            'Approved',
            'Approved with Conditions',
            'Rejected',
            'Withdrawn',
            'Rescinded'
        )),
    CONSTRAINT CK_NOVATION_DECISION_INFORMATION
        CHECK (
            NOVATION_STATUS IS NULL
            OR NOVATION_STATUS = 'Proposed'
            OR (NOVATION_STATUS = 'Approved' AND DECISION_DATE IS NOT NULL AND DECISION_BY IS NOT NULL)
            OR (NOVATION_STATUS IN ('Approved with Conditions', 'Rejected', 'Withdrawn')
                AND DECISION_DATE IS NOT NULL AND DECISION_REASON IS NOT NULL AND DECISION_BY IS NOT NULL)
            OR (NOVATION_STATUS = 'Rescinded' AND DECISION_DATE IS NOT NULL AND DECISION_BY IS NOT NULL)
        ),
    CONSTRAINT CK_NOVATION_RESCISSION_INFORMATION
        CHECK (
            (NOVATION_STATUS = 'Rescinded'
                AND RESCINDED_DATE IS NOT NULL
                AND RESCINDED_REASON IS NOT NULL
                AND RESCINDED_BY IS NOT NULL)
            OR (NOVATION_STATUS IS DISTINCT FROM 'Rescinded'
                AND RESCINDED_DATE IS NULL
                AND RESCINDED_REASON IS NULL
                AND RESCINDED_BY IS NULL)
        )
);

CREATE TABLE NOVATION_LINK (
    GUID                     UUID         NOT NULL,
    VERSION                  BIGINT,
    CREATED_BY               VARCHAR(255),
    CREATED_TIMESTAMP        TIMESTAMP(6) WITH TIME ZONE,
    LAST_UPDATED_BY          VARCHAR(255),
    LAST_UPDATED_TIMESTAMP   TIMESTAMP(6) WITH TIME ZONE,
    NOVATION_GUID            UUID         NOT NULL,
    PREVIOUS_PROVIDER_GUID   UUID         NOT NULL,
    NEW_PROVIDER_GUID        UUID         NOT NULL,
    PREVIOUS_OFFICE_GUID     UUID,
    NEW_OFFICE_GUID          UUID,
    PREVIOUS_CONTRACT_GUID   UUID,
    NEW_CONTRACT_GUID        UUID,
    PREVIOUS_SCHEDULE_GUID   UUID,
    NEW_SCHEDULE_GUID        UUID,
    NOTES                    TEXT,
    PRIMARY KEY (GUID),
    CONSTRAINT FK_NOVATION_LINK_NOVATION
        FOREIGN KEY (NOVATION_GUID) REFERENCES NOVATION,
    CONSTRAINT FK_NOVATION_LINK_PREVIOUS_PROVIDER
        FOREIGN KEY (PREVIOUS_PROVIDER_GUID) REFERENCES PROVIDER,
    CONSTRAINT FK_NOVATION_LINK_NEW_PROVIDER
        FOREIGN KEY (NEW_PROVIDER_GUID) REFERENCES PROVIDER,
    CONSTRAINT FK_NOVATION_LINK_PREVIOUS_OFFICE
        FOREIGN KEY (PREVIOUS_OFFICE_GUID) REFERENCES PROVIDER_OFFICE_LINK,
    CONSTRAINT FK_NOVATION_LINK_NEW_OFFICE
        FOREIGN KEY (NEW_OFFICE_GUID) REFERENCES PROVIDER_OFFICE_LINK,
    CONSTRAINT CK_NOVATION_LINK_CONTRACT_PAIR
        CHECK ((PREVIOUS_CONTRACT_GUID IS NULL) = (NEW_CONTRACT_GUID IS NULL)),
    CONSTRAINT CK_NOVATION_LINK_SCHEDULE_PAIR
        CHECK ((PREVIOUS_SCHEDULE_GUID IS NULL) = (NEW_SCHEDULE_GUID IS NULL))
);

CREATE INDEX IX_NOVATION_LINK_NOVATION_GUID
    ON NOVATION_LINK (NOVATION_GUID);

CREATE INDEX IX_NOVATION_LINK_PREVIOUS_PROVIDER_GUID
    ON NOVATION_LINK (PREVIOUS_PROVIDER_GUID);

CREATE INDEX IX_NOVATION_LINK_NEW_PROVIDER_GUID
    ON NOVATION_LINK (NEW_PROVIDER_GUID);

CREATE INDEX IX_NOVATION_EFFECTIVE_DATE
    ON NOVATION (NOVATION_EFFECTIVE_DATE);
