package uk.gov.justice.laa.providerdata.config;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
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
 * Startup seeding component for local development profile. Populates foundation tables with sample
 * data on application startup. Uses CommandLineRunner to ensure Hibernate schema creation is
 * complete before seeding.
 */
@Slf4j
@Component
@Profile("local")
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

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    // Give Hibernate time to create schema before seeding
    log.info("Starting local data seeding for foundation tables");

    seedBankAccounts();
    seedProviders();
    seedOffices();
    seedContractManagers();
    seedLiaisonManagers();
    seedProviderBankAccountLinks();
    seedProviderOfficeLinks();
    seedProviderParentLinks();
    seedOfficeBankAccountLinks();
    seedOfficeContractManagerLinks();
    seedOfficeLiaisonManagerLinks();

    log.info("Local data seeding completed successfully");
  }

  private void seedBankAccounts() {
    log.info("Seeding BankAccount table");

    BankAccountEntity account1 =
        BankAccountEntity.builder()
            .accountName("Test Bank Account 1")
            .sortCode("200000")
            .accountNumber("12345678")
            .build();

    BankAccountEntity account2 =
        BankAccountEntity.builder()
            .accountName("Test Bank Account 2")
            .sortCode("201000")
            .accountNumber("87654321")
            .build();

    bankAccountRepository.save(account1);
    bankAccountRepository.save(account2);
    log.info("Seeded {} BankAccount records", 2);
  }

  private void seedProviders() {
    log.info("Seeding Provider table");

    ProviderEntity provider1 =
        ProviderEntity.builder()
            .firmNumber("FRM001")
            .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
            .name("Test Legal Services Provider Ltd")
            .build();

    ProviderEntity provider2 =
        ProviderEntity.builder()
            .firmNumber("FRM002")
            .firmType(FirmType.CHAMBERS)
            .name("Test Chambers")
            .build();

    ProviderEntity provider3 =
        ProviderEntity.builder()
            .firmNumber("FRM003")
            .firmType(FirmType.ADVOCATE)
            .name("Test Advocate")
            .build();

    providerRepository.save(provider1);
    providerRepository.save(provider2);
    providerRepository.save(provider3);
    log.info("Seeded {} Provider records", 3);
  }

  private void seedOffices() {
    log.info("Seeding Office table");

    OfficeEntity office1 =
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
            .build();

    OfficeEntity office2 =
        OfficeEntity.builder()
            .addressLine1("456 Test Avenue")
            .addressTownOrCity("Manchester")
            .addressCounty("Manchester")
            .addressPostCode("M1 1AA")
            .telephoneNumber("0161 123 4567")
            .emailAddress("manchester@test.example.com")
            .build();

    OfficeEntity office3 =
        OfficeEntity.builder()
            .addressLine1("1 Barrister Court")
            .addressTownOrCity("London")
            .addressCounty("London")
            .addressPostCode("EC4A 1AA")
            .telephoneNumber("020 7000 1234")
            .emailAddress("advocate@test.example.com")
            .build();

    officeRepository.save(office1);
    officeRepository.save(office2);
    officeRepository.save(office3);
    log.info("Seeded {} Office records", 3);
  }

  private void seedContractManagers() {
    log.info("Seeding ContractManager table");

    ContractManagerEntity manager1 =
        ContractManagerEntity.builder()
            .contractManagerId("CM001")
            .firstName("John")
            .lastName("Smith")
            .build();

    ContractManagerEntity manager2 =
        ContractManagerEntity.builder()
            .contractManagerId("CM002")
            .firstName("Jane")
            .lastName("Doe")
            .build();

    contractManagerRepository.save(manager1);
    contractManagerRepository.save(manager2);
    log.info("Seeded {} ContractManager records", 2);
  }

  private void seedLiaisonManagers() {
    log.info("Seeding LiaisonManager table");

    LiaisonManagerEntity liaison1 =
        LiaisonManagerEntity.builder()
            .firstName("Alice")
            .lastName("Johnson")
            .emailAddress("alice@test.example.com")
            .telephoneNumber("020 9876 5432")
            .build();

    LiaisonManagerEntity liaison2 =
        LiaisonManagerEntity.builder()
            .firstName("Bob")
            .lastName("Williams")
            .emailAddress("bob@test.example.com")
            .telephoneNumber("0161 5432 1098")
            .build();

    liaisonManagerRepository.save(liaison1);
    liaisonManagerRepository.save(liaison2);
    log.info("Seeded {} LiaisonManager records", 2);
  }

  private void seedProviderBankAccountLinks() {
    log.info("Seeding ProviderBankAccountLink table");

    var providers = providerRepository.findAll();
    var bankAccounts = bankAccountRepository.findAll();

    if (!providers.isEmpty() && !bankAccounts.isEmpty()) {
      ProviderBankAccountLinkEntity link1 =
          ProviderBankAccountLinkEntity.builder()
              .provider(providers.get(0))
              .bankAccount(bankAccounts.get(0))
              .build();

      providerBankAccountLinkRepository.save(link1);
      log.info("Seeded {} ProviderBankAccountLink records", 1);
    }
  }

  private void seedProviderOfficeLinks() {
    log.info("Seeding ProviderOfficeLink table");

    var providers = providerRepository.findAll();
    var offices = officeRepository.findAll();

    if (providers.size() >= 3 && offices.size() >= 3) {
      // LSP Provider office link
      LspProviderOfficeLinkEntity lspLink =
          LspProviderOfficeLinkEntity.builder()
              .provider(providers.get(0))
              .office(offices.get(0))
              .accountNumber("ACC001")
              .headOfficeFlag(true)
              .website("https://example-lsp.com")
              .intervenedFlag(false)
              .vatRegistrationNumber("GB123456789")
              .paymentMethod("EFT")
              .paymentHeldFlag(false)
              .debtRecoveryFlag(false)
              .falseBalanceFlag(false)
              .build();

      // Chambers Provider office link
      ChamberProviderOfficeLinkEntity chamberLink =
          ChamberProviderOfficeLinkEntity.builder()
              .provider(providers.get(1))
              .office(offices.get(1))
              .accountNumber("ACC002")
              .headOfficeFlag(true)
              .website("https://example-chambers.com")
              .build();

      // Advocate Provider office link (uses the Chambers' office)
      AdvocateProviderOfficeLinkEntity advocateLink =
          AdvocateProviderOfficeLinkEntity.builder()
              .provider(providers.get(2))
              .office(offices.get(1))
              .accountNumber("ACC003")
              .headOfficeFlag(true)
              .intervenedFlag(false)
              .paymentMethod("BACS")
              .paymentHeldFlag(false)
              .debtRecoveryFlag(false)
              .falseBalanceFlag(false)
              .build();

      providerOfficeLinkRepository.save(lspLink);
      providerOfficeLinkRepository.save(chamberLink);
      providerOfficeLinkRepository.save(advocateLink);
      log.info("Seeded {} ProviderOfficeLink records", 3);
    }
  }

  private void seedProviderParentLinks() {
    log.info("Seeding ProviderParentLink table");

    var providers = providerRepository.findAll();

    if (providers.size() >= 3) {
      // Advocate (FRM003) has Chambers (FRM002) as its parent
      ProviderParentLinkEntity parentLink =
          ProviderParentLinkEntity.builder()
              .provider(providers.get(2))
              .parent(providers.get(1))
              .build();

      providerParentLinkRepository.save(parentLink);
      log.info("Seeded {} ProviderParentLink records", 1);
    }
  }

  private void seedOfficeBankAccountLinks() {
    log.info("Seeding OfficeBankAccountLink table");

    var providerOfficeLinks = providerOfficeLinkRepository.findAll();
    var bankAccounts = bankAccountRepository.findAll();

    if (!providerOfficeLinks.isEmpty() && !bankAccounts.isEmpty()) {
      OfficeBankAccountLinkEntity link =
          OfficeBankAccountLinkEntity.builder()
              .providerOfficeLink(providerOfficeLinks.get(0))
              .bankAccount(bankAccounts.get(0))
              .primaryFlag(true)
              .activeDateFrom(java.time.LocalDate.now())
              .build();

      officeBankAccountLinkRepository.save(link);
      log.info("Seeded {} OfficeBankAccountLink records", 1);
    }
  }

  private void seedOfficeContractManagerLinks() {
    log.info("Seeding OfficeContractManagerLink table");

    var offices = officeRepository.findAll();
    var contractManagers = contractManagerRepository.findAll();

    if (!offices.isEmpty() && !contractManagers.isEmpty()) {
      OfficeContractManagerLinkEntity link =
          OfficeContractManagerLinkEntity.builder()
              .office(offices.get(0))
              .contractManager(contractManagers.get(0))
              .build();

      officeContractManagerLinkRepository.save(link);
      log.info("Seeded {} OfficeContractManagerLink records", 1);
    }
  }

  private void seedOfficeLiaisonManagerLinks() {
    log.info("Seeding OfficeLiaisonManagerLink table");

    var offices = officeRepository.findAll();
    var liaisonManagers = liaisonManagerRepository.findAll();

    if (!offices.isEmpty() && !liaisonManagers.isEmpty()) {
      OfficeLiaisonManagerLinkEntity link =
          OfficeLiaisonManagerLinkEntity.builder()
              .office(offices.get(0))
              .liaisonManager(liaisonManagers.get(0))
              .activeDateFrom(LocalDate.now())
              .linkedFlag(true)
              .build();

      officeLiaisonManagerLinkRepository.save(link);
      log.info("Seeded {} OfficeLiaisonManagerLink records", 1);
    }
  }
}
