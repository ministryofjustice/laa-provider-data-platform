package uk.gov.justice.laa.providerdata;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountCommandService;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountEntity;
import uk.gov.justice.laa.providerdata.contractmanager.ContractManagerCommandService;
import uk.gov.justice.laa.providerdata.contractmanager.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.liaisonmanager.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerCommandService;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.office.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.OfficeCommandService;
import uk.gov.justice.laa.providerdata.office.OfficeEntity;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.provider.ChamberProviderEntity;
import uk.gov.justice.laa.providerdata.provider.LspProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderCommandService;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;

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
  private final ProviderCommandService providerPersistService;
  private final ProviderQueryService providerQueryService;
  private final OfficeCommandService officeCommandService;
  private final BankAccountCommandService bankAccountCommandService;
  private final ContractManagerCommandService contractManagerCommandService;
  private final OfficeLiaisonManagerCommandService officeLiaisonManagerCommandService;

  // Firm numbers used as stable identifiers for test data.
  private static final String LSP_FIRM_NUMBER = "100001";
  private static final String CHAMBERS_FIRM_NUMBER = "100002";
  private static final String ADVOCATE_FIRM_NUMBER = "100003";
  private static final String CHAMBERS_DX_FIRM_NUMBER = "100004";

  @Override
  @Transactional
  public void run(String... args) {
    if (providerQueryService.existsByFirmNumber(LSP_FIRM_NUMBER)) {
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
    seedBankAccountData(providers[0], lspLink);
    seedLiaisonManagerData(lspLink, chambersLink);
    var contractManagers = seedContractManagers();
    seedOfficeContractManagerLinks(lspLink, contractManagers);
    log.info("Local data seeding completed successfully");
    seedDxChambersIfMissing();
  }

  private void seedDxChambersIfMissing() {
    if (providerQueryService.existsByFirmNumber(CHAMBERS_DX_FIRM_NUMBER)) {
      return;
    }
    log.info("Seeding DX Chambers (firmNumber {})", CHAMBERS_DX_FIRM_NUMBER);
    OfficeEntity dxOffice =
        officeCommandService.save(
            OfficeEntity.builder()
                .addressLine1("789 DX Street")
                .addressTownOrCity("Birmingham")
                .addressPostCode("B2 2BB")
                .dxDetailsNumber("DX456")
                .dxDetailsCentre("Birmingham DX Centre")
                .build());
    var dxChambers =
        providerPersistService.save(
            ChamberProviderEntity.builder()
                .firmNumber(CHAMBERS_DX_FIRM_NUMBER)
                .name("Test Chambers DX")
                .build());
    officeCommandService.saveProviderOfficeLink(
        ChamberProviderOfficeLinkEntity.builder()
            .provider(dxChambers)
            .office(dxOffice)
            .accountNumber("ACC004")
            .headOfficeFlag(true)
            .build());
    log.info("Seeded DX Chambers (firmNumber {})", CHAMBERS_DX_FIRM_NUMBER);
  }

  private void seedBankAccountData(ProviderEntity provider, LspProviderOfficeLinkEntity lspLink) {
    log.info("Seeding BankAccount, ProviderBankAccountLink, OfficeBankAccountLink tables");
    BankAccountEntity template =
        BankAccountEntity.builder()
            .accountName("Test Bank Account 1")
            .sortCode("200000")
            .accountNumber("12345678")
            .build();
    bankAccountCommandService.createAndLink(template, provider, lspLink, LocalDate.now());
    log.info("Seeded BankAccount, ProviderBankAccountLink, OfficeBankAccountLink records");
  }

  private ProviderEntity[] seedProviders() {
    log.info("Seeding Provider table");
    // firmNumber NOT NULL UNIQUE, firmType NOT NULL, name NOT NULL.
    ProviderEntity provider1 =
        providerPersistService.save(
            LspProviderEntity.builder()
                .firmNumber(LSP_FIRM_NUMBER)
                .name("Test Legal Services Provider Ltd")
                .build());
    ProviderEntity provider2 =
        providerPersistService.save(
            ChamberProviderEntity.builder()
                .firmNumber(CHAMBERS_FIRM_NUMBER)
                .name("Test Chambers")
                .build());
    ProviderEntity provider3 =
        providerPersistService.save(
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
        officeCommandService.save(
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
        officeCommandService.save(
            OfficeEntity.builder()
                .addressLine1("456 Test Avenue")
                .addressTownOrCity("Manchester")
                .addressCounty("Manchester")
                .addressPostCode("M1 1AA")
                .telephoneNumber("0161 123 4567")
                .emailAddress("manchester@test.example.com")
                .build());
    OfficeEntity office3 =
        officeCommandService.save(
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
        contractManagerCommandService.save(
            ContractManagerEntity.builder()
                .contractManagerId("CM001")
                .firstName("John")
                .lastName("Smith")
                .build());
    ContractManagerEntity manager2 =
        contractManagerCommandService.save(
            ContractManagerEntity.builder()
                .contractManagerId("CM002")
                .firstName("Jane")
                .lastName("Doe")
                .build());
    log.info("Seeded {} ContractManager records", 2);
    return new ContractManagerEntity[] {manager1, manager2};
  }

  private void seedLiaisonManagerData(
      LspProviderOfficeLinkEntity lspLink, ChamberProviderOfficeLinkEntity chambersLink) {
    log.info("Seeding LiaisonManager and OfficeLiaisonManagerLink tables");
    officeLiaisonManagerCommandService.createAndLink(
        LiaisonManagerEntity.builder()
            .firstName("Alice")
            .lastName("Johnson")
            .emailAddress("alice@test.example.com")
            .telephoneNumber("020 9876 5432")
            .build(),
        OfficeLiaisonManagerLinkEntity.builder()
            .officeLink(lspLink)
            .activeDateFrom(LocalDate.now())
            .linkedFlag(true)
            .build());
    officeLiaisonManagerCommandService.createAndLink(
        LiaisonManagerEntity.builder()
            .firstName("Bob")
            .lastName("Williams")
            .emailAddress("bob@test.example.com")
            .telephoneNumber("0161 5432 1098")
            .build(),
        OfficeLiaisonManagerLinkEntity.builder()
            .officeLink(chambersLink)
            .activeDateFrom(LocalDate.now())
            .linkedFlag(false)
            .build());
    log.info("Seeded LiaisonManager and OfficeLiaisonManagerLink records");
  }

  private ProviderOfficeLinkEntity[] seedProviderOfficeLinks(
      ProviderEntity[] providers, OfficeEntity[] offices) {
    log.info("Seeding ProviderOfficeLink table");
    // composite unique constraint (PROVIDER_GUID, OFFICE_GUID, FIRM_TYPE).
    // accountNumber NOT NULL, headOfficeFlag NOT NULL,
    // LSP/Advocate: flags NOT NULL, paymentMethod NOT NULL.
    ChamberProviderOfficeLinkEntity chambersLink =
        (ChamberProviderOfficeLinkEntity)
            officeCommandService.saveProviderOfficeLink(
                ChamberProviderOfficeLinkEntity.builder()
                    .provider(providers[1])
                    .office(offices[1])
                    .accountNumber("ACC002")
                    .headOfficeFlag(true)
                    .website("https://example-chambers.com")
                    .build());
    // Advocate office link uses the Chambers office.
    officeCommandService.saveProviderOfficeLink(
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
            officeCommandService.saveProviderOfficeLink(
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
    providerPersistService.saveParentLink(
        ProviderParentLinkEntity.builder().provider(providers[2]).parent(providers[1]).build());
    log.info("Seeded {} ProviderParentLink records", 1);
  }

  private void seedOfficeContractManagerLinks(
      LspProviderOfficeLinkEntity lspLink, ContractManagerEntity[] contractManagers) {
    log.info("Seeding OfficeContractManagerLink table");
    contractManagerCommandService.linkToOffice(lspLink, contractManagers[0]);
    log.info("Seeded {} OfficeContractManagerLink records", 1);
  }
}
