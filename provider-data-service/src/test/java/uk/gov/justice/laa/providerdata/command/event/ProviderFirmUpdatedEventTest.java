package uk.gov.justice.laa.providerdata.command.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

class ProviderFirmUpdatedEventTest {

  @Test
  void of_populatesAllFields() {
    UUID guid = UUID.randomUUID();
    ProviderPatchV2 patch = new ProviderPatchV2().name("Test");

    ProviderFirmUpdatedEvent event = ProviderFirmUpdatedEvent.of(guid, "100001", patch);

    assertThat(event.providerFirmGUID()).isEqualTo(guid);
    assertThat(event.firmNumber()).isEqualTo("100001");
    assertThat(event.commandType()).isEqualTo("UpdateProviderFirm");
    assertThat(event.patch()).isSameAs(patch);
    assertThat(event.occurredAt()).isNotNull();
  }
}
