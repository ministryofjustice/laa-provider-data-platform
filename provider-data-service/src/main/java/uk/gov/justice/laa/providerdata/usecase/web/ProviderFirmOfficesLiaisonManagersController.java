package uk.gov.justice.laa.providerdata.usecase.web;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ProviderFirmOfficesLiaisonManagersApi;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerQueryService;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOfficeLiaisonManager201Response;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOfficeLiaisonManager201ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeLiaisonManagers200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeLiaisonManagers200ResponseData;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkChambersV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerLinkHeadOfficeV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerV2;
import uk.gov.justice.laa.providerdata.model.OfficeLiaisonManagerCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.shared.PageLinks;
import uk.gov.justice.laa.providerdata.shared.PageMetadata;
import uk.gov.justice.laa.providerdata.shared.PageParamValidator;
import uk.gov.justice.laa.providerdata.usecase.EventContext;
import uk.gov.justice.laa.providerdata.usecase.LiaisonManagerLinkResult;
import uk.gov.justice.laa.providerdata.usecase.OfficeFirmUseCase;

/** REST controller for provider firm office liaison manager operations. */
@RestController
@RequiredArgsConstructor
public class ProviderFirmOfficesLiaisonManagersController
    implements ProviderFirmOfficesLiaisonManagersApi {

  private final OfficeLiaisonManagerQueryService officeLiaisonManagerQueryService;
  private final OfficeFirmUseCase officeFirmUseCase;

  /**
   * Retrieves liaison managers associated with a provider firm office.
   *
   * @param providerFirmGUIDorFirmNumber provider identifier as a GUID or firm number
   * @param officeGUIDorCode office identifier as a GUID or account number
   * @return 200 OK with paginated liaison manager list
   * @throws uk.gov.justice.laa.providerdata.shared.ItemNotFoundException if provider or office
   *     cannot be resolved
   */
  @Override
  public ResponseEntity<GetProviderFirmOfficeLiaisonManagers200Response>
      getProviderFirmOfficeLiaisonManagers(
          String providerFirmGUIDorFirmNumber,
          String officeGUIDorCode,
          String xCorrelationId,
          String traceparent,
          Integer page,
          Integer pageSize) {

    var pageParams = PageParamValidator.resolve(page, pageSize);
    Page<LiaisonManagerV2> managers =
        officeLiaisonManagerQueryService
            .getOfficeLiaisonManagers(providerFirmGUIDorFirmNumber, officeGUIDorCode, pageParams)
            .map(ProviderFirmOfficesLiaisonManagersController::toLiaisonManagerV2);
    return ResponseEntity.ok(
        new GetProviderFirmOfficeLiaisonManagers200Response()
            .data(
                new GetProviderFirmOfficeLiaisonManagers200ResponseData()
                    .content(managers.getContent())
                    .metadata(PageMetadata.of(managers))
                    .links(PageLinks.of(managers))));
  }

  /**
   * Creates or links a liaison manager for a provider firm office.
   *
   * <p>The request body describes whether to create a new liaison manager or link the existing
   * active manager from the provider's head office or chambers office.
   *
   * @param providerFirmGUIDorFirmNumber provider identifier as a GUID or firm number
   * @param officeGUIDorCode office identifier as a GUID or account number
   * @param request one-of body: create, linkHeadOffice, or linkChambers
   * @return 201 Created with provider, office, and liaison manager identifiers
   * @throws uk.gov.justice.laa.providerdata.shared.ItemNotFoundException if provider, office, or
   *     source liaison manager cannot be resolved
   * @throws IllegalArgumentException if required fields are missing for the selected operation
   */
  @Override
  public ResponseEntity<CreateProviderFirmOfficeLiaisonManager201Response>
      createProviderFirmOfficeLiaisonManager(
          String providerFirmGUIDorFirmNumber,
          String officeGUIDorCode,
          OfficeLiaisonManagerCreateOrLinkV2 request,
          String xCorrelationId,
          String traceparent) {

    validateRequest(request);

    LiaisonManagerLinkResult result =
        officeFirmUseCase.postOfficeLiaisonManager(
            providerFirmGUIDorFirmNumber,
            officeGUIDorCode,
            request,
            EventContext.of(xCorrelationId, traceparent));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CreateProviderFirmOfficeLiaisonManager201Response()
                .data(
                    new CreateProviderFirmOfficeLiaisonManager201ResponseData()
                        .providerFirmGUID(result.providerFirmGuid())
                        .providerFirmNumber(result.providerFirmNumber())
                        .officeGUID(result.officeGuid())
                        .officeCode(result.officeCode())
                        .liaisonManagerGUID(result.liaisonManagerGuid())));
  }

  private static LiaisonManagerV2 toLiaisonManagerV2(OfficeLiaisonManagerLinkEntity link) {
    var m = link.getLiaisonManager();
    return new LiaisonManagerV2()
        .guid(m.getGuid())
        .version(m.getVersion())
        .createdBy(m.getCreatedBy())
        .createdTimestamp(m.getCreatedTimestamp())
        .lastUpdatedBy(m.getLastUpdatedBy())
        .lastUpdatedTimestamp(m.getLastUpdatedTimestamp())
        .firstName(m.getFirstName())
        .lastName(m.getLastName())
        .emailAddress(m.getEmailAddress())
        .telephoneNumber(m.getTelephoneNumber())
        .activeDateFrom(link.getActiveDateFrom())
        .activeDateTo(link.getActiveDateTo())
        .linkedFlag(link.getLinkedFlag());
  }

  private static void validateRequest(OfficeLiaisonManagerCreateOrLinkV2 request) {
    if (request == null) {
      throw new IllegalArgumentException("Request body must be provided");
    }

    switch (request) {
      case LiaisonManagerCreateV2 create -> {
        if (isBlank(create.getFirstName())
            || isBlank(create.getLastName())
            || isBlank(create.getEmailAddress())
            || isBlank(create.getTelephoneNumber())) {
          throw new IllegalArgumentException(
              "create requires firstName, lastName, emailAddress, telephoneNumber");
        }
      }
      case LiaisonManagerLinkHeadOfficeV2 _ -> { // no additional validation needed
      }
      case LiaisonManagerLinkChambersV2 _ -> { // no additional validation needed
      }
      default ->
          throw new IllegalArgumentException(
              "Unsupported liaison manager request type: " + request);
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
