package uk.gov.justice.laa.providerdata.controller;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ProviderFirmBankAccountsApi;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.mapper.BankAccountMapper;
import uk.gov.justice.laa.providerdata.model.BankAccountV2;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmBankAccounts200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmBankAccounts200ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeBankAccounts200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeBankAccounts200ResponseData;
import uk.gov.justice.laa.providerdata.model.OfficeBankAccountV2;
import uk.gov.justice.laa.providerdata.model.PaginatedSearchV2;
import uk.gov.justice.laa.providerdata.model.PaginationV2;
import uk.gov.justice.laa.providerdata.model.SearchCriteriaV2;
import uk.gov.justice.laa.providerdata.service.BankDetailsService;
import uk.gov.justice.laa.providerdata.service.OfficeService;
import uk.gov.justice.laa.providerdata.service.ProviderService;
import uk.gov.justice.laa.providerdata.util.PageLinksBuilder;

/** REST controller for provider firm bank account retrieval. */
@RestController
public class ProviderFirmBankAccountsController implements ProviderFirmBankAccountsApi {

  private static final int DEFAULT_PAGE_SIZE = 100;

  private final ProviderService providerService;
  private final OfficeService officeService;
  private final BankDetailsService bankDetailsService;
  private final BankAccountMapper bankAccountMapper;

  /**
   * Inject dependencies.
   *
   * @param providerService to resolve provider firms
   * @param officeService to resolve office links
   * @param bankDetailsService to query bank account data
   * @param bankAccountMapper to map bank account entities to response models
   */
  public ProviderFirmBankAccountsController(
      ProviderService providerService,
      OfficeService officeService,
      BankDetailsService bankDetailsService,
      BankAccountMapper bankAccountMapper) {
    this.providerService = providerService;
    this.officeService = officeService;
    this.bankDetailsService = bankDetailsService;
    this.bankAccountMapper = bankAccountMapper;
  }

  @Override
  public ResponseEntity<GetProviderFirmBankAccounts200Response> getProviderFirmBankAccounts(
      String providerFirmGUIDorFirmNumber,
      @Nullable String xCorrelationId,
      @Nullable String transparent,
      @Nullable String bankAccountNumber,
      @Nullable BigDecimal page,
      @Nullable BigDecimal pageSize) {

    int pageIndex = page != null ? page.intValue() : 0;
    int size = pageSize != null ? pageSize.intValue() : DEFAULT_PAGE_SIZE;

    if (pageIndex < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }
    if (size < 1) {
      throw new IllegalArgumentException("pageSize must be at least 1");
    }

    ProviderEntity provider = providerService.getProvider(providerFirmGUIDorFirmNumber);

    if (FirmType.CHAMBERS.equals(provider.getFirmType())) {
      throw new IllegalArgumentException(
          "Bank account details are not available for Chambers providers");
    }

    Page<ProviderBankAccountLinkEntity> linkPage =
        bankDetailsService.getProviderBankAccounts(
            provider, bankAccountNumber, PageRequest.of(pageIndex, size));

    List<BankAccountV2> accounts =
        linkPage.getContent().stream().map(bankAccountMapper::toBankAccountV2).toList();

    PaginationV2 pagination =
        new PaginationV2()
            .currentPage(BigDecimal.valueOf(linkPage.getNumber()))
            .pageSize(BigDecimal.valueOf(linkPage.getSize()))
            .totalPages(BigDecimal.valueOf(linkPage.getTotalPages()))
            .totalItems(BigDecimal.valueOf(linkPage.getTotalElements()));

    PaginatedSearchV2 metadata =
        new PaginatedSearchV2().searchCriteria(new SearchCriteriaV2()).pagination(pagination);

    return ResponseEntity.ok(
        new GetProviderFirmBankAccounts200Response()
            .data(
                new GetProviderFirmBankAccounts200ResponseData()
                    .content(accounts)
                    .metadata(metadata)
                    .links(PageLinksBuilder.build(pageIndex, size, linkPage.getTotalPages()))));
  }

  @Override
  public ResponseEntity<GetProviderFirmOfficeBankAccounts200Response>
      getProviderFirmOfficeBankAccounts(
          String providerFirmGUIDorFirmNumber,
          String officeGUIDorCode,
          @Nullable String xCorrelationId,
          @Nullable String transparent,
          @Nullable String bankAccountNumber,
          @Nullable BigDecimal page,
          @Nullable BigDecimal pageSize) {

    int pageIndex = page != null ? page.intValue() : 0;
    int size = pageSize != null ? pageSize.intValue() : DEFAULT_PAGE_SIZE;

    if (pageIndex < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }
    if (size < 1) {
      throw new IllegalArgumentException("pageSize must be at least 1");
    }

    LspProviderOfficeLinkEntity officeLink =
        officeService.getLspOffice(providerFirmGUIDorFirmNumber, officeGUIDorCode);

    Page<OfficeBankAccountLinkEntity> linkPage =
        bankDetailsService.getOfficeBankAccounts(
            officeLink, bankAccountNumber, PageRequest.of(pageIndex, size));

    List<OfficeBankAccountV2> accounts =
        linkPage.getContent().stream().map(bankAccountMapper::toOfficeBankAccountV2).toList();

    PaginationV2 pagination =
        new PaginationV2()
            .currentPage(BigDecimal.valueOf(linkPage.getNumber()))
            .pageSize(BigDecimal.valueOf(linkPage.getSize()))
            .totalPages(BigDecimal.valueOf(linkPage.getTotalPages()))
            .totalItems(BigDecimal.valueOf(linkPage.getTotalElements()));

    PaginatedSearchV2 metadata =
        new PaginatedSearchV2().searchCriteria(new SearchCriteriaV2()).pagination(pagination);

    return ResponseEntity.ok(
        new GetProviderFirmOfficeBankAccounts200Response()
            .data(
                new GetProviderFirmOfficeBankAccounts200ResponseData()
                    .content(accounts)
                    .metadata(metadata)
                    .links(PageLinksBuilder.build(pageIndex, size, linkPage.getTotalPages()))));
  }
}
