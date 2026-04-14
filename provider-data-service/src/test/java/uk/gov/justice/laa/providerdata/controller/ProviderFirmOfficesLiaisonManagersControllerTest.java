package uk.gov.justice.laa.providerdata.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.providerdata.config.JacksonConfig;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.service.OfficeLiaisonManagerService;

/** Web layer tests for {@link ProviderFirmOfficesLiaisonManagersController}. */
@WebMvcTest(ProviderFirmOfficesLiaisonManagersController.class)
@Import(JacksonConfig.class)
class ProviderFirmOfficesLiaisonManagersControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private OfficeLiaisonManagerService service;

  @Test
  void getOfficeLiaisonManagers_returns200_withPaginationMetadata() throws Exception {
    LiaisonManagerEntity lm1 = new LiaisonManagerEntity();
    lm1.setGuid(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
    lm1.setFirstName("Alice");
    lm1.setLastName("Jones");
    lm1.setEmailAddress("alice@example.com");
    lm1.setTelephoneNumber("0123456789");
    lm1.setCreatedBy("SYSTEM");
    lm1.setLastUpdatedBy("SYSTEM");
    lm1.setVersion(0L);

    OfficeLiaisonManagerLinkEntity link1 = new OfficeLiaisonManagerLinkEntity();
    link1.setLiaisonManager(lm1);
    link1.setLinkedFlag(false);
    link1.setActiveDateFrom(LocalDate.of(2024, 1, 1));

    LiaisonManagerEntity lm2 = new LiaisonManagerEntity();
    lm2.setGuid(UUID.fromString("11111111-2222-3333-4444-555555555555"));
    lm2.setFirstName("Bob");
    lm2.setLastName("Smith");
    lm2.setEmailAddress("bob@example.com");
    lm2.setTelephoneNumber("0987654321");
    lm2.setCreatedBy("SYSTEM");
    lm2.setLastUpdatedBy("SYSTEM");
    lm2.setVersion(0L);

    OfficeLiaisonManagerLinkEntity link2 = new OfficeLiaisonManagerLinkEntity();
    link2.setLiaisonManager(lm2);
    link2.setLinkedFlag(true);
    link2.setActiveDateFrom(LocalDate.of(2024, 2, 1));

    when(service.getOfficeLiaisonManagers("100001", "ACC001", PageRequest.of(0, 100)))
        .thenReturn(new PageImpl<>(List.of(link1, link2), PageRequest.of(0, 100), 2));

    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "100001",
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
        .andExpect(jsonPath("$.data.content[0].version").value(0))
        .andExpect(jsonPath("$.data.content[0].createdBy").value("SYSTEM"))
        .andExpect(jsonPath("$.data.content[0].lastUpdatedBy").value("SYSTEM"))
        .andExpect(jsonPath("$.data.content[0].linkedFlag").value(false))
        .andExpect(jsonPath("$.data.content[0].activeDateFrom").value("2024-01-01"))
        .andExpect(jsonPath("$.data.content[1].guid").value("11111111-2222-3333-4444-555555555555"))
        .andExpect(jsonPath("$.data.content[1].firstName").value("Bob"))
        .andExpect(jsonPath("$.data.content[1].lastName").value("Smith"))
        .andExpect(jsonPath("$.data.content[1].emailAddress").value("bob@example.com"))
        .andExpect(jsonPath("$.data.content[1].telephoneNumber").value("0987654321"))
        .andExpect(jsonPath("$.data.content[1].version").value(0))
        .andExpect(jsonPath("$.data.content[1].createdBy").value("SYSTEM"))
        .andExpect(jsonPath("$.data.content[1].lastUpdatedBy").value("SYSTEM"))
        .andExpect(jsonPath("$.data.content[1].linkedFlag").value(true))
        .andExpect(jsonPath("$.data.content[1].activeDateFrom").value("2024-02-01"))
        .andExpect(jsonPath("$.data.metadata.pagination.currentPage").value(0))
        .andExpect(jsonPath("$.data.metadata.pagination.pageSize").value(100))
        .andExpect(jsonPath("$.data.metadata.pagination.totalPages").value(1))
        .andExpect(jsonPath("$.data.metadata.pagination.totalItems").value(2))
        .andExpect(jsonPath("$.data.links.self", containsString("page=0&pageSize=100")))
        .andExpect(jsonPath("$.data.links.first", containsString("page=0&pageSize=100")))
        .andExpect(jsonPath("$.data.links.last", containsString("page=0&pageSize=100")));

    verify(service).getOfficeLiaisonManagers("100001", "ACC001", PageRequest.of(0, 100));
  }

  @Test
  void getOfficeLiaisonManagers_returns404_whenServiceThrowsNotFound() throws Exception {
    when(service.getOfficeLiaisonManagers("999404", "ACC404", PageRequest.of(0, 100)))
        .thenThrow(new ItemNotFoundException("Office not found for provider"));

    mockMvc
        .perform(
            get(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "999404",
                    "ACC404")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Office not found for provider"));

    verify(service).getOfficeLiaisonManagers("999404", "ACC404", PageRequest.of(0, 100));
  }

  @Test
  void postOfficeLiaisonManagers_returnsBadRequest_whenBodyIsEmpty() throws Exception {
    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "100001",
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
                      "unknownField": "value"
                    }
                    """;

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "100001",
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
                eq("100100"), eq("0Q731M"), any(OfficeLiaisonManagerCreateOrLinkV2.class)))
        .willReturn(
            new OfficeLiaisonManagerService.OfficeLiaisonManagerOperationResult(
                providerGuid, "100100", officeGuid, "0Q731M", liaisonManagerGuid));

    String validJson =
        """
                    {
                      "firstName": "Alice",
                      "lastName": "Jones",
                      "emailAddress": "alice@example.com",
                      "telephoneNumber": "0123456789"
                    }
                    """;

    mockMvc
        .perform(
            post(
                    "/provider-firms/{providerFirmGUIDorFirmNumber}"
                        + "/offices/{officeGUIDorCode}/liaison-managers",
                    "100100",
                    "0Q731M")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validJson))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.data.providerFirmGUID").value(providerGuid.toString()))
        .andExpect(jsonPath("$.data.providerFirmNumber").value("100100"))
        .andExpect(jsonPath("$.data.officeGUID").value(officeGuid.toString()))
        .andExpect(jsonPath("$.data.officeCode").value("0Q731M"))
        .andExpect(jsonPath("$.data.liaisonManagerGUID").value(liaisonManagerGuid.toString()));

    verify(service)
        .postOfficeLiaisonManager(
            eq("100100"), eq("0Q731M"), any(OfficeLiaisonManagerCreateOrLinkV2.class));
  }
}
