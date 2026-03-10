package uk.gov.justice.laa.providerdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;

@SpringBootTest
@ActiveProfiles("test")
class ProviderOfficeLinkRepositoryTest {

  @Autowired private ProviderRepository providerRepository;
  @Autowired private OfficeRepository officeRepository;
  @Autowired private ProviderOfficeLinkRepository repository;

  private ProviderEntity provider;
  private OfficeEntity office;
  private LspProviderOfficeLinkEntity link;

  @BeforeEach
  void setUp() {
    provider =
        providerRepository.save(
            ProviderEntity.builder()
                .firmNumber("FRM-LINK-TEST")
                .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
                .name("Link Test Firm")
                .build());

    office =
        officeRepository.save(
            OfficeEntity.builder()
                .addressLine1("1 Link Street")
                .addressTownOrCity("London")
                .addressPostCode("EC1A 1BB")
                .build());

    link = new LspProviderOfficeLinkEntity();
    link.setProvider(provider);
    link.setOffice(office);
    link.setAccountNumber("LNK001");
    link.setFirmType("Legal Services Provider");
    link = (LspProviderOfficeLinkEntity) repository.save(link);
  }

  @Test
  void findByProvider_returnsSavedLink() {
    Page<ProviderOfficeLinkEntity> page =
        repository.findByProvider(provider, PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent().getFirst().getAccountNumber()).isEqualTo("LNK001");
  }

  @Test
  void findByProviderAndOffice_Guid_returnsLink() {
    Optional<ProviderOfficeLinkEntity> result =
        repository.findByProviderAndOffice_Guid(provider, office.getGuid());

    assertThat(result).isPresent();
    assertThat(result.get().getAccountNumber()).isEqualTo("LNK001");
  }

  @Test
  void findByProviderAndAccountNumber_returnsLink() {
    Optional<ProviderOfficeLinkEntity> result =
        repository.findByProviderAndAccountNumber(provider, "LNK001");

    assertThat(result).isPresent();
    assertThat(result.get().getOffice().getGuid()).isEqualTo(office.getGuid());
  }

  @Test
  void findByProvider_returnsEmpty_forDifferentProvider() {
    ProviderEntity other =
        providerRepository.save(
            ProviderEntity.builder()
                .firmNumber("FRM-OTHER")
                .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
                .name("Other Firm")
                .build());

    Page<ProviderOfficeLinkEntity> page = repository.findByProvider(other, PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isZero();
  }
}
