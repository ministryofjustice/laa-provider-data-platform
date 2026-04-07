package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

class OfficeLiaisonManagerServiceTest extends PostgresqlSpringBootTest {

  @Autowired private OfficeLiaisonManagerService service;

  @Autowired private ProviderRepository providerRepository;
  @Autowired private OfficeRepository officeRepository;
  @Autowired private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Autowired private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;

  @Test
  void post_createsLiaisonManager_and_linksToOffice_byOfficeCode() {
    final OffsetDateTime now = OffsetDateTime.now(); // only used for test data below

    ProviderEntity provider =
        LspProviderEntity.builder().firmNumber("FRM100").name("Test Firm").build();
    provider = providerRepository.save(provider);
    final var providerGuid = provider.getGuid();

    OfficeEntity office =
        OfficeEntity.builder()
            .addressLine1("1 Test Street")
            .addressTownOrCity("London")
            .addressPostCode("SW1A 1AA")
            .build();
    office = officeRepository.save(office);

    LspProviderOfficeLinkEntity link =
        LspProviderOfficeLinkEntity.builder()
            .provider(provider)
            .office(office)
            .accountNumber("0Q731M")
            .headOfficeFlag(true)
            .createdBy("test")
            .createdTimestamp(now)
            .lastUpdatedBy("test")
            .lastUpdatedTimestamp(now)
            .build();
    link = providerOfficeLinkRepository.save(link);
    final var officeLinkGuid = link.getGuid();

    LiaisonManagerCreateV2 request = new LiaisonManagerCreateV2();
    request.setFirstName("Alice");
    request.setLastName("Jones");
    request.setEmailAddress("alice@example.com");
    request.setTelephoneNumber("0123456789");

    var result = service.postOfficeLiaisonManager("FRM100", "0Q731M", request);

    assertThat(result).isNotNull();
    assertThat(result.providerFirmGuid()).isEqualTo(providerGuid);
    assertThat(result.providerFirmNumber()).isEqualTo("FRM100");
    assertThat(result.officeGuid()).isEqualTo(officeLinkGuid);
    assertThat(result.officeCode()).isEqualTo("0Q731M");
    assertThat(result.liaisonManagerGuid()).isNotNull();

    var persistedLinks = officeLiaisonManagerLinkRepository.findByOfficeLink_Guid(officeLinkGuid);
    assertThat(persistedLinks).isNotEmpty();
    assertThat(persistedLinks)
        .anySatisfy(
            persisted -> {
              assertThat(persisted.getOfficeLink().getGuid()).isEqualTo(officeLinkGuid);
              assertThat(persisted.getLiaisonManager().getGuid())
                  .isEqualTo(result.liaisonManagerGuid());
              assertThat(persisted.getActiveDateTo()).isNull();
              assertThat(persisted.getLinkedFlag()).isTrue();
            });
  }
}
