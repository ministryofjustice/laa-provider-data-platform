package uk.gov.justice.laa.providerdata.controller;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ProviderFirmOfficesApi;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.mapper.OfficeMapper;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOffice201Response;
import uk.gov.justice.laa.providerdata.model.CreateProviderFirmOffice201ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeByGUID200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOffices200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOffices200ResponseData;
import uk.gov.justice.laa.providerdata.model.LSPOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.OfficePatchV2;
import uk.gov.justice.laa.providerdata.model.OfficeV2;
import uk.gov.justice.laa.providerdata.service.OfficeCreationResult;
import uk.gov.justice.laa.providerdata.service.OfficeService;
import uk.gov.justice.laa.providerdata.util.PageLinks;
import uk.gov.justice.laa.providerdata.util.PageMetadata;
import uk.gov.justice.laa.providerdata.util.PageParamValidator;

/**
 * REST controller implementing the Provider Firm Offices API.
 *
 * <p>Delegates to {@link OfficeService}. Operations not yet implemented return 501.
 */
@RestController
public class ProviderFirmOfficesController implements ProviderFirmOfficesApi {

  private final OfficeService officeService;
  private final OfficeMapper officeMapper;

  public ProviderFirmOfficesController(OfficeService officeService, OfficeMapper officeMapper) {
    this.officeService = officeService;
    this.officeMapper = officeMapper;
  }

  @Override
  public ResponseEntity<CreateProviderFirmOffice201Response> createProviderFirmOffice(
      String providerFirmGUIDorFirmNumber,
      LSPOfficeCreateV2 lspOfficeCreateV2,
      String xCorrelationId,
      String transparent) {

    LiaisonManagerEntity lmEntity = null;
    OfficeLiaisonManagerLinkEntity lmLinkTemplate = null;
    boolean linkToHeadOffice = false;

    if (lspOfficeCreateV2.getLiaisonManager() instanceof LiaisonManagerCreateV2 lmCreate) {
      lmEntity = officeMapper.toLiaisonManagerEntity(lmCreate);
      lmLinkTemplate = officeMapper.toLiaisonManagerLinkTemplate(lmCreate);
    } else if (lspOfficeCreateV2.getLiaisonManager() != null) {
      // LiaisonManagerLinkHeadOfficeV2: link to head office's active liaison manager
      linkToHeadOffice = true;
    }

    OfficeCreationResult result =
        officeService.createLspOffice(
            providerFirmGUIDorFirmNumber,
            officeMapper.toOfficeEntity(lspOfficeCreateV2),
            officeMapper.toLinkTemplate(lspOfficeCreateV2),
            lmEntity,
            lmLinkTemplate,
            linkToHeadOffice,
            lspOfficeCreateV2.getPayment());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new CreateProviderFirmOffice201Response()
                .data(
                    new CreateProviderFirmOffice201ResponseData()
                        .providerFirmGUID(result.providerGUID().toString())
                        .providerFirmNumber(result.firmNumber())
                        .officeGUID(result.officeGUID().toString())
                        .officeCode(result.accountNumber())));
  }

  @Override
  public ResponseEntity<GetProviderFirmOffices200Response> getOffices(
      String xCorrelationId,
      String transparent,
      List<String> officeGUID,
      List<String> officeCode,
      Boolean allProviderOffices,
      BigDecimal page,
      BigDecimal pageSize) {
    var pageParams = PageParamValidator.resolve(page, pageSize);

    Page<ProviderOfficeLinkEntity> results =
        officeService.getOfficesGlobal(officeGUID, officeCode, allProviderOffices, pageParams);

    List<OfficeV2> offices = results.getContent().stream().map(officeMapper::toOfficeV2).toList();

    return ResponseEntity.ok(
        new GetProviderFirmOffices200Response()
            .data(
                new GetProviderFirmOffices200ResponseData()
                    .content(offices)
                    .metadata(
                        PageMetadata.builder(results)
                            .search("officeGUID", officeGUID)
                            .search("officeCode", officeCode)
                            .search("allProviderOffices", allProviderOffices)
                            .build())
                    .links(PageLinks.of(results))));
  }

  @Override
  public ResponseEntity<GetProviderFirmOfficeByGUID200Response> getProviderFirmOfficeByGUID(
      String providerFirmGUIDorFirmNumber,
      String officeGUIDorCode,
      String xCorrelationId,
      String transparent) {
    LspProviderOfficeLinkEntity link =
        officeService.getLspOffice(providerFirmGUIDorFirmNumber, officeGUIDorCode);
    return ResponseEntity.ok(
        new GetProviderFirmOfficeByGUID200Response().data(officeMapper.toLspOfficeV2(link)));
  }

  @Override
  public ResponseEntity<GetProviderFirmOffices200Response> getProviderFirmOffices(
      String providerFirmGUIDorFirmNumber,
      String xCorrelationId,
      String transparent,
      BigDecimal page,
      BigDecimal pageSize) {
    var pageParams = PageParamValidator.resolve(page, pageSize);

    Page<ProviderOfficeLinkEntity> results =
        officeService.getOffices(providerFirmGUIDorFirmNumber, pageParams);

    List<OfficeV2> offices = results.getContent().stream().map(officeMapper::toOfficeV2).toList();

    return ResponseEntity.ok(
        new GetProviderFirmOffices200Response()
            .data(
                new GetProviderFirmOffices200ResponseData()
                    .content(offices)
                    .metadata(PageMetadata.of(results))
                    .links(PageLinks.of(results))));
  }

  @Override
  public ResponseEntity<CreateProviderFirmOffice201Response> updateProviderFirmOffice(
      String providerFirmGUIDorFirmNumber,
      String officeGUIDorCode,
      OfficePatchV2 officePatchV2,
      String xCorrelationId,
      String transparent) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
