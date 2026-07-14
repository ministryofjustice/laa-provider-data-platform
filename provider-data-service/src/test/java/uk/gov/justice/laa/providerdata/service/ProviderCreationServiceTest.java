package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
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
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.ChambersProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ChambersProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.BankAccountMapper;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.LiaisonManagerCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf1;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ChambersProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ContractManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeContractManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class ProviderCreationServiceTest {

  @Mock private ProviderRepository providerRepository;
  @Mock private OfficeRepository officeRepository;
  @Mock private LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  @Mock private ChambersProviderOfficeLinkRepository chambersProviderOfficeLinkRepository;
  @Mock private AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  @Mock private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Mock private LiaisonManagerRepository liaisonManagerRepository;
  @Mock private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;
  @Mock private ProviderParentLinkRepository providerParentLinkRepository;
  @Mock private ContractManagerRepository contractManagerRepository;
  @Mock private OfficeContractManagerLinkRepository officeContractManagerLinkRepository;
  @Mock private BankDetailsService bankDetailsService;
  @Mock private BankAccountMapper bankAccountMapper;
  @Mock private Counter lspFirmCreationCounter;
  @Mock private Counter chambersFirmCreationCounter;
  @Mock private Counter practitionerFirmCreationCounter;
  @Mock private Timer lspFirmCreationTimer;
  @Mock private Timer chambersFirmCreationTimer;
  @Mock private Timer practitionerFirmCreationTimer;

  @InjectMocks private ProviderCreationService service;

  @Test
  void createLspFirm_savesProviderOfficeAndLink_returnsAllIdentifiers() {
    UUID providerGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    UUID officeGuid = UUID.randomUUID();
    when(officeRepository.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity office = inv.getArgument(0);
              office.setGuid(officeGuid);
              return office;
            });
    UUID officeLinkGuid = UUID.randomUUID();
    when(lspProviderOfficeLinkRepository.save(any()))
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
            null,
            null,
            false);

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).isNotBlank();
    assertThat(result.headOfficeGUID()).isEqualTo(officeLinkGuid);
    assertThat(result.headOfficeAccountNumber()).isNotBlank();
    assertThat(linkTemplate.getHeadOfficeFlag()).isTrue();
    verify(liaisonManagerRepository, never()).save(any());
    verify(officeLiaisonManagerLinkRepository, never()).save(any());
  }

  @Test
  void createLspFirm_withLiaisonManager_savesLmAndLink() {
    UUID providerGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    UUID officeGuid = UUID.randomUUID();
    when(officeRepository.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity office = inv.getArgument(0);
              office.setGuid(officeGuid);
              return office;
            });
    UUID officeLinkGuid = UUID.randomUUID();
    when(lspProviderOfficeLinkRepository.save(any()))
        .thenAnswer(
            inv -> {
              LspProviderOfficeLinkEntity officeLink = inv.getArgument(0);
              officeLink.setGuid(officeLinkGuid);
              return officeLink;
            });
    UUID lmGuid = UUID.randomUUID();
    when(liaisonManagerRepository.save(any()))
        .thenAnswer(
            inv -> {
              LiaisonManagerEntity lm = inv.getArgument(0);
              lm.setGuid(lmGuid);
              return lm;
            });
    when(officeLiaisonManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

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
            null,
            null,
            false);

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    verify(liaisonManagerRepository).save(lmTemplate);
    assertThat(lmLink.getLiaisonManager()).isNotNull();
    assertThat(lmLink.getLiaisonManager().getGuid()).isEqualTo(lmGuid);
    assertThat(lmLink.getOfficeLink()).isNotNull();
    assertThat(lmLink.getOfficeLink().getGuid()).isEqualTo(officeLinkGuid);
    verify(officeLiaisonManagerLinkRepository).save(lmLink);
  }

  @Test
  void createChambersFirm_savesProviderOfficeAndLink_returnsAllIdentifiers() {
    UUID providerGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    UUID officeGuid = UUID.randomUUID();
    when(officeRepository.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity office = inv.getArgument(0);
              office.setGuid(officeGuid);
              return office;
            });
    UUID officeLinkGuid = UUID.randomUUID();
    when(chambersProviderOfficeLinkRepository.save(any()))
        .thenAnswer(
            inv -> {
              ChambersProviderOfficeLinkEntity officeLink = inv.getArgument(0);
              officeLink.setGuid(officeLinkGuid);
              return officeLink;
            });

    var linkTemplate = new ChambersProviderOfficeLinkEntity();
    linkTemplate.setHeadOfficeFlag(Boolean.TRUE);

    var result =
        service.createChambersFirm(
            ChambersProviderEntity.builder().name("My Chambers").build(),
            OfficeEntity.builder().addressLine1("1 Test St").build(),
            linkTemplate,
            null,
            null,
            null,
            null,
            false);

    assertThat(result.providerFirmGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).isNotBlank();
    assertThat(result.headOfficeGUID()).isEqualTo(officeLinkGuid);
    assertThat(result.headOfficeAccountNumber()).isNotBlank();
    verify(liaisonManagerRepository, never()).save(any());
  }

  @Test
  void createChambersFirm_withExistingLmGuid_linksLmByGuid() {
    UUID providerGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    when(officeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(chambersProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UUID lmGuid = UUID.randomUUID();
    LiaisonManagerEntity existingLm = new LiaisonManagerEntity();
    existingLm.setGuid(lmGuid);
    when(liaisonManagerRepository.findById(lmGuid)).thenReturn(Optional.of(existingLm));
    when(officeLiaisonManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createChambersFirm(
        ChambersProviderEntity.builder().name("My Chambers").build(),
        OfficeEntity.builder().addressLine1("1 Test St").build(),
        new ChambersProviderOfficeLinkEntity(),
        null,
        null,
        lmGuid,
        null,
        false);

    verify(liaisonManagerRepository, never()).save(any());
    verify(liaisonManagerRepository).findById(lmGuid);
    org.mockito.ArgumentCaptor<OfficeLiaisonManagerLinkEntity> captor =
        org.mockito.ArgumentCaptor.forClass(OfficeLiaisonManagerLinkEntity.class);
    verify(officeLiaisonManagerLinkRepository).save(captor.capture());
    assertThat(captor.getValue().getLiaisonManager()).isEqualTo(existingLm);
    assertThat(captor.getValue().getLinkedFlag()).isFalse();
  }

  @Test
  void createChambersFirm_withNewLiaisonManager_savesLmAndLink() {
    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    UUID officeLinkGuid = UUID.randomUUID();
    when(chambersProviderOfficeLinkRepository.save(any()))
        .thenAnswer(
            inv -> {
              ChambersProviderOfficeLinkEntity officeLink = inv.getArgument(0);
              officeLink.setGuid(officeLinkGuid);
              return officeLink;
            });
    UUID lmGuid = UUID.randomUUID();
    when(liaisonManagerRepository.save(any()))
        .thenAnswer(
            inv -> {
              LiaisonManagerEntity lm = inv.getArgument(0);
              lm.setGuid(lmGuid);
              return lm;
            });
    when(officeLiaisonManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var lmTemplate = LiaisonManagerEntity.builder().firstName("Jane").lastName("Smith").build();
    var lmLink = new OfficeLiaisonManagerLinkEntity();
    lmLink.setActiveDateFrom(LocalDate.of(2024, 1, 1));

    service.createChambersFirm(
        ChambersProviderEntity.builder().name("My Chambers").build(),
        OfficeEntity.builder().addressLine1("1 Test St").build(),
        new ChambersProviderOfficeLinkEntity(),
        lmTemplate,
        lmLink,
        null,
        null,
        false);

    verify(liaisonManagerRepository).save(lmTemplate);
    assertThat(lmLink.getLiaisonManager().getGuid()).isEqualTo(lmGuid);
    assertThat(lmLink.getOfficeLink().getGuid()).isEqualTo(officeLinkGuid);
    verify(officeLiaisonManagerLinkRepository).save(lmLink);
  }

  @Test
  void createPractitionerFirm_withNullParentFirms_throwsIllegalArgumentException() {
    assertThatThrownBy(
            () ->
                service.createPractitionerFirm(
                    AdvocatePractitionerEntity.builder().name("A. Barrister").build(),
                    null,
                    practitionerLiaisonManagerCreateRequest(),
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Exactly one parent Chamber");
  }

  @Test
  void createPractitionerFirm_withParentFirmByNumber_savesParentLink() {
    stubLiaisonManagerCreatePersistence();
    UUID providerGuid = UUID.randomUUID();
    UUID parentGuid = UUID.randomUUID();
    ProviderEntity parent = ChambersProviderEntity.builder().build();
    parent.setGuid(parentGuid);
    OfficeEntity parentOffice = new OfficeEntity();
    ProviderOfficeLinkEntity parentOfficeLink = new ProviderOfficeLinkEntity();
    parentOfficeLink.setOffice(parentOffice);

    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    when(providerRepository.findByFirmNumber("100002")).thenReturn(Optional.of(parent));
    when(providerParentLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(parent))
        .thenReturn(Optional.of(parentOfficeLink));
    when(advocateProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createPractitionerFirm(
        AdvocatePractitionerEntity.builder().name("A. Advocate").build(),
        List.of(new PractitionerDetailsParentUpdateV2OneOf1("100002")),
        practitionerLiaisonManagerCreateRequest(),
        null);

    verify(providerParentLinkRepository).save(any(ProviderParentLinkEntity.class));
    verify(advocateProviderOfficeLinkRepository).save(any(AdvocateProviderOfficeLinkEntity.class));
  }

  @Test
  void createPractitionerFirm_withParentFirmByGuid_savesParentLink() {
    stubLiaisonManagerCreatePersistence();
    UUID providerGuid = UUID.randomUUID();
    UUID parentGuid = UUID.randomUUID();
    ProviderEntity parent = ChambersProviderEntity.builder().build();
    parent.setGuid(parentGuid);
    OfficeEntity parentOffice = new OfficeEntity();
    ProviderOfficeLinkEntity parentOfficeLink = new ProviderOfficeLinkEntity();
    parentOfficeLink.setOffice(parentOffice);

    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    when(providerRepository.findById(parentGuid)).thenReturn(Optional.of(parent));
    when(providerParentLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(parent))
        .thenReturn(Optional.of(parentOfficeLink));
    when(advocateProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createPractitionerFirm(
        AdvocatePractitionerEntity.builder().name("A. Advocate").build(),
        List.of(new PractitionerDetailsParentUpdateV2OneOf(parentGuid)),
        practitionerLiaisonManagerCreateRequest(),
        null);

    verify(providerParentLinkRepository).save(any(ProviderParentLinkEntity.class));
    verify(advocateProviderOfficeLinkRepository).save(any(AdvocateProviderOfficeLinkEntity.class));
  }

  @Test
  void createPractitionerFirm_withParentFirm_returnsOfficeFieldsFromCreatedLink() {
    stubLiaisonManagerCreatePersistence();
    UUID parentGuid = UUID.randomUUID();
    ProviderEntity parent = ChambersProviderEntity.builder().build();
    parent.setGuid(parentGuid);
    UUID chambersOfficeGuid = UUID.randomUUID();
    OfficeEntity parentOffice = new OfficeEntity();
    parentOffice.setGuid(chambersOfficeGuid);
    ProviderOfficeLinkEntity parentOfficeLink = new ProviderOfficeLinkEntity();
    parentOfficeLink.setOffice(parentOffice);
    UUID providerGuid = UUID.randomUUID();

    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    when(providerRepository.findByFirmNumber("100002")).thenReturn(Optional.of(parent));
    when(providerParentLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(parent))
        .thenReturn(Optional.of(parentOfficeLink));
    UUID advocateOfficeLinkGuid = UUID.randomUUID();
    when(advocateProviderOfficeLinkRepository.save(any()))
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
            practitionerLiaisonManagerCreateRequest(),
            null);

    assertThat(result.headOfficeGUID()).isEqualTo(advocateOfficeLinkGuid);
    assertThat(result.headOfficeAccountNumber()).isNotBlank();
  }

  @Test
  void createPractitionerFirm_throwsNotFound_whenParentHasNoHeadOffice() {
    UUID parentGuid = UUID.randomUUID();
    ProviderEntity parent = ChambersProviderEntity.builder().build();
    parent.setGuid(parentGuid);

    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerRepository.findByFirmNumber("CH-NO-OFFICE")).thenReturn(Optional.of(parent));
    when(providerParentLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(parent))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.createPractitionerFirm(
                    AdvocatePractitionerEntity.builder().name("A.").build(),
                    List.of(new PractitionerDetailsParentUpdateV2OneOf1("CH-NO-OFFICE")),
                    practitionerLiaisonManagerCreateRequest(),
                    null))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("no head office");
  }

  @Test
  void createPractitionerFirm_withParentFirmByNumber_throwsNotFound_whenParentMissing() {
    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerRepository.findByFirmNumber("UNKNOWN")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.createPractitionerFirm(
                    AdvocatePractitionerEntity.builder().name("A. Advocate").build(),
                    List.of(new PractitionerDetailsParentUpdateV2OneOf1("UNKNOWN")),
                    practitionerLiaisonManagerCreateRequest(),
                    null))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("UNKNOWN");
  }

  @Test
  void createPractitionerFirm_withEftPayment_persistsBankAccount() {
    stubLiaisonManagerCreatePersistence();
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity parent = ChambersProviderEntity.builder().build();
    OfficeEntity parentOffice = new OfficeEntity();
    ProviderOfficeLinkEntity parentOfficeLink = new ProviderOfficeLinkEntity();
    parentOfficeLink.setOffice(parentOffice);

    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    when(providerRepository.findByFirmNumber("100002")).thenReturn(Optional.of(parent));
    when(providerParentLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(parent))
        .thenReturn(Optional.of(parentOfficeLink));
    when(advocateProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var createDetails =
        new BankAccountProviderOfficeCreateV2("Advocate Account", "12-34-56", "87654321");
    var accountTemplate = new BankAccountEntity();
    when(bankAccountMapper.toBankAccountEntity(createDetails)).thenReturn(accountTemplate);

    var payment =
        new PaymentDetailsCreateV2(PaymentDetailsPaymentMethodV2.EFT)
            .bankAccountDetails(createDetails);

    service.createPractitionerFirm(
        AdvocatePractitionerEntity.builder().name("A. Advocate").build(),
        List.of(new PractitionerDetailsParentUpdateV2OneOf1("100002")),
        practitionerLiaisonManagerCreateRequest(),
        payment);

    verify(bankDetailsService)
        .createAndLink(
            eq(accountTemplate), any(), any(AdvocateProviderOfficeLinkEntity.class), isNull());
  }

  @Test
  void createPractitionerFirm_withCheckPayment_doesNotPersistBankAccount() {
    stubLiaisonManagerCreatePersistence();
    ProviderEntity parent = ChambersProviderEntity.builder().build();
    OfficeEntity parentOffice = new OfficeEntity();
    ProviderOfficeLinkEntity parentOfficeLink = new ProviderOfficeLinkEntity();
    parentOfficeLink.setOffice(parentOffice);

    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerRepository.findByFirmNumber("100002")).thenReturn(Optional.of(parent));
    when(providerParentLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(parent))
        .thenReturn(Optional.of(parentOfficeLink));
    when(advocateProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var payment = new PaymentDetailsCreateV2(PaymentDetailsPaymentMethodV2.CHECK);

    service.createPractitionerFirm(
        AdvocatePractitionerEntity.builder().name("A. Advocate").build(),
        List.of(new PractitionerDetailsParentUpdateV2OneOf1("100002")),
        practitionerLiaisonManagerCreateRequest(),
        payment);

    verify(bankDetailsService, never()).createAndLink(any(), any(), any(), any());
  }

  // --- bank details wiring ---

  @Test
  void createLspFirm_withEftPayment_persistsBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
        .thenAnswer(
            inv -> {
              ProviderEntity provider = inv.getArgument(0);
              provider.setGuid(providerGuid);
              return provider;
            });
    when(officeRepository.save(any()))
        .thenAnswer(
            inv -> {
              OfficeEntity office = inv.getArgument(0);
              office.setGuid(officeGuid);
              return office;
            });

    var linkTemplate = new LspProviderOfficeLinkEntity();
    when(lspProviderOfficeLinkRepository.save(linkTemplate)).thenReturn(linkTemplate);

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
        payment,
        null,
        false);

    verify(bankDetailsService)
        .createAndLink(eq(accountTemplate), any(), eq(linkTemplate), isNull());
  }

  @Test
  void createLspFirm_withCheckPayment_doesNotPersistBankAccount() {
    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(lspProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var payment = new PaymentDetailsCreateV2(PaymentDetailsPaymentMethodV2.CHECK);

    service.createLspFirm(
        LspProviderEntity.builder().name("My LSP").build(),
        OfficeEntity.builder().addressLine1("1 Test St").build(),
        new LspProviderOfficeLinkEntity(),
        null,
        null,
        payment,
        null,
        false);

    verify(bankDetailsService, never()).createAndLink(any(), any(), any(), any());
  }

  @Test
  void createPractitionerFirm_withNullLiaisonManager_throwsIllegalArgumentException() {
    ProviderEntity parent = ChambersProviderEntity.builder().build();
    OfficeEntity parentOffice = new OfficeEntity();
    ProviderOfficeLinkEntity parentOfficeLink = new ProviderOfficeLinkEntity();
    parentOfficeLink.setOffice(parentOffice);
    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerRepository.findByFirmNumber("100002")).thenReturn(Optional.of(parent));
    when(providerParentLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(providerOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(parent))
        .thenReturn(Optional.of(parentOfficeLink));
    when(advocateProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    assertThatThrownBy(
            () ->
                service.createPractitionerFirm(
                    AdvocatePractitionerEntity.builder().name("A. Advocate").build(),
                    List.of(new PractitionerDetailsParentUpdateV2OneOf1("100002")),
                    null,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("liaisonManager must be provided");
  }

  private LiaisonManagerCreateV2 practitionerLiaisonManagerCreateRequest() {
    return new LiaisonManagerCreateV2()
        .firstName("A")
        .lastName("Manager")
        .emailAddress("advocate.lm@example.com")
        .telephoneNumber("020 1234 5678");
  }

  private void stubLiaisonManagerCreatePersistence() {
    when(liaisonManagerRepository.save(any()))
        .thenAnswer(
            inv -> {
              LiaisonManagerEntity lm = inv.getArgument(0);
              if (lm.getGuid() == null) {
                lm.setGuid(UUID.randomUUID());
              }
              return lm;
            });
    when(officeLiaisonManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void createLspFirm_withContractManagerGuid_savesLink() {
    UUID cmGuid = UUID.randomUUID();
    ContractManagerEntity cm = new ContractManagerEntity();
    cm.setGuid(cmGuid);

    LspProviderOfficeLinkEntity savedLink = new LspProviderOfficeLinkEntity();
    savedLink.setGuid(UUID.randomUUID());

    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(lspProviderOfficeLinkRepository.save(any())).thenReturn(savedLink);
    when(contractManagerRepository.findById(cmGuid)).thenReturn(Optional.of(cm));
    when(officeContractManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createLspFirm(
        LspProviderEntity.builder().name("My LSP").build(),
        OfficeEntity.builder().addressLine1("1 Test St").build(),
        new LspProviderOfficeLinkEntity(),
        null,
        null,
        null,
        cmGuid,
        false);

    ArgumentCaptor<OfficeContractManagerLinkEntity> captor =
        ArgumentCaptor.forClass(OfficeContractManagerLinkEntity.class);
    verify(officeContractManagerLinkRepository).save(captor.capture());
    assertThat(captor.getValue().getContractManager()).isEqualTo(cm);
    assertThat(captor.getValue().getOfficeLink()).isEqualTo(savedLink);
  }

  @Test
  void createLspFirm_withUnknownContractManagerGuid_throwsBeforeCommit() {
    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(lspProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UUID unknownCmGuid = UUID.randomUUID();
    when(contractManagerRepository.findById(unknownCmGuid)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.createLspFirm(
                    LspProviderEntity.builder().name("My LSP").build(),
                    OfficeEntity.builder().addressLine1("1 Test St").build(),
                    new LspProviderOfficeLinkEntity(),
                    null,
                    null,
                    null,
                    unknownCmGuid,
                    false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(unknownCmGuid.toString());

    verify(officeContractManagerLinkRepository, never()).save(any());
  }

  @Test
  void createLspFirm_withUseDefaultContractManager_savesDefaultLink() {
    UUID cmGuid = UUID.randomUUID();
    ContractManagerEntity defaultCm = new ContractManagerEntity();
    defaultCm.setGuid(cmGuid);

    LspProviderOfficeLinkEntity savedLink = new LspProviderOfficeLinkEntity();
    savedLink.setGuid(UUID.randomUUID());

    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(lspProviderOfficeLinkRepository.save(any())).thenReturn(savedLink);
    when(contractManagerRepository.findByDefaultContractManagerTrue())
        .thenReturn(Optional.of(defaultCm));
    when(officeContractManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createLspFirm(
        LspProviderEntity.builder().name("My LSP").build(),
        OfficeEntity.builder().addressLine1("1 Test St").build(),
        new LspProviderOfficeLinkEntity(),
        null,
        null,
        null,
        null,
        true);

    ArgumentCaptor<OfficeContractManagerLinkEntity> captor =
        ArgumentCaptor.forClass(OfficeContractManagerLinkEntity.class);
    verify(officeContractManagerLinkRepository).save(captor.capture());
    assertThat(captor.getValue().getContractManager()).isEqualTo(defaultCm);
    assertThat(captor.getValue().getOfficeLink()).isEqualTo(savedLink);
  }

  @Test
  void createLspFirm_withUseDefaultContractManager_throwsWhenNoDefaultConfigured() {
    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(lspProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(contractManagerRepository.findByDefaultContractManagerTrue()).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.createLspFirm(
                    LspProviderEntity.builder().name("My LSP").build(),
                    OfficeEntity.builder().addressLine1("1 Test St").build(),
                    new LspProviderOfficeLinkEntity(),
                    null,
                    null,
                    null,
                    null,
                    true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No default contract manager is configured");

    verify(officeContractManagerLinkRepository, never()).save(any());
  }

  @Test
  void createChambersFirm_withContractManagerGuid_savesLink() {
    UUID cmGuid = UUID.randomUUID();
    ContractManagerEntity cm = new ContractManagerEntity();
    cm.setGuid(cmGuid);

    ChambersProviderOfficeLinkEntity savedLink = new ChambersProviderOfficeLinkEntity();
    savedLink.setGuid(UUID.randomUUID());

    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(chambersProviderOfficeLinkRepository.save(any())).thenReturn(savedLink);
    when(contractManagerRepository.findById(cmGuid)).thenReturn(Optional.of(cm));
    when(officeContractManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createChambersFirm(
        ChambersProviderEntity.builder().name("My Chambers").build(),
        OfficeEntity.builder().addressLine1("1 Test St").build(),
        new ChambersProviderOfficeLinkEntity(),
        null,
        null,
        null,
        cmGuid,
        false);

    ArgumentCaptor<OfficeContractManagerLinkEntity> captor =
        ArgumentCaptor.forClass(OfficeContractManagerLinkEntity.class);
    verify(officeContractManagerLinkRepository).save(captor.capture());
    assertThat(captor.getValue().getContractManager()).isEqualTo(cm);
    assertThat(captor.getValue().getOfficeLink()).isEqualTo(savedLink);
  }

  @Test
  void createChambersFirm_withUseDefaultContractManager_savesDefaultLink() {
    UUID cmGuid = UUID.randomUUID();
    ContractManagerEntity defaultCm = new ContractManagerEntity();
    defaultCm.setGuid(cmGuid);

    ChambersProviderOfficeLinkEntity savedLink = new ChambersProviderOfficeLinkEntity();
    savedLink.setGuid(UUID.randomUUID());

    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(chambersProviderOfficeLinkRepository.save(any())).thenReturn(savedLink);
    when(contractManagerRepository.findByDefaultContractManagerTrue())
        .thenReturn(Optional.of(defaultCm));
    when(officeContractManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createChambersFirm(
        ChambersProviderEntity.builder().name("My Chambers").build(),
        OfficeEntity.builder().addressLine1("1 Test St").build(),
        new ChambersProviderOfficeLinkEntity(),
        null,
        null,
        null,
        null,
        true);

    ArgumentCaptor<OfficeContractManagerLinkEntity> captor =
        ArgumentCaptor.forClass(OfficeContractManagerLinkEntity.class);
    verify(officeContractManagerLinkRepository).save(captor.capture());
    assertThat(captor.getValue().getContractManager()).isEqualTo(defaultCm);
    assertThat(captor.getValue().getOfficeLink()).isEqualTo(savedLink);
  }

  @Test
  void createChambersFirm_withNoContractManagerOptions_doesNotLinkContractManager() {
    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(officeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(chambersProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createChambersFirm(
        ChambersProviderEntity.builder().name("My Chambers").build(),
        OfficeEntity.builder().addressLine1("1 Test St").build(),
        new ChambersProviderOfficeLinkEntity(),
        null,
        null,
        null,
        null,
        false);

    verify(officeContractManagerLinkRepository, never()).save(any());
    verify(contractManagerRepository, never()).findById(any());
    verify(contractManagerRepository, never()).findByDefaultContractManagerTrue();
  }
}
