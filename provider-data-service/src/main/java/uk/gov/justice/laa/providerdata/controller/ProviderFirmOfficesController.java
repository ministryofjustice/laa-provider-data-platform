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
import uk.gov.justice.laa.providerdata.model.PaginatedSearchV2;
import uk.gov.justice.laa.providerdata.model.PaginationV2;
import uk.gov.justice.laa.providerdata.model.SearchCriteriaV2;
import uk.gov.justice.laa.providerdata.service.OfficeCreationResult;
import uk.gov.justice.laa.providerdata.service.OfficeService;
import uk.gov.justice.laa.providerdata.util.PageLinksBuilder;

/**
 * REST controller implementing the Provider Firm Offices API.
 *
 * <p>Delegates to {@link OfficeService}. Operations not yet implemented return 501.
 */
@RestController
public class ProviderFirmOfficesController implements ProviderFirmOfficesApi {

  private static final int DEFAULT_PAGE_SIZE = 100;

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
      String correlationId,
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
            linkToHeadOffice);
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
    LspProviderOfficeLinkEntity link =
        officeService.getLspOffice(providerFirmGUIDorFirmNumber, officeGUIDorCode);
    return ResponseEntity.ok(
        new GetProviderFirmOfficeByGUID200Response().data(officeMapper.toLspOfficeV2(link)));
  }

  @Override
  public ResponseEntity<GetProviderFirmOffices200Response> getProviderFirmOffices(
      String providerFirmGUIDorFirmNumber,
      String correlationId,
      String transparent,
      BigDecimal page,
      BigDecimal pageSize) {
    int pageIndex = page != null ? page.intValue() : 0;
    int size = pageSize != null ? pageSize.intValue() : DEFAULT_PAGE_SIZE;

    if (pageIndex < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }
    if (size < 1) {
      throw new IllegalArgumentException("pageSize must be at least 1");
    }

    Page<LspProviderOfficeLinkEntity> linkPage =
        officeService.getLspOffices(providerFirmGUIDorFirmNumber, pageIndex, size);

    List<OfficeV2> offices =
        linkPage.getContent().stream().map(officeMapper::toLspOfficeV2).toList();

    PaginationV2 pagination =
        new PaginationV2()
            .currentPage(BigDecimal.valueOf(linkPage.getNumber()))
            .pageSize(BigDecimal.valueOf(linkPage.getSize()))
            .totalPages(BigDecimal.valueOf(linkPage.getTotalPages()))
            .totalItems(BigDecimal.valueOf(linkPage.getTotalElements()));

    PaginatedSearchV2 metadata =
        new PaginatedSearchV2().searchCriteria(new SearchCriteriaV2()).pagination(pagination);

    return ResponseEntity.ok(
        new GetProviderFirmOffices200Response()
            .data(
                new GetProviderFirmOffices200ResponseData()
                    .content(offices)
                    .metadata(metadata)
                    .links(PageLinksBuilder.build(pageIndex, size, linkPage.getTotalPages()))));
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
