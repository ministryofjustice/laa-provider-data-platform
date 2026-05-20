package uk.gov.justice.laa.providerdata.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.OutboxEventEntity;
import uk.gov.justice.laa.providerdata.entity.OutboxEventStatus;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class ProviderFirmUpdatedOutboxPersistenceAdapterTest {

  @Mock private OutboxEventRepository outboxEventRepository;

  @InjectMocks private ProviderFirmUpdatedOutboxPersistenceAdapter adapter;

  @Test
  void enqueue_writesPendingOutboxRecord() {
    UUID providerGuid = UUID.randomUUID();
    ProviderPatchV2 patch = new ProviderPatchV2().name("Updated Name");

    adapter.enqueue(providerGuid, "100001", patch);

    ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
    verify(outboxEventRepository).save(captor.capture());

    OutboxEventEntity saved = captor.getValue();
    assertThat(saved.getAggregateType()).isEqualTo("ProviderFirm");
    assertThat(saved.getAggregateId()).isEqualTo(providerGuid);
    assertThat(saved.getEventType()).isEqualTo("ProviderFirmUpdated");
    assertThat(saved.getFirmNumber()).isEqualTo("100001");
    assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(saved.getAttemptCount()).isEqualTo(0);
    assertThat(saved.getOccurredAt()).isNotNull();
    assertThat(saved.getEventPayload()).contains("providerFirmGuid=" + providerGuid);
    assertThat(saved.getEventPayload()).contains("firmNumber=100001");
    assertThat(saved.getEventPayload()).contains("changedFields=name");
  }

  @Test
  void summariseChangedFields_returnsCommaSeparatedTopLevelFields() {
    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .name("Updated Name")
            .legalServicesProvider(new uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2())
            .practitioner(new uk.gov.justice.laa.providerdata.model.PractitionerDetailsPatchV2());

    String summary = ProviderFirmUpdatedOutboxPersistenceAdapter.summariseChangedFields(patch);

    assertThat(summary).isEqualTo("name,legalServicesProvider,practitioner");
  }

  @Test
  void summariseChangedFields_nullPatch_returnsNull() {
    assertThat(ProviderFirmUpdatedOutboxPersistenceAdapter.summariseChangedFields(null)).isNull();
  }
}

