package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {

  @Mock private ProviderRepository providerRepository;
  @Mock private OfficeRepository officeRepository;
  @Mock private LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;

  @InjectMocks private OfficeService service;

  @Test
  void createLspOffice_lookupByGuid_returnsPopulatedResult() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(officeGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(savedOffice);

    OfficeCreationResult result =
        service.createLspOffice(
            providerGuid.toString(), new OfficeEntity(), new LspProviderOfficeLinkEntity());

    assertThat(result.providerGUID()).isEqualTo(providerGuid);
    assertThat(result.firmNumber()).isEqualTo("FRM001");
    assertThat(result.officeGUID()).isEqualTo(officeGuid);
    assertThat(result.accountNumber()).isNotBlank();
  }

  @Test
  void createLspOffice_lookupByFirmNumber_returnsPopulatedResult() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM999").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(officeGuid);

    when(providerRepository.findByFirmNumber("FRM999")).thenReturn(Optional.of(provider));
    when(officeRepository.save(any())).thenReturn(savedOffice);

    OfficeCreationResult result =
        service.createLspOffice("FRM999", new OfficeEntity(), new LspProviderOfficeLinkEntity());

    assertThat(result.firmNumber()).isEqualTo("FRM999");
    assertThat(result.officeGUID()).isEqualTo(officeGuid);
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
  void getLspOffices_byGuid_returnsPageFromRepository() {
    UUID providerGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    LspProviderOfficeLinkEntity link = lspLink();

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(lspProviderOfficeLinkRepository.findByProvider(eq(provider), any()))
        .thenReturn(new PageImpl<>(List.of(link), PageRequest.of(0, 20), 1));

    var result = service.getLspOffices(providerGuid.toString(), 0, 20);

    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent()).containsExactly(link);
  }

  @Test
  void getLspOffices_throwsWhenProviderNotFound() {
    when(providerRepository.findByFirmNumber("UNKNOWN")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getLspOffices("UNKNOWN", 0, 20))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("UNKNOWN");
  }

  // --- getLspOffice ---

  @Test
  void getLspOffice_byOfficeGuid_returnsEntity() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    LspProviderOfficeLinkEntity link = lspLink();

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(lspProviderOfficeLinkRepository.findByProviderAndOffice_Guid(provider, officeGuid))
        .thenReturn(Optional.of(link));

    assertThat(service.getLspOffice(providerGuid.toString(), officeGuid.toString())).isSameAs(link);
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

    assertThat(service.getLspOffice(providerGuid.toString(), "ABC123")).isSameAs(link);
  }

  @Test
  void getLspOffice_throwsWhenOfficeNotFound() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();
    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(lspProviderOfficeLinkRepository.findByProviderAndOffice_Guid(provider, officeGuid))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getLspOffice(providerGuid.toString(), officeGuid.toString()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(officeGuid.toString());
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
}
