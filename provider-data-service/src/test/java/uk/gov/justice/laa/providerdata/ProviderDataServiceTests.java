package uk.gov.justice.laa.providerdata;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // ✅ Forces H2 config
class ProviderDataServiceTests {

  @Test
  void contextLoads() {
    // Verifies Spring context starts successfully
  }
}
