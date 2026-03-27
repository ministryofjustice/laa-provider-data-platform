package uk.gov.justice.laa.providerdata.controller;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ChamberOfficePractitionersApi;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.mapper.ProviderMapper;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficePractitioners200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficePractitioners200ResponseData;
import uk.gov.justice.laa.providerdata.model.OfficePractitionerV2;
import uk.gov.justice.laa.providerdata.service.ProviderService;
import uk.gov.justice.laa.providerdata.util.PageLinks;
import uk.gov.justice.laa.providerdata.util.PageMetadata;
import uk.gov.justice.laa.providerdata.util.PageParamValidator;

/** REST controller implementing the chamber practitioners endpoint. */
@RestController
public class ChamberOfficePractitionersController implements ChamberOfficePractitionersApi {

  private final ProviderService providerService;
  private final ProviderMapper providerMapper;

  public ChamberOfficePractitionersController(
      ProviderService providerService, ProviderMapper providerMapper) {
    this.providerService = providerService;
    this.providerMapper = providerMapper;
  }

  @Override
  public ResponseEntity<GetProviderFirmOfficePractitioners200Response>
      getProviderFirmOfficePractitioners(
          String providerFirmGUIDorFirmNumber,
          String xCorrelationId,
          String traceparent,
          BigDecimal page,
          BigDecimal pageSize) {
    var pageParams = PageParamValidator.resolve(page, pageSize);
    Page<OfficePractitionerV2> practitioners =
        providerService
            .getPractitionersByChambers(providerFirmGUIDorFirmNumber, pageParams)
            .map(
                link -> {
                  ProviderEntity practitioner = link.getProvider();
                  return providerMapper.toOfficePractitionerV2(
                      practitioner,
                      providerService.getAdvocateOfficeLink(practitioner).orElse(null),
                      providerService.getParentLinks(practitioner));
                });

    return ResponseEntity.ok(
        new GetProviderFirmOfficePractitioners200Response()
            .data(
                new GetProviderFirmOfficePractitioners200ResponseData()
                    .content(practitioners.getContent())
                    .metadata(PageMetadata.of(practitioners))
                    .links(PageLinks.of(practitioners))));
  }
}
