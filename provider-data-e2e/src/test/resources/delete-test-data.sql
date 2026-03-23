-- E2E test data teardown
-- This script runs once after the e2e test suite completes.
-- Deletes in reverse FK order to respect foreign key constraints.
-- Handles both SQL-inserted data (e2e00000-* UUIDs) and data created by POST endpoints.

-- ============================================================
-- 1. Clean up data created by POST /provider-firms endpoint
--    (providers with names starting with "New ")
-- ============================================================
DELETE FROM office_liaison_manager_link WHERE office_guid IN
  (SELECT office_guid FROM provider_office_link WHERE provider_guid IN
    (SELECT guid FROM provider WHERE name LIKE 'New %'));
DELETE FROM office_contract_manager_link WHERE office_guid IN
  (SELECT office_guid FROM provider_office_link WHERE provider_guid IN
    (SELECT guid FROM provider WHERE name LIKE 'New %'));
DELETE FROM office_bank_account_link WHERE provider_office_link_guid IN
  (SELECT guid FROM provider_office_link WHERE provider_guid IN
    (SELECT guid FROM provider WHERE name LIKE 'New %'));
DELETE FROM provider_office_link WHERE provider_guid IN
  (SELECT guid FROM provider WHERE name LIKE 'New %');
DELETE FROM provider_bank_account_link WHERE provider_guid IN
  (SELECT guid FROM provider WHERE name LIKE 'New %');
DELETE FROM provider_parent_link WHERE provider_guid IN
  (SELECT guid FROM provider WHERE name LIKE 'New %')
  OR parent_guid IN
  (SELECT guid FROM provider WHERE name LIKE 'New %');
DELETE FROM provider WHERE name LIKE 'New %';

-- ============================================================
-- 2. Clean up data created by POST .../offices endpoint
--    (offices linked to E2E providers but not E2E-inserted)
-- ============================================================
DELETE FROM office_liaison_manager_link WHERE office_guid IN
  (SELECT office_guid FROM provider_office_link WHERE provider_guid IN
    ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003'));
DELETE FROM office_contract_manager_link WHERE office_guid IN
  (SELECT office_guid FROM provider_office_link WHERE provider_guid IN
    ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003'));
DELETE FROM office_bank_account_link WHERE provider_office_link_guid IN
  (SELECT guid FROM provider_office_link WHERE provider_guid IN
    ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003'));
DELETE FROM provider_office_link WHERE provider_guid IN
  ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003');

-- ============================================================
-- 3. Clean up data created by POST .../contract-managers endpoint
--    (links to E2E contract managers)
-- ============================================================
DELETE FROM office_contract_manager_link WHERE contract_manager_guid IN
  ('e2e00000-0000-0000-0000-000000000030', 'e2e00000-0000-0000-0000-000000000031');

-- ============================================================
-- 4. Clean up data created by POST .../liaison-managers endpoint
--    (liaison managers linked to E2E offices)
-- ============================================================
DELETE FROM office_liaison_manager_link WHERE office_guid IN
  ('e2e00000-0000-0000-0000-000000000010', 'e2e00000-0000-0000-0000-000000000011', 'e2e00000-0000-0000-0000-000000000012');

-- ============================================================
-- 5. Clean up SQL-inserted E2E data (reverse order of insert)
-- ============================================================
DELETE FROM office_bank_account_link WHERE provider_office_link_guid IN
  ('e2e00000-0000-0000-0000-000000000050', 'e2e00000-0000-0000-0000-000000000051', 'e2e00000-0000-0000-0000-000000000052');

DELETE FROM provider_parent_link WHERE provider_guid IN
  ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003')
  OR parent_guid IN
  ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003');
DELETE FROM provider_bank_account_link WHERE provider_guid IN
  ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003');

-- Liaison managers (E2E-inserted)
DELETE FROM office_liaison_manager_link WHERE liaison_manager_guid IN
  ('e2e00000-0000-0000-0000-000000000040', 'e2e00000-0000-0000-0000-000000000041');
DELETE FROM liaison_manager WHERE guid IN
  ('e2e00000-0000-0000-0000-000000000040', 'e2e00000-0000-0000-0000-000000000041');

-- Contract managers (E2E-inserted)
DELETE FROM contract_manager WHERE guid IN
  ('e2e00000-0000-0000-0000-000000000030', 'e2e00000-0000-0000-0000-000000000031');

-- Bank accounts (E2E-inserted)
DELETE FROM bank_account WHERE guid IN
  ('e2e00000-0000-0000-0000-000000000020', 'e2e00000-0000-0000-0000-000000000021');

-- Offices (E2E-inserted)
DELETE FROM office WHERE guid IN
  ('e2e00000-0000-0000-0000-000000000010', 'e2e00000-0000-0000-0000-000000000011', 'e2e00000-0000-0000-0000-000000000012');

-- Providers (E2E-inserted)
DELETE FROM provider WHERE guid IN
  ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003');
