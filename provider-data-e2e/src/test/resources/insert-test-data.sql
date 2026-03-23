-- E2E test data setup
-- This script runs once before the e2e test suite.
-- Uses fixed UUIDs prefixed with 'e2e' so they are easy to identify and clean up.

-- ---- Providers ----
INSERT INTO provider (guid, firm_number, firm_type, name, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000001', 'E2E-LSP-001', 'Legal Services Provider', 'E2E Test LSP Firm', 0, 'e2e', NOW(), 'e2e', NOW()),
  ('e2e00000-0000-0000-0000-000000000002', 'E2E-CHM-001', 'Chambers', 'E2E Test Chambers', 0, 'e2e', NOW(), 'e2e', NOW()),
  ('e2e00000-0000-0000-0000-000000000003', 'E2E-ADV-001', 'Advocate', 'E2E Test Advocate', 0, 'e2e', NOW(), 'e2e', NOW());

-- ---- Offices ----
INSERT INTO office (guid, address_line1, address_line2, address_town_or_city, address_county, address_post_code, telephone_number, email_address, dx_details_number, dx_details_centre, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000010', '10 E2E Street', 'Floor 1', 'London', 'London', 'E2E 1AA', '020 1111 2222', 'e2e-office1@test.example.com', 'DX-E2E-1', 'E2E Centre', 0, 'e2e', NOW(), 'e2e', NOW()),
  ('e2e00000-0000-0000-0000-000000000011', '20 E2E Avenue', NULL, 'Manchester', 'Manchester', 'E2E 2BB', '0161 3333 4444', 'e2e-office2@test.example.com', NULL, NULL, 0, 'e2e', NOW(), 'e2e', NOW()),
  ('e2e00000-0000-0000-0000-000000000012', '30 E2E Lane', NULL, 'Birmingham', 'West Midlands', 'E2E 3CC', '0121 5555 6666', 'e2e-office3@test.example.com', NULL, NULL, 0, 'e2e', NOW(), 'e2e', NOW());

-- ---- Bank Accounts ----
INSERT INTO bank_account (guid, account_name, sort_code, account_number, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000020', 'E2E Bank Account 1', '100000', 'E2E00001', 0, 'e2e', NOW(), 'e2e', NOW()),
  ('e2e00000-0000-0000-0000-000000000021', 'E2E Bank Account 2', '100001', 'E2E00002', 0, 'e2e', NOW(), 'e2e', NOW());

-- ---- Contract Managers ----
INSERT INTO contract_manager (guid, contract_manager_id, first_name, last_name, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000030', 'E2E-CM-001', 'E2E-John', 'E2E-Smith', 0, 'e2e', NOW(), 'e2e', NOW()),
  ('e2e00000-0000-0000-0000-000000000031', 'E2E-CM-002', 'E2E-Jane', 'E2E-Doe', 0, 'e2e', NOW(), 'e2e', NOW());

-- ---- Liaison Managers ----
INSERT INTO liaison_manager (guid, first_name, last_name, email_address, telephone_number, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000040', 'E2E-Alice', 'E2E-Johnson', 'e2e-alice@test.example.com', '020 9999 0000', 0, 'e2e', NOW(), 'e2e', NOW()),
  ('e2e00000-0000-0000-0000-000000000041', 'E2E-Bob', 'E2E-Williams', 'e2e-bob@test.example.com', '0161 8888 0000', 0, 'e2e', NOW(), 'e2e', NOW());

-- ---- Provider-Office Links ----
-- LSP link (with full LSP-specific fields)
INSERT INTO provider_office_link (guid, account_number, provider_guid, office_guid, firm_type, head_office_flag, website, intervened_flag, vat_registration_number, payment_method, payment_held_flag, debt_recovery_flag, false_balance_flag, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000050', 'E2E-ACC-001', 'e2e00000-0000-0000-0000-000000000001', 'e2e00000-0000-0000-0000-000000000010', 'Legal Services Provider', true, 'https://e2e-lsp.example.com', false, 'GB-E2E-VAT-001', 'EFT', false, false, false, 0, 'e2e', NOW(), 'e2e', NOW());

-- Chambers link
INSERT INTO provider_office_link (guid, account_number, provider_guid, office_guid, firm_type, head_office_flag, website, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000051', 'E2E-ACC-002', 'e2e00000-0000-0000-0000-000000000002', 'e2e00000-0000-0000-0000-000000000011', 'Chambers', true, 'https://e2e-chambers.example.com', 0, 'e2e', NOW(), 'e2e', NOW());

-- Advocate link
INSERT INTO provider_office_link (guid, account_number, provider_guid, office_guid, firm_type, head_office_flag, intervened_flag, payment_method, payment_held_flag, debt_recovery_flag, false_balance_flag, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000052', 'E2E-ACC-003', 'e2e00000-0000-0000-0000-000000000003', 'e2e00000-0000-0000-0000-000000000011', 'Advocate', true, false, 'BACS', false, false, false, 0, 'e2e', NOW(), 'e2e', NOW());

-- ---- Provider-Bank Account Links ----
INSERT INTO provider_bank_account_link (guid, bank_account_guid, provider_guid, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000060', 'e2e00000-0000-0000-0000-000000000020', 'e2e00000-0000-0000-0000-000000000001', 0, 'e2e', NOW(), 'e2e', NOW());

-- ---- Provider Parent Links (Advocate -> Chambers) ----
INSERT INTO provider_parent_link (guid, provider_guid, parent_guid, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000070', 'e2e00000-0000-0000-0000-000000000003', 'e2e00000-0000-0000-0000-000000000002', 0, 'e2e', NOW(), 'e2e', NOW());

-- ---- Office-Bank Account Links ----
INSERT INTO office_bank_account_link (guid, bank_account_guid, provider_office_link_guid, primary_flag, active_date_from, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000080', 'e2e00000-0000-0000-0000-000000000020', 'e2e00000-0000-0000-0000-000000000050', true, CURRENT_DATE, 0, 'e2e', NOW(), 'e2e', NOW());

-- ---- Office-Contract Manager Links ----
INSERT INTO office_contract_manager_link (guid, contract_manager_guid, office_guid, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-000000000090', 'e2e00000-0000-0000-0000-000000000030', 'e2e00000-0000-0000-0000-000000000010', 0, 'e2e', NOW(), 'e2e', NOW());

-- ---- Office-Liaison Manager Links ----
INSERT INTO office_liaison_manager_link (guid, liaison_manager_guid, office_guid, active_date_from, linked_flag, version, created_by, created_timestamp, last_updated_by, last_updated_timestamp)
VALUES
  ('e2e00000-0000-0000-0000-0000000000a0', 'e2e00000-0000-0000-0000-000000000040', 'e2e00000-0000-0000-0000-000000000010', CURRENT_DATE, true, 0, 'e2e', NOW(), 'e2e', NOW());
