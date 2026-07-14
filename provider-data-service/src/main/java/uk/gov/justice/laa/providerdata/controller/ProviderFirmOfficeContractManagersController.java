package uk.gov.justice.laa.providerdata.controller;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.justice.laa.providerdata.api.ProviderFirmOfficesContractManagersApi;
import uk.gov.justice.laa.providerdata.model.ContractManagerLinkByGUIDV2;
import uk.gov.justice.laa.providerdata.model.ContractManagerLinkDefaultV2;
import uk.gov.justice.laa.providerdata.model.ContractManagerLinkHeadOfficeV2;
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
   * @param contractManagerService service handling retrieval of contract manager assignments
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
   *   <li>Exactly one of {@code contractManagerGUID}, {@code useDefaultContractManager}, or {@code
   *       useHeadOfficeContractManager} must be provided; any other combination is rejected with
   *       400.
   *   <li>The service replaces any existing assignment for that office.
   *   <li>The response includes the provider office link GUID and contract manager ID.
   * </ul>
   *
   * @param providerFirmGUIDorFirmNumber provider firm identifier
   * @param officeGUIDorCode the provider office link GUID or office code
   * @param contractManagerProviderPatchV2 body specifying the contract manager to assign
   * @param traceparent request traceId and spanId (optional)
   * @return HTTP 201 response containing minimal assignment details
   */
  @Override
  public ResponseEntity<CreateProviderFirmOfficeContractManager201Response>
      createProviderFirmOfficeContractManager(
          String providerFirmGUIDorFirmNumber,
          String officeGUIDorCode,
          ContractManagerProviderPatchV2 contractManagerProviderPatchV2,
          String traceparent) {
    ContractManagerSelection selection = resolveContractManager(contractManagerProviderPatchV2);
    OfficeContractManagerAssignmentService.AssignmentResult result =
        assignmentService.assign(
            providerFirmGUIDorFirmNumber,
            officeGUIDorCode,
            selection.guid(),
            selection.useDefault(),
            selection.useHeadOffice());

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

  private ContractManagerSelection resolveContractManager(
      ContractManagerProviderPatchV2 contractManager) {
    if (contractManager instanceof ContractManagerLinkDefaultV2) {
      return new ContractManagerSelection(null, true, false);
    } else if (contractManager instanceof ContractManagerLinkHeadOfficeV2) {
      return new ContractManagerSelection(null, false, true);
    } else if (contractManager instanceof ContractManagerLinkByGUIDV2 link) {
      UUID guid = link.getContractManagerGUID();
      if (guid == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "contractManagerGUID must not be null");
      }
      return new ContractManagerSelection(guid, false, false);
    } else {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unrecognised contract manager instruction");
    }
  }

  private record ContractManagerSelection(
      @Nullable UUID guid, boolean useDefault, boolean useHeadOffice) {}
}
