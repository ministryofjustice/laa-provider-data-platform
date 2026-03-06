package uk.gov.justice.laa.providerdata.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.model.LiaisonManagersResponse;
import uk.gov.justice.laa.providerdata.api.model.OfficeLiaisonManagerPostRequest;
import uk.gov.justice.laa.providerdata.service.OfficeLiaisonManagerService;

/** java doc. */
@RestController
public class ProviderFirmOfficesLiaisonManagersController {

  private final OfficeLiaisonManagerService service;

  public ProviderFirmOfficesLiaisonManagersController(OfficeLiaisonManagerService service) {
    this.service = service;
  }

  /** java doc. */
  @PostMapping(
      path =
          "/provider-firms/{providerFirmGUIDorFirmNumber}"
              + "/offices/{officeGUIDorCode}/liaison-managers",
      consumes = "application/json",
      produces = "application/json")
  public ResponseEntity<LiaisonManagersResponse> postOfficeLiaisonManagers(
      @PathVariable String providerFirmGUIDorFirmNumber,
      @PathVariable String officeGUIDorCode,
      @Valid @RequestBody OfficeLiaisonManagerPostRequest request) {

    var managers =
        service.postOfficeLiaisonManager(providerFirmGUIDorFirmNumber, officeGUIDorCode, request);

    var response =
        new LiaisonManagersResponse(
            managers.stream()
                .map(
                    lm ->
                        new LiaisonManagersResponse.LiaisonManagerDto(
                            lm.getGuid().toString(),
                            lm.getFirstName(),
                            lm.getLastName(),
                            lm.getEmailAddress(),
                            lm.getTelephoneNumber()))
                .toList());

    return ResponseEntity.ok(response);
  }
}
