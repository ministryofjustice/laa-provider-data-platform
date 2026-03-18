package uk.gov.justice.laa.providerdata.controller;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ContractManagersApi;
import uk.gov.justice.laa.providerdata.model.ContractManagerV2;
import uk.gov.justice.laa.providerdata.model.GetProviderContractManagers200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderContractManagers200ResponseData;
import uk.gov.justice.laa.providerdata.service.ProviderContractManagersService;

/**
 * REST controller implementing the contract manager list endpoint defined in {@link
 * ContractManagersApi}.
 */
@RestController
@RequiredArgsConstructor
public class ProviderContractManagersController implements ContractManagersApi {

  private final ProviderContractManagersService providerContractManagersService;

  @Override
  public ResponseEntity<GetProviderContractManagers200Response> getProviderContractManagers(
      String xCorrelationId,
      String traceparent,
      List<String> contractManagerId,
      String name,
      BigDecimal page,
      BigDecimal pageSize) {

    List<ContractManagerV2> content =
        providerContractManagersService.getContractManagers(contractManagerId, name);

    GetProviderContractManagers200ResponseData data =
        new GetProviderContractManagers200ResponseData();
    data.setContent(content);
    // MVP: pagination/links optional for now (leave null)

    GetProviderContractManagers200Response response = new GetProviderContractManagers200Response();
    response.setData(data);

    return ResponseEntity.ok(response);
  }
}
