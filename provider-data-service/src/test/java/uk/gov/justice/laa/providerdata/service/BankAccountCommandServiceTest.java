package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.OfficeBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderBankAccountLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.repository.BankAccountRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeBankAccountLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderBankAccountLinkRepository;

@ExtendWith(MockitoExtension.class)
class BankAccountCommandServiceTest {

  @Mock private BankAccountRepository bankAccountRepository;
  @Mock private ProviderBankAccountLinkRepository providerBankAccountLinkRepository;
  @Mock private OfficeBankAccountLinkRepository officeBankAccountLinkRepository;

  @InjectMocks private BankAccountCommandService service;

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
