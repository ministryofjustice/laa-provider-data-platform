package uk.gov.justice.laa.providerdata.controller;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ProviderFirmOfficesApi;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOffice201Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeByGUID200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOffices200Response;
import uk.gov.justice.laa.providerdata.model.LSPOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.OfficePatchV2;
import uk.gov.justice.laa.providerdata.service.OfficeService;

/**
 * REST controller implementing the Provider Firm Offices API.
 *
 * <p>Delegates to {@link OfficeService}. Operations not yet implemented return 501.
 */
@RestController
public class ProviderFirmOfficesController implements ProviderFirmOfficesApi {

  private final OfficeService officeService;

  public ProviderFirmOfficesController(OfficeService officeService) {
    this.officeService = officeService;
  }

  @Override
  public ResponseEntity<CreateProviderFirmOffice201Response> createProviderFirmOffice(
      String providerFirmGUIDorFirmNumber,
      LSPOfficeCreateV2 lspOfficeCreateV2,
      String correlationId,
      String transparent) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(officeService.createLspOffice(providerFirmGUIDorFirmNumber, lspOfficeCreateV2));
  }

  @Override
  public ResponseEntity<GetProviderFirmOffices200Response> getOffices(
      String correlationId,
      String transparent,
      List<String> officeGUID,
      List<String> officeCode,
      Boolean allProviderOffices,
      BigDecimal page,
      BigDecimal pageSize) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<GetProviderFirmOfficeByGUID200Response> getProviderFirmOfficeByGUID(
      String providerFirmGUIDorFirmNumber,
      String officeGUIDorCode,
      String correlationId,
      String transparent) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<GetProviderFirmOffices200Response> getProviderFirmOffices(
      String providerFirmGUIDorFirmNumber,
      String correlationId,
      String transparent,
      BigDecimal page,
      BigDecimal pageSize) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public ResponseEntity<CreateProviderFirmOffice201Response> updateProviderFirmOffice(
      String providerFirmGUIDorFirmNumber,
      String officeGUIDorCode,
      OfficePatchV2 officePatchV2,
      String correlationId,
      String transparent) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
