package uk.gov.justice.laa.providerdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;

@Transactional
class LspProviderOfficeLinkRepositoryTest extends PostgresqlSpringBootTest {

  @Autowired private ProviderRepository providerRepository;
  @Autowired private OfficeRepository officeRepository;
  @Autowired private LspProviderOfficeLinkRepository lspRepository;
  @Autowired private ProviderOfficeLinkRepository providerOfficeLinkRepository;

  private ProviderEntity provider;
  private OfficeEntity lspOffice;
  private OfficeEntity nonLspOffice;
  private LspProviderOfficeLinkEntity savedLspLink;

  @BeforeEach
  void setUp() {
    provider =
        providerRepository.save(
            ProviderEntity.builder()
                .firmNumber("FRM-LSP-REPO-TEST")
                .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
                .name("LSP Repo Test Firm")
                .build());

    lspOffice =
        officeRepository.save(
            OfficeEntity.builder()
                .addressLine1("1 LSP Street")
                .addressTownOrCity("London")
                .addressPostCode("EC1A 1AA")
                .build());

    nonLspOffice =
        officeRepository.save(
            OfficeEntity.builder()
                .addressLine1("2 Non-LSP Street")
                .addressTownOrCity("London")
                .addressPostCode("EC1A 1BB")
                .build());

    LspProviderOfficeLinkEntity lspLink = new LspProviderOfficeLinkEntity();
    lspLink.setProvider(provider);
    lspLink.setOffice(lspOffice);
    lspLink.setAccountNumber("LSP-001");
    lspLink.setHeadOfficeFlag(true);
    savedLspLink = lspRepository.save(lspLink);

    ProviderOfficeLinkEntity nonLspLink =
        ProviderOfficeLinkEntity.builder()
            .provider(provider)
            .office(nonLspOffice)
            .accountNumber("NLSP-001")
            .headOfficeFlag(false)
            .build();
    providerOfficeLinkRepository.save(nonLspLink);
  }

  @Test
  void findByProvider_returnsOnlyLspLinks() {
    Page<LspProviderOfficeLinkEntity> page =
        lspRepository.findByProvider(provider, PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent().getFirst().getAccountNumber()).isEqualTo("LSP-001");
  }

  @Test
  void findByProviderAndGuid_returnsLspLink() {
    Optional<LspProviderOfficeLinkEntity> result =
        lspRepository.findByProviderAndGuid(provider, savedLspLink.getGuid());

    assertThat(result).isPresent();
    assertThat(result.get().getAccountNumber()).isEqualTo("LSP-001");
  }

  @Test
  void findByProviderAndOffice_Guid_returnsLspLink() {
    Optional<LspProviderOfficeLinkEntity> result =
        lspRepository.findByProviderAndOffice_Guid(provider, lspOffice.getGuid());

    assertThat(result).isPresent();
    assertThat(result.get().getAccountNumber()).isEqualTo("LSP-001");
  }

  @Test
  void findByProviderAndOffice_Guid_emptyForNonLspLink() {
    Optional<LspProviderOfficeLinkEntity> result =
        lspRepository.findByProviderAndOffice_Guid(provider, nonLspOffice.getGuid());

    assertThat(result).isEmpty();
  }

  @Test
  void findByProviderAndAccountNumber_returnsLspLink() {
    Optional<LspProviderOfficeLinkEntity> result =
        lspRepository.findByProviderAndAccountNumber(provider, "LSP-001");

    assertThat(result).isPresent();
    assertThat(result.get().getOffice().getGuid()).isEqualTo(lspOffice.getGuid());
  }

  @Test
  void findByProviderAndAccountNumber_emptyForNonLspAccountNumber() {
    Optional<LspProviderOfficeLinkEntity> result =
        lspRepository.findByProviderAndAccountNumber(provider, "NLSP-001");

    assertThat(result).isEmpty();
  }
}
