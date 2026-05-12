package uk.gov.justice.laa.providerdata.usecase.web;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ProviderFirmOfficesContractManagersApi;
import uk.gov.justice.laa.providerdata.contractmanager.OfficeContractManagerQueryService;
import uk.gov.justice.laa.providerdata.model.ContractManagerProviderPatchV2;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOfficeContractManager201Response;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOfficeContractManager201ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeContractManagers200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeContractManagers200ResponseData;
import uk.gov.justice.laa.providerdata.shared.PageLinks;
import uk.gov.justice.laa.providerdata.shared.PageMetadata;
import uk.gov.justice.laa.providerdata.shared.PageParamValidator;
import uk.gov.justice.laa.providerdata.usecase.ContractManagerAssignmentResult;
import uk.gov.justice.laa.providerdata.usecase.EventContext;
import uk.gov.justice.laa.providerdata.usecase.OfficeFirmUseCase;

/**
 * REST controller implementing the contract manager endpoints defined in {@link
 * ProviderFirmOfficesContractManagersApi}.
 */
@RestController
@RequiredArgsConstructor
public class ProviderFirmOfficeContractManagersController
    implements ProviderFirmOfficesContractManagersApi {

  private final OfficeFirmUseCase officeFirmUseCase;
  private final OfficeContractManagerQueryService contractManagerService;

  /**
   * Creates (or replaces) an assignment between an office and a contract manager.
   *
   * <ul>
   *   <li>The provider identifier may be either a GUID or a firm number.
   *   <li>The office identifier may be either the provider office link GUID or the office code.
   *   <li>The request body must contain a {@code contractManagerGUID}.
   *   <li>Any existing assignment for that office is replaced.
   *   <li>Re-assigning the same contract manager to the same office is idempotent.
   * </ul>
   *
   * @param providerFirmGUIDorFirmNumber provider firm identifier
   * @param officeGUIDorCode provider office link GUID or office code
   * @param contractManagerProviderPatchV2 body containing the contract manager GUID
   * @param xCorrelationId request correlation ID (optional)
   * @param traceparent trace parent header (optional)
   * @return 201 Created with the office GUID and contract manager ID
   * @throws IllegalArgumentException if {@code contractManagerGUID} is missing or unknown
   */
  @Override
  public ResponseEntity<CreateProviderFirmOfficeContractManager201Response>
      createProviderFirmOfficeContractManager(
          String providerFirmGUIDorFirmNumber,
          String officeGUIDorCode,
          ContractManagerProviderPatchV2 contractManagerProviderPatchV2,
          String xCorrelationId,
          String traceparent) {

    UUID contractManagerGuid = contractManagerProviderPatchV2.getContractManagerGUID();
    if (contractManagerGuid == null) {
      throw new IllegalArgumentException("contractManagerGUID must be provided");
    }

    ContractManagerAssignmentResult result =
        officeFirmUseCase.assignContractManager(
            providerFirmGUIDorFirmNumber,
            officeGUIDorCode,
            contractManagerGuid,
            EventContext.of(xCorrelationId, traceparent));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CreateProviderFirmOfficeContractManager201Response()
                .data(
                    new CreateProviderFirmOfficeContractManager201ResponseData()
                        .officeGUID(result.officeGuid())
                        .contractManagerId(result.contractManagerId())));
  }

  /**
   * Retrieves contract manager assignments for a provider firm office.
   *
   * @param providerFirmGUIDorFirmNumber provider firm identifier
   * @param officeGUIDorCode office identifier
   * @param xCorrelationId request correlation ID
   * @param traceparent trace parent header
   * @param page requested page number
   * @param pageSize requested page size
   * @return 200 OK with paginated contract manager response data
   */
  @Override
  public ResponseEntity<GetProviderFirmOfficeContractManagers200Response>
      getProviderFirmOfficeContractManagers(
          String providerFirmGUIDorFirmNumber,
          String officeGUIDorCode,
          String xCorrelationId,
          String traceparent,
          Integer page,
          Integer pageSize) {

    var pageParams = PageParamValidator.resolve(page, pageSize);
    var managers =
        contractManagerService.getContractManagers(
            providerFirmGUIDorFirmNumber, officeGUIDorCode, pageParams);

    return ResponseEntity.ok(
        new GetProviderFirmOfficeContractManagers200Response()
            .data(
                new GetProviderFirmOfficeContractManagers200ResponseData()
                    .content(managers.getContent())
                    .metadata(PageMetadata.of(managers))
                    .links(PageLinks.of(managers))));
  }
}
