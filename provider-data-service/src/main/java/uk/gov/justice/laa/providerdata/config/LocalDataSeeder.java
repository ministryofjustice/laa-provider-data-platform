package uk.gov.justice.laa.providerdata.config;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.repository.BankAccountRepository;
import uk.gov.justice.laa.providerdata.repository.ContractManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeContractManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/**
 * Startup test data seeding component for local development and preview (pull request) profiles.
 * Populates foundation tables with sample data on application startup. Uses `CommandLineRunner` to
 * ensure Flyway DB schema creation is complete before inserting data.
 */
@Slf4j
@Component
@Profile("local | preview")
@RequiredArgsConstructor
public class LocalDataSeeder implements CommandLineRunner {
  private final ProviderRepository providerRepository;
  private final OfficeRepository officeRepository;
  private final BankAccountRepository bankAccountRepository;
  private final ContractManagerRepository contractManagerRepository;
  private final LiaisonManagerRepository liaisonManagerRepository;
  private final ProviderBankAccountLinkRepository providerBankAccountLinkRepository;
  private final OfficeBankAccountLinkRepository officeBankAccountLinkRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;
  private final OfficeContractManagerLinkRepository officeContractManagerLinkRepository;
  private final OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;

  // Firm numbers used as stable identifiers for test data.
  private static final String LSP_FIRM_NUMBER = "100001";
  private static final String CHAMBERS_FIRM_NUMBER = "100002";
  private static final String ADVOCATE_FIRM_NUMBER = "100003";
  private static final String CHAMBERS_DX_FIRM_NUMBER = "100004";

  @Override
  @Transactional
  public void run(String... args) {
    if (providerRepository.findByFirmNumber(LSP_FIRM_NUMBER).isPresent()) {
      log.info("Base test data already seeded");
      seedDxChambersIfMissing();
      return;
    }
    log.info("Starting local data seeding for foundation tables");
    var providers = seedProviders();
    seedProviderParentLinks(providers);
    var offices = seedOffices();
    var officeLinks = seedProviderOfficeLinks(providers, offices);
    var lspLink = (LspProviderOfficeLinkEntity) officeLinks[0];
    var chambersLink = (ChamberProviderOfficeLinkEntity) officeLinks[1];
    var bankAccounts = seedBankAccounts();
    seedProviderBankAccountLinks(providers, bankAccounts);
    seedOfficeBankAccountLinks(lspLink, bankAccounts);
    var liaisonManagers = seedLiaisonManagers();
    seedOfficeLiaisonManagerLinks(lspLink, chambersLink, liaisonManagers);
    var contractManagers = seedContractManagers();
    seedOfficeContractManagerLinks(lspLink, contractManagers);
    log.info("Local data seeding completed successfully");
    seedDxChambersIfMissing();
  }

  private void seedDxChambersIfMissing() {
    if (providerRepository.findByFirmNumber(CHAMBERS_DX_FIRM_NUMBER).isPresent()) {
      return;
    }
    log.info("Seeding DX Chambers (firmNumber {})", CHAMBERS_DX_FIRM_NUMBER);
    OfficeEntity dxOffice =
        officeRepository.save(
            OfficeEntity.builder()
                .addressLine1("789 DX Street")
                .addressTownOrCity("Birmingham")
                .addressPostCode("B2 2BB")
                .dxDetailsNumber("DX456")
                .dxDetailsCentre("Birmingham DX Centre")
                .build());
    var dxChambers =
        providerRepository.save(
            ChamberProviderEntity.builder()
                .firmNumber(CHAMBERS_DX_FIRM_NUMBER)
                .name("Test Chambers DX")
                .build());
    providerOfficeLinkRepository.save(
        ChamberProviderOfficeLinkEntity.builder()
            .provider(dxChambers)
            .office(dxOffice)
            .accountNumber("ACC004")
            .headOfficeFlag(true)
            .build());
    log.info("Seeded DX Chambers (firmNumber {})", CHAMBERS_DX_FIRM_NUMBER);
  }

  private BankAccountEntity[] seedBankAccounts() {
    log.info("Seeding BankAccount table");
    // composite unique constraint (SORT_CODE, ACCOUNT_NUMBER) and NOT NULL columns.
    BankAccountEntity account1 =
        bankAccountRepository.save(
            BankAccountEntity.builder()
                .accountName("Test Bank Account 1")
                .sortCode("200000")
                .accountNumber("12345678")
                .build());
    BankAccountEntity account2 =
        bankAccountRepository.save(
            BankAccountEntity.builder()
                .accountName("Test Bank Account 2")
                .sortCode("201000")
                .accountNumber("87654321")
                .build());
    log.info("Seeded {} BankAccount records", 2);
    return new BankAccountEntity[] {account1, account2};
  }

