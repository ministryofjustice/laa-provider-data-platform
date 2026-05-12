package uk.gov.justice.laa.providerdata.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountCommandService;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountEntity;
import uk.gov.justice.laa.providerdata.bankaccount.BankAccountMapper;
import uk.gov.justice.laa.providerdata.liaisonmanager.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerCommandService;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.model.AdvocateOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeLinkV2;
import uk.gov.justice.laa.providerdata.model.ChambersOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.DXPatchV2;
import uk.gov.justice.laa.providerdata.model.LSPOfficePatchV2;
import uk.gov.justice.laa.providerdata.model.OfficeAddressV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.office.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.OfficeCreationResult;
import uk.gov.justice.laa.providerdata.office.OfficeEntity;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;
import uk.gov.justice.laa.providerdata.shared.ItemNotFoundException;

@ExtendWith(MockitoExtension.class)
class OfficeFirmUseCaseTest {

  @Mock private ProviderQueryService providerQueryService;

  @Mock private uk.gov.justice.laa.providerdata.office.OfficeCommandService officeCommandService;

  @Mock private uk.gov.justice.laa.providerdata.office.OfficeQueryService officeQueryService;
  @Mock private OfficeLiaisonManagerCommandService officeLiaisonManagerCommandService;
  @Mock private BankAccountCommandService bankDetailsService;
  @Mock private BankAccountMapper bankAccountMapper;

  @Mock
  private uk.gov.justice.laa.providerdata.contractmanager.OfficeContractManagerCommandService
      officeContractManagerCommandService;

  @Mock private ProviderEventPublisher providerEventPublisher;

  @InjectMocks private OfficeFirmUseCase service;

  @Test
  void createLspOffice_lookupByGuid_returnsPopulatedResult() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeLinkGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(UUID.randomUUID());

