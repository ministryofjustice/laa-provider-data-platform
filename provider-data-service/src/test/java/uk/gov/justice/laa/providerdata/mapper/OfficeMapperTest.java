package uk.gov.justice.laa.providerdata.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.model.OfficeV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;

class OfficeMapperTest {

  // Uses the MapStruct-generated implementation to cover both generated and default-method logic.
  private final OfficeMapper mapper = new OfficeMapperImpl();

  @Test
  void uriToString_returnsNullForNullInput() {
    assertThat(mapper.uriToString(null)).isNull();
  }

  @Test
  void uriToString_returnsStringRepresentation() {
    URI uri = URI.create("https://example.com/firm");
    assertThat(mapper.uriToString(uri)).isEqualTo("https://example.com/firm");
  }

  @Test
  void paymentMethodValue_returnsNullForNullPayment() {
    assertThat(mapper.paymentMethodValue(null)).isNull();
  }

  @Test
  void paymentMethodValue_returnsNullWhenPaymentMethodNotSet() {
    assertThat(mapper.paymentMethodValue(new PaymentDetailsCreateOrLinkV2())).isNull();
  }

  @Test
  void paymentMethodValue_returnsValueStringFromEnum() {
    var payment =
        new PaymentDetailsCreateOrLinkV2().paymentMethod(PaymentDetailsPaymentMethodV2.EFT);
    assertThat(mapper.paymentMethodValue(payment)).isEqualTo("EFT");
  }

  @Test
  void stringToUri_returnsNullForNull() {
    assertThat(mapper.stringToUri(null)).isNull();
  }

  @Test
  void stringToUri_parsesValidUri() {
    assertThat(mapper.stringToUri("https://westgate.example"))
        .isEqualTo(URI.create("https://westgate.example"));
  }

  @Test
  void stringToUri_returnsNullForMalformedUri() {
    assertThat(mapper.stringToUri("not a valid uri ://")).isNull();
  }

