package uk.gov.justice.laa.providerdata.controller;

import java.util.List;
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
import uk.gov.justice.laa.providerdata.model.OfficeContractManagerV2;
import uk.gov.justice.laa.providerdata.service.ContractManagerService;
import uk.gov.justice.laa.providerdata.service.OfficeContractManagerAssignmentService;

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
   * <p>MVP behaviour:
   *
   * <ul>
   *   <li>The office identifier must be a valid GUID.
   *   <li>The request body must contain a <code>contractManagerGUID</code>, also a valid GUID.
   *   <li>The service replaces any existing assignment for that office.
   *   <li>The response echoes the linked identifiers without performing further lookups.
   * </ul>
   *
   * @param providerFirmGUIDorFirmNumber provider firm identifier (unused in MVP)
   * @param officeGUIDorCode the office GUID as a string
   * @param contractManagerProviderPatchV2 body containing the contract manager GUID
   * @param xCorrelationId request correlation ID (optional)
   * @param transparent used for API debugging or pass‑through behaviour (unused in MVP)
   * @return HTTP 201 response containing minimal assignment details
   * @throws IllegalArgumentException if the given office or contract manager IDs are not GUIDs
   */
  @Override
  public ResponseEntity<CreateProviderFirmOfficeContractManager201Response>
      createProviderFirmOfficeContractManager(
          String providerFirmGUIDorFirmNumber,
          String officeGUIDorCode,
          ContractManagerProviderPatchV2 contractManagerProviderPatchV2,
          String xCorrelationId,
          String transparent) {

    UUID officeGuid = parseGuidOrThrow("officeGUIDorCode", officeGUIDorCode);

    String rawMgrGuid = contractManagerProviderPatchV2.getContractManagerGUID();
    if (rawMgrGuid == null || rawMgrGuid.isBlank()) {
      throw new IllegalArgumentException("contractManagerGUID must be provided");
    }
    UUID contractManagerGuid = parseGuidOrThrow("contractManagerGUID", rawMgrGuid);

    OfficeContractManagerAssignmentService.AssignmentResult result =
        assignmentService.assign(officeGuid, contractManagerGuid);

    // MVP response: populate what we can without additional lookups
    CreateProviderFirmOfficeContractManager201ResponseData data =
        new CreateProviderFirmOfficeContractManager201ResponseData()
            .officeGUID(result.officeGuid().toString())
            .contractManagerId(result.contractManagerId());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new CreateProviderFirmOfficeContractManager201Response().data(data));
  }

  /**
   * Retrieves all contract manager assignments for an office.
   *
   * <p>Currently not required for MVP and therefore returns HTTP 501 NOT_IMPLEMENTED.
   *
   * @param providerFirmGUIDorFirmNumber provider firm identifier (unused)
   * @param officeGUIDorCode office identifier (unused)
   * @param xCorrelationId request correlation ID (unused)
   * @param transparent debugging/trace option (unused)
   * @return 501 NOT_IMPLEMENTED with an empty response body
   */
  @Override
  public ResponseEntity<GetProviderFirmOfficeContractManagers200Response>
      getProviderFirmOfficeContractManagers(
          String providerFirmGUIDorFirmNumber,
          String officeGUIDorCode,
          String xCorrelationId,
          String transparent) {

    List<OfficeContractManagerV2> managers =
        contractManagerService.getContractManagers(officeGUIDorCode, providerFirmGUIDorFirmNumber);

    GetProviderFirmOfficeContractManagers200Response response =
        new GetProviderFirmOfficeContractManagers200Response();

    GetProviderFirmOfficeContractManagers200ResponseData data =
        new GetProviderFirmOfficeContractManagers200ResponseData();
    data.setContent(managers);
    response.setData(data);

    return ResponseEntity.ok(response);
  }

  /**
   * Parses a GUID from a string or throws an informative {@link IllegalArgumentException}.
   *
   * @param field the logical name of the field being parsed (used in error messages)
   * @param value the string to parse
   * @return parsed {@link UUID}
   * @throws IllegalArgumentException if the value is not a valid UUID
   */
  private static UUID parseGuidOrThrow(String field, String value) {
    try {
      return UUID.fromString(value);
    } catch (Exception e) {
      throw new IllegalArgumentException(field + " must be a GUID for MVP. Value was: " + value);
    }
  }
}
