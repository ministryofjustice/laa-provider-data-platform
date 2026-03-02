package uk.gov.justice.laa.providerdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.providerdata.entity.ProviderFirmEntity;

@SpringBootTest
@ActiveProfiles("test")
class ProviderFirmRepositoryTest {

  @Autowired private ProviderFirmRepository repository;

  @Test
  void save_and_findByFirmNumber_roundTrips() {
    UUID guid = UUID.randomUUID();

    ProviderFirmEntity saved =
        repository.save(
            ProviderFirmEntity.builder()
                .guid(guid)
                .version(1L)
                .createdBy("test.user")
                .createdTimestamp(OffsetDateTime.now())
                .lastUpdatedBy("test.user")
                .lastUpdatedTimestamp(OffsetDateTime.now())
                .firmNumber("F12345")
                .firmType("Legal Services Provider")
                .name("Test Firm")
                .build());

    assertThat(saved.getGuid()).isEqualTo(guid);

    var reloaded = repository.findByFirmNumber("F12345");
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getName()).isEqualTo("Test Firm");
    assertThat(reloaded.get().getFirmType()).isEqualTo("Legal Services Provider");
  }
}
