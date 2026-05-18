package uk.gov.justice.laa.providerdata.adapters.out.persistence;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.providerdata.application.providerfirm.port.out.ProviderFirmUpdatedOutboxPort;
import uk.gov.justice.laa.providerdata.entity.OutboxEventEntity;
import uk.gov.justice.laa.providerdata.entity.OutboxEventStatus;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.repository.OutboxEventRepository;

/** Persistence adapter that writes provider update events to the durable outbox table. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderFirmUpdatedOutboxPersistenceAdapter implements ProviderFirmUpdatedOutboxPort {

  private static final String AGGREGATE_TYPE = "ProviderFirm";
  private static final String EVENT_TYPE = "ProviderFirmUpdated";

  private final OutboxEventRepository outboxEventRepository;

  @Override
  public void enqueue(UUID providerFirmGuid, String firmNumber, ProviderPatchV2 patch) {
    OutboxEventEntity event =
        OutboxEventEntity.builder()
            .aggregateType(AGGREGATE_TYPE)
            .aggregateId(providerFirmGuid)
            .eventType(EVENT_TYPE)
            .firmNumber(firmNumber)
            .eventPayload(buildPayload(providerFirmGuid, firmNumber, patch))
            .status(OutboxEventStatus.PENDING)
            .attemptCount(0)
            .occurredAt(OffsetDateTime.now())
            .build();

    outboxEventRepository.save(event);

    log.info(
        "Outbox row written: aggregateType={} aggregateId={} eventType={} firmNumber={} status={}",
        AGGREGATE_TYPE,
        providerFirmGuid,
        EVENT_TYPE,
        firmNumber,
        OutboxEventStatus.PENDING);
  }

  static String buildPayload(UUID providerFirmGuid, String firmNumber, ProviderPatchV2 patch) {
    String changedFields = summariseChangedFields(patch);
    return "providerFirmGuid="
        + providerFirmGuid
        + ";firmNumber="
        + firmNumber
        + ";changedFields="
        + (changedFields != null ? changedFields : "");
  }

  static String summariseChangedFields(ProviderPatchV2 patch) {
    if (patch == null) {
      return null;
    }

    List<String> fields = new ArrayList<>();
    if (patch.getName() != null) {
      fields.add("name");
    }
    if (patch.getLegalServicesProvider() != null) {
      fields.add("legalServicesProvider");
    }
    if (patch.getPractitioner() != null) {
      fields.add("practitioner");
    }

    return fields.isEmpty() ? null : String.join(",", fields);
  }
}
