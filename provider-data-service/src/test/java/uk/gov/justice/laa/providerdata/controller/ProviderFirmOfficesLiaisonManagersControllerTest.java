package uk.gov.justice.laa.providerdata.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.exception.GlobalExceptionHandler;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.service.OfficeLiaisonManagerService;

/**
 * Web layer tests for {@link ProviderFirmOfficesLiaisonManagersController}.
 *
 * <p>Uses {@code MockMvcBuilders.standaloneSetup} to avoid loading the full Spring context.
 */
class ProviderFirmOfficesLiaisonManagersControllerTest {

  private MockMvc mockMvc;
  private OfficeLiaisonManagerService service;

  @BeforeEach
  @SuppressWarnings("removal")
  void setUp() {
    service = mock(OfficeLiaisonManagerService.class);

    ObjectMapper objectMapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();

    module.addDeserializer(
        OfficeLiaisonManagerCreateOrLinkV2.class,
        new StdDeserializer<>(OfficeLiaisonManagerCreateOrLinkV2.class) {
          @Override
          public OfficeLiaisonManagerCreateOrLinkV2 deserialize(
              JsonParser p, DeserializationContext ctx) throws JacksonException {
            try {
              JsonNode node = p.readValueAsTree();

              JsonNode createNode = node.get("create");
              if (createNode != null && !createNode.isNull()) {
                return ctx.readTreeAsValue(createNode, LiaisonManagerCreateV2.class);
              }

              JsonNode linkHeadOfficeNode = node.get("linkHeadOffice");
              if (linkHeadOfficeNode != null && !linkHeadOfficeNode.isNull()) {
                return ctx.readTreeAsValue(
                    linkHeadOfficeNode, LiaisonManagerLinkHeadOfficeV2.class);
              }

              JsonNode linkChambersNode = node.get("linkChambers");
              if (linkChambersNode != null && !linkChambersNode.isNull()) {
                return ctx.readTreeAsValue(linkChambersNode, LiaisonManagerLinkChambersV2.class);
              }

              throw new IllegalArgumentException(
                  "Exactly one of create, linkHeadOffice, linkChambers must be provided");
            } catch (IOException e) {
              throw new com.fasterxml.jackson.core.JsonParseException(
                  p, "Failed to deserialize OfficeLiaisonManagerCreateOrLinkV2", e);
            }
          }
        });

    objectMapper.registerModule(module);

    mockMvc =
        MockMvcBuilders.standaloneSetup(new ProviderFirmOfficesLiaisonManagersController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
  }

  @Test
  void getOfficeLiaisonManagers_returns200_withContentOnly() throws Exception {
    LiaisonManagerEntity lm1 = new LiaisonManagerEntity();
    lm1.setGuid(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
    lm1.setFirstName("Alice");
    lm1.setLastName("Jones");
    lm1.setEmailAddress("alice@example.com");
    lm1.setTelephoneNumber("0123456789");

    LiaisonManagerEntity lm2 = new LiaisonManagerEntity();
    lm2.setGuid(UUID.fromString("11111111-2222-3333-4444-555555555555"));
    lm2.setFirstName("Bob");
    lm2.setLastName("Smith");
    lm2.setEmailAddress("bob@example.com");
    lm2.setTelephoneNumber("0987654321");

    when(service.getOfficeLiaisonManagers("FRM001", "ACC001")).thenReturn(List.of(lm1, lm2));

    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "FRM001",
                    "ACC001")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content[0].guid").value("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
        .andExpect(jsonPath("$.data.content[0].firstName").value("Alice"))
        .andExpect(jsonPath("$.data.content[0].lastName").value("Jones"))
        .andExpect(jsonPath("$.data.content[0].emailAddress").value("alice@example.com"))
        .andExpect(jsonPath("$.data.content[0].telephoneNumber").value("0123456789"))
        .andExpect(jsonPath("$.data.content[1].guid").value("11111111-2222-3333-4444-555555555555"))
        .andExpect(jsonPath("$.data.content[1].firstName").value("Bob"))
        .andExpect(jsonPath("$.data.content[1].lastName").value("Smith"))
        .andExpect(jsonPath("$.data.content[1].emailAddress").value("bob@example.com"))
        .andExpect(jsonPath("$.data.content[1].telephoneNumber").value("0987654321"));

    verify(service).getOfficeLiaisonManagers("FRM001", "ACC001");
  }

  @Test
  void getOfficeLiaisonManagers_returns404_whenServiceThrowsNotFound() throws Exception {
    when(service.getOfficeLiaisonManagers("FRM404", "ACC404"))
        .thenThrow(new ItemNotFoundException("Office not found for provider"));

    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "FRM404",
                    "ACC404")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Office not found for provider"));

    verify(service).getOfficeLiaisonManagers("FRM404", "ACC404");
  }

  @Test
  void postOfficeLiaisonManagers_returnsBadRequest_whenBodyIsEmpty() throws Exception {
    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "FRM001",
                    "0Q731M")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postOfficeLiaisonManagers_returnsBadRequest_whenExactlyOneNotProvided() throws Exception {
    String invalidJson =
        """
                    {
                      "create": null,
                      "linkHeadOffice": null,
                      "linkChambers": null
                    }
                    """;

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "FRM001",
                    "0Q731M")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postOfficeLiaisonManagers_returnsCreated_withResponseBody() throws Exception {
    var providerGuid = UUID.fromString("11111111-2222-3333-4444-555555555555");
    var officeGuid = UUID.fromString("66666666-7777-8888-9999-aaaaaaaaaaaa");
    var liaisonManagerGuid = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    given(
            service.postOfficeLiaisonManager(
                eq("FRM100"), eq("0Q731M"), any(OfficeLiaisonManagerCreateOrLinkV2.class)))
        .willReturn(
            new OfficeLiaisonManagerService.OfficeLiaisonManagerOperationResult(
                providerGuid, "FRM100", officeGuid, "0Q731M", liaisonManagerGuid));

    String validJson =
        """
                    {
                      "create": {
                        "firstName": "Alice",
                        "lastName": "Jones",
                        "emailAddress": "alice@example.com",
                        "telephoneNumber": "0123456789"
                      }
                    }
                    """;

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "FRM100",
                    "0Q731M")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.data.providerFirmGUID").value(providerGuid.toString()))
        .andExpect(jsonPath("$.data.providerFirmNumber").value("FRM100"))
        .andExpect(jsonPath("$.data.officeGUID").value(officeGuid.toString()))
        .andExpect(jsonPath("$.data.officeCode").value("0Q731M"))
        .andExpect(jsonPath("$.data.liaisonManagerGUID").value(liaisonManagerGuid.toString()));

    verify(service)
        .postOfficeLiaisonManager(
            eq("FRM100"), eq("0Q731M"), any(OfficeLiaisonManagerCreateOrLinkV2.class));
  }
}
