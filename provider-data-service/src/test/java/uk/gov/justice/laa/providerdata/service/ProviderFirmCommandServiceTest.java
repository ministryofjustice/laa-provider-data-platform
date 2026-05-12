package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

/**
 * Unit tests for {@link ProviderFirmCommandService}.
 *
 * <p>Verifies that each command method correctly delegates to the underlying service, and that the
 * result is forwarded to the caller.
 */
@ExtendWith(MockitoExtension.class)
class ProviderFirmCommandServiceTest {

  @Mock private ProviderCreationService providerCreationService;
  @Mock private ProviderService providerService;

  @InjectMocks private ProviderFirmCommandService commandService;

  @Test
  void createLspFirm_delegatesToCreationService_andReturnsResult() {
    UUID guid = UUID.randomUUID();
    ProviderCreationResult expected =
        new ProviderCreationResult(guid, "LSP-0001", UUID.randomUUID(), "ACC-001");
    when(providerCreationService.createLspFirm(any(), any(), any(), any(), any(), any()))
        .thenReturn(expected);

    ProviderCreationResult result =
        commandService.createLspFirm(
            LspProviderEntity.builder().name("LSP").build(),
            new OfficeEntity(),
            new LspProviderOfficeLinkEntity(),
            null,
            null,
            null);

    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    assertThat(result.firmNumber()).isEqualTo("LSP-0001");
    verify(providerCreationService).createLspFirm(any(), any(), any(), any(), any(), any());
  }

  @Test
  void createChambersFirm_delegatesToCreationService_andReturnsResult() {
    UUID guid = UUID.randomUUID();
    ProviderCreationResult expected =
        new ProviderCreationResult(guid, "CH-0001", UUID.randomUUID(), "ACC-002");
    when(providerCreationService.createChambersFirm(any(), any(), any(), any(), any()))
        .thenReturn(expected);

    ProviderCreationResult result =
        commandService.createChambersFirm(
            ChamberProviderEntity.builder().name("Chambers").build(),
            new OfficeEntity(),
            new ChamberProviderOfficeLinkEntity(),
            null,
            null);

    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    assertThat(result.firmNumber()).isEqualTo("CH-0001");
    verify(providerCreationService).createChambersFirm(any(), any(), any(), any(), any());
  }

  @Test
  void createPractitionerFirm_delegatesToCreationService_andReturnsResult() {
    UUID guid = UUID.randomUUID();
    ProviderCreationResult expected = ProviderCreationResult.withoutOffice(guid, "ADV-0001");
    when(providerCreationService.createPractitionerFirm(any(), any(), any())).thenReturn(expected);

    ProviderCreationResult result =
        commandService.createPractitionerFirm(
            uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity.builder()
                .name("Advocate")
                .build(),
            null,
            null);

    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    assertThat(result.firmNumber()).isEqualTo("ADV-0001");
    assertThat(result.headOfficeGUID()).isNull();
    verify(providerCreationService).createPractitionerFirm(any(), any(), any());
  }

  @Test
  void patchProviderFirm_delegatesToProviderService_andReturnsResult() {
    UUID guid = UUID.randomUUID();
    ProviderCreationResult expected = ProviderCreationResult.withoutOffice(guid, "LSP-0001");
    ProviderPatchV2 patch = new ProviderPatchV2().name("Updated");
    when(providerService.patchProvider("LSP-0001", patch)).thenReturn(expected);

    ProviderCreationResult result = commandService.patchProviderFirm("LSP-0001", patch);

    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    verify(providerService).patchProvider("LSP-0001", patch);
  }
}
