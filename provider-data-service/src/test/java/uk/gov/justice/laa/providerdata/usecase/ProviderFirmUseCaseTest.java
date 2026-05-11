package uk.gov.justice.laa.providerdata.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsConstitutionalStatusV2;
import uk.gov.justice.laa.providerdata.model.LSPDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsAdvocateLevelV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf1;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsPatchV2;
import uk.gov.justice.laa.providerdata.model.ProviderPatchV2;
import uk.gov.justice.laa.providerdata.office.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.office.OfficeEntity;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.provider.ChamberProviderEntity;
import uk.gov.justice.laa.providerdata.provider.LspProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;
import uk.gov.justice.laa.providerdata.shared.FirmType;
import uk.gov.justice.laa.providerdata.shared.ItemNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProviderFirmUseCaseTest {

  @Mock
  private uk.gov.justice.laa.providerdata.provider.ProviderCommandService providerPersistService;

  @Mock private ProviderQueryService providerQueryService;
  @Mock private uk.gov.justice.laa.providerdata.office.OfficeCommandService officeCommandService;
  @Mock private uk.gov.justice.laa.providerdata.office.OfficeQueryService officeQueryService;
  @Mock private OfficeLiaisonManagerCommandService officeLiaisonManagerCommandService;
  @Mock private BankAccountCommandService bankDetailsService;
  @Mock private BankAccountMapper bankAccountMapper;

  @InjectMocks private ProviderFirmUseCase service;

  @Test
  void createLspFirm_savesProviderOfficeAndLink_returnsAllIdentifiers() {
    UUID providerGuid = UUID.randomUUID();
    when(providerPersistService.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    UUID officeGuid = UUID.randomUUID();
    when(officeCommandService.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity office = inv.getArgument(0);
              office.setGuid(officeGuid);
              return office;
            });
    UUID officeLinkGuid = UUID.randomUUID();
    when(officeCommandService.saveLspOfficeLink(any()))
        .thenAnswer(
            inv -> {
              LspProviderOfficeLinkEntity officeLink = inv.getArgument(0);
              officeLink.setGuid(officeLinkGuid);
              return officeLink;
            });

    var linkTemplate = new LspProviderOfficeLinkEntity();
    linkTemplate.setHeadOfficeFlag(Boolean.TRUE);

    var result =
        service.createLspFirm(
            LspProviderEntity.builder().name("My LSP").build(),
            OfficeEntity.builder().addressLine1("1 Test St").build(),
            linkTemplate,
            null,
            null,
            null);

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).isNotBlank();
    assertThat(result.headOfficeGUID()).isEqualTo(officeLinkGuid);
    assertThat(result.headOfficeAccountNumber()).isNotBlank();
    assertThat(linkTemplate.getHeadOfficeFlag()).isTrue();
    verify(officeLiaisonManagerCommandService, never()).createAndLink(any(), any());
  }

  @Test
  void createLspFirm_withLiaisonManager_savesLmAndLink() {
    UUID providerGuid = UUID.randomUUID();
    when(providerPersistService.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    UUID officeGuid = UUID.randomUUID();
    when(officeCommandService.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity office = inv.getArgument(0);
              office.setGuid(officeGuid);
              return office;
            });
    UUID officeLinkGuid = UUID.randomUUID();
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

    var result =
        service.createLspFirm(
            LspProviderEntity.builder().name("My LSP").build(),
            OfficeEntity.builder().addressLine1("1 Test St").build(),
            new LspProviderOfficeLinkEntity(),
            lmTemplate,
            lmLink,
            null);

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    assertThat(lmLink.getOfficeLink()).isNotNull();
    assertThat(lmLink.getOfficeLink().getGuid()).isEqualTo(officeLinkGuid);
    verify(officeLiaisonManagerCommandService).createAndLink(lmTemplate, lmLink);
  }

  @Test
  void createChambersFirm_savesProviderOfficeAndLink_returnsAllIdentifiers() {
    UUID providerGuid = UUID.randomUUID();
    when(providerPersistService.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    UUID officeGuid = UUID.randomUUID();
    when(officeCommandService.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity office = inv.getArgument(0);
              office.setGuid(officeGuid);
              return office;
            });
    UUID officeLinkGuid = UUID.randomUUID();
    when(officeCommandService.saveChamberOfficeLink(any()))
        .thenAnswer(
            inv -> {
              ChamberProviderOfficeLinkEntity officeLink = inv.getArgument(0);
              officeLink.setGuid(officeLinkGuid);
              return officeLink;
            });

    var linkTemplate = new ChamberProviderOfficeLinkEntity();
    linkTemplate.setHeadOfficeFlag(Boolean.TRUE);

    var result =
        service.createChambersFirm(
            ChamberProviderEntity.builder().name("My Chambers").build(),
            OfficeEntity.builder().addressLine1("1 Test St").build(),
            linkTemplate,
            null,
            null);

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).isNotBlank();
    assertThat(result.headOfficeGUID()).isEqualTo(officeLinkGuid);
    assertThat(result.headOfficeAccountNumber()).isNotBlank();
    verify(officeLiaisonManagerCommandService, never()).createAndLink(any(), any());
  }

  @Test
  void createPractitionerFirm_savesProviderOnly_returnsNullOfficeFields() {
    UUID providerGuid = UUID.randomUUID();
    when(providerPersistService.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });

    var result =
        service.createPractitionerFirm(
            AdvocatePractitionerEntity.builder().name("A. Barrister").build(), null, null);

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).isNotBlank();
    assertThat(result.headOfficeGUID()).isNull();
    assertThat(result.headOfficeAccountNumber()).isNull();
  }

  @Test
  void createPractitionerFirm_withParentFirmByNumber_savesParentLink() {
    UUID providerGuid = UUID.randomUUID();
    UUID parentGuid = UUID.randomUUID();
    ProviderEntity parent = ChamberProviderEntity.builder().build();
    parent.setGuid(parentGuid);
    OfficeEntity parentOffice = new OfficeEntity();
    ProviderOfficeLinkEntity parentOfficeLink = new ProviderOfficeLinkEntity();
    parentOfficeLink.setOffice(parentOffice);

    when(providerPersistService.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    when(providerQueryService.getProvider("100002")).thenReturn(parent);
    when(providerPersistService.saveParentLink(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeQueryService.getProviderHeadOfficeLink(parent))
        .thenReturn(Optional.of(parentOfficeLink));
    when(officeCommandService.saveAdvocateOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createPractitionerFirm(
        AdvocatePractitionerEntity.builder().name("A. Advocate").build(),
        List.of(new PractitionerDetailsParentUpdateV2OneOf1("100002")),
        null);

    verify(providerPersistService).saveParentLink(any(ProviderParentLinkEntity.class));
    verify(officeCommandService)
        .saveAdvocateOfficeLink(any(AdvocateProviderOfficeLinkEntity.class));
  }

  @Test
  void createPractitionerFirm_withParentFirmByGuid_savesParentLink() {
    UUID providerGuid = UUID.randomUUID();
    UUID parentGuid = UUID.randomUUID();
    ProviderEntity parent = ChamberProviderEntity.builder().build();
    parent.setGuid(parentGuid);
    OfficeEntity parentOffice = new OfficeEntity();
    ProviderOfficeLinkEntity parentOfficeLink = new ProviderOfficeLinkEntity();
    parentOfficeLink.setOffice(parentOffice);

    when(providerPersistService.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    when(providerQueryService.getProvider(parentGuid.toString())).thenReturn(parent);
    when(providerPersistService.saveParentLink(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeQueryService.getProviderHeadOfficeLink(parent))
        .thenReturn(Optional.of(parentOfficeLink));
    when(officeCommandService.saveAdvocateOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createPractitionerFirm(
        AdvocatePractitionerEntity.builder().name("A. Advocate").build(),
        List.of(new PractitionerDetailsParentUpdateV2OneOf(parentGuid)),
        null);

    verify(providerPersistService).saveParentLink(any(ProviderParentLinkEntity.class));
    verify(officeCommandService)
        .saveAdvocateOfficeLink(any(AdvocateProviderOfficeLinkEntity.class));
  }

  @Test
  void createPractitionerFirm_withParentFirm_returnsOfficeFieldsFromCreatedLink() {
    UUID parentGuid = UUID.randomUUID();
    ProviderEntity parent = ChamberProviderEntity.builder().build();
    parent.setGuid(parentGuid);
    UUID chambersOfficeGuid = UUID.randomUUID();
    OfficeEntity parentOffice = new OfficeEntity();
    parentOffice.setGuid(chambersOfficeGuid);
    ProviderOfficeLinkEntity parentOfficeLink = new ProviderOfficeLinkEntity();
    parentOfficeLink.setOffice(parentOffice);
    UUID providerGuid = UUID.randomUUID();

    when(providerPersistService.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    when(providerQueryService.getProvider("100002")).thenReturn(parent);
    when(providerPersistService.saveParentLink(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeQueryService.getProviderHeadOfficeLink(parent))
        .thenReturn(Optional.of(parentOfficeLink));
    UUID advocateOfficeLinkGuid = UUID.randomUUID();
    when(officeCommandService.saveAdvocateOfficeLink(any()))
        .thenAnswer(
            inv -> {
              AdvocateProviderOfficeLinkEntity link = inv.getArgument(0);
              link.setGuid(advocateOfficeLinkGuid);
              return link;
            });

    var result =
        service.createPractitionerFirm(
            AdvocatePractitionerEntity.builder().name("A. Advocate").build(),
            List.of(new PractitionerDetailsParentUpdateV2OneOf1("100002")),
            null);

    assertThat(result.headOfficeGUID()).isEqualTo(advocateOfficeLinkGuid);
    assertThat(result.headOfficeAccountNumber()).isNotBlank();
  }

  @Test
  void createPractitionerFirm_throwsNotFound_whenParentHasNoHeadOffice() {
    UUID parentGuid = UUID.randomUUID();
    ProviderEntity parent = ChamberProviderEntity.builder().build();
    parent.setGuid(parentGuid);

    when(providerPersistService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerQueryService.getProvider("CH-NO-OFFICE")).thenReturn(parent);
    when(providerPersistService.saveParentLink(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeQueryService.getProviderHeadOfficeLink(parent)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.createPractitionerFirm(
                    AdvocatePractitionerEntity.builder().name("A.").build(),
                    List.of(new PractitionerDetailsParentUpdateV2OneOf1("CH-NO-OFFICE")),
                    null))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("no head office");
  }

  @Test
  void createPractitionerFirm_withParentFirmByNumber_throwsNotFound_whenParentMissing() {
    when(providerPersistService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerQueryService.getProvider("UNKNOWN"))
        .thenThrow(new ItemNotFoundException("Provider not found: UNKNOWN"));

    assertThatThrownBy(
            () ->
                service.createPractitionerFirm(
                    AdvocatePractitionerEntity.builder().name("A. Advocate").build(),
                    List.of(new PractitionerDetailsParentUpdateV2OneOf1("UNKNOWN")),
                    null))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("UNKNOWN");
  }

  @Test
  void createPractitionerFirm_withEftPayment_persistsBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    when(providerPersistService.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });

    var createDetails =
        new BankAccountProviderOfficeCreateV2("Advocate Account", "12-34-56", "87654321");
    var accountTemplate = new BankAccountEntity();
    when(bankAccountMapper.toBankAccountEntity(createDetails)).thenReturn(accountTemplate);

    var payment =
        new PaymentDetailsCreateV2(PaymentDetailsPaymentMethodV2.EFT)
            .bankAccountDetails(createDetails);

    service.createPractitionerFirm(
        AdvocatePractitionerEntity.builder().name("A. Advocate").build(), null, payment);

    verify(bankDetailsService).createAndLinkToProvider(eq(accountTemplate), any());
  }

  @Test
  void createPractitionerFirm_withCheckPayment_doesNotPersistBankAccount() {
    when(providerPersistService.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var payment = new PaymentDetailsCreateV2(PaymentDetailsPaymentMethodV2.CHECK);

    service.createPractitionerFirm(
        AdvocatePractitionerEntity.builder().name("A. Advocate").build(), null, payment);

    verify(bankDetailsService, never()).createAndLinkToProvider(any(), any());
  }

  // --- bank details wiring ---

  @Test
  void createLspFirm_withEftPayment_persistsBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    when(providerPersistService.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    when(officeCommandService.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity office = inv.getArgument(0);
              office.setGuid(officeGuid);
              return office;
            });

    var linkTemplate = new LspProviderOfficeLinkEntity();
    when(officeCommandService.saveLspOfficeLink(linkTemplate)).thenReturn(linkTemplate);

    var createDetails = new BankAccountProviderOfficeCreateV2("Test Bank", "12-34-56", "87654321");
    var accountTemplate = new BankAccountEntity();
    when(bankAccountMapper.toBankAccountEntity(createDetails)).thenReturn(accountTemplate);

    var payment =
        new PaymentDetailsCreateV2(PaymentDetailsPaymentMethodV2.EFT)
            .bankAccountDetails(createDetails);

    service.createLspFirm(
        LspProviderEntity.builder().name("My LSP").build(),
        OfficeEntity.builder().addressLine1("1 Test St").build(),
        linkTemplate,
        null,
        null,
        payment);

    verify(bankDetailsService)
        .createAndLink(eq(accountTemplate), any(), eq(linkTemplate), isNull());
  }

  @Test
  void createLspFirm_withCheckPayment_doesNotPersistBankAccount() {
    when(providerPersistService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeCommandService.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeCommandService.saveLspOfficeLink(any())).thenAnswer(inv -> inv.getArgument(0));

    var payment = new PaymentDetailsCreateV2(PaymentDetailsPaymentMethodV2.CHECK);

    service.createLspFirm(
        LspProviderEntity.builder().name("My LSP").build(),
        OfficeEntity.builder().addressLine1("1 Test St").build(),
        new LspProviderOfficeLinkEntity(),
        null,
        null,
        payment);

    verify(bankDetailsService, never()).createAndLink(any(), any(), any(), any());
  }

  @Test
  void patchProvider_updatesLspNameAndBasicFields_andReturnsIdentifiers() {
    UUID guid = UUID.randomUUID();

    LspProviderEntity existing =
        LspProviderEntity.builder().firmNumber("100001").name("Old Name").build();
    existing.setGuid(guid);

    when(providerQueryService.getProvider(guid.toString())).thenReturn(existing);
    when(providerPersistService.save(any(ProviderEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .name("New Name")
            .legalServicesProvider(
                new LSPDetailsPatchV2()
                    .constitutionalStatus(LSPDetailsConstitutionalStatusV2.PARTNERSHIP)
                    .indemnityReceivedDate(LocalDate.of(2024, 1, 2))
                    .companiesHouseNumber("12345678"));

    ProviderCreationResult result = service.patchProvider(guid.toString(), patch);

    assertThat(existing.getName()).isEqualTo("New Name");
    assertThat(existing.getConstitutionalStatus()).isEqualTo("Partnership");
    assertThat(existing.getIndemnityReceivedDate()).isEqualTo(LocalDate.of(2024, 1, 2));
    assertThat(existing.getCompaniesHouseNumber()).isEqualTo("12345678");

    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    assertThat(result.firmNumber()).isEqualTo("100001");
  }

  @Test
  void patchProvider_whenProviderNotLsp_rejectsLspPatch() {
    UUID guid = UUID.randomUUID();

    ProviderEntity existing =
        ProviderEntity.builder()
            .firmType(FirmType.CHAMBERS)
            .firmNumber("100002")
            .name("Chambers")
            .build();
    existing.setGuid(guid);

    when(providerQueryService.getProvider(guid.toString())).thenReturn(existing);

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .name("New")
            .legalServicesProvider(new LSPDetailsPatchV2().companiesHouseNumber("X"));

    assertThatThrownBy(() -> service.patchProvider(guid.toString(), patch))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("legalServicesProvider updates require a Legal Services Provider");
  }

  @Test
  void patchProvider_headOfficeReassignmentRejected() {
    UUID guid = UUID.randomUUID();

    LspProviderEntity existing =
        LspProviderEntity.builder().firmNumber("100001").name("Old Name").build();
    existing.setGuid(guid);

    when(providerQueryService.getProvider(guid.toString())).thenReturn(existing);

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .legalServicesProvider(
                new LSPDetailsPatchV2()
                    .headOffice(
                        new uk.gov.justice.laa.providerdata.model.LSPHeadOfficeDetailsPatchV2()));

    assertThatThrownBy(() -> service.patchProvider(guid.toString(), patch))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Head office reassignment is not supported");
  }

  @Test
  void patchProvider_updatesAdvocatePractitionerFields() {
    UUID guid = UUID.randomUUID();

    AdvocatePractitionerEntity existing =
        AdvocatePractitionerEntity.builder().firmNumber("100003").name("Old Name").build();
    existing.setGuid(guid);

    when(providerQueryService.getProvider(guid.toString())).thenReturn(existing);
    when(providerPersistService.save(any(ProviderEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .practitioner(
                new PractitionerDetailsPatchV2()
                    .advocateLevel(PractitionerDetailsAdvocateLevelV2.KC)
                    .solicitorRegulationAuthorityRollNumber("SRA-123"));

    ProviderCreationResult result = service.patchProvider(guid.toString(), patch);

    assertThat(existing.getAdvocateLevel()).isEqualTo("KC");
    assertThat(existing.getSolicitorRegulationAuthorityRollNumber()).isEqualTo("SRA-123");
    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    assertThat(result.firmNumber()).isEqualTo("100003");
  }

  @Test
  void patchProvider_updatesParentFirms() {
    UUID guid = UUID.randomUUID();
    UUID parentGuid = UUID.randomUUID();
    String parentFirmNumber = "200001";

    AdvocatePractitionerEntity existing =
        AdvocatePractitionerEntity.builder().firmNumber("100003").name("Practitioner").build();
    existing.setGuid(guid);

    ProviderEntity parentByGuid = ProviderEntity.builder().name("Parent 1").build();
    parentByGuid.setGuid(parentGuid);

    ProviderEntity parentByFirmNumber =
        ProviderEntity.builder().firmNumber(parentFirmNumber).name("Parent 2").build();
    parentByFirmNumber.setGuid(UUID.randomUUID());

    when(providerQueryService.getProvider(guid.toString())).thenReturn(existing);
    when(providerQueryService.getProvider(parentGuid.toString())).thenReturn(parentByGuid);
    when(providerQueryService.getProvider(parentFirmNumber)).thenReturn(parentByFirmNumber);
    when(providerPersistService.save(any(ProviderEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .practitioner(
                new PractitionerDetailsPatchV2()
                    .parentFirms(
                        List.of(
                            new PractitionerDetailsParentUpdateV2OneOf(parentGuid),
                            new PractitionerDetailsParentUpdateV2OneOf1(parentFirmNumber))));

    service.patchProvider(guid.toString(), patch);

    @SuppressWarnings("unchecked")
    org.mockito.ArgumentCaptor<List<ProviderParentLinkEntity>> linksCaptor =
        org.mockito.ArgumentCaptor.forClass(
            (Class<List<ProviderParentLinkEntity>>) (Class<?>) List.class);
    verify(providerPersistService).replaceParentLinks(eq(existing), linksCaptor.capture());

    List<ProviderParentLinkEntity> savedLinks = linksCaptor.getValue();
    assertThat(savedLinks).hasSize(2);
    assertThat(savedLinks.get(0).getProvider()).isEqualTo(existing);
    assertThat(savedLinks.get(0).getParent()).isEqualTo(parentByGuid);
    assertThat(savedLinks.get(1).getProvider()).isEqualTo(existing);
    assertThat(savedLinks.get(1).getParent()).isEqualTo(parentByFirmNumber);
  }

  @Test
  void patchProvider_ignoresBarristerFieldsForAdvocatePractitioner() {
    UUID guid = UUID.randomUUID();

    AdvocatePractitionerEntity existing =
        AdvocatePractitionerEntity.builder().firmNumber("100003").name("Old Name").build();
    existing.setGuid(guid);

    when(providerQueryService.getProvider(guid.toString())).thenReturn(existing);
    when(providerPersistService.save(any(ProviderEntity.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ProviderPatchV2 patch =
        new ProviderPatchV2()
            .practitioner(new PractitionerDetailsPatchV2().barCouncilRollNumber("BAR-123"));

    ProviderCreationResult result = service.patchProvider(guid.toString(), patch);

    assertThat(result.providerFirmGUID()).isEqualTo(guid);
    assertThat(result.firmNumber()).isEqualTo("100003");
  }
}
