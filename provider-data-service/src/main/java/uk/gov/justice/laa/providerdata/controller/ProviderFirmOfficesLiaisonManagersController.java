package uk.gov.justice.laa.providerdata.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOfficeLiaisonManager201Response;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOfficeLiaisonManager201ResponseData;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;
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
  public ResponseEntity<CreateProviderFirmOfficeLiaisonManager201Response>
      postOfficeLiaisonManagers(
          @PathVariable String providerFirmGUIDorFirmNumber,
          @PathVariable String officeGUIDorCode,
          @RequestBody OfficeLiaisonManagerCreateOrLinkV2 request) {

    validateRequest(request);

    var result =
        service.postOfficeLiaisonManager(providerFirmGUIDorFirmNumber, officeGUIDorCode, request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CreateProviderFirmOfficeLiaisonManager201Response()
                .data(
                    new CreateProviderFirmOfficeLiaisonManager201ResponseData()
                        .providerFirmGUID(result.providerFirmGuid().toString())
                        .providerFirmNumber(result.providerFirmNumber())
                        .officeGUID(result.officeGuid().toString())
                        .officeCode(result.officeCode())
                        .liaisonManagerGUID(result.liaisonManagerGuid().toString())));
  }

  private static void validateRequest(OfficeLiaisonManagerCreateOrLinkV2 request) {
    if (request == null) {
      throw new IllegalArgumentException("Request body must be provided");
    }

    if (request instanceof LiaisonManagerCreateV2 create) {
      if (isBlank(create.getFirstName())
          || isBlank(create.getLastName())
          || isBlank(create.getEmailAddress())
          || isBlank(create.getTelephoneNumber())) {
        throw new IllegalArgumentException(
            "create requires firstName, lastName, emailAddress, telephoneNumber");
      }
      return;
    }

    if (request instanceof LiaisonManagerLinkHeadOfficeV2) {
      return;
    }

    if (request instanceof LiaisonManagerLinkChambersV2) {
      return;
    }

    throw new IllegalArgumentException(
        "Request must be exactly one of: LiaisonManagerCreateV2, LiaisonManagerLinkHeadOfficeV2, "
            + "LiaisonManagerLinkChambersV2");
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
