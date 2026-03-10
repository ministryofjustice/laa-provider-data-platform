package uk.gov.justice.laa.providerdata.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOfficeLiaisonManager201Response;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOfficeLiaisonManager201ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeLiaisonManagers200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeLiaisonManagers200ResponseData;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.service.OfficeLiaisonManagerService;

/** REST controller for provider firm office liaison manager operations. */
@RestController
public class ProviderFirmOfficesLiaisonManagersController {

  private final OfficeLiaisonManagerService service;

  public ProviderFirmOfficesLiaisonManagersController(OfficeLiaisonManagerService service) {
    this.service = service;
  }

  /**
   * Retrieves liaison managers associated with a provider firm office.
   *
   * <p>This endpoint resolves the provider using {@code providerFirmGUIDorFirmNumber}, then
   * resolves the office using {@code officeGUIDorCode}. It returns the liaison managers linked to
   * that office as content only.
   *
   * <p>The service layer returns a list of {@link
   * uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity} records for the resolved office.
   * These are mapped into the OpenAPI-generated DTOs ({@link
   * uk.gov.justice.laa.providerdata.model.LiaisonManagerV2}) and wrapped in the generated response
   * model ({@link
   * uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeLiaisonManagers200Response}).
   *
   * @param providerFirmGUIDorFirmNumber provider identifier supplied as either a GUID (UUID string)
   *     or a firm number
   * @param officeGUIDorCode office identifier supplied as either a GUID (UUID string) or an office
   *     code/account number
   * @return 200 OK with a JSON body containing {@code data.content} as a list of liaison manager
   *     DTOs
   * @throws uk.gov.justice.laa.providerdata.exception.ItemNotFoundException if the provider cannot
   *     be resolved from the supplied identifier, or if the office cannot be resolved for the
   *     provider
   * @throws IllegalArgumentException if identifiers are malformed in a way that prevents resolution
   */
  @GetMapping(
      path =
          "/provider-firms/{providerFirmGUIDorFirmNumber}"
              + "/offices/{officeGUIDorCode}/liaison-managers",
      produces = "application/json")
  public ResponseEntity<GetProviderFirmOfficeLiaisonManagers200Response> getOfficeLiaisonManagers(
      @PathVariable String providerFirmGUIDorFirmNumber, @PathVariable String officeGUIDorCode) {

    List<LiaisonManagerEntity> managers =
        service.getOfficeLiaisonManagers(providerFirmGUIDorFirmNumber, officeGUIDorCode);

    List<LiaisonManagerV2> content =
        managers.stream()
            .map(ProviderFirmOfficesLiaisonManagersController::toLiaisonManagerV2)
            .toList();

    return ResponseEntity.ok(
        new GetProviderFirmOfficeLiaisonManagers200Response()
            .data(new GetProviderFirmOfficeLiaisonManagers200ResponseData().content(content)));
  }

  /**
   * Creates or links a liaison manager for a provider firm office.
   *
   * <p>The provider is resolved using {@code providerFirmGUIDorFirmNumber}. The office is resolved
   * using {@code officeGUIDorCode}.
   *
   * <p>The request body is represented by the OpenAPI-generated {@link
   * uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2} one-of interface and
   * is validated in the controller to ensure it represents exactly one supported operation:
   *
   * <ul>
   *   <li><b>Create</b> a new liaison manager for the office
   *   <li><b>Link</b> the liaison manager currently configured on the provider's head office
   *   <li><b>Link</b> the liaison manager currently configured on the provider's chambers office
   * </ul>
   *
   * <p>On success, returns {@code 201 Created} with the identifiers of the provider, office, and
   * the liaison manager that is now linked.
   *
   * @param providerFirmGUIDorFirmNumber provider identifier supplied as either a GUID (UUID string)
   *     or a firm number
   * @param officeGUIDorCode office identifier supplied as either a GUID (UUID string) or an office
   *     code/account number
   * @param request one-of request describing whether to create a liaison manager or link an
   *     existing one, represented by the generated OpenAPI model
   * @return 201 Created with a JSON body containing the provider, office and liaison manager
   *     identifiers in {@code data}
   * @throws uk.gov.justice.laa.providerdata.exception.ItemNotFoundException if the provider cannot
   *     be resolved, the office cannot be resolved for the provider, or a requested link operation
   *     requires a source liaison manager that does not exist
   * @throws IllegalArgumentException if the request does not represent exactly one supported
   *     operation, or required fields for the selected operation are missing/blank
   */
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

  private static LiaisonManagerV2 toLiaisonManagerV2(LiaisonManagerEntity entity) {
    return new LiaisonManagerV2()
        .guid(entity.getGuid().toString())
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .emailAddress(entity.getEmailAddress())
        .telephoneNumber(entity.getTelephoneNumber());
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

    throw new IllegalArgumentException("Unsupported liaison manager request type: " + request);
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
