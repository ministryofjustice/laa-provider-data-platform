package uk.gov.justice.laa.providerdata.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmByGUIDorFirmNumber200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirms200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirms200ResponseData;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;
import uk.gov.justice.laa.providerdata.service.ProviderFirmQueryService;
import uk.gov.justice.laa.providerdata.util.PageLinks;
import uk.gov.justice.laa.providerdata.util.PageMetadata;
import uk.gov.justice.laa.providerdata.util.PageParamValidator;

/**
 * REST controller for provider firm read (query) operations.
 *
 * <p>Handles all GET endpoints for provider firms, delegating to {@link ProviderFirmQueryService}.
 * Write operations are handled by {@code ProviderFirmCommandController}.
 */
@RestController
public class ProviderFirmQueryController {

  private final ProviderFirmQueryService queryService;

  /**
   * Inject dependencies.
   *
   * @param queryService handles provider firm read operations and returns {@link ProviderV2} models
   */
  public ProviderFirmQueryController(ProviderFirmQueryService queryService) {
    this.queryService = queryService;
  }

  /**
   * Retrieves a paginated list of provider firms with optional filters.
   *
   * <p>Supports filtering by GUID, firm number, name, type, and active status. Multiple filters are
   * combined using AND logic. Pagination is supported via page and pageSize parameters.
   *
   * @return paginated list of provider firms as {@link ProviderV2} read models
   */
  @GetMapping(path = "/v2/provider-firms", produces = "application/json")
  public ResponseEntity<GetProviderFirms200Response> getProviderFirms(
      @RequestHeader(value = "X-Correlation-Id", required = false) String xCorrelationId,
      @RequestHeader(value = "traceparent", required = false) String traceparent,

      // Filters
      @RequestParam(required = false) List<String> providerFirmGUID,
      @RequestParam(required = false) List<String> providerFirmNumber,
      @RequestParam(required = false) String name,
      @Parameter(
              description = "Filter by provider firm type",
              schema = @Schema(implementation = ProviderFirmTypeV2.class))
          @RequestParam(required = false)
          List<ProviderFirmTypeV2> type,
      @RequestParam(required = false) String activeStatus,

      // Pagination
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "pageSize", required = false) Integer pageSize) {

    Pageable pageable = PageParamValidator.resolve(page, pageSize);

    Page<ProviderV2> result =
        queryService.searchProviderFirms(
            providerFirmGUID, providerFirmNumber, name, type, pageable);

    return ResponseEntity.ok(
        new GetProviderFirms200Response()
            .data(
                new GetProviderFirms200ResponseData()
                    .content(result.getContent())
                    .metadata(
                        PageMetadata.builder(result)
                            .search("providerFirmGUID", providerFirmGUID)
                            .search("providerFirmNumber", providerFirmNumber)
                            .search("name", name)
                            .search(
                                "type",
                                type != null
                                    ? type.stream().map(ProviderFirmTypeV2::getValue).toList()
                                    : null)
                            .build())
                    .links(PageLinks.of(result))));
  }

  /**
   * Retrieves a provider firm by GUID or firm number.
   *
   * @param providerFirmGUIDorFirmNumber provider GUID (primary key) or firm number (unique key)
   * @return 200 with the provider details as a {@link ProviderV2} read model
   */
  @GetMapping(
      path = "/v2/provider-firms/{providerFirmGUIDorFirmNumber}",
      produces = "application/json")
  public ResponseEntity<GetProviderFirmByGUIDorFirmNumber200Response> getProviderFirm(
      @PathVariable String providerFirmGUIDorFirmNumber) {
    ProviderV2 readModel = queryService.getProviderFirm(providerFirmGUIDorFirmNumber);
    return ResponseEntity.ok(new GetProviderFirmByGUIDorFirmNumber200Response().data(readModel));
  }
}
