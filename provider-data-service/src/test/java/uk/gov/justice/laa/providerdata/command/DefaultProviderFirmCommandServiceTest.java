package uk.gov.justice.laa.providerdata.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import uk.gov.justice.laa.providerdata.command.event.ProviderFirmUpdatedEvent;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.service.ProviderCreationResult;
import uk.gov.justice.laa.providerdata.service.ProviderService;

@ExtendWith(MockitoExtension.class)
class DefaultProviderFirmCommandServiceTest {

  @Mock private ProviderService providerService;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private DefaultProviderFirmCommandService commandService;

  @Test
  void handle_validCommand_dispatchesToProviderService() {
    UUID providerGuid = UUID.randomUUID();
    String providerId = providerGuid.toString();
    ProviderPatchV2 patch = new ProviderPatchV2().name("Updated Name");
    ProviderCreationResult expectedResult =
        ProviderCreationResult.withoutOffice(providerGuid, "100001");

    when(providerService.patchProvider(providerId, patch)).thenReturn(expectedResult);

    UpdateProviderFirmCommand command = new UpdateProviderFirmCommand(providerId, patch);
    ProviderCreationResult result = commandService.handle(command);

    assertThat(result).isEqualTo(expectedResult);
    verify(providerService).patchProvider(providerId, patch);
  }

  @Test
  void handle_validCommand_publishesProviderFirmUpdatedEvent() {
    UUID providerGuid = UUID.randomUUID();
    String providerId = providerGuid.toString();
    ProviderPatchV2 patch = new ProviderPatchV2().name("Event Test");
    when(providerService.patchProvider(providerId, patch))
        .thenReturn(ProviderCreationResult.withoutOffice(providerGuid, "100001"));

    commandService.handle(new UpdateProviderFirmCommand(providerId, patch));

    verify(eventPublisher).publishEvent(any(ProviderFirmUpdatedEvent.class));
  }

  @Test
  void handle_commandWithNullId_throwsValidationError() {
    ProviderPatchV2 patch = new ProviderPatchV2().name("Updated Name");
    UpdateProviderFirmCommand command = new UpdateProviderFirmCommand(null, patch);

    assertThatThrownBy(() -> commandService.handle(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("providerFirmId must be provided");
  }

  @Test
  void handle_commandWithBlankId_throwsValidationError() {
    ProviderPatchV2 patch = new ProviderPatchV2().name("Updated Name");
    UpdateProviderFirmCommand command = new UpdateProviderFirmCommand("  ", patch);

    assertThatThrownBy(() -> commandService.handle(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("providerFirmId must be provided");
  }

  @Test
  void handle_commandWithNullPatch_throwsValidationError() {
    UpdateProviderFirmCommand command =
        new UpdateProviderFirmCommand(UUID.randomUUID().toString(), null);

    assertThatThrownBy(() -> commandService.handle(command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("patch must be provided");
  }

  @Test
  void handle_lspPatch_dispatchesToProviderService() {
    UUID providerGuid = UUID.randomUUID();
    String providerId = providerGuid.toString();
    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .name("Updated LSP")
            .legalServicesProvider(
                new LSPDetailsPatchV2().companiesHouseNumber("12345678"));
    ProviderCreationResult expectedResult =
        ProviderCreationResult.withoutOffice(providerGuid, "100001");

    when(providerService.patchProvider(providerId, patch)).thenReturn(expectedResult);

    UpdateProviderFirmCommand command = new UpdateProviderFirmCommand(providerId, patch);
    ProviderCreationResult result = commandService.handle(command);

    assertThat(result).isEqualTo(expectedResult);
    verify(providerService).patchProvider(providerId, patch);
  }
}

