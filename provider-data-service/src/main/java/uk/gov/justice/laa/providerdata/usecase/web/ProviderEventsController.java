package uk.gov.justice.laa.providerdata.usecase.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.justice.laa.providerdata.api.EventsApi;
import uk.gov.justice.laa.providerdata.model.EventHeaderV2;
import uk.gov.justice.laa.providerdata.model.EventSourceV2;
import uk.gov.justice.laa.providerdata.model.EventTypeV2;
import uk.gov.justice.laa.providerdata.model.EventV2;
import uk.gov.justice.laa.providerdata.model.GetEventByGUID200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderEvents200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderEvents200ResponseData;
import uk.gov.justice.laa.providerdata.model.ProviderFirmChangedSnapshotEventV2Payload;
import uk.gov.justice.laa.providerdata.shared.PageLinks;
import uk.gov.justice.laa.providerdata.shared.PageMetadata;
import uk.gov.justice.laa.providerdata.shared.PageParamValidator;
import uk.gov.justice.laa.providerdata.usecase.ProviderEventEntity;
import uk.gov.justice.laa.providerdata.usecase.ProviderEventQueryService;

/**
 * REST controller for the provider events query API.
 *
 * <p>The list endpoint returns event headers only (no payload). The single-event endpoint
 * deserialises the stored JSON payload and returns the full {@link EventV2}.
 */
@RestController
@RequiredArgsConstructor
public class ProviderEventsController implements EventsApi {

  private final ProviderEventQueryService providerEventQueryService;
  private final JsonMapper objectMapper;

  /**
   * Returns a paginated list of event headers, optionally filtered by event type and/or correlation
   * ID.
   */
  @Override
  public ResponseEntity<GetProviderEvents200Response> getProviderEvents(
      @Nullable String xCorrelationId,
      @Nullable String traceparent,
      @Nullable List<EventTypeV2> eventType,
      @Nullable String correlationId,
      @Nullable Integer page,
      @Nullable Integer pageSize) {

    var pageParams = PageParamValidator.resolve(page, pageSize);
    Page<EventHeaderV2> headers =
        providerEventQueryService
            .getEvents(eventType, correlationId, pageParams)
            .map(this::toEventHeader);

    return ResponseEntity.ok(
        new GetProviderEvents200Response()
            .data(
                new GetProviderEvents200ResponseData()
                    .content(headers.getContent())
                    .metadata(PageMetadata.of(headers))
                    .links(PageLinks.of(headers))));
  }

  /** Returns the full event (header + payload) for the given GUID. */
  @Override
  public ResponseEntity<GetEventByGUID200Response> getEventByGUID(
      String eventGUID, @Nullable String xCorrelationId, @Nullable String traceparent) {

    ProviderEventEntity entity = providerEventQueryService.getEvent(eventGUID);
    return ResponseEntity.ok(new GetEventByGUID200Response().data(toEventV2(entity)));
  }

  private EventHeaderV2 toEventHeader(ProviderEventEntity entity) {
    return new EventHeaderV2()
        .guid(entity.getGuid())
        .version(entity.getVersion())
        .createdBy(entity.getCreatedBy())
        .createdTimestamp(entity.getCreatedTimestamp())
        .lastUpdatedBy(entity.getLastUpdatedBy())
        .lastUpdatedTimestamp(entity.getLastUpdatedTimestamp())
        .eventType(EventTypeV2.fromValue(entity.getEventType()))
        .eventSource(EventSourceV2.fromValue(entity.getEventSource()))
        .correlationId(entity.getCorrelationId())
        .traceId(entity.getTraceId());
  }

  private EventV2 toEventV2(ProviderEventEntity entity) {
    ProviderFirmChangedSnapshotEventV2Payload payload =
        objectMapper.readValue(
            entity.getPayload(), ProviderFirmChangedSnapshotEventV2Payload.class);
    return new EventV2(toEventHeader(entity), payload);
  }
}
