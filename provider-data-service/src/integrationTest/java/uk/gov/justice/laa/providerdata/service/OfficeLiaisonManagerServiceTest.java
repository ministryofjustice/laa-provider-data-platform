package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.FirmType;
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
        ProviderEntity.builder()
            .firmNumber("FRM100")
            .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
            .name("Test Firm")
            .build();
    provider = providerRepository.save(provider);
    final var providerGuid = provider.getGuid();

    OfficeEntity office =
        OfficeEntity.builder()
            .addressLine1("1 Test Street")
            .addressTownOrCity("London")
            .addressPostCode("SW1A 1AA")
            .build();
    office = officeRepository.save(office);
    final var officeGuid = office.getGuid();

    LspProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();
    link.setProvider(provider);
    link.setOffice(office);
    link.setAccountNumber("0Q731M");
    link.setFirmType("Legal Services Provider");
    link.setHeadOfficeFlag(true);
    link.setCreatedBy("test");
    link.setCreatedTimestamp(now);
    link.setLastUpdatedBy("test");
    link.setLastUpdatedTimestamp(now);
    providerOfficeLinkRepository.save(link);

    LiaisonManagerCreateV2 request = new LiaisonManagerCreateV2();
    request.setFirstName("Alice");
    request.setLastName("Jones");
    request.setEmailAddress("alice@example.com");
    request.setTelephoneNumber("0123456789");

    var result = service.postOfficeLiaisonManager("FRM100", "0Q731M", request);

    assertThat(result).isNotNull();
    assertThat(result.providerFirmGuid()).isEqualTo(providerGuid);
    assertThat(result.providerFirmNumber()).isEqualTo("FRM100");
    assertThat(result.officeGuid()).isEqualTo(officeGuid);
    assertThat(result.officeCode()).isEqualTo("0Q731M");
    assertThat(result.liaisonManagerGuid()).isNotNull();

    var persistedLinks = officeLiaisonManagerLinkRepository.findByOffice_Guid(officeGuid);
    assertThat(persistedLinks).isNotEmpty();
    assertThat(persistedLinks)
        .anySatisfy(
            persisted -> {
              assertThat(persisted.getOffice().getGuid()).isEqualTo(officeGuid);
              assertThat(persisted.getLiaisonManager().getGuid())
                  .isEqualTo(result.liaisonManagerGuid());
              assertThat(persisted.getActiveDateTo()).isNull();
              assertThat(persisted.getLinkedFlag()).isTrue();
            });
  }
}
