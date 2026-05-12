package uk.gov.justice.laa.providerdata.bankaccount.web;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.providerdata.api.ProviderFirmBankAccountsApi;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountMapper;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountQueryService;
import uk.gov.justice.laa.providerdata.model.BankAccountV2;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmBankAccounts200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmBankAccounts200ResponseData;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeBankAccounts200Response;
import uk.gov.justice.laa.providerdata.model.GetProviderFirmOfficeBankAccounts200ResponseData;
import uk.gov.justice.laa.providerdata.model.OfficeBankAccountV2;
import uk.gov.justice.laa.providerdata.office.OfficeQueryService;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;
import uk.gov.justice.laa.providerdata.shared.PageLinks;
import uk.gov.justice.laa.providerdata.shared.PageMetadata;
import uk.gov.justice.laa.providerdata.shared.PageParamValidator;

/** REST controller for provider firm bank account retrieval. */
@RestController
@RequiredArgsConstructor
public class ProviderFirmBankAccountsController implements ProviderFirmBankAccountsApi {

  private final ProviderQueryService providerService;
  private final OfficeQueryService officeService;
  private final BankAccountQueryService bankDetailsService;
  private final BankAccountMapper bankAccountMapper;

  @Override
  public ResponseEntity<GetProviderFirmBankAccounts200Response> getProviderFirmBankAccounts(
      String providerFirmGUIDorFirmNumber,
      @Nullable String xCorrelationId,
      @Nullable String traceparent,
      @Nullable String bankAccountNumber,
      @Nullable Integer page,
      @Nullable Integer pageSize) {

    var pageParams = PageParamValidator.resolve(page, pageSize);

    ProviderEntity provider = providerService.getProvider(providerFirmGUIDorFirmNumber);

    Page<BankAccountV2> results =
        bankDetailsService
            .getProviderBankAccounts(provider, bankAccountNumber, pageParams)
            .map(bankAccountMapper::toBankAccountV2);

    return ResponseEntity.ok(
        new GetProviderFirmBankAccounts200Response()
            .data(
                new GetProviderFirmBankAccounts200ResponseData()
                    .content(results.getContent())
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
          @Nullable Integer page,
          @Nullable Integer pageSize) {

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
