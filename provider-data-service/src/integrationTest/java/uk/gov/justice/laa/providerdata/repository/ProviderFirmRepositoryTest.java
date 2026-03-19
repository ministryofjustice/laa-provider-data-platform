package uk.gov.justice.laa.providerdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;

class ProviderFirmRepositoryTest extends PostgresqlSpringBootTest {

  @Autowired private ProviderRepository repository;

  @Test
  void save_and_findByFirmNumber_roundTrips() {
    ProviderEntity saved =
        repository.save(
            ProviderEntity.builder()
                .firmNumber("F12345")
                .firmType(FirmType.LEGAL_SERVICES_PROVIDER)
                .name("Test Firm")
                .build());

    assertThat(saved.getGuid()).isNotNull(); // GUID is assigned by JPA on save

    var reloaded = repository.findByFirmNumber("F12345");
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getName()).isEqualTo("Test Firm");
    assertThat(reloaded.get().getFirmType()).isEqualTo(FirmType.LEGAL_SERVICES_PROVIDER);
  }
}
