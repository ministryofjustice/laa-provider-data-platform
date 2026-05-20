package uk.gov.justice.laa.providerdata.application.providerfirm.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.application.providerfirm.port.out.ProviderFirmPatchPort;
import uk.gov.justice.laa.providerdata.application.providerfirm.port.out.ProviderFirmUpdatedOutboxPort;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;

@ExtendWith(MockitoExtension.class)
class DefaultUpdateProviderFirmUseCaseTest {

  @Mock private ProviderFirmPatchPort providerFirmPatchPort;
  @Mock private ProviderFirmUpdatedOutboxPort providerFirmUpdatedOutboxPort;

  @InjectMocks private DefaultUpdateProviderFirmUseCase useCase;

  @Test
  void execute_validCommand_dispatchesToPatchPortAndReturnsResult() {
    UUID providerGuid = UUID.randomUUID();
    String providerId = providerGuid.toString();
    ProviderPatchV2 patch = new ProviderPatchV2().name("Updated Name");
    ProviderCreationResult expectedResult =
        ProviderCreationResult.withoutOffice(providerGuid, "100001");

    when(providerFirmPatchPort.patchProvider(providerId, patch)).thenReturn(expectedResult);

    UpdateProviderFirmCommand command = new UpdateProviderFirmCommand(providerId, patch);
    ProviderCreationResult result = useCase.execute(command);

    assertThat(result).isEqualTo(expectedResult);
    verify(providerFirmPatchPort).patchProvider(providerId, patch);
    verify(providerFirmUpdatedOutboxPort).enqueue(providerGuid, "100001", patch);
  }

  @Test
  void execute_validCommand_enqueuesOutboxEvent() {
    UUID providerGuid = UUID.randomUUID();
    String providerId = providerGuid.toString();
    ProviderPatchV2 patch = new ProviderPatchV2().name("Event Test");
    ProviderCreationResult expectedResult =
        ProviderCreationResult.withoutOffice(providerGuid, "100001");
    when(providerFirmPatchPort.patchProvider(providerId, patch)).thenReturn(expectedResult);

    useCase.execute(new UpdateProviderFirmCommand(providerId, patch));

    verify(providerFirmUpdatedOutboxPort).enqueue(providerGuid, "100001", patch);
  }

  @Test
  void execute_commandWithNullId_throwsValidationError() {
    ProviderPatchV2 patch = new ProviderPatchV2().name("Updated Name");
    UpdateProviderFirmCommand command = new UpdateProviderFirmCommand(null, patch);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("providerFirmId must be provided");
  }

  @Test
  void execute_commandWithBlankId_throwsValidationError() {
    ProviderPatchV2 patch = new ProviderPatchV2().name("Updated Name");
    UpdateProviderFirmCommand command = new UpdateProviderFirmCommand("  ", patch);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("providerFirmId must be provided");
  }

  @Test
  void execute_commandWithNullPatch_throwsValidationError() {
    UpdateProviderFirmCommand command = new UpdateProviderFirmCommand(UUID.randomUUID().toString(), null);

    assertThatThrownBy(() -> useCase.execute(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("patch must be provided");
  }

  @Test
  void execute_lspPatch_dispatchesToPatchPort() {
    UUID providerGuid = UUID.randomUUID();
    String providerId = providerGuid.toString();
    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .name("Updated LSP")
            .legalServicesProvider(new LSPDetailsPatchV2().companiesHouseNumber("12345678"));
    ProviderCreationResult expectedResult =
        ProviderCreationResult.withoutOffice(providerGuid, "100001");

    when(providerFirmPatchPort.patchProvider(providerId, patch)).thenReturn(expectedResult);

    ProviderCreationResult result = useCase.execute(new UpdateProviderFirmCommand(providerId, patch));

    assertThat(result).isEqualTo(expectedResult);
    verify(providerFirmUpdatedOutboxPort).enqueue(providerGuid, "100001", patch);
    verify(providerFirmPatchPort).patchProvider(providerId, patch);
  }
}

