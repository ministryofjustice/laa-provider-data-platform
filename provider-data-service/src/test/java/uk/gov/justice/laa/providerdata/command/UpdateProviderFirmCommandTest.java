package uk.gov.justice.laa.providerdata.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

class UpdateProviderFirmCommandTest {

  @Test
  void validate_validCommand_doesNotThrow() {
    UpdateProviderFirmCommand command =
        new UpdateProviderFirmCommand(
            UUID.randomUUID().toString(), new ProviderPatchV2().name("Test"));

    // Should not throw
    command.validate();
  }

  @Test
  void validate_nullProviderId_throwsException() {
    UpdateProviderFirmCommand command =
        new UpdateProviderFirmCommand(null, new ProviderPatchV2().name("Test"));

    assertThatThrownBy(command::validate)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("providerFirmId must be provided");
  }

  @Test
  void validate_blankProviderId_throwsException() {
    UpdateProviderFirmCommand command =
        new UpdateProviderFirmCommand("   ", new ProviderPatchV2().name("Test"));

    assertThatThrownBy(command::validate)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("providerFirmId must be provided");
  }

  @Test
  void validate_nullPatch_throwsException() {
    UpdateProviderFirmCommand command =
        new UpdateProviderFirmCommand(UUID.randomUUID().toString(), null);

    assertThatThrownBy(command::validate)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("patch must be provided");
  }
}
