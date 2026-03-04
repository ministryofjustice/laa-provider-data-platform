package uk.gov.justice.laa.providerdata.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.api.model.ProviderCreateBase;
import uk.gov.justice.laa.providerdata.api.model.ProviderCreateLsp;

class ProviderFirmCreateMapperTest {

  @Test
  void fromLsp_setsFirmTypeAndName() {
    var req = new ProviderCreateLsp(new ProviderCreateBase("PF999", "My LSP"));

    var entity = ProviderFirmCreateMapper.fromLsp(req, "tester");

    assertThat(entity.getFirmType()).isEqualTo("Legal Services Provider");
    assertThat(entity.getFirmNumber()).isEqualTo("PF999");
    assertThat(entity.getName()).isEqualTo("My LSP");
    assertThat(entity.getGuid()).isNotNull();
  }
}
