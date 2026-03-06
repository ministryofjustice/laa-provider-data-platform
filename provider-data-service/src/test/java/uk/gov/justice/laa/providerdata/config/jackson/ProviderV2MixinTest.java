package uk.gov.justice.laa.providerdata.config.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import tools.jackson.databind.ObjectMapper;
import uk.gov.justice.laa.providerdata.model.LSPDetailsV2;
import uk.gov.justice.laa.providerdata.model.PractitionerDetailsV2;
import uk.gov.justice.laa.providerdata.model.ProviderFirmTypeV2;
import uk.gov.justice.laa.providerdata.model.ProviderV2;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class ProviderV2MixinTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  void serialisation_omitsNullVariantFields() throws Exception {
    ProviderV2 lsp =
        new ProviderV2()
            .guid("abc-123")
            .firmType(ProviderFirmTypeV2.LEGAL_SERVICES_PROVIDER)
            .name("Test LSP")
            .legalServicesProvider(new LSPDetailsV2());

    String json = objectMapper.writeValueAsString(lsp);

    assertThat(json).contains("\"legalServicesProvider\"");
    assertThat(json).doesNotContain("\"chambers\"");
    assertThat(json).doesNotContain("\"practitioner\"");
  }

  @Test
  void serialisation_omitsNullVariantFields_forAdvocate() throws Exception {
    ProviderV2 advocate =
        new ProviderV2()
            .guid("def-456")
            .firmType(ProviderFirmTypeV2.ADVOCATE)
            .name("J. Smith")
            .practitioner(new PractitionerDetailsV2());

    String json = objectMapper.writeValueAsString(advocate);

    assertThat(json).contains("\"practitioner\"");
    assertThat(json).doesNotContain("\"legalServicesProvider\"");
    assertThat(json).doesNotContain("\"chambers\"");
  }
}
