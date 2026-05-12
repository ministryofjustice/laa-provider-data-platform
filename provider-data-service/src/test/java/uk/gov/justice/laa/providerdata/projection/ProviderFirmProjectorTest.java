package uk.gov.justice.laa.providerdata.projection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.event.ProviderFirmUpdatedEvent;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.service.ProviderService;

/**
 * Unit tests for {@link ProviderFirmProjector}.
 *
 * <p>Verifies event handling, projection content, and resilience when either the entity load or the
 * read store write fails.
 */
@ExtendWith(MockitoExtension.class)
class ProviderFirmProjectorTest {

  @Mock private ProviderService providerService;
  @Mock private ProviderFirmReadStore readStore;

  @InjectMocks private ProviderFirmProjector projector;

  @Test
  void onProviderFirmUpdated_loadsEntityAndWritesProjection() {
    UUID guid = UUID.randomUUID();
    LspProviderEntity entity = LspProviderEntity.builder().name("My LSP").build();
    entity.setGuid(guid);
    entity.setFirmNumber("LSP-0001");
    when(providerService.getProvider(guid.toString())).thenReturn(entity);

    projector.onProviderFirmUpdated(new ProviderFirmUpdatedEvent(guid));

    ArgumentCaptor<ProviderFirmReadModel> captor =
        ArgumentCaptor.forClass(ProviderFirmReadModel.class);
    verify(readStore).save(captor.capture());
    ProviderFirmReadModel saved = captor.getValue();
    assertProjection(saved, guid, "LSP-0001", "My LSP", FirmType.LEGAL_SERVICES_PROVIDER);
  }

  @Test
  void onProviderFirmUpdated_entityNotFound_doesNotWriteToStore() {
    UUID guid = UUID.randomUUID();
    when(providerService.getProvider(guid.toString()))
        .thenThrow(new ItemNotFoundException("not found"));

    projector.onProviderFirmUpdated(new ProviderFirmUpdatedEvent(guid));

    verify(readStore, never()).save(any());
  }

  @Test
  void onProviderFirmUpdated_readStoreWriteFails_doesNotThrow() {
    UUID guid = UUID.randomUUID();
    ProviderEntity entity = LspProviderEntity.builder().name("Firm").build();
    entity.setGuid(guid);
    entity.setFirmNumber("LSP-0002");
    when(providerService.getProvider(guid.toString())).thenReturn(entity);
    doThrow(new RuntimeException("Redis down")).when(readStore).save(any());

    // Must not propagate the exception
    projector.onProviderFirmUpdated(new ProviderFirmUpdatedEvent(guid));
  }

  @Test
  void onProviderFirmUpdated_projectionContainsCorrectFirmType() {
    UUID guid = UUID.randomUUID();
    uk.gov.justice.laa.providerdata.entity.ChamberProviderEntity entity =
        uk.gov.justice.laa.providerdata.entity.ChamberProviderEntity.builder()
            .name("My Chambers")
            .build();
    entity.setGuid(guid);
    entity.setFirmNumber("CH-0001");
    when(providerService.getProvider(guid.toString())).thenReturn(entity);

    projector.onProviderFirmUpdated(new ProviderFirmUpdatedEvent(guid));

    ArgumentCaptor<ProviderFirmReadModel> captor =
        ArgumentCaptor.forClass(ProviderFirmReadModel.class);
    verify(readStore).save(captor.capture());
    assertProjection(captor.getValue(), guid, "CH-0001", "My Chambers", FirmType.CHAMBERS);
  }

  private static void assertProjection(
      ProviderFirmReadModel model,
      UUID expectedGuid,
      String expectedFirmNumber,
      String expectedName,
      String expectedFirmType) {
    org.assertj.core.api.Assertions.assertThat(model.getGuid()).isEqualTo(expectedGuid);
    org.assertj.core.api.Assertions.assertThat(model.getFirmNumber()).isEqualTo(expectedFirmNumber);
    org.assertj.core.api.Assertions.assertThat(model.getName()).isEqualTo(expectedName);
    org.assertj.core.api.Assertions.assertThat(model.getFirmType()).isEqualTo(expectedFirmType);
  }
}
