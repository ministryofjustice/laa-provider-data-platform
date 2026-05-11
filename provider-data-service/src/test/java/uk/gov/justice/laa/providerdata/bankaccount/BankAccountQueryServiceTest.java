package uk.gov.justice.laa.providerdata.bankaccount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.justice.laa.providerdata.bankaccount.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.bankaccount.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.bankaccount.repository.OfficeBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.bankaccount.repository.ProviderBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.office.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;
import uk.gov.justice.laa.providerdata.shared.FirmType;

@ExtendWith(MockitoExtension.class)
class BankAccountQueryServiceTest {

  @Mock private ProviderBankAccountLinkRepository providerBankAccountLinkRepository;
  @Mock private OfficeBankAccountLinkRepository officeBankAccountLinkRepository;
  @Mock private ProviderQueryService providerQueryService;
  @Mock private uk.gov.justice.laa.providerdata.office.OfficeQueryService officeQueryService;

  @InjectMocks private BankAccountQueryService service;

  // --- getProviderBankAccounts ---

  @Test
  void getProviderBankAccounts_lspWithNoFilter_queriesByProviderOnly() {
    ProviderEntity provider = providerEntity(FirmType.LEGAL_SERVICES_PROVIDER);
    var pageable = PageRequest.of(0, 10);
    var page = new PageImpl<ProviderBankAccountLinkEntity>(List.of());

    when(providerBankAccountLinkRepository.findByProviderIn(List.of(provider), pageable))
        .thenReturn(page);

    var result = service.getProviderBankAccounts(provider, null, pageable);

    assertThat(result).isEqualTo(page);
    verify(providerBankAccountLinkRepository).findByProviderIn(List.of(provider), pageable);
  }

  @Test
  void getProviderBankAccounts_lspWithFilter_queriesWithAccountNumberFilter() {
    ProviderEntity provider = providerEntity(FirmType.LEGAL_SERVICES_PROVIDER);
    var pageable = PageRequest.of(0, 10);
    var page = new PageImpl<ProviderBankAccountLinkEntity>(List.of());

    when(providerBankAccountLinkRepository
            .findByProviderInAndBankAccount_AccountNumberContainingIgnoreCase(
                List.of(provider), "1234", pageable))
        .thenReturn(page);

    var result = service.getProviderBankAccounts(provider, "1234", pageable);

    assertThat(result).isEqualTo(page);
  }

  @Test
  void getProviderBankAccounts_advocate_queriesJustTheAdvocate() {
    ProviderEntity advocate = providerEntity(FirmType.ADVOCATE);
    var pageable = PageRequest.of(0, 10);
    var page = new PageImpl<ProviderBankAccountLinkEntity>(List.of());

    when(providerBankAccountLinkRepository.findByProviderIn(List.of(advocate), pageable))
        .thenReturn(page);

    var result = service.getProviderBankAccounts(advocate, null, pageable);

    assertThat(result).isEqualTo(page);
    verify(providerBankAccountLinkRepository).findByProviderIn(List.of(advocate), pageable);
  }

  @Test
  void getProviderBankAccounts_chambers_queriesAllPractitioners() {
    ProviderEntity chambers = providerEntity(FirmType.CHAMBERS);
    ProviderEntity advocate1 = providerEntity(FirmType.ADVOCATE);
    ProviderEntity advocate2 = providerEntity(FirmType.ADVOCATE);

    ProviderParentLinkEntity link1 = new ProviderParentLinkEntity();
    link1.setProvider(advocate1);

    ProviderParentLinkEntity link2 = new ProviderParentLinkEntity();
    link2.setProvider(advocate2);

    var pageable = PageRequest.of(0, 10);
    var page = new PageImpl<ProviderBankAccountLinkEntity>(List.of());

    when(providerQueryService.getChildLinks(chambers)).thenReturn(List.of(link1, link2));
    when(providerBankAccountLinkRepository.findByProviderIn(
            List.of(advocate1, advocate2), pageable))
        .thenReturn(page);

    var result = service.getProviderBankAccounts(chambers, null, pageable);

    assertThat(result).isEqualTo(page);
    verify(providerBankAccountLinkRepository)
        .findByProviderIn(List.of(advocate1, advocate2), pageable);
  }

  // --- getOfficeBankAccounts ---

  @Test
  void getOfficeBankAccounts_noFilter_queriesByOfficeLinkOnly() {
    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();
    var pageable = PageRequest.of(0, 10);
    var page = new PageImpl<OfficeBankAccountLinkEntity>(List.of());

    when(officeBankAccountLinkRepository.findByProviderOfficeLinkIn(List.of(officeLink), pageable))
        .thenReturn(page);

    var result = service.getOfficeBankAccounts(officeLink, null, pageable);

    assertThat(result).isEqualTo(page);
  }

