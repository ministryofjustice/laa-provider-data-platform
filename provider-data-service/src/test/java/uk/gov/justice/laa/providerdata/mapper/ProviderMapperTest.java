package uk.gov.justice.laa.providerdata.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;

@SpringBootTest
class ProviderMapperTest {

  @Autowired private ProviderMapper mapper;

  @Test
  void toProviderV2_mapsBasicFields() {
    UUID guid = UUID.randomUUID();
    ProviderEntity entity =
        ProviderEntity.builder()
            .firmNumber("LSP-ABC123")
            .firmType("Legal Services Provider")
            .name("Westgate Legal Services LLP")
            .build();
    entity.setGuid(guid);
    entity.setVersion(3L);
    entity.setCreatedBy("user1");

    ProviderV2 result = mapper.toProviderV2(entity);

    assertThat(result.getGuid()).isEqualTo(guid.toString());
    assertThat(result.getVersion()).isEqualByComparingTo("3");
    assertThat(result.getFirmNumber()).isEqualTo("LSP-ABC123");
    assertThat(result.getFirmType()).isEqualTo(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER);
    assertThat(result.getName()).isEqualTo("Westgate Legal Services LLP");
    assertThat(result.getCreatedBy()).isEqualTo("user1");
    assertThat(result.getLegalServicesProvider()).isNull();
    assertThat(result.getChambers()).isNull();
    assertThat(result.getPractitioner()).isNull();
  }

  @Test
  void toProviderV2_nullFirmType_returnsNullFirmType() {
    ProviderEntity entity = ProviderEntity.builder().firmNumber("X").name("Y").build();
    entity.setGuid(UUID.randomUUID());

    ProviderV2 result = mapper.toProviderV2(entity);

    assertThat(result.getFirmType()).isNull();
  }

  @Test
  void toProviderV2_nullVersion_returnsNullVersion() {
    ProviderEntity entity = ProviderEntity.builder().firmNumber("X").name("Y").build();
    entity.setGuid(UUID.randomUUID());

    ProviderV2 result = mapper.toProviderV2(entity);

    assertThat(result.getVersion()).isNull();
  }
}
