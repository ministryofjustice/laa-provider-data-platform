package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.OfficeMapper;
import uk.gov.justice.laa.providerdata.model.LSPOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.OfficeAddressV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {

  @Mock private ProviderRepository providerRepository;
  @Mock private OfficeRepository officeRepository;
  @Mock private ProviderOfficeLinkRepository providerOfficeLinkRepository;
  @Mock private OfficeMapper mapper;

  @InjectMocks private OfficeService service;

  @Test
  void createLspOffice_lookupByGuid_returnsPopulatedResponse() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM001").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(officeGuid);

    when(providerRepository.findById(providerGuid)).thenReturn(Optional.of(provider));
    when(mapper.toOfficeEntity(any())).thenReturn(new OfficeEntity());
    when(officeRepository.save(any())).thenReturn(savedOffice);
    when(mapper.toLinkEntity(any(), any(), any(), any())).thenReturn(null);

    var response = service.createLspOffice(providerGuid.toString(), minimalRequest());

    assertThat(response.getData().getProviderFirmGUID()).isEqualTo(providerGuid.toString());
    assertThat(response.getData().getProviderFirmNumber()).isEqualTo("FRM001");
    assertThat(response.getData().getOfficeGUID()).isEqualTo(officeGuid.toString());
    assertThat(response.getData().getOfficeCode()).isNotBlank();
  }

  @Test
  void createLspOffice_lookupByFirmNumber_returnsPopulatedResponse() {
    UUID providerGuid = UUID.randomUUID();
    UUID officeGuid = UUID.randomUUID();

    ProviderEntity provider = ProviderEntity.builder().firmNumber("FRM999").build();
    provider.setGuid(providerGuid);

    OfficeEntity savedOffice = new OfficeEntity();
    savedOffice.setGuid(officeGuid);

    when(providerRepository.findByFirmNumber("FRM999")).thenReturn(Optional.of(provider));
    when(mapper.toOfficeEntity(any())).thenReturn(new OfficeEntity());
    when(officeRepository.save(any())).thenReturn(savedOffice);
    when(mapper.toLinkEntity(any(), any(), any(), any())).thenReturn(null);

    var response = service.createLspOffice("FRM999", minimalRequest());

    assertThat(response.getData().getProviderFirmNumber()).isEqualTo("FRM999");
    assertThat(response.getData().getOfficeGUID()).isEqualTo(officeGuid.toString());
  }

  @Test
  void createLspOffice_throwsWhenGuidNotFound() {
    UUID unknownGuid = UUID.randomUUID();
    when(providerRepository.findById(unknownGuid)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createLspOffice(unknownGuid.toString(), minimalRequest()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining(unknownGuid.toString());
  }

  @Test
  void createLspOffice_throwsWhenFirmNumberNotFound() {
    when(providerRepository.findByFirmNumber("UNKNOWN")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createLspOffice("UNKNOWN", minimalRequest()))
        .isInstanceOf(ItemNotFoundException.class)
        .hasMessageContaining("UNKNOWN");
  }

  private static LSPOfficeCreateV2 minimalRequest() {
    return new LSPOfficeCreateV2(
        new PaymentDetailsCreateOrLinkV2().paymentMethod(PaymentDetailsPaymentMethodV2.EFT),
        new OfficeAddressV2().line1("1 Test Street").townOrCity("London").postcode("SW1A 1AA"),
        null);
  }
}
