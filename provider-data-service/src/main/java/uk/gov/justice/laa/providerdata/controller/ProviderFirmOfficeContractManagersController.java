package uk.gov.justice.laa.providerdata.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ProviderFirmOfficesContractManagersApi;
import uk.gov.justice.laa.providerdata.model.ContractManagerProviderPatchV2;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOfficeContractManager201Response;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOfficeContractManager201ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeContractManagers200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeContractManagers200ResponseData;
import uk.gov.justice.laa.providerdata.service.ContractManagerService;
import uk.gov.justice.laa.providerdata.service.OfficeContractManagerAssignmentService;
import uk.gov.justice.laa.providerdata.util.PageLinks;
import uk.gov.justice.laa.providerdata.util.PageMetadata;
import uk.gov.justice.laa.providerdata.util.PageParamValidator;

/**
 * REST controller implementing the contract manager endpoints defined in {@link
 * ProviderFirmOfficesContractManagersApi}.
 */
@RestController
public class ProviderFirmOfficeContractManagersController
    implements ProviderFirmOfficesContractManagersApi {

  private final OfficeContractManagerAssignmentService assignmentService;

  private final ContractManagerService contractManagerService;

  /**
   * Constructs the controller with required dependencies.
   *
   * @param assignmentService service handling creation of office-to-contract-manager assignments
   */
  public ProviderFirmOfficeContractManagersController(
      OfficeContractManagerAssignmentService assignmentService,
      ContractManagerService contractManagerService) {
    this.assignmentService = assignmentService;
    this.contractManagerService = contractManagerService;
  }

  /**
   * Creates (or replaces) an assignment between an office and a contract manager.
   *
   * <ul>
   *   <li>The provider identifier may be either a GUID or a firm number.
   *   <li>The office identifier may be either the provider office link GUID or the office code.
   *   <li>The request body must contain a <code>contractManagerGUID</code>, which must be a valid
   *       GUID.
   *   <li>The service replaces any existing assignment for that office.
   *   <li>The response includes the provider office link GUID and contract manager ID.
   * </ul>
   *
   * @param providerFirmGUIDorFirmNumber provider firm identifier
   * @param officeGUIDorCode the provider office link GUID or office code
   * @param contractManagerProviderPatchV2 body containing the contract manager GUID
   * @param xCorrelationId request correlation ID (optional)
   * @param traceparent request traceId and spanId (optional)
   * @return HTTP 201 response containing minimal assignment details
   * @throws IllegalArgumentException if the given contract manager ID is not a GUID
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

    OfficeContractManagerAssignmentService.AssignmentResult result =
        assignmentService.assign(
            providerFirmGUIDorFirmNumber, officeGUIDorCode, contractManagerGuid);

    // populate what we can without additional lookups
    CreateProviderFirmOfficeContractManager201ResponseData data =
        new CreateProviderFirmOfficeContractManager201ResponseData()
            .officeGUID(result.officeGuid())
            .contractManagerId(result.contractManagerId());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CreateProviderFirmOfficeContractManager201Response().data(data));
  }

  /**
   * Retrieves contract manager assignments for an office.
   *
   * @param providerFirmGUIDorFirmNumber provider firm identifier
   * @param officeGUIDorCode office identifier
   * @param xCorrelationId request correlation ID
   * @param traceparent debugging/trace option
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

    GetProviderFirmOfficeContractManagers200ResponseData data =
        new GetProviderFirmOfficeContractManagers200ResponseData();
    data.setContent(managers.getContent());
    data.setMetadata(PageMetadata.of(managers));
    data.setLinks(PageLinks.of(managers));

    GetProviderFirmOfficeContractManagers200Response response =
        new GetProviderFirmOfficeContractManagers200Response();
    response.setData(data);

    return ResponseEntity.ok(response);
  }
}
