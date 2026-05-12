package uk.gov.justice.laa.providerdata.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirm201Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmByGUIDorFirmNumber200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirms200Response;
import uk.gov.justice.laa.providerdata.model.ProviderCreateV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;

/**
 * REST controller for provider firm operations.
 *
 * <p>This controller is kept for backwards compatibility. Each method delegates to either {@link
 * ProviderFirmCommandController} (POST, PATCH) or {@link ProviderFirmQueryController} (GET). New
 * consumers should use the versioned {@code /v2/provider-firms} endpoints directly.
 */
@RestController
public class ProviderFirmController {

  private final ProviderFirmCommandController commandController;
  private final ProviderFirmQueryController queryController;

  /**
   * Inject dependencies.
   *
   * @param commandController handles provider firm write operations (POST, PATCH)
   * @param queryController handles provider firm read operations (GET)
   */
  public ProviderFirmController(
      ProviderFirmCommandController commandController,
      ProviderFirmQueryController queryController) {
    this.commandController = commandController;
    this.queryController = queryController;
  }

  /**
   * Creates a new provider firm. Delegates to {@link ProviderFirmCommandController}.
   *
   * @param request the provider creation request
   * @return 201 with the assigned GUID and firm number
   */
  @PostMapping(
      path = "/provider-firms",
      consumes = "application/json",
      produces = "application/json")
  public ResponseEntity<CreateProviderFirm201Response> createProviderFirm(
      @RequestBody ProviderCreateV2 request) {
    return commandController.createProviderFirm(request);
  }

  /**
   * Retrieves a paginated list of provider firms. Delegates to {@link ProviderFirmQueryController}.
   *
   * @return paginated list of provider firms
   */
  @GetMapping(path = "/provider-firms", produces = "application/json")
  public ResponseEntity<GetProviderFirms200Response> getProviderFirms(
      @RequestHeader(value = "X-Correlation-Id", required = false) String xCorrelationId,
      @RequestHeader(value = "traceparent", required = false) String traceparent,
      @RequestParam(required = false) List<String> providerFirmGUID,
      @RequestParam(required = false) List<String> providerFirmNumber,
      @RequestParam(required = false) String name,
      @Parameter(
              description = "Filter by provider firm type",
              schema = @Schema(implementation = ProviderFirmTypeV2.class))
          @RequestParam(required = false)
          List<ProviderFirmTypeV2> type,
      @RequestParam(required = false) String activeStatus,
      @RequestParam(required = false) List<String> accountNumber,
      @RequestParam(required = false) List<String> practitionerRollNumber,
      @RequestParam(required = false) List<String> parentFirmGUID,
      @RequestParam(required = false) List<String> parentFirmNumber,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "pageSize", required = false) Integer pageSize) {
    return queryController.getProviderFirms(
        xCorrelationId,
        traceparent,
        providerFirmGUID,
        providerFirmNumber,
        name,
        type,
        activeStatus,
        page,
        pageSize);
  }

  /**
   * Retrieves a provider firm by GUID or firm number. Delegates to {@link
   * ProviderFirmQueryController}.
   *
   * @param providerFirmGUIDorFirmNumber provider GUID or firm number
   * @return 200 with the provider details
   */
  @GetMapping(
      path = "/provider-firms/{providerFirmGUIDorFirmNumber}",
      produces = "application/json")
  public ResponseEntity<GetProviderFirmByGUIDorFirmNumber200Response> getProviderFirm(
      @PathVariable String providerFirmGUIDorFirmNumber) {
    return queryController.getProviderFirm(providerFirmGUIDorFirmNumber);
  }

  /**
   * Updates supported provider details. Delegates to {@link ProviderFirmCommandController}.
   *
   * @param providerFirmGUIDorFirmNumber provider GUID or firm number
   * @param request patch request
   * @return 200 with the updated identifiers
   */
  @PatchMapping(
      path = "/provider-firms/{providerFirmGUIDorFirmNumber}",
      consumes = "application/json",
      produces = "application/json")
  public ResponseEntity<CreateProviderFirm201Response> patchProviderFirm(
      @PathVariable String providerFirmGUIDorFirmNumber, @RequestBody ProviderPatchV2 request) {
    return commandController.patchProviderFirm(providerFirmGUIDorFirmNumber, request);
  }
}
