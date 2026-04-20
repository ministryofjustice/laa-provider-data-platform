-- The original full unique constraint on (PROVIDER_OFFICE_LINK_GUID, BANK_ACCOUNT_GUID)
-- prevents re-linking a bank account that was previously active on the same office.
-- The spec requires support for historical re-linking, so replace it with a partial
-- unique index that only covers active (non-end-dated) rows.

ALTER TABLE OFFICE_BANK_ACCOUNT_LINK
    DROP CONSTRAINT IF EXISTS UK_OFFICE_BA_LINK_PROVIDER_OFFICE_BANK_ACCOUNT;

CREATE UNIQUE INDEX IF NOT EXISTS UIX_OFFICE_BA_LINK_ACTIVE
    ON OFFICE_BANK_ACCOUNT_LINK (PROVIDER_OFFICE_LINK_GUID, BANK_ACCOUNT_GUID)
    WHERE ACTIVE_DATE_TO IS NULL;