  @Test
  void getOfficeBankAccounts_withFilter_queriesWithAccountNumberFilter() {
    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();
    var pageable = PageRequest.of(0, 10);
    var page = new PageImpl<OfficeBankAccountLinkEntity>(List.of());

    when(officeBankAccountLinkRepository
            .findByProviderOfficeLinkInAndBankAccount_AccountNumberContainingIgnoreCase(
                List.of(officeLink), "5678", pageable))
        .thenReturn(page);

    var result = service.getOfficeBankAccounts(officeLink, "5678", pageable);

    assertThat(result).isEqualTo(page);
  }

  @Test
  void getOfficeBankAccounts_blankFilter_treatedAsNoFilter() {
    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();
    var pageable = PageRequest.of(0, 10);
    var page = new PageImpl<OfficeBankAccountLinkEntity>(List.of());

    when(officeBankAccountLinkRepository.findByProviderOfficeLinkIn(List.of(officeLink), pageable))
        .thenReturn(page);

    var result = service.getOfficeBankAccounts(officeLink, "  ", pageable);

    assertThat(result).isEqualTo(page);
    verify(officeBankAccountLinkRepository)
        .findByProviderOfficeLinkIn(List.of(officeLink), pageable);
  }

  @Test
  void getOfficeBankAccounts_chambersOfficeLink_aggregatesAdvocateOfficeLinks() {
    var chambersFirm = providerEntity(FirmType.CHAMBERS);
    var chambersLink = new ChamberProviderOfficeLinkEntity();
    chambersLink.setProvider(chambersFirm);

    var advocateFirm = providerEntity(FirmType.ADVOCATE);
    var parentLink = new ProviderParentLinkEntity();
    parentLink.setProvider(advocateFirm);
    parentLink.setParent(chambersFirm);

    var advocate1Link = new AdvocateProviderOfficeLinkEntity();
    var advocate2Link = new AdvocateProviderOfficeLinkEntity();
    var pageable = PageRequest.of(0, 10);
    var page = new PageImpl<OfficeBankAccountLinkEntity>(List.of());

    when(providerQueryService.getChildLinks(chambersFirm)).thenReturn(List.of(parentLink));
    when(officeQueryService.getAdvocateOfficeLinks(advocateFirm))
        .thenReturn(List.of(advocate1Link, advocate2Link));
    when(officeBankAccountLinkRepository.findByProviderOfficeLinkIn(
            List.of(advocate1Link, advocate2Link), pageable))
        .thenReturn(page);

    var result = service.getOfficeBankAccounts(chambersLink, null, pageable);

    assertThat(result).isEqualTo(page);
    verify(providerQueryService).getChildLinks(chambersFirm);
    verify(officeQueryService).getAdvocateOfficeLinks(advocateFirm);
  }

  @Test
  void getOfficeBankAccounts_chambersOfficeLink_withFilter_aggregatesAdvocateOfficeLinks() {
    var chambersFirm = providerEntity(FirmType.CHAMBERS);
    var chambersLink = new ChamberProviderOfficeLinkEntity();
    chambersLink.setProvider(chambersFirm);

    var advocateFirm = providerEntity(FirmType.ADVOCATE);
    var parentLink = new ProviderParentLinkEntity();
    parentLink.setProvider(advocateFirm);
    parentLink.setParent(chambersFirm);

    var advocateLink = new AdvocateProviderOfficeLinkEntity();
    var pageable = PageRequest.of(0, 10);
    var page = new PageImpl<OfficeBankAccountLinkEntity>(List.of());

    when(providerQueryService.getChildLinks(chambersFirm)).thenReturn(List.of(parentLink));
    when(officeQueryService.getAdvocateOfficeLinks(advocateFirm)).thenReturn(List.of(advocateLink));
    when(officeBankAccountLinkRepository
            .findByProviderOfficeLinkInAndBankAccount_AccountNumberContainingIgnoreCase(
                List.of(advocateLink), "1234", pageable))
        .thenReturn(page);

    var result = service.getOfficeBankAccounts(chambersLink, "1234", pageable);

    assertThat(result).isEqualTo(page);
  }

  // --- helpers ---

  private static ProviderEntity providerEntity(String firmType) {
    return ProviderEntity.builder().firmType(firmType).firmNumber("100001").build();
  }
}
