package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.justice.laa.providerdata.entity.BankAccountEntity;
import uk.gov.justice.laa.providerdata.entity.LiaisonManagerEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.BankAccountMapper;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.BankAccountProviderOfficeLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {

  @Mock private ProviderRepository providerRepository;
  @Mock private OfficeRepository officeRepository;
  @Mock private LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  @Mock private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Mock private LiaisonManagerRepository liaisonManagerRepository;
  @Mock private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;
  @Mock private BankDetailsService bankDetailsService;
  @Mock private BankAccountMapper bankAccountMapper;

  @InjectMocks private OfficeService service;

  @Test
  void createLspOffice_lookupByGuid_returnsPopulatedResult() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeLinkGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(UUID.randomUUID());

    LspProviderOfficeLinkEntity savedLink = new LspProviderOfficeLinkEntity();
    savedLink.setGuid(officeLinkGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(savedOffice);
    when(lspProviderOfficeLinkRepository.save(any())).thenReturn(savedLink);

    OfficeCreationResult result =
        service.createLspOffice(
            providerGuid.toString(), new OfficeEntity(), new LspProviderOfficeLinkEntity());

    assertThat(result.providerGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).isEqualTo("FRM001");
    assertThat(result.officeGUID()).isEqualTo(officeLinkGuid);
    assertThat(result.accountNumber()).isNotBlank();
  }

  @Test
  void createLspOffice_lookupByFirmNumber_returnsPopulatedResult() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeLinkGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM999").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(UUID.randomUUID());

    LspProviderOfficeLinkEntity savedLink = new LspProviderOfficeLinkEntity();
    savedLink.setGuid(officeLinkGuid);

    when(providerRepository.findByFirmNumber("FRM999")).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(savedOffice);
    when(lspProviderOfficeLinkRepository.save(any())).thenReturn(savedLink);

    OfficeCreationResult result =
        service.createLspOffice("FRM999", new OfficeEntity(), new LspProviderOfficeLinkEntity());

    assertThat(result.firmNumber()).isEqualTo("FRM999");
    assertThat(result.officeGUID()).isEqualTo(officeLinkGuid);
  }

  @Test
  void createLspOffice_throwsWhenGuidNotFound() {
    UUID unknownGuid = UUID.randomUUID();
    when(providerRepository.findById(unknownGuid)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.createLspOffice(
                    unknownGuid.toString(), new OfficeEntity(), new LspProviderOfficeLinkEntity()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(unknownGuid.toString());
  }

  @Test
  void createLspOffice_throwsWhenFirmNumberNotFound() {
    when(providerRepository.findByFirmNumber("UNKNOWN")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.createLspOffice(
                    "UNKNOWN", new OfficeEntity(), new LspProviderOfficeLinkEntity()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("UNKNOWN");
  }

  // --- getLspOffices ---

  @Test
  void createLspOffice_withNewLiaisonManager_savesLmAndLink() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    UUID officeGuid = UUID.randomUUID();
    OfficeEntity savedOffice = new OfficeEntity();

    UUID officeLinkGuid = UUID.randomUUID();
    UUID lmGuid = UUID.randomUUID();
    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(savedOffice);
    when(lspProviderOfficeLinkRepository.save(any()))
        .thenAnswer(
            inv -> {
              LspProviderOfficeLinkEntity officeLink = inv.getArgument(0);
              officeLink.setGuid(officeLinkGuid);
              return officeLink;
            });
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

    service.createLspOffice(
        providerGuid.toString(),
        new OfficeEntity(),
        new LspProviderOfficeLinkEntity(),
        lmTemplate,
        lmLink,
        false,
        null);

    verify(liaisonManagerRepository).save(lmTemplate);
    assertThat(lmLink.getLiaisonManager().getGuid()).isEqualTo(lmGuid);
    assertThat(lmLink.getOfficeLink().getGuid()).isEqualTo(officeLinkGuid);
    verify(officeLiaisonManagerLinkRepository).save(lmLink);
  }

  @Test
  void createLspOffice_withLinkToHeadOfficeLiaisonManager_linksExistingLm() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    UUID headOfficeGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(officeGuid);

    OfficeEntity headOffice = new OfficeEntity();
    headOffice.setGuid(headOfficeGuid);

    LspProviderOfficeLinkEntity headOfficeLink = new LspProviderOfficeLinkEntity();
    headOfficeLink.setOffice(headOffice);
    headOfficeLink.setHeadOfficeFlag(Boolean.TRUE);

    LiaisonManagerEntity existingLm =
        LiaisonManagerEntity.builder().firstName("Bob").lastName("Jones").build();

    OfficeLiaisonManagerLinkEntity headOfficeLmLink = new OfficeLiaisonManagerLinkEntity();
    headOfficeLmLink.setLiaisonManager(existingLm);
    headOfficeLmLink.setOfficeLink(headOfficeLink);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(savedOffice);
    when(lspProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(lspProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider))
        .thenReturn(Optional.of(headOfficeLink));
    when(officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(headOfficeLink))
        .thenReturn(List.of(headOfficeLmLink));
    when(officeLiaisonManagerLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createLspOffice(
        providerGuid.toString(),
        new OfficeEntity(),
        new LspProviderOfficeLinkEntity(),
        null,
        null,
        true,
        null);

    verify(liaisonManagerRepository, never()).save(any());
    verify(officeLiaisonManagerLinkRepository).save(any(OfficeLiaisonManagerLinkEntity.class));
  }

  @Test
  void createLspOffice_withLinkToHeadOffice_throwsWhenNoActiveManagerFound() {
    UUID providerGuid = UUID.randomUUID();
    UUID headOfficeGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(UUID.randomUUID());

    OfficeEntity headOffice = new OfficeEntity();
    headOffice.setGuid(headOfficeGuid);

    LspProviderOfficeLinkEntity headOfficeLink = new LspProviderOfficeLinkEntity();
    headOfficeLink.setOffice(headOffice);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(savedOffice);
    when(lspProviderOfficeLinkRepository.findByProviderAndHeadOfficeFlagTrue(provider))
        .thenReturn(Optional.of(headOfficeLink));
    when(officeLiaisonManagerLinkRepository.findByOfficeLinkAndActiveDateToIsNull(headOfficeLink))
        .thenReturn(List.of());

    assertThatThrownBy(
            () ->
                service.createLspOffice(
                    providerGuid.toString(),
                    new OfficeEntity(),
                    new LspProviderOfficeLinkEntity(),
                    null,
                    null,
                    true,
                    null))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("No active liaison manager found");
  }

  @Test
  void getLspOffices_byGuid_returnsPageFromRepository() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    LspProviderOfficeLinkEntity link = lspLink();

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(lspProviderOfficeLinkRepository.findByProvider(eq(provider), any()))
        .thenReturn(new PageImpl<>(List.of(link), PageRequest.of(0, 20), 1));

    var result = service.getLspOffices(providerGuid.toString(), PageRequest.of(0, 20));

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent()).containsExactly(link);
  }

  @Test
  void getLspOffices_throwsWhenProviderNotFound() {
    when(providerRepository.findByFirmNumber("UNKNOWN")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getLspOffices("UNKNOWN", PageRequest.of(0, 20)))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("UNKNOWN");
  }

  // --- getLspOffice ---

  @Test
  void getLspOffice_byOfficeLinkGuid_returnsEntity() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeLinkGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    LspProviderOfficeLinkEntity link = lspLink();

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(lspProviderOfficeLinkRepository.findByProviderAndGuid(provider, officeLinkGuid))
        .thenReturn(Optional.of(link));

    assertThat(service.getLspOfficeLink(providerGuid.toString(), officeLinkGuid.toString()))
        .isSameAs(link);
  }

  @Test
  void getLspOffice_byAccountNumber_returnsEntity() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    LspProviderOfficeLinkEntity link = lspLink();

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(lspProviderOfficeLinkRepository.findByProviderAndAccountNumber(provider, "ABC123"))
        .thenReturn(Optional.of(link));

    assertThat(service.getLspOfficeLink(providerGuid.toString(), "ABC123")).isSameAs(link);
  }

  @Test
  void getLspOffice_throwsWhenOfficeNotFound() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeLinkGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(lspProviderOfficeLinkRepository.findByProviderAndGuid(provider, officeLinkGuid))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.getLspOfficeLink(providerGuid.toString(), officeLinkGuid.toString()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(officeLinkGuid.toString());
  }

  private static LspProviderOfficeLinkEntity lspLink() {
    OfficeEntity office = new OfficeEntity();
    office.setGuid(UUID.randomUUID());
    office.setVersion(1L);

    LspProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();
    link.setOffice(office);
    link.setAccountNumber("ABC123");
    link.setFirmType("Legal Services Provider");
    return link;
  }

  // --- getOfficeLink ---

  @Test
  void getOfficeLink_byOfficeLinkGuid_returnsEntity() {
    UUID officeLinkGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    ProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();

    when(providerOfficeLinkRepository.findByProviderAndGuid(provider, officeLinkGuid))
        .thenReturn(Optional.of(link));

    assertThat(service.getProviderOfficeLink(provider, officeLinkGuid.toString())).isSameAs(link);
  }

  @Test
  void getOfficeLink_byAccountNumber_returnsEntity() {
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    ProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();

    when(providerOfficeLinkRepository.findByProviderAndAccountNumber(provider, "ACC001"))
        .thenReturn(Optional.of(link));

    assertThat(service.getProviderOfficeLink(provider, "ACC001")).isSameAs(link);
  }

  @Test
  void getOfficeLink_throwsWhenNotFound() {
    UUID officeLinkGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();

    when(providerOfficeLinkRepository.findByProviderAndGuid(provider, officeLinkGuid))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProviderOfficeLink(provider, officeLinkGuid.toString()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(officeLinkGuid.toString());
  }

  // --- bank details wiring ---

  @Test
  void createLspOffice_withEftCreatePayment_persistsBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(new OfficeEntity());

    var linkTemplate = new LspProviderOfficeLinkEntity();
    when(lspProviderOfficeLinkRepository.save(linkTemplate)).thenReturn(linkTemplate);

    var createDetails = new BankAccountProviderOfficeCreateV2("Test Bank", "12-34-56", "87654321");
    var accountTemplate = new BankAccountEntity();
    when(bankAccountMapper.toBankAccountEntity(createDetails)).thenReturn(accountTemplate);

    var payment =
        new PaymentDetailsCreateOrLinkV2(PaymentDetailsPaymentMethodV2.EFT)
            .bankAccountDetails(createDetails);

    service.createLspOffice(
        providerGuid.toString(), new OfficeEntity(), linkTemplate, null, null, false, payment);

    verify(bankDetailsService)
        .createAndLink(accountTemplate, provider, linkTemplate, createDetails.getActiveDateFrom());
  }

  @Test
  void createLspOffice_withEftLinkPayment_linksExistingBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(new OfficeEntity());

    var linkTemplate = new LspProviderOfficeLinkEntity();
    when(lspProviderOfficeLinkRepository.save(linkTemplate)).thenReturn(linkTemplate);

    UUID bankGuid = UUID.randomUUID();
    var linkDetails = new BankAccountProviderOfficeLinkV2().bankAccountGUID(bankGuid.toString());
    var payment =
        new PaymentDetailsCreateOrLinkV2(PaymentDetailsPaymentMethodV2.EFT)
            .bankAccountDetails(linkDetails);

    service.createLspOffice(
        providerGuid.toString(), new OfficeEntity(), linkTemplate, null, null, false, payment);

    verify(bankDetailsService).linkExisting(bankGuid, provider, linkTemplate, null);
    verify(bankAccountMapper, never()).toBankAccountEntity(any());
  }

  @Test
  void createLspOffice_withCheckPayment_doesNotPersistBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(new OfficeEntity());
    when(lspProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var payment = new PaymentDetailsCreateOrLinkV2(PaymentDetailsPaymentMethodV2.CHECK);

    service.createLspOffice(
        providerGuid.toString(),
        new OfficeEntity(),
        new LspProviderOfficeLinkEntity(),
        null,
        null,
        false,
        payment);

    verify(bankDetailsService, never()).createAndLink(any(), any(), any(), any());
    verify(bankDetailsService, never()).linkExisting(any(), any(), any(), any());
  }

  @Test
  void createLspOffice_withNullPayment_doesNotPersistBankAccount() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(new OfficeEntity());
    when(lspProviderOfficeLinkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    service.createLspOffice(
        providerGuid.toString(),
        new OfficeEntity(),
        new LspProviderOfficeLinkEntity(),
        null,
        null,
        false,
        null);

    verify(bankDetailsService, never()).createAndLink(any(), any(), any(), any());
    verify(bankDetailsService, never()).linkExisting(any(), any(), any(), any());
  }

  @Test
  void getOfficesGlobal_noFilters_returnsAllFromRepository() {
    var pageable = PageRequest.of(0, 10);
    var link = new ProviderOfficeLinkEntity();
    var expected = new PageImpl<>(List.of(link), pageable, 1);
    when(providerOfficeLinkRepository.findAll(pageable)).thenReturn(expected);

    var result = service.getOfficesGlobal(null, null, null, pageable);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getOfficesGlobal_emptyLists_returnsAllFromRepository() {
    var pageable = PageRequest.of(0, 10);
    Page<ProviderOfficeLinkEntity> expected = new PageImpl<>(List.of(), pageable, 0);
    when(providerOfficeLinkRepository.findAll(pageable)).thenReturn(expected);

    var result = service.getOfficesGlobal(List.of(), List.of(), false, pageable);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getOfficesGlobal_guidFilter_returnsFilteredPage() {
    var pageable = PageRequest.of(0, 10);
    var guid = UUID.randomUUID();
    var link = new ProviderOfficeLinkEntity();
    var expected = new PageImpl<>(List.of(link), pageable, 1);
    when(providerOfficeLinkRepository.findByGuidInOrAccountNumberIn(
            List.of(guid), List.of(), pageable))
        .thenReturn(expected);

    var result = service.getOfficesGlobal(List.of(guid.toString()), null, false, pageable);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getOfficesGlobal_codeFilter_returnsFilteredPage() {
    var pageable = PageRequest.of(0, 10);
    var link = new ProviderOfficeLinkEntity();
    var expected = new PageImpl<>(List.of(link), pageable, 1);
    when(providerOfficeLinkRepository.findByGuidInOrAccountNumberIn(
            List.of(), List.of("ABC001"), pageable))
        .thenReturn(expected);

    var result = service.getOfficesGlobal(null, List.of("ABC001"), false, pageable);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getOfficesGlobal_allProviderOffices_expandsToProviderOffices() {
    var pageable = PageRequest.of(0, 10);
    var guid = UUID.randomUUID();
    var provider = new ProviderEntity();
    var matchingLink = new ProviderOfficeLinkEntity();
    matchingLink.setProvider(provider);
    var allLink = new ProviderOfficeLinkEntity();
    var expected = new PageImpl<>(List.of(matchingLink, allLink), pageable, 2);

    when(providerOfficeLinkRepository.findByGuidInOrAccountNumberIn(List.of(guid), List.of()))
        .thenReturn(List.of(matchingLink));
    when(providerOfficeLinkRepository.findByProviderIn(Set.of(provider), pageable))
        .thenReturn(expected);

    var result = service.getOfficesGlobal(List.of(guid.toString()), null, true, pageable);

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getOfficesGlobal_allProviderOffices_noMatchingOffices_returnsEmptyPage() {
    var pageable = PageRequest.of(0, 10);
    var guid = UUID.randomUUID();
    when(providerOfficeLinkRepository.findByGuidInOrAccountNumberIn(List.of(guid), List.of()))
        .thenReturn(List.of());

    var result = service.getOfficesGlobal(List.of(guid.toString()), null, true, pageable);

    assertThat(result.getContent()).isEmpty();
  }
}
