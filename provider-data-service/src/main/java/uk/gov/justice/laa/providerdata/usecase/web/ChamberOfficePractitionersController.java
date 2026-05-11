package uk.gov.justice.laa.providerdata.usecase.web;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ChamberOfficePractitionersApi;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficePractitioners200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficePractitioners200ResponseData;
import uk.gov.justice.laa.providerdata.model.OfficePractitionerV2;
import uk.gov.justice.laa.providerdata.office.OfficeQueryService;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;
import uk.gov.justice.laa.providerdata.shared.PageLinks;
import uk.gov.justice.laa.providerdata.shared.PageMetadata;
import uk.gov.justice.laa.providerdata.shared.PageParamValidator;
import uk.gov.justice.laa.providerdata.usecase.ProviderMapper;

/** REST controller implementing the chamber practitioners endpoint. */
@RestController
@RequiredArgsConstructor
public class ChamberOfficePractitionersController implements ChamberOfficePractitionersApi {

  private final ProviderQueryService providerService;
  private final OfficeQueryService officeQueryService;
  private final ProviderMapper providerMapper;

  @Override
  public ResponseEntity<GetProviderFirmOfficePractitioners200Response>
      getProviderFirmOfficePractitioners(
          String providerFirmGUIDorFirmNumber,
          String xCorrelationId,
          String traceparent,
          Integer page,
          Integer pageSize) {
    var pageParams = PageParamValidator.resolve(page, pageSize);
    Page<OfficePractitionerV2> practitioners =
        providerService
            .getPractitionersByChambers(providerFirmGUIDorFirmNumber, pageParams)
            .map(
                link -> {
                  ProviderEntity practitioner = link.getProvider();
                  return providerMapper.toOfficePractitionerV2(
                      practitioner,
                      officeQueryService.getAdvocateOfficeLink(practitioner).orElse(null),
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
