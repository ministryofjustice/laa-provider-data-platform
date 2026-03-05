package uk.gov.justice.laa.providerdata.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.model.LSPOfficeCreateV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsCreateOrLinkV2;
import uk.gov.justice.laa.providerdata.model.PaymentDetailsPaymentMethodV2;

class OfficeMapperTest {

  // Test the default methods via a stub implementation; MapStruct wiring is verified at build time.
  private final OfficeMapper mapper =
      new OfficeMapper() {
        @Override
        public OfficeEntity toOfficeEntity(LSPOfficeCreateV2 request) {
          return null;
        }

        @Override
        public LspProviderOfficeLinkEntity toLinkEntity(
            LSPOfficeCreateV2 request,
            ProviderEntity provider,
            OfficeEntity office,
            String accountNumber) {
          return null;
        }
      };

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
}