    LspProviderOfficeLinkEntity savedLink = new LspProviderOfficeLinkEntity();
    savedLink.setGuid(officeLinkGuid);

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeCommandService.save(any())).thenReturn(savedOffice);
    when(officeCommandService.saveLspOfficeLink(any())).thenReturn(savedLink);

    OfficeCreationResult result =
        service.createLspOffice(
            providerGuid.toString(),
            new OfficeEntity(),
            new LspProviderOfficeLinkEntity(),
            EventContext.empty());

    assertThat(result.providerGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).isEqualTo("100001");
    assertThat(result.officeGUID()).isEqualTo(officeLinkGuid);
    assertThat(result.accountNumber()).isNotBlank();
  }

  @Test
  void createLspOffice_lookupByFirmNumber_returnsPopulatedResult() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeLinkGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("100999").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(UUID.randomUUID());

    LspProviderOfficeLinkEntity savedLink = new LspProviderOfficeLinkEntity();
    savedLink.setGuid(officeLinkGuid);

    when(providerQueryService.getProvider("100999")).thenReturn(provider);
    when(officeCommandService.save(any())).thenReturn(savedOffice);
    when(officeCommandService.saveLspOfficeLink(any())).thenReturn(savedLink);

    OfficeCreationResult result =
        service.createLspOffice(
            "100999", new OfficeEntity(), new LspProviderOfficeLinkEntity(), EventContext.empty());

    assertThat(result.firmNumber()).isEqualTo("100999");
    assertThat(result.officeGUID()).isEqualTo(officeLinkGuid);
  }

  @Test
  void createLspOffice_throwsWhenGuidNotFound() {
    UUID unknownGuid = UUID.randomUUID();
    when(providerQueryService.getProvider(unknownGuid.toString()))
        .thenThrow(new ItemNotFoundException("Provider not found: " + unknownGuid));

    assertThatThrownBy(
            () ->
                service.createLspOffice(
                    unknownGuid.toString(),
                    new OfficeEntity(),
                    new LspProviderOfficeLinkEntity(),
                    EventContext.empty()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(unknownGuid.toString());
  }

  @Test
  void createLspOffice_throwsWhenFirmNumberNotFound() {
    when(providerQueryService.getProvider("UNKNOWN"))
        .thenThrow(new ItemNotFoundException("Provider not found: " + "UNKNOWN"));

    assertThatThrownBy(
            () ->
                service.createLspOffice(
                    "UNKNOWN",
                    new OfficeEntity(),
                    new LspProviderOfficeLinkEntity(),
                    EventContext.empty()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("UNKNOWN");
  }

  @Test
  void createLspOffice_withNewLiaisonManager_savesLmAndLink() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();

    UUID officeLinkGuid = UUID.randomUUID();
    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeCommandService.save(any())).thenReturn(savedOffice);
    when(officeCommandService.saveLspOfficeLink(any()))
        .thenAnswer(
            inv -> {
              LspProviderOfficeLinkEntity officeLink = inv.getArgument(0);
              officeLink.setGuid(officeLinkGuid);
              return officeLink;
            });

    var lmTemplate = LiaisonManagerEntity.builder().firstName("Jane").lastName("Smith").build();
    var lmLink = new OfficeLiaisonManagerLinkEntity();
    lmLink.setActiveDateFrom(LocalDate.of(2024, 1, 1));

    service.createLspOffice(
        providerGuid.toString(),
        new OfficeEntity(),
        new LspProviderOfficeLinkEntity(),
        lmTemplate,
        lmLink,
        false,
        null,
        EventContext.empty());

    assertThat(lmLink.getOfficeLink().getGuid()).isEqualTo(officeLinkGuid);
    verify(officeLiaisonManagerCommandService).createAndLink(lmTemplate, lmLink);
  }

  @Test
  void createLspOffice_withLinkToHeadOfficeLiaisonManager_linksExistingLm() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(officeGuid);

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeCommandService.save(any())).thenReturn(savedOffice);
    when(officeCommandService.saveLspOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createLspOffice(
        providerGuid.toString(),
        new OfficeEntity(),
        new LspProviderOfficeLinkEntity(),
        null,
        null,
        true,
        null,
        EventContext.empty());

    verify(officeLiaisonManagerCommandService).copyFromHeadOfficeToOffice(eq(provider), any());
  }

  @Test
  void createLspOffice_withLinkToHeadOffice_throwsWhenNoActiveManagerFound() {
    UUID providerGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(UUID.randomUUID());

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeCommandService.save(any())).thenReturn(savedOffice);
    doThrow(new ItemNotFoundException("No active liaison manager found on head office"))
        .when(officeLiaisonManagerCommandService)
        .copyFromHeadOfficeToOffice(any(), any());

    assertThatThrownBy(
            () ->
                service.createLspOffice(
                    providerGuid.toString(),
                    new OfficeEntity(),
                    new LspProviderOfficeLinkEntity(),
                    null,
                    null,
                    true,
                    null,
                    EventContext.empty()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("No active liaison manager found");
  }

  // --- bank details wiring ---

  @Test
  void createLspOffice_withEftCreatePayment_persistsBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeCommandService.save(any())).thenReturn(new OfficeEntity());

    var linkTemplate = new LspProviderOfficeLinkEntity();
    when(officeCommandService.saveLspOfficeLink(linkTemplate)).thenReturn(linkTemplate);

    var createDetails = new BankAccountProviderOfficeCreateV2("Test Bank", "12-34-56", "87654321");
    var accountTemplate = new BankAccountEntity();
    when(bankAccountMapper.toBankAccountEntity(createDetails)).thenReturn(accountTemplate);

    var payment =
        new PaymentDetailsCreateOrLinkV2(PaymentDetailsPaymentMethodV2.EFT)
            .bankAccountDetails(createDetails);

    service.createLspOffice(
        providerGuid.toString(),
        new OfficeEntity(),
        linkTemplate,
        null,
        null,
        false,
        payment,
        EventContext.empty());

    verify(bankDetailsService)
        .createAndLink(accountTemplate, provider, linkTemplate, createDetails.getActiveDateFrom());
  }

  @Test
  void createLspOffice_withEftLinkPayment_linksExistingBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeCommandService.save(any())).thenReturn(new OfficeEntity());

    var linkTemplate = new LspProviderOfficeLinkEntity();
    when(officeCommandService.saveLspOfficeLink(linkTemplate)).thenReturn(linkTemplate);

    UUID bankGuid = UUID.randomUUID();
    var linkDetails = new BankAccountProviderOfficeLinkV2().bankAccountGUID(bankGuid);
    var payment =
        new PaymentDetailsCreateOrLinkV2(PaymentDetailsPaymentMethodV2.EFT)
            .bankAccountDetails(linkDetails);

    service.createLspOffice(
        providerGuid.toString(),
        new OfficeEntity(),
        linkTemplate,
        null,
        null,
        false,
        payment,
        EventContext.empty());

    verify(bankDetailsService).linkExisting(bankGuid, provider, linkTemplate, null);
    verify(bankAccountMapper, never()).toBankAccountEntity(any());
  }

  @Test
  void createLspOffice_withCheckPayment_doesNotPersistBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeCommandService.save(any())).thenReturn(new OfficeEntity());
    when(officeCommandService.saveLspOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));

    var payment = new PaymentDetailsCreateOrLinkV2(PaymentDetailsPaymentMethodV2.CHECK);

    service.createLspOffice(
        providerGuid.toString(),
        new OfficeEntity(),
        new LspProviderOfficeLinkEntity(),
        null,
        null,
        false,
        payment,
        EventContext.empty());

    verify(bankDetailsService, never()).createAndLink(any(), any(), any(), any());
    verify(bankDetailsService, never()).linkExisting(any(), any(), any(), any());
  }

  @Test
  void createLspOffice_withNullPayment_doesNotPersistBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeCommandService.save(any())).thenReturn(new OfficeEntity());
    when(officeCommandService.saveLspOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createLspOffice(
        providerGuid.toString(),
        new OfficeEntity(),
        new LspProviderOfficeLinkEntity(),
        null,
        null,
        false,
        null,
        EventContext.empty());

    verify(bankDetailsService, never()).createAndLink(any(), any(), any(), any());
    verify(bankDetailsService, never()).linkExisting(any(), any(), any(), any());
  }

  // patchOffice

  @Test
  void patchOffice_updatesContactFields_forLspPatch() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    var office = new OfficeEntity();
    var link = new LspProviderOfficeLinkEntity();
    link.setGuid(linkGuid);
    link.setAccountNumber("ABC123");
    link.setOffice(office);

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeQueryService.findProviderOfficeLink(provider, linkGuid.toString()))
        .thenReturn(Optional.of(link));
    when(officeCommandService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeCommandService.saveProviderOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));

    var patch =
        new LSPOfficePatchV2()
            .address(
                new OfficeAddressV2().line1("1 High St").townOrCity("London").postcode("SW1A 1AA"))
            .telephoneNumber("0207 111 2222")
            .emailAddress("info@example.com")
            .website(java.net.URI.create("https://www.example.com"))
            .dxDetails(new DXPatchV2().dxNumber("DX 1234").dxCentre("London"));

    var result =
        service.patchOffice(
            providerGuid.toString(), linkGuid.toString(), patch, EventContext.empty());

    assertThat(office.getAddressLine1()).isEqualTo("1 High St");
    assertThat(office.getAddressTownOrCity()).isEqualTo("London");
    assertThat(office.getAddressPostCode()).isEqualTo("SW1A 1AA");
    assertThat(office.getTelephoneNumber()).isEqualTo("0207 111 2222");
    assertThat(office.getEmailAddress()).isEqualTo("info@example.com");
    assertThat(link.getWebsite()).isEqualTo("https://www.example.com");
    assertThat(office.getDxDetailsNumber()).isEqualTo("DX 1234");
    assertThat(office.getDxDetailsCentre()).isEqualTo("London");
    assertThat(result.providerGUID()).isEqualTo(providerGuid);
    assertThat(result.officeGUID()).isEqualTo(linkGuid);
    assertThat(result.accountNumber()).isEqualTo("ABC123");
  }

  @Test
  void patchOffice_updatesContactFields_forChambersPatch() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100002").build();
    provider.setGuid(providerGuid);

    var office = new OfficeEntity();
    var link = new LspProviderOfficeLinkEntity();
    link.setGuid(linkGuid);
    link.setAccountNumber("DEF456");
    link.setOffice(office);

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeQueryService.findProviderOfficeLink(provider, linkGuid.toString()))
        .thenReturn(Optional.of(link));
    when(officeCommandService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeCommandService.saveProviderOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));

    var patch = new ChambersOfficePatchV2().telephoneNumber("0161 999 8888");

    service.patchOffice(providerGuid.toString(), linkGuid.toString(), patch, EventContext.empty());

    assertThat(office.getTelephoneNumber()).isEqualTo("0161 999 8888");
  }

  @Test
  void patchOffice_doesNotModifyOffice_forAdvocatePatch() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100003").build();
    provider.setGuid(providerGuid);

    var office = new OfficeEntity();
    office.setAddressLine1("Original");
    var link = new LspProviderOfficeLinkEntity();
    link.setGuid(linkGuid);
    link.setAccountNumber("GHI789");
    link.setOffice(office);

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeQueryService.findProviderOfficeLink(provider, linkGuid.toString()))
        .thenReturn(Optional.of(link));
    when(officeCommandService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeCommandService.saveProviderOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));

    service.patchOffice(
        providerGuid.toString(),
        linkGuid.toString(),
        new AdvocateOfficePatchV2(),
        EventContext.empty());

    assertThat(office.getAddressLine1()).isEqualTo("Original");
  }

  @Test
  void patchOffice_throwsItemNotFound_whenOfficeNotFound() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    when(providerQueryService.getProvider(providerGuid.toString())).thenReturn(provider);
    when(officeQueryService.findProviderOfficeLink(provider, linkGuid.toString()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.patchOffice(
                    providerGuid.toString(),
                    linkGuid.toString(),
                    new ChambersOfficePatchV2(),
                    EventContext.empty()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("Office not found");
  }

  // patchOffice — activation, flags, cascade

  private LspProviderOfficeLinkEntity lspLinkWithOffice(UUID linkGuid, String accountNumber) {
    var office = new OfficeEntity();
    var link = new LspProviderOfficeLinkEntity();
    link.setGuid(linkGuid);
    link.setAccountNumber(accountNumber);
    link.setOffice(office);
    link.setDebtRecoveryFlag(Boolean.FALSE);
    link.setFalseBalanceFlag(Boolean.FALSE);
    return link;
  }

  private void stubProviderAndLink(
      ProviderEntity provider, UUID linkGuid, ProviderOfficeLinkEntity link) {
    when(providerQueryService.getProvider(provider.getGuid().toString())).thenReturn(provider);
    when(officeQueryService.findProviderOfficeLink(provider, linkGuid.toString()))
        .thenReturn(Optional.of(link));
  }

  private void stubSaves() {
    when(officeCommandService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeCommandService.saveProviderOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void patchOffice_setsActiveDateTo_andResetsDebtRecoveryFlag() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    LspProviderOfficeLinkEntity link = lspLinkWithOffice(linkGuid, "ACC001");
    link.setDebtRecoveryFlag(Boolean.TRUE);
    link.setHeadOfficeFlag(Boolean.FALSE);

    stubProviderAndLink(provider, linkGuid, link);
    stubSaves();

    var deactivationDate = LocalDate.of(2025, 6, 30);
    service.patchOffice(
        providerGuid.toString(),
        linkGuid.toString(),
        new LSPOfficePatchV2().activeDateTo(deactivationDate),
        EventContext.empty());

    assertThat(link.getActiveDateTo()).isEqualTo(deactivationDate);
    assertThat(link.getDebtRecoveryFlag()).isFalse();
  }

  @Test
  void patchOffice_cascadesDeactivation_toLspChildOffices() {
    var providerGuid = UUID.randomUUID();
    var headLinkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    var headLink = lspLinkWithOffice(headLinkGuid, "ACC001");
    headLink.setHeadOfficeFlag(Boolean.TRUE);

    var childLink = lspLinkWithOffice(UUID.randomUUID(), "ACC002");
    childLink.setDebtRecoveryFlag(Boolean.TRUE);

    stubProviderAndLink(provider, headLinkGuid, headLink);
    stubSaves();
    when(officeCommandService.saveLspOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeQueryService.getLspActiveChildOffices(provider)).thenReturn(List.of(childLink));

    var deactivationDate = LocalDate.of(2025, 6, 30);
    service.patchOffice(
        providerGuid.toString(),
        headLinkGuid.toString(),
        new LSPOfficePatchV2().activeDateTo(deactivationDate),
        EventContext.empty());

    assertThat(headLink.getActiveDateTo()).isEqualTo(deactivationDate);
    assertThat(childLink.getActiveDateTo()).isEqualTo(deactivationDate);
    assertThat(childLink.getDebtRecoveryFlag()).isFalse();
    verify(officeCommandService).saveLspOfficeLink(childLink);
  }

  @Test
  void patchOffice_throwsIllegalArgument_whenChambersHasActivePractitioners() {
    var chambersProviderGuid = UUID.randomUUID();
    var chambersLinkGuid = UUID.randomUUID();

    var chambersProvider = ProviderEntity.builder().firmNumber("100002").build();
    chambersProvider.setGuid(chambersProviderGuid);

    var chambersLink = new ChamberProviderOfficeLinkEntity();
    chambersLink.setGuid(chambersLinkGuid);
    chambersLink.setAccountNumber("CHM001");
    chambersLink.setOffice(new OfficeEntity());
    chambersLink.setProvider(chambersProvider);

    stubProviderAndLink(chambersProvider, chambersLinkGuid, chambersLink);
    when(officeQueryService.existsActivePractitionerForChambers(chambersProvider)).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.patchOffice(
                    chambersProviderGuid.toString(),
                    chambersLinkGuid.toString(),
                    new ChambersOfficePatchV2().activeDateTo(LocalDate.of(2025, 6, 30)),
                    EventContext.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("practitioner");
  }

  @Test
  void patchOffice_deactivatesChambers_whenNoPractitionersLinked() {
    var chambersProviderGuid = UUID.randomUUID();
    var chambersLinkGuid = UUID.randomUUID();

    var chambersProvider = ProviderEntity.builder().firmNumber("100002").build();
    chambersProvider.setGuid(chambersProviderGuid);

    var chambersLink = new ChamberProviderOfficeLinkEntity();
    chambersLink.setGuid(chambersLinkGuid);
    chambersLink.setAccountNumber("CHM001");
    chambersLink.setOffice(new OfficeEntity());
    chambersLink.setProvider(chambersProvider);

    stubProviderAndLink(chambersProvider, chambersLinkGuid, chambersLink);
    stubSaves();
    when(officeQueryService.existsActivePractitionerForChambers(chambersProvider))
        .thenReturn(false);

    var deactivationDate = LocalDate.of(2025, 6, 30);
    service.patchOffice(
        chambersProviderGuid.toString(),
        chambersLinkGuid.toString(),
        new ChambersOfficePatchV2().activeDateTo(deactivationDate),
        EventContext.empty());

    assertThat(chambersLink.getActiveDateTo()).isEqualTo(deactivationDate);
    verify(officeQueryService).existsActivePractitionerForChambers(chambersProvider);
  }

  @Test
  void patchOffice_throwsIllegalArgument_whenDebtRecoverySetTrueOnInactiveOffice() {
    UUID providerGuid = UUID.randomUUID();
    UUID linkGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    LspProviderOfficeLinkEntity link = lspLinkWithOffice(linkGuid, "ACC001");
    link.setActiveDateTo(LocalDate.of(2025, 1, 1));

    stubProviderAndLink(provider, linkGuid, link);

    assertThatThrownBy(
            () ->
                service.patchOffice(
                    providerGuid.toString(),
                    linkGuid.toString(),
                    new LSPOfficePatchV2().debtRecoveryFlag(Boolean.TRUE),
                    EventContext.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("debtRecoveryFlag");
  }

  @Test
  void patchOffice_throwsIllegalArgument_whenFalseBalanceSetTrueOnActiveOffice() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    var link = lspLinkWithOffice(linkGuid, "ACC001");

    stubProviderAndLink(provider, linkGuid, link);

    assertThatThrownBy(
            () ->
                service.patchOffice(
                    providerGuid.toString(),
                    linkGuid.toString(),
                    new LSPOfficePatchV2().falseBalanceFlag(Boolean.TRUE),
                    EventContext.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("falseBalanceFlag");
  }

  @Test
  void patchOffice_throwsIllegalArgument_whenDeactivatingAndDebtRecoverySetTrue() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    var link = lspLinkWithOffice(linkGuid, "ACC001");
    link.setHeadOfficeFlag(Boolean.FALSE);

    stubProviderAndLink(provider, linkGuid, link);

    assertThatThrownBy(
            () ->
                service.patchOffice(
                    providerGuid.toString(),
                    linkGuid.toString(),
                    new LSPOfficePatchV2()
                        .activeDateTo(LocalDate.of(2025, 6, 30))
                        .debtRecoveryFlag(Boolean.TRUE),
                    EventContext.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("debtRecoveryFlag");
  }

  @Test
  void patchOffice_setsFalseBalanceFlag_onInactiveOffice() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    var link = lspLinkWithOffice(linkGuid, "ACC001");
    link.setActiveDateTo(LocalDate.of(2025, 1, 1));
    link.setHeadOfficeFlag(Boolean.FALSE);

    stubProviderAndLink(provider, linkGuid, link);
    stubSaves();

    service.patchOffice(
        providerGuid.toString(),
        linkGuid.toString(),
        new LSPOfficePatchV2().falseBalanceFlag(Boolean.TRUE),
        EventContext.empty());

    assertThat(link.getFalseBalanceFlag()).isTrue();
  }

  @Test
  void patchOffice_reactivatesLspOffice_clearsActiveDateToAndResetsFalseBalanceFlag() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    var link = lspLinkWithOffice(linkGuid, "ACC001");
    link.setActiveDateTo(LocalDate.of(2025, 6, 30));
    link.setFalseBalanceFlag(Boolean.TRUE);
    link.setHeadOfficeFlag(Boolean.FALSE);

    stubProviderAndLink(provider, linkGuid, link);
    stubSaves();

    service.patchOffice(
        providerGuid.toString(),
        linkGuid.toString(),
        new LSPOfficePatchV2().clearActiveDateTo(Boolean.TRUE),
        EventContext.empty());

    assertThat(link.getActiveDateTo()).isNull();
    assertThat(link.getFalseBalanceFlag()).isFalse();
  }

  @Test
  void patchOffice_reactivatesAdvocateOffice_clearsActiveDateToAndResetsFalseBalanceFlag() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    var link = new AdvocateProviderOfficeLinkEntity();
    link.setGuid(linkGuid);
    link.setAccountNumber("ACC002");
    link.setOffice(new OfficeEntity());
    link.setActiveDateTo(LocalDate.of(2025, 6, 30));
    link.setFalseBalanceFlag(Boolean.TRUE);

    stubProviderAndLink(provider, linkGuid, link);
    stubSaves();

    service.patchOffice(
        providerGuid.toString(),
        linkGuid.toString(),
        new AdvocateOfficePatchV2().clearActiveDateTo(Boolean.TRUE),
        EventContext.empty());

    assertThat(link.getActiveDateTo()).isNull();
    assertThat(link.getFalseBalanceFlag()).isFalse();
  }

  @Test
  void patchOffice_reactivatesChambersOffice_clearsActiveDateTo() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    var link = new ChamberProviderOfficeLinkEntity();
    link.setGuid(linkGuid);
    link.setAccountNumber("ACC003");
    link.setOffice(new OfficeEntity());
    link.setActiveDateTo(LocalDate.of(2025, 6, 30));

    stubProviderAndLink(provider, linkGuid, link);
    stubSaves();

    service.patchOffice(
        providerGuid.toString(),
        linkGuid.toString(),
        new ChambersOfficePatchV2().clearActiveDateTo(Boolean.TRUE),
        EventContext.empty());

    assertThat(link.getActiveDateTo()).isNull();
  }

  @Test
  void patchOffice_throwsIllegalArgument_whenActiveDateToAndClearActiveDateToSetTogether() {
    var providerGuid = UUID.randomUUID();
    var linkGuid = UUID.randomUUID();

    var provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    var link = lspLinkWithOffice(linkGuid, "ACC001");
    link.setHeadOfficeFlag(Boolean.FALSE);

    stubProviderAndLink(provider, linkGuid, link);

    assertThatThrownBy(
            () ->
                service.patchOffice(
                    providerGuid.toString(),
                    linkGuid.toString(),
                    new LSPOfficePatchV2()
                        .activeDateTo(LocalDate.of(2025, 6, 30))
                        .clearActiveDateTo(Boolean.TRUE),
                    EventContext.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("clearActiveDateTo");
  }
}
