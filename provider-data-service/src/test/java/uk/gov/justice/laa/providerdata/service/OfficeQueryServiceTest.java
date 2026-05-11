package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.justice.laa.providerdata.entity.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class OfficeQueryServiceTest {

  @Mock private ProviderRepository providerRepository;
  @Mock private LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  @Mock private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Mock private OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;

  @InjectMocks private OfficeQueryService service;

  // --- getLspOffices ---

  @Test
  void getLspOffices_byGuid_returnsPageFromRepository() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
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

  // --- getLspOfficeLink ---

  @Test
  void getLspOffice_byOfficeLinkGuid_returnsEntity() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeLinkGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
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
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
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
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(lspProviderOfficeLinkRepository.findByProviderAndGuid(provider, officeLinkGuid))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.getLspOfficeLink(providerGuid.toString(), officeLinkGuid.toString()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(officeLinkGuid.toString());
  }

  // --- getProviderOfficeLink(String, String) ---

  @Test
  void getProviderOfficeLink_byString_byOfficeLinkGuid_returnsEntity() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeLinkGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);
    ProviderOfficeLinkEntity link = new ChamberProviderOfficeLinkEntity();

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(providerOfficeLinkRepository.findByProviderAndGuid(provider, officeLinkGuid))
        .thenReturn(Optional.of(link));

    assertThat(service.getProviderOfficeLink(providerGuid.toString(), officeLinkGuid.toString()))
        .isSameAs(link);
    verify(providerRepository, never()).findByFirmNumber(any());
    verify(providerOfficeLinkRepository, never()).findByProviderAndAccountNumber(any(), any());
  }

  @Test
  void getProviderOfficeLink_byString_byAccountNumber_returnsEntity() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);
    ProviderOfficeLinkEntity link = new ChamberProviderOfficeLinkEntity();

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(providerOfficeLinkRepository.findByProviderAndAccountNumber(provider, "CH001"))
        .thenReturn(Optional.of(link));

    assertThat(service.getProviderOfficeLink(providerGuid.toString(), "CH001")).isSameAs(link);
    verify(providerRepository, never()).findByFirmNumber(any());
    verify(providerOfficeLinkRepository, never()).findByProviderAndGuid(any(), any());
  }

  @Test
  void getProviderOfficeLink_byString_throwsWhenOfficeNotFound() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeLinkGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    provider.setGuid(providerGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(providerOfficeLinkRepository.findByProviderAndGuid(provider, officeLinkGuid))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.getProviderOfficeLink(providerGuid.toString(), officeLinkGuid.toString()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(officeLinkGuid.toString());
    verify(providerRepository, never()).findByFirmNumber(any());
    verify(providerOfficeLinkRepository).findByProviderAndGuid(provider, officeLinkGuid);
  }

  // --- getProviderOfficeLink(ProviderEntity, String) ---

  @Test
  void getOfficeLink_byOfficeLinkGuid_returnsEntity() {
    UUID officeLinkGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    ProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();

    when(providerOfficeLinkRepository.findByProviderAndGuid(provider, officeLinkGuid))
        .thenReturn(Optional.of(link));

    assertThat(service.getProviderOfficeLink(provider, officeLinkGuid.toString())).isSameAs(link);
  }

  @Test
  void getOfficeLink_byAccountNumber_returnsEntity() {
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();
    ProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();

    when(providerOfficeLinkRepository.findByProviderAndAccountNumber(provider, "ACC001"))
        .thenReturn(Optional.of(link));

    assertThat(service.getProviderOfficeLink(provider, "ACC001")).isSameAs(link);
  }

  @Test
  void getOfficeLink_throwsWhenNotFound() {
    UUID officeLinkGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("100001").build();

    when(providerOfficeLinkRepository.findByProviderAndGuid(provider, officeLinkGuid))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProviderOfficeLink(provider, officeLinkGuid.toString()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(officeLinkGuid.toString());
  }

  // --- getOfficesGlobal ---

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
    var expected = new PageImpl<ProviderOfficeLinkEntity>(List.of(), pageable, 0);
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

  private static LspProviderOfficeLinkEntity lspLink() {
    OfficeEntity office = new OfficeEntity();
    office.setGuid(UUID.randomUUID());
    office.setVersion(1L);

    LspProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();
    link.setOffice(office);
    link.setAccountNumber("ABC123");
    link.setFirmType(FirmType.LEGAL_SERVICES_PROVIDER);
    return link;
  }
}
