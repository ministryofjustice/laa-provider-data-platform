package uk.gov.justice.laa.providerdata.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.providerdata.entity.FirmType;

@DisplayName("MetricsConfiguration")
class MetricsConfigurationTest {

  private MetricsConfiguration configuration;
  private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    configuration = new MetricsConfiguration();
    meterRegistry = new SimpleMeterRegistry();
  }

  @Test
  @DisplayName("should create LSP firm creation counter with correct tags")
  void testLspFirmCreationCounterConfiguration() {
    Counter counter = configuration.lspFirmCreationCounter(meterRegistry);

    assertThat(counter).isNotNull();
    assertThat(counter.getId().getTag("firmware_type")).isEqualTo(FirmType.LEGAL_SERVICES_PROVIDER);
    assertThat(counter.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create Chambers firm creation counter with correct tags")
  void testChambersFirmCreationCounterConfiguration() {
    Counter counter = configuration.chambersFirmCreationCounter(meterRegistry);

    assertThat(counter).isNotNull();
    assertThat(counter.getId().getTag("firmware_type")).isEqualTo(FirmType.CHAMBERS);
    assertThat(counter.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create Practitioner firm creation counter with correct tags")
  void testPractitionerFirmCreationCounterConfiguration() {
    Counter counter = configuration.practitionerFirmCreationCounter(meterRegistry);

    assertThat(counter).isNotNull();
    assertThat(counter.getId().getTag("firmware_type")).isEqualTo(FirmType.ADVOCATE);
    assertThat(counter.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create LSP firm creation timer with correct tags")
  void testLspFirmCreationTimerConfiguration() {
    Timer timer = configuration.lspFirmCreationTimer(meterRegistry);

    assertThat(timer).isNotNull();
    assertThat(timer.getId().getTag("firmware_type")).isEqualTo(FirmType.LEGAL_SERVICES_PROVIDER);
    assertThat(timer.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create Chambers firm creation timer with correct tags")
  void testChambersFirmCreationTimerConfiguration() {
    Timer timer = configuration.chambersFirmCreationTimer(meterRegistry);

    assertThat(timer).isNotNull();
    assertThat(timer.getId().getTag("firmware_type")).isEqualTo(FirmType.CHAMBERS);
    assertThat(timer.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create Practitioner firm creation timer with correct tags")
  void testPractitionerFirmCreationTimerConfiguration() {
    Timer timer = configuration.practitionerFirmCreationTimer(meterRegistry);

    assertThat(timer).isNotNull();
    assertThat(timer.getId().getTag("firmware_type")).isEqualTo(FirmType.ADVOCATE);
    assertThat(timer.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create office creation counter with correct tags")
  void testOfficeCreationCounterConfiguration() {
    Counter counter = configuration.officeCreationCounter(meterRegistry);

    assertThat(counter).isNotNull();
    assertThat(counter.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create office creation timer with correct tags")
  void testOfficeCreationTimerConfiguration() {
    Timer timer = configuration.officeCreationTimer(meterRegistry);

    assertThat(timer).isNotNull();
    assertThat(timer.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create bank account creation counter with correct tags")
  void testBankAccountCreationCounterConfiguration() {
    Counter counter = configuration.bankAccountCreationCounter(meterRegistry);

    assertThat(counter).isNotNull();
    assertThat(counter.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create bank account creation timer with correct tags")
  void testBankAccountCreationTimerConfiguration() {
    Timer timer = configuration.bankAccountCreationTimer(meterRegistry);

    assertThat(timer).isNotNull();
    assertThat(timer.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create bank account link counter with correct tags")
  void testBankAccountLinkCounterConfiguration() {
    Counter counter = configuration.bankAccountLinkCounter(meterRegistry);

    assertThat(counter).isNotNull();
    assertThat(counter.getId().getTag("application")).isEqualTo("provider-data-platform");
  }

  @Test
  @DisplayName("should create bank account link timer with correct tags")
  void testBankAccountLinkTimerConfiguration() {
    Timer timer = configuration.bankAccountLinkTimer(meterRegistry);

    assertThat(timer).isNotNull();
    assertThat(timer.getId().getTag("application")).isEqualTo("provider-data-platform");
  }
}
