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
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
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
  @Mock private AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;

  @InjectMocks private BankDetailsService service;

  // --- createAndLinkToProvider ---

  @Test
  void createAndLinkToProvider_savesAccountAndProviderLink_withoutOfficeLink() {
    AdvocatePractitionerEntity practitioner = advocateEntity();
    BankAccountEntity template = new BankAccountEntity();
    BankAccountEntity saved = accountWithGuid();

    when(bankAccountRepository.save(template)).thenReturn(saved);
    when(providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(
            practitioner, saved.getGuid()))
        .thenReturn(false);

    service.createAndLinkToProvider(template, practitioner);

    verify(bankAccountRepository).save(template);
    verify(providerBankAccountLinkRepository).save(any(ProviderBankAccountLinkEntity.class));
    verify(officeBankAccountLinkRepository, never()).save(any());
  }

  // --- createAndLink ---

  @Test
  void createAndLink_savesAccountProviderLinkAndOfficeLink() {
    BankAccountEntity saved = new BankAccountEntity();
    UUID savedGuid = UUID.randomUUID();
    saved.setGuid(savedGuid);

    BankAccountEntity template = new BankAccountEntity();
    ProviderEntity provider = providerEntity(FirmType.LEGAL_SERVICES_PROVIDER);
    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();
    when(bankAccountRepository.save(template)).thenReturn(saved);
    when(providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(provider, savedGuid))
        .thenReturn(false);
    when(officeBankAccountLinkRepository.findByProviderOfficeLinkAndPrimaryFlagTrue(officeLink))
        .thenReturn(Optional.empty());
    when(officeBankAccountLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

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
  void createAndLink_endDatesExistingPrimary_whenOneExists() {
    OfficeBankAccountLinkEntity existing = new OfficeBankAccountLinkEntity();
    existing.setPrimaryFlag(Boolean.TRUE);

    BankAccountEntity saved = accountWithGuid();
    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();

    when(bankAccountRepository.save(any())).thenReturn(saved);
    when(providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(any(), any()))
        .thenReturn(false);
    when(officeBankAccountLinkRepository.findByProviderOfficeLinkAndPrimaryFlagTrue(officeLink))
        .thenReturn(Optional.of(existing));
    when(officeBankAccountLinkRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeBankAccountLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProviderEntity provider = providerEntity(FirmType.LEGAL_SERVICES_PROVIDER);
    LocalDate newDate = LocalDate.of(2025, 1, 1);

    service.createAndLink(new BankAccountEntity(), provider, officeLink, newDate);

    assertThat(existing.getPrimaryFlag()).isFalse();
    assertThat(existing.getActiveDateTo()).isEqualTo(newDate);

    // saveAndFlush called once for the end-dated old record; save called once for the new link
    verify(officeBankAccountLinkRepository).saveAndFlush(any(OfficeBankAccountLinkEntity.class));
    verify(officeBankAccountLinkRepository).save(any(OfficeBankAccountLinkEntity.class));
  }

  @Test
  void createAndLink_defaultsActiveDateFromToToday_whenNull() {
    ProviderEntity provider = providerEntity(FirmType.LEGAL_SERVICES_PROVIDER);
    BankAccountEntity saved = accountWithGuid();
    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();

    when(bankAccountRepository.save(any())).thenReturn(saved);
    when(providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(any(), any()))
        .thenReturn(false);
    when(officeBankAccountLinkRepository.findByProviderOfficeLinkAndPrimaryFlagTrue(officeLink))
        .thenReturn(Optional.empty());
    when(officeBankAccountLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    OfficeBankAccountLinkEntity result =
        service.createAndLink(new BankAccountEntity(), provider, officeLink, null);

    assertThat(result.getActiveDateFrom()).isEqualTo(LocalDate.now());
  }

  @Test
  void createAndLink_doesNotDuplicateProviderLink_whenAlreadyExists() {
    ProviderEntity provider = providerEntity(FirmType.LEGAL_SERVICES_PROVIDER);
    BankAccountEntity saved = accountWithGuid();
    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();

    when(bankAccountRepository.save(any())).thenReturn(saved);
    when(providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(
            provider, saved.getGuid()))
        .thenReturn(true);
    when(officeBankAccountLinkRepository.findByProviderOfficeLinkAndPrimaryFlagTrue(officeLink))
        .thenReturn(Optional.empty());
    when(officeBankAccountLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createAndLink(new BankAccountEntity(), provider, officeLink, null);

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
    ProviderOfficeLinkEntity officeLink = new ProviderOfficeLinkEntity();

    when(bankAccountRepository.findById(guid)).thenReturn(Optional.of(account));
    when(providerBankAccountLinkRepository.existsByProviderAndBankAccount_Guid(provider, guid))
        .thenReturn(false);
    when(officeBankAccountLinkRepository.findByProviderOfficeLinkAndPrimaryFlagTrue(officeLink))
        .thenReturn(Optional.empty());
    when(officeBankAccountLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    OfficeBankAccountLinkEntity result = service.linkExisting(guid, provider, officeLink, null);

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

    when(providerParentLinkRepository.findByParent(chambersFirm)).thenReturn(List.of(parentLink));
    when(advocateProviderOfficeLinkRepository.findByProvider(advocateFirm))
        .thenReturn(List.of(advocate1Link, advocate2Link));
    when(officeBankAccountLinkRepository.findByProviderOfficeLinkIn(
            List.of(advocate1Link, advocate2Link), pageable))
        .thenReturn(page);

    var result = service.getOfficeBankAccounts(chambersLink, null, pageable);

    assertThat(result).isEqualTo(page);
    verify(providerParentLinkRepository).findByParent(chambersFirm);
    verify(advocateProviderOfficeLinkRepository).findByProvider(advocateFirm);
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

    when(providerParentLinkRepository.findByParent(chambersFirm)).thenReturn(List.of(parentLink));
    when(advocateProviderOfficeLinkRepository.findByProvider(advocateFirm))
        .thenReturn(List.of(advocateLink));
    when(officeBankAccountLinkRepository
            .findByProviderOfficeLinkInAndBankAccount_AccountNumberContainingIgnoreCase(
                List.of(advocateLink), "1234", pageable))
        .thenReturn(page);

    var result = service.getOfficeBankAccounts(chambersLink, "1234", pageable);

    assertThat(result).isEqualTo(page);
  }

  // --- helpers ---

  private static AdvocatePractitionerEntity advocateEntity() {
    return AdvocatePractitionerEntity.builder()
        .firmType(FirmType.ADVOCATE)
        .firmNumber("100003")
        .build();
  }

  private static ProviderEntity providerEntity(String firmType) {
    return ProviderEntity.builder().firmType(firmType).firmNumber("100001").build();
  }

  private static BankAccountEntity accountWithGuid() {
    BankAccountEntity account = new BankAccountEntity();
    account.setGuid(UUID.randomUUID());
    return account;
  }
}
