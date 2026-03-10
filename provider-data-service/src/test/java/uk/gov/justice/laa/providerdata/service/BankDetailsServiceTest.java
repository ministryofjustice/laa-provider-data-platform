package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.repository.BankAccountRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;

@ExtendWith(MockitoExtension.class)
class BankDetailsServiceTest {

  @Mock private BankAccountRepository bankAccountRepository;
  @Mock private ProviderBankAccountLinkRepository providerBankAccountLinkRepository;
  @Mock private OfficeBankAccountLinkRepository officeBankAccountLinkRepository;
  @Mock private ProviderParentLinkRepository providerParentLinkRepository;

  @InjectMocks private BankDetailsService service;

  // --- createAndLink ---

  @Test
  void createAndLink_savesAccountProviderLinkAndOfficeLink() {
    BankAccountEntity saved = new BankAccountEntity();
    UUID savedGuid = UUID.randomUUID();
    saved.setGuid(savedGuid);

    BankAccountEntity template = new BankAccountEntity();
    ProviderEntity provider = providerEntity(FirmType.LEGAL_SERVICES_PROVIDER);
    when(bankAccountRepository.save(template)).thenReturn(saved);
    when(providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(provider, savedGuid))
        .thenReturn(false);
    when(officeBankAccountLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();
    OfficeBankAccountLinkEntity result =
        service.createAndLink(template, provider, officeLink, LocalDate.of(2024, 6, 1));

    verify(bankAccountRepository).save(template);
    verify(providerBankAccountLinkRepository).save(any(ProviderBankAccountLinkEntity.class));
    ArgumentCaptor<OfficeBankAccountLinkEntity> captor =
        ArgumentCaptor.forClass(OfficeBankAccountLinkEntity.class);
    verify(officeBankAccountLinkRepository).save(captor.capture());

    OfficeBankAccountLinkEntity officeBankAccountLink = captor.getValue();
    assertThat(officeBankAccountLink.getBankAccount()).isEqualTo(saved);
    assertThat(officeBankAccountLink.getPrimaryFlag()).isTrue();
    assertThat(officeBankAccountLink.getActiveDateFrom()).isEqualTo(LocalDate.of(2024, 6, 1));
    assertThat(result).isEqualTo(officeBankAccountLink);
  }

  @Test
  void createAndLink_defaultsActiveDateFromToToday_whenNull() {
    ProviderEntity provider = providerEntity(FirmType.LEGAL_SERVICES_PROVIDER);
    BankAccountEntity saved = accountWithGuid();

    when(bankAccountRepository.save(any())).thenReturn(saved);
    when(providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(any(), any()))
        .thenReturn(false);
    when(officeBankAccountLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    OfficeBankAccountLinkEntity result =
        service.createAndLink(
            new BankAccountEntity(), provider, new ProviderOfficeLinkEntity(), null);

    assertThat(result.getActiveDateFrom()).isEqualTo(LocalDate.now());
  }

  @Test
  void createAndLink_doesNotDuplicateProviderLink_whenAlreadyExists() {
    ProviderEntity provider = providerEntity(FirmType.LEGAL_SERVICES_PROVIDER);
    BankAccountEntity saved = accountWithGuid();

    when(bankAccountRepository.save(any())).thenReturn(saved);
    when(providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(
            provider, saved.getGuid()))
        .thenReturn(true);
    when(officeBankAccountLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createAndLink(new BankAccountEntity(), provider, new ProviderOfficeLinkEntity(), null);

    verify(providerBankAccountLinkRepository, never())
        .save(any(ProviderBankAccountLinkEntity.class));
  }

  // --- linkExisting ---

  @Test
  void linkExisting_linksExistingAccountToOffice() {
    UUID guid = UUID.randomUUID();
    ProviderEntity provider = providerEntity(FirmType.LEGAL_SERVICES_PROVIDER);
    BankAccountEntity account = new BankAccountEntity();
    account.setGuid(guid);

    when(bankAccountRepository.findById(guid)).thenReturn(Optional.of(account));
    when(providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(provider, guid))
        .thenReturn(false);
    when(officeBankAccountLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    OfficeBankAccountLinkEntity result =
        service.linkExisting(guid, provider, new ProviderOfficeLinkEntity(), null);

    assertThat(result.getBankAccount()).isEqualTo(account);
    assertThat(result.getPrimaryFlag()).isTrue();
  }

  @Test
  void linkExisting_throwsItemNotFoundException_whenGuidNotFound() {
    UUID guid = UUID.randomUUID();

    when(bankAccountRepository.findById(guid)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.linkExisting(
                    guid,
                    providerEntity(FirmType.LEGAL_SERVICES_PROVIDER),
                    new ProviderOfficeLinkEntity(),
                    null))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(guid.toString());
  }

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

    when(providerParentLinkRepository.findByParent(chambers)).thenReturn(List.of(link1, link2));
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

    when(officeBankAccountLinkRepository.findByProviderOfficeLink(officeLink, pageable))
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
            .findByProviderOfficeLinkAndBankAccount_AccountNumberContainingIgnoreCase(
                officeLink, "5678", pageable))
        .thenReturn(page);

    var result = service.getOfficeBankAccounts(officeLink, "5678", pageable);

    assertThat(result).isEqualTo(page);
  }

  @Test
  void getOfficeBankAccounts_blankFilter_treatedAsNoFilter() {
    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();
    var pageable = PageRequest.of(0, 10);
    var page = new PageImpl<OfficeBankAccountLinkEntity>(List.of());

    when(officeBankAccountLinkRepository.findByProviderOfficeLink(officeLink, pageable))
        .thenReturn(page);

    var result = service.getOfficeBankAccounts(officeLink, "  ", pageable);

    assertThat(result).isEqualTo(page);
    verify(officeBankAccountLinkRepository).findByProviderOfficeLink(officeLink, pageable);
  }

  // --- helpers ---

  private static ProviderEntity providerEntity(String firmType) {
    return ProviderEntity.builder().firmType(firmType).firmNumber("FRM001").build();
  }

  private static BankAccountEntity accountWithGuid() {
    BankAccountEntity account = new BankAccountEntity();
    account.setGuid(UUID.randomUUID());
    return account;
  }
}
