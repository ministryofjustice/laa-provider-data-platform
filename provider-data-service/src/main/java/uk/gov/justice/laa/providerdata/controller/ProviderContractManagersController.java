package uk.gov.justice.laa.providerdata.controller;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ContractManagersApi;
import uk.gov.justice.laa.providerdata.mapper.ContractManagerMapper;
import uk.gov.justice.laa.providerdata.model.ContractManagerV2;
import uk.gov.justice.laa.providerdata.model.GetProviderContractManagers200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderContractManagers200ResponseData;
import uk.gov.justice.laa.providerdata.service.ProviderContractManagersService;
import uk.gov.justice.laa.providerdata.util.PageLinks;
import uk.gov.justice.laa.providerdata.util.PageMetadata;
import uk.gov.justice.laa.providerdata.util.PageParamValidator;

/**
 * REST controller implementing the contract manager list endpoint defined in {@link
 * ContractManagersApi}.
 */
@RestController
@RequiredArgsConstructor
public class ProviderContractManagersController implements ContractManagersApi {

  private final ProviderContractManagersService providerContractManagersService;
  private final ContractManagerMapper contractManagerMapper;

  @Override
  public ResponseEntity<GetProviderContractManagers200Response> getProviderContractManagers(
      String xCorrelationId,
      String traceparent,
      List<String> contractManagerId,
      String name,
      BigDecimal page,
      BigDecimal pageSize) {

    var pageParams = PageParamValidator.resolve(page, pageSize);

    Page<ContractManagerV2> results =
        providerContractManagersService
            .getContractManagers(contractManagerId, name, pageParams)
            .map(contractManagerMapper::toContractManagerV2);

    return ResponseEntity.ok(
        new GetProviderContractManagers200Response()
            .data(
                new GetProviderContractManagers200ResponseData()
                    .content(results.getContent())
                    .metadata(
                        PageMetadata.builder(results)
                            .search("contractManagerId", contractManagerId)
                            .search("name", name)
                            .build())
                    .links(PageLinks.of(results))));
  }
}