  private ProviderEntity[] seedProviders() {
    log.info("Seeding Provider table");
    // firmNumber NOT NULL UNIQUE, firmType NOT NULL, name NOT NULL.
    ProviderEntity provider1 =
        providerRepository.save(
            LspProviderEntity.builder()
                .firmNumber(LSP_FIRM_NUMBER)
                .name("Test Legal Services Provider Ltd")
                .constitutionalStatus("Partnership")
                .companiesHouseNumber("12345678")
                .indemnityReceivedDate(java.time.LocalDate.of(2020, 1, 1))
                .build());
    ProviderEntity provider2 =
        providerRepository.save(
            ChamberProviderEntity.builder()
                .firmNumber(CHAMBERS_FIRM_NUMBER)
                .name("Test Chambers")
                .build());
    ProviderEntity provider3 =
        providerRepository.save(
            AdvocatePractitionerEntity.builder()
                .firmNumber(ADVOCATE_FIRM_NUMBER)
                .name("Test Advocate")
                .build());
    log.info("Seeded {} Provider records", 3);
    return new ProviderEntity[] {provider1, provider2, provider3};
  }

  private OfficeEntity[] seedOffices() {
    log.info("Seeding Office table");
    // addressLine1 NOT NULL, addressTownOrCity NOT NULL, addressPostCode NOT NULL.
    OfficeEntity office1 =
        officeRepository.save(
            OfficeEntity.builder()
                .addressLine1("123 Test Street")
                .addressLine2("Suite 100")
                .addressTownOrCity("London")
                .addressCounty("London")
                .addressPostCode("SW1A 1AA")
                .telephoneNumber("020 1234 5678")
                .emailAddress("office@test.example.com")
                .dxDetailsNumber("DX123")
                .dxDetailsCentre("Test Centre")
                .build());
    OfficeEntity office2 =
        officeRepository.save(
            OfficeEntity.builder()
                .addressLine1("456 Test Avenue")
                .addressTownOrCity("Manchester")
                .addressCounty("Manchester")
                .addressPostCode("M1 1AA")
                .telephoneNumber("0161 123 4567")
                .emailAddress("manchester@test.example.com")
                .build());
    OfficeEntity office3 =
        officeRepository.save(
            OfficeEntity.builder()
                .addressLine1("1 Barrister Court")
                .addressTownOrCity("London")
                .addressCounty("London")
                .addressPostCode("EC4A 1AA")
                .telephoneNumber("020 7000 1234")
                .emailAddress("advocate@test.example.com")
                .build());
    log.info("Seeded {} Office records", 3);
    return new OfficeEntity[] {office1, office2, office3};
  }

  private ContractManagerEntity[] seedContractManagers() {
    log.info("Seeding ContractManager table");
    // contractManagerId NOT NULL UNIQUE, firstName NOT NULL, lastName NOT NULL.
    ContractManagerEntity manager1 =
        contractManagerRepository.save(
            ContractManagerEntity.builder()
                .contractManagerId("CM001")
                .firstName("John")
                .lastName("Smith")
                .build());
    ContractManagerEntity manager2 =
        contractManagerRepository.save(
            ContractManagerEntity.builder()
                .contractManagerId("CM002")
                .firstName("Jane")
                .lastName("Doe")
                .build());
    log.info("Seeded {} ContractManager records", 2);
    return new ContractManagerEntity[] {manager1, manager2};
  }

  private LiaisonManagerEntity[] seedLiaisonManagers() {
    log.info("Seeding LiaisonManager table");
    // firstName NOT NULL, lastName NOT NULL, emailAddress NOT NULL.
    LiaisonManagerEntity liaison1 =
        liaisonManagerRepository.save(
            LiaisonManagerEntity.builder()
                .firstName("Alice")
                .lastName("Johnson")
                .emailAddress("alice@test.example.com")
                .telephoneNumber("020 9876 5432")
                .build());
    LiaisonManagerEntity liaison2 =
        liaisonManagerRepository.save(
            LiaisonManagerEntity.builder()
                .firstName("Bob")
                .lastName("Williams")
                .emailAddress("bob@test.example.com")
                .telephoneNumber("0161 5432 1098")
                .build());
    log.info("Seeded {} LiaisonManager records", 2);
    return new LiaisonManagerEntity[] {liaison1, liaison2};
  }

  private void seedProviderBankAccountLinks(
      ProviderEntity[] providers, BankAccountEntity[] bankAccounts) {
    log.info("Seeding ProviderBankAccountLink table");
    // composite unique constraint (PROVIDER_GUID, BANK_ACCOUNT_GUID).
    providerBankAccountLinkRepository.save(
        ProviderBankAccountLinkEntity.builder()
            .provider(providers[0])
            .bankAccount(bankAccounts[0])
            .build());
    log.info("Seeded {} ProviderBankAccountLink records", 1);
  }

