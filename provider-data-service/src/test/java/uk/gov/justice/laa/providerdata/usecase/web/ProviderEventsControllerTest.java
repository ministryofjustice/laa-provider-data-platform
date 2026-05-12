package uk.gov.justice.laa.providerdata.usecase.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.shared.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.shared.config.JacksonConfig;
import uk.gov.justice.laa.providerdata.usecase.ProviderEventEntity;
import uk.gov.justice.laa.providerdata.usecase.ProviderEventQueryService;

@WebMvcTest(ProviderEventsController.class)
@Import(JacksonConfig.class)
class ProviderEventsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ProviderEventQueryService providerEventQueryService;

  private ProviderEventEntity buildEntity(UUID guid) {
    return ProviderEventEntity.builder()
        .guid(guid)
        .eventType("ProviderFirmChangedSnapshotEvent")
        .eventSource("apiV2")
        .correlationId("corr-abc")
        .traceId("trace-xyz")
        .version(0L)
        .createdBy("user1")
        .createdTimestamp(OffsetDateTime.now())
        .lastUpdatedBy("user1")
        .lastUpdatedTimestamp(OffsetDateTime.now())
        .payload("{}")
        .build();
  }

  @Test
  void getProviderEvents_returns200WithPagedHeaders() throws Exception {
    UUID guid = UUID.randomUUID();
    ProviderEventEntity entity = buildEntity(guid);

    when(providerEventQueryService.getEvents(isNull(), isNull(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    mockMvc
        .perform(get("/provider-events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].correlationId").value("corr-abc"));
  }

  @Test
  void getEventByGUID_found_returns200WithEventPayload() throws Exception {
    UUID guid = UUID.randomUUID();
    ProviderEventEntity entity = buildEntity(guid);

    when(providerEventQueryService.getEvent(guid.toString())).thenReturn(entity);

    mockMvc
        .perform(get("/provider-events/{guid}", guid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.eventHeader.guid").value(guid.toString()));
  }

  @Test
  void getEventByGUID_notFound_returns404() throws Exception {
    when(providerEventQueryService.getEvent(any()))
        .thenThrow(new ItemNotFoundException("not found"));

    mockMvc
        .perform(get("/provider-events/00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getEventByGUID_invalidUuid_returns400() throws Exception {
    when(providerEventQueryService.getEvent("not-a-uuid"))
        .thenThrow(new IllegalArgumentException("invalid UUID"));

    mockMvc.perform(get("/provider-events/not-a-uuid")).andExpect(status().isBadRequest());
  }
}
