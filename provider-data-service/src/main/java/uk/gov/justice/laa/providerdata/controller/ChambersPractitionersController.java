package uk.gov.justice.laa.providerdata.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ChambersPractitionersApi;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.mapper.ProviderMapper;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmPractitioners200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmPractitioners200ResponseData;
import uk.gov.justice.laa.providerdata.model.OfficePractitionerV2;
import uk.gov.justice.laa.providerdata.service.ProviderService;
import uk.gov.justice.laa.providerdata.util.PageLinks;
import uk.gov.justice.laa.providerdata.util.PageMetadata;
import uk.gov.justice.laa.providerdata.util.PageParamValidator;

/** REST controller implementing the Chambers' practitioners endpoint. */
@RestController
public class ChambersPractitionersController implements ChambersPractitionersApi {

  private final ProviderService providerService;
  private final ProviderMapper providerMapper;

  public ChambersPractitionersController(
      ProviderService providerService, ProviderMapper providerMapper) {
    this.providerService = providerService;
    this.providerMapper = providerMapper;
  }

  @Override
  public ResponseEntity<GetProviderFirmPractitioners200Response> getProviderFirmPractitioners(
      String providerFirmGUIDorFirmNumber, String traceparent, Integer page, Integer pageSize) {
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
        new GetProviderFirmPractitioners200Response()
            .data(
                new GetProviderFirmPractitioners200ResponseData()
                    .content(practitioners.getContent())
                    .metadata(PageMetadata.of(practitioners))
                    .links(PageLinks.of(practitioners))));
  }
}