  private ProviderOfficeLinkEntity[] seedProviderOfficeLinks(
      ProviderEntity[] providers, OfficeEntity[] offices) {
    log.info("Seeding ProviderOfficeLink table");
    // composite unique constraint (PROVIDER_GUID, OFFICE_GUID, FIRM_TYPE).
    // accountNumber NOT NULL, headOfficeFlag NOT NULL,
    // LSP/Advocate: flags NOT NULL, paymentMethod NOT NULL.
    ChamberProviderOfficeLinkEntity chambersLink =
        (ChamberProviderOfficeLinkEntity)
            providerOfficeLinkRepository.save(
                ChamberProviderOfficeLinkEntity.builder()
                    .provider(providers[1])
                    .office(offices[1])
                    .accountNumber("ACC002")
                    .headOfficeFlag(true)
                    .website("https://example-chambers.com")
                    .build());
    // Advocate office link uses the Chambers office.
    providerOfficeLinkRepository.save(
        AdvocateProviderOfficeLinkEntity.builder()
            .provider(providers[2])
            .office(offices[1])
            .accountNumber("ACC003")
            .headOfficeFlag(true)
            .intervenedFlag(false)
            .paymentMethod("BACS")
            .paymentHeldFlag(false)
            .debtRecoveryFlag(false)
            .falseBalanceFlag(false)
            .build());
    LspProviderOfficeLinkEntity lspLink =
        (LspProviderOfficeLinkEntity)
            providerOfficeLinkRepository.save(
                LspProviderOfficeLinkEntity.builder()
                    .provider(providers[0])
                    .office(offices[0])
                    .accountNumber("ACC001")
                    .headOfficeFlag(true)
                    .website("https://example-lsp.com")
                    .intervenedFlag(false)
                    .vatRegistrationNumber("GB123456789")
                    .paymentMethod("EFT")
                    .paymentHeldFlag(false)
                    .debtRecoveryFlag(false)
                    .falseBalanceFlag(false)
                    .build());
    log.info("Seeded {} ProviderOfficeLink records", 3);
    return new ProviderOfficeLinkEntity[] {lspLink, chambersLink};
  }

  private void seedProviderParentLinks(ProviderEntity[] providers) {
    log.info("Seeding ProviderParentLink table");
    // composite unique constraint (PROVIDER_GUID, PARENT_GUID).
    // Advocate (100003) has Chambers (100002) as its parent.
    providerParentLinkRepository.save(
        ProviderParentLinkEntity.builder().provider(providers[2]).parent(providers[1]).build());
    log.info("Seeded {} ProviderParentLink records", 1);
  }

  private void seedOfficeBankAccountLinks(
      LspProviderOfficeLinkEntity lspLink, BankAccountEntity[] bankAccounts) {
    log.info("Seeding OfficeBankAccountLink table");
    // composite unique constraint (PROVIDER_OFFICE_LINK_GUID, BANK_ACCOUNT_GUID).
    // primaryFlag NOT NULL, activeDateFrom NOT NULL.
    officeBankAccountLinkRepository.save(
        OfficeBankAccountLinkEntity.builder()
            .providerOfficeLink(lspLink)
            .bankAccount(bankAccounts[0])
            .primaryFlag(true)
            .activeDateFrom(LocalDate.now())
            .build());
    log.info("Seeded {} OfficeBankAccountLink records", 1);
  }

  private void seedOfficeContractManagerLinks(
      LspProviderOfficeLinkEntity lspLink, ContractManagerEntity[] contractManagers) {
    log.info("Seeding OfficeContractManagerLink table");
    officeContractManagerLinkRepository.save(
        OfficeContractManagerLinkEntity.builder()
            .officeLink(lspLink)
            .contractManager(contractManagers[0])
            .build());
    log.info("Seeded {} OfficeContractManagerLink records", 1);
  }

  private void seedOfficeLiaisonManagerLinks(
      LspProviderOfficeLinkEntity lspLink,
      ChamberProviderOfficeLinkEntity chambersLink,
      LiaisonManagerEntity[] liaisonManagers) {
    log.info("Seeding OfficeLiaisonManagerLink table");
    officeLiaisonManagerLinkRepository.save(
        OfficeLiaisonManagerLinkEntity.builder()
            .officeLink(lspLink)
            .liaisonManager(liaisonManagers[0])
            .activeDateFrom(LocalDate.now())
            .linkedFlag(true)
            .build());
    officeLiaisonManagerLinkRepository.save(
        OfficeLiaisonManagerLinkEntity.builder()
            .officeLink(chambersLink)
            .liaisonManager(liaisonManagers[1])
            .activeDateFrom(LocalDate.now())
            .linkedFlag(false)
            .build());
    log.info("Seeded {} OfficeLiaisonManagerLink records", 2);
  }
}
