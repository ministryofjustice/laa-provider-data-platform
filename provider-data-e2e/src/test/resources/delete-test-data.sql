-- E2E test data teardown
-- This script runs once after the e2e test suite completes.
-- Delete in reverse order of insert to respect foreign key constraints.
-- Uses broader deletes to also clean up data created by modifying tests.

-- Clean up links referencing E2E offices (includes data created by modifying tests)
DELETE FROM office_liaison_manager_link WHERE office_guid IN
  ('e2e00000-0000-0000-0000-000000000010', 'e2e00000-0000-0000-0000-000000000011', 'e2e00000-0000-0000-0000-000000000012');
DELETE FROM office_contract_manager_link WHERE office_guid IN
  ('e2e00000-0000-0000-0000-000000000010', 'e2e00000-0000-0000-0000-000000000011', 'e2e00000-0000-0000-0000-000000000012');

-- Clean up links referencing E2E provider-office links
DELETE FROM office_bank_account_link WHERE provider_office_link_guid IN
  ('e2e00000-0000-0000-0000-000000000050', 'e2e00000-0000-0000-0000-000000000051', 'e2e00000-0000-0000-0000-000000000052');

-- Clean up links referencing E2E providers
DELETE FROM provider_parent_link WHERE provider_guid IN
  ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003')
  OR parent_guid IN
  ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003');
DELETE FROM provider_bank_account_link WHERE provider_guid IN
  ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003');

-- Clean up provider-office links (also catches offices created by modifying tests)
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

-- Clean up E2E liaison managers (including ones created by modifying tests via created_by)
DELETE FROM office_liaison_manager_link WHERE liaison_manager_guid IN
  ('e2e00000-0000-0000-0000-000000000040', 'e2e00000-0000-0000-0000-000000000041');
DELETE FROM liaison_manager WHERE guid IN ('e2e00000-0000-0000-0000-000000000040', 'e2e00000-0000-0000-0000-000000000041');

-- Clean up E2E contract managers
DELETE FROM office_contract_manager_link WHERE contract_manager_guid IN
  ('e2e00000-0000-0000-0000-000000000030', 'e2e00000-0000-0000-0000-000000000031');
DELETE FROM contract_manager WHERE guid IN ('e2e00000-0000-0000-0000-000000000030', 'e2e00000-0000-0000-0000-000000000031');

-- Clean up E2E bank accounts
DELETE FROM bank_account WHERE guid IN ('e2e00000-0000-0000-0000-000000000020', 'e2e00000-0000-0000-0000-000000000021');

-- Clean up offices created by modifying tests (linked to E2E providers)
DELETE FROM office WHERE guid IN ('e2e00000-0000-0000-0000-000000000010', 'e2e00000-0000-0000-0000-000000000011', 'e2e00000-0000-0000-0000-000000000012');

-- Clean up E2E providers
DELETE FROM provider WHERE guid IN ('e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000003');
