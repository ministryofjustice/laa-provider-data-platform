package uk.gov.justice.laa.providerdata.event;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.ProviderEventEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.EventTypeV2;
import uk.gov.justice.laa.providerdata.repository.ProviderEventRepository;

/** Read-only queries against the permanent provider event store. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProviderEventQueryService {

  private final ProviderEventRepository providerEventRepository;

  /**
   * Returns a page of events, optionally filtered by event type and/or correlation ID.
   *
   * @param eventTypes list of event types to include; {@code null} or empty means no filter
   * @param correlationId correlation ID to match; {@code null} means no filter
   * @param pageable pagination and sort parameters
   */
  public Page<ProviderEventEntity> getEvents(
      @Nullable List<EventTypeV2> eventTypes, @Nullable String correlationId, Pageable pageable) {

    List<String> types =
        (eventTypes == null || eventTypes.isEmpty())
            ? null
            : eventTypes.stream().map(EventTypeV2::getValue).toList();

    boolean hasTypes = types != null;
    boolean hasCorrelation = correlationId != null && !correlationId.isBlank();

    if (hasTypes && hasCorrelation) {
      return providerEventRepository.findByEventTypeInAndCorrelationId(
          types, correlationId, pageable);
    }
    if (hasTypes) {
      return providerEventRepository.findByEventTypeIn(types, pageable);
    }
    if (hasCorrelation) {
      return providerEventRepository.findByCorrelationId(correlationId, pageable);
    }
    return providerEventRepository.findAll(pageable);
  }

  /**
   * Returns the event with the given GUID.
   *
   * @param eventGuid GUID string; must be a valid UUID
   * @throws IllegalArgumentException if {@code eventGuid} is not a valid UUID format
   * @throws ItemNotFoundException if no event exists with that GUID
   */
  public ProviderEventEntity getEvent(String eventGuid) {
    UUID id = UUID.fromString(eventGuid);
    return providerEventRepository
        .findById(id)
        .orElseThrow(() -> new ItemNotFoundException("No event found with GUID: " + eventGuid));
  }
}
