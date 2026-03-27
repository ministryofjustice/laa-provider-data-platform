package uk.gov.justice.laa.providerdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.service.OfficeContractManagerAssignmentService;

class OfficeContractManagerLinkRepositoryTest extends PostgresqlSpringBootTest {

  @Autowired private ContractManagerRepository contractManagerRepository;

  @Autowired private OfficeContractManagerLinkRepository linkRepository;

  @Autowired private ProviderRepository providerRepository;

  @Autowired private OfficeRepository officeRepository;

  @Autowired private ProviderOfficeLinkRepository providerOfficeLinkRepository;

  @Autowired private OfficeContractManagerAssignmentService service;

  @Test
  @Transactional
  void assign_byProviderFirmNumberAndOfficeCode_thenRepositoryCanQueryByOfficeGuid() {
    TestData testData = createTestData();

    var result = service.assign("FRM-CM-TEST", "ACC001", testData.contractManager().getGuid());

    assertAssignmentPersisted(testData, result.officeGuid());
  }

  @Test
  @Transactional
  void assign_byProviderGuidAndOfficeLinkGuid_thenRepositoryCanQueryByOfficeGuid() {
    TestData testData = createTestData();

    var result =
        service.assign(
            testData.provider().getGuid().toString(),
            testData.providerOfficeLink().getGuid().toString(),
            testData.contractManager().getGuid());

    assertAssignmentPersisted(testData, result.officeGuid());
  }

  private TestData createTestData() {
    ContractManagerEntity contractManager =
        ContractManagerEntity.builder()
            .contractManagerId("CM-001")
            .firstName("Pat")
            .lastName("Jones")
            .build();
    contractManager = contractManagerRepository.saveAndFlush(contractManager);

    ProviderEntity provider =
        providerRepository.save(
            ProviderEntity.builder()
                .firmNumber("FRM-CM-TEST")
                .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
                .name("Contract Manager Test Firm")
                .build());

    OfficeEntity office =
        officeRepository.save(
            OfficeEntity.builder()
                .addressLine1("1 Test Street")
                .addressTownOrCity("London")
                .addressPostCode("SW1A 1AA")
                .build());

    UUID officeGuid = office.getGuid();

    LspProviderOfficeLinkEntity providerOfficeLink =
        LspProviderOfficeLinkEntity.builder()
            .provider(provider)
            .office(office)
            .accountNumber("ACC001")
            .headOfficeFlag(true)
            .build();
    providerOfficeLink =
        (LspProviderOfficeLinkEntity) providerOfficeLinkRepository.save(providerOfficeLink);

    return new TestData(contractManager, provider, office, providerOfficeLink);
  }

  private void assertAssignmentPersisted(TestData testData, UUID returnedProviderOfficeLinkGuid) {
    assertThat(returnedProviderOfficeLinkGuid).isEqualTo(testData.providerOfficeLink().getGuid());
    var links = linkRepository.findByOfficeLink_Guid(testData.providerOfficeLink.getGuid());
    assertThat(links).hasSize(1);
    assertThat(links.get(0).getContractManager().getContractManagerId()).isEqualTo("CM-001");
  }

  private record TestData(
      ContractManagerEntity contractManager,
      ProviderEntity provider,
      OfficeEntity office,
      LspProviderOfficeLinkEntity providerOfficeLink) {}
}
