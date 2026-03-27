package uk.gov.justice.laa.providerdata.controller;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ProviderFirmBankAccountsApi;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.mapper.BankAccountMapper;
import uk.gov.justice.laa.providerdata.model.BankAccountV2;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmBankAccounts200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmBankAccounts200ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeBankAccounts200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeBankAccounts200ResponseData;
import uk.gov.justice.laa.providerdata.model.OfficeBankAccountV2;
import uk.gov.justice.laa.providerdata.service.BankDetailsService;
import uk.gov.justice.laa.providerdata.service.OfficeService;
import uk.gov.justice.laa.providerdata.service.ProviderService;
import uk.gov.justice.laa.providerdata.util.PageLinks;
import uk.gov.justice.laa.providerdata.util.PageMetadata;
import uk.gov.justice.laa.providerdata.util.PageParamValidator;

/** REST controller for provider firm bank account retrieval. */
@RestController
public class ProviderFirmBankAccountsController implements ProviderFirmBankAccountsApi {

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
      @Nullable String traceparent,
      @Nullable String bankAccountNumber,
      @Nullable BigDecimal page,
      @Nullable BigDecimal pageSize) {

    var pageParams = PageParamValidator.resolve(page, pageSize);

    ProviderEntity provider = providerService.getProvider(providerFirmGUIDorFirmNumber);

    Page<ProviderBankAccountLinkEntity> results =
        bankDetailsService.getProviderBankAccounts(provider, bankAccountNumber, pageParams);

    List<BankAccountV2> accounts =
        results.getContent().stream().map(bankAccountMapper::toBankAccountV2).toList();

    return ResponseEntity.ok(
        new GetProviderFirmBankAccounts200Response()
            .data(
                new GetProviderFirmBankAccounts200ResponseData()
                    .content(accounts)
                    .metadata(
                        PageMetadata.builder(results)
                            .search("bankAccountNumber", bankAccountNumber)
                            .build())
                    .links(PageLinks.of(results))));
  }

  @Override
  public ResponseEntity<GetProviderFirmOfficeBankAccounts200Response>
      getProviderFirmOfficeBankAccounts(
          String providerFirmGUIDorFirmNumber,
          String officeGUIDorCode,
          @Nullable String xCorrelationId,
          @Nullable String traceparent,
          @Nullable String bankAccountNumber,
          @Nullable BigDecimal page,
          @Nullable BigDecimal pageSize) {

    var pageParams = PageParamValidator.resolve(page, pageSize);

    ProviderEntity provider = providerService.getProvider(providerFirmGUIDorFirmNumber);

    ProviderOfficeLinkEntity officeLink =
        officeService.getProviderOfficeLink(provider, officeGUIDorCode);
    Page<OfficeBankAccountV2> results =
        bankDetailsService
            .getOfficeBankAccounts(officeLink, bankAccountNumber, pageParams)
            .map(bankAccountMapper::toOfficeBankAccountV2);

    return ResponseEntity.ok(
        new GetProviderFirmOfficeBankAccounts200Response()
            .data(
                new GetProviderFirmOfficeBankAccounts200ResponseData()
                    .content(results.getContent())
                    .metadata(
                        PageMetadata.builder(results)
                            .search("bankAccountNumber", bankAccountNumber)
                            .build())
                    .links(PageLinks.of(results))));
  }
}