  @Test
  void toLspOfficeV2_mapsAllFields() {
    UUID officeGuid = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();

    OfficeEntity office = new OfficeEntity();
    office.setGuid(officeGuid);
    office.setVersion(3L);
    office.setCreatedBy("user1");
    office.setCreatedTimestamp(now);
    office.setLastUpdatedBy("user2");
    office.setLastUpdatedTimestamp(now);
    office.setAddressLine1("1 High Street");
    office.setAddressLine2("Floor 2");
    office.setAddressLine3(null);
    office.setAddressLine4(null);
    office.setAddressTownOrCity("London");
    office.setAddressCounty("Greater London");
    office.setAddressPostCode("EC1A 1BB");
    office.setTelephoneNumber("0191 498 0001");
    office.setEmailAddress("info@westgate.example");
    office.setDxDetailsNumber("DX 12345");
    office.setDxDetailsCentre("London");

    LspProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();
    link.setOffice(office);
    link.setAccountNumber("ABC123");
    link.setFirmType("Legal Services Provider");
    link.setActiveDateTo(LocalDate.of(2030, 12, 31));
    link.setWebsite("https://westgate.example");
    link.setVatRegistrationNumber("GB123456789");
    link.setPaymentMethod("EFT");
    link.setPaymentHeldFlag(Boolean.TRUE);
    link.setPaymentHeldReason("Under review");
    link.setDebtRecoveryFlag(Boolean.FALSE);
    link.setFalseBalanceFlag(Boolean.FALSE);
    link.setIntervenedFlag(Boolean.TRUE);
    link.setIntervenedChangeDate(LocalDate.of(2025, 6, 1));

    OfficeV2 result = mapper.toLspOfficeV2(link);

    assertThat(result.getGuid()).isEqualTo(officeGuid.toString());
    assertThat(result.getVersion()).isEqualTo(BigDecimal.valueOf(3L));
    assertThat(result.getCreatedBy()).isEqualTo("user1");
    assertThat(result.getCreatedTimestamp()).isEqualTo(now);
    assertThat(result.getLastUpdatedBy()).isEqualTo("user2");
    assertThat(result.getLastUpdatedTimestamp()).isEqualTo(now);
    assertThat(result.getFirmType()).isEqualTo(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER);
    assertThat(result.getAccountNumber()).isEqualTo("ABC123");
    assertThat(result.getActiveDateTo()).isEqualTo(LocalDate.of(2030, 12, 31));
    assertThat(result.getDebtRecoveryFlag()).isFalse();
    assertThat(result.getFalseBalanceFlag()).isFalse();
    assertThat(result.getWebsite()).isEqualTo(URI.create("https://westgate.example"));

    assertThat(result.getAddress().getLine1()).isEqualTo("1 High Street");
    assertThat(result.getAddress().getLine2()).isEqualTo("Floor 2");
    assertThat(result.getAddress().getTownOrCity()).isEqualTo("London");
    assertThat(result.getAddress().getCounty()).isEqualTo("Greater London");
    assertThat(result.getAddress().getPostcode()).isEqualTo("EC1A 1BB");

    assertThat(result.getTelephoneNumber()).isEqualTo("0191 498 0001");
    assertThat(result.getEmailAddress()).isEqualTo("info@westgate.example");

    assertThat(result.getDxDetails().getDxNumber()).isEqualTo("DX 12345");
    assertThat(result.getDxDetails().getDxCentre()).isEqualTo("London");

    assertThat(result.getVatRegistration().getVatNumber()).isEqualTo("GB123456789");

    assertThat(result.getPayment().getPaymentMethod()).isEqualTo(PaymentDetailsPaymentMethodV2.EFT);
    assertThat(result.getPayment().getPaymentHeldFlag()).isTrue();
    assertThat(result.getPayment().getPaymentHeldReason()).isEqualTo("Under review");

    assertThat(result.getIntervened().getIntervenedFlag()).isTrue();
    assertThat(result.getIntervened().getIntervenedChangeDate())
        .isEqualTo(LocalDate.of(2025, 6, 1));
  }

  @Test
  void toLspOfficeV2_omitsDxDetails_whenBothNull() {
    OfficeEntity office = officeWithGuid();
    LspProviderOfficeLinkEntity link = linkWith(office);

    assertThat(mapper.toLspOfficeV2(link).getDxDetails()).isNull();
  }

  @Test
  void toLspOfficeV2_omitsVatRegistration_whenNull() {
    OfficeEntity office = officeWithGuid();
    LspProviderOfficeLinkEntity link = linkWith(office);

    assertThat(mapper.toLspOfficeV2(link).getVatRegistration()).isNull();
  }

  @Test
  void toLspOfficeV2_handlesNullPaymentMethod() {
    OfficeEntity office = officeWithGuid();
    LspProviderOfficeLinkEntity link = linkWith(office);
    link.setPaymentMethod(null);

    assertThat(mapper.toLspOfficeV2(link).getPayment().getPaymentMethod()).isNull();
  }

  @Test
  void toLspOfficeV2_handlesNullVersion() {
    OfficeEntity office = officeWithGuid();
    office.setVersion(null);
    LspProviderOfficeLinkEntity link = linkWith(office);

    assertThat(mapper.toLspOfficeV2(link).getVersion()).isNull();
  }

  private static OfficeEntity officeWithGuid() {
    OfficeEntity office = new OfficeEntity();
    office.setGuid(UUID.randomUUID());
    office.setVersion(1L);
    office.setAddressLine1("1 Test St");
    office.setAddressTownOrCity("London");
    office.setAddressPostCode("SW1A 1AA");
    return office;
  }

  private static LspProviderOfficeLinkEntity linkWith(OfficeEntity office) {
    LspProviderOfficeLinkEntity link = new LspProviderOfficeLinkEntity();
    link.setOffice(office);
    link.setAccountNumber("TST001");
    link.setFirmType("Legal Services Provider");
    return link;
  }
}
