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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.BankAccountMapper;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsParentUpdateV2OneOf1;
import uk.gov.justice.laa.providerdata.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ChamberProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
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
  @Mock private ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository;
  @Mock private AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;
  @Mock private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Mock private LiaisonManagerRepository liaisonManagerRepository;
  @Mock private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;
  @Mock private ProviderParentLinkRepository providerParentLinkRepository;
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
            null);

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
            null);

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
    when(chamberProviderOfficeLinkRepository.save(any()))
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
    verify(liaisonManagerRepository, never()).save(any());
  }

  @Test
  void createPractitionerFirm_savesProviderOnly_returnsNullOfficeFields() {
    UUID providerGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
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
        null);

    verify(providerParentLinkRepository).save(any(ProviderParentLinkEntity.class));
    verify(advocateProviderOfficeLinkRepository).save(any(AdvocateProviderOfficeLinkEntity.class));
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
        null);

    verify(providerParentLinkRepository).save(any(ProviderParentLinkEntity.class));
    verify(advocateProviderOfficeLinkRepository).save(any(AdvocateProviderOfficeLinkEntity.class));
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
            null);

    assertThat(result.headOfficeGUID()).isEqualTo(advocateOfficeLinkGuid);
    assertThat(result.headOfficeAccountNumber()).isNotBlank();
  }

  @Test
  void createPractitionerFirm_throwsNotFound_whenParentHasNoHeadOffice() {
    UUID parentGuid = UUID.randomUUID();
    ProviderEntity parent = ChamberProviderEntity.builder().build();
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
                    null))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("UNKNOWN");
  }

  @Test
  void createPractitionerFirm_withEftPayment_persistsBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    when(providerRepository.save(any()))
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
    when(providerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

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
        payment);

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
        payment);

    verify(bankDetailsService, never()).createAndLink(any(), any(), any(), any());
  }
}
