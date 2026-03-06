package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.providerdata.api.model.LiaisonManagerCreate;
import uk.gov.justice.laa.providerdata.api.model.OfficeLiaisonManagerPostRequest;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@SpringBootTest
@ActiveProfiles("test")
class OfficeLiaisonManagerServiceTest {

  @Autowired private OfficeLiaisonManagerService service;

  @Autowired private ProviderRepository providerRepository;
  @Autowired private OfficeRepository officeRepository;
  @Autowired private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Autowired private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;

  @Test
  void post_createsLiaisonManager_and_linksToOffice_byOfficeCode() {
    final OffsetDateTime now = OffsetDateTime.now(); // only used for link/test data below

    // Build ONLY subclass fields; JPA generates GUID, auditing sets audit fields.
    ProviderEntity provider =
        ProviderEntity.builder()
            .firmNumber("FRM100")
            .firmType("Legal Services Provider")
            .name("Test Firm")
            .build();
    provider = providerRepository.save(provider);

    OfficeEntity office =
        OfficeEntity.builder()
            .addressLine1("1 Test Street")
            .addressTownOrCity("London")
            .addressPostCode("SW1A 1AA")
            .build();
    office = officeRepository.save(office);

    LspProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();
    // If GUID is generated in that entity too, do NOT set it—otherwise set it here.
    link.setProvider(provider);
    link.setOffice(office);
    link.setAccountNumber("0Q731M");
    link.setFirmType("Legal Services Provider");
    link.setHeadOfficeFlag(true);
    // Optionally set explicit timestamps if your link entity doesn’t audit:
    link.setCreatedBy("test");
    link.setCreatedTimestamp(now);
    link.setLastUpdatedBy("test");
    link.setLastUpdatedTimestamp(now);
    providerOfficeLinkRepository.save(link);

    var request =
        new OfficeLiaisonManagerPostRequest(
            new LiaisonManagerCreate("Alice", "Jones", "alice@example.com", "0123456789"),
            null,
            null);

    var result = service.postOfficeLiaisonManager("FRM100", "0Q731M", request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getFirstName()).isEqualTo("Alice");
    assertThat(result.get(0).getEmailAddress()).isEqualTo("alice@example.com");

    var persistedLinks = officeLiaisonManagerLinkRepository.findByOffice_Guid(office.getGuid());
    assertThat(persistedLinks).isNotEmpty();
  }
}
