package uk.gov.justice.laa.providerdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;

class ProviderFirmRepositoryTest extends PostgresqlSpringBootTest {

  @Autowired private ProviderRepository repository;

  @Test
  void save_and_findByFirmNumber_roundTrips() {
    ProviderEntity saved =
        repository.save(LspProviderEntity.builder().firmNumber("F12345").name("Test Firm").build());

    assertThat(saved.getGuid()).isNotNull();

    var reloaded = repository.findByFirmNumber("F12345");
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get()).isInstanceOf(LspProviderEntity.class);
    assertThat(reloaded.get().getName()).isEqualTo("Test Firm");
    assertThat(reloaded.get().getFirmType()).isEqualTo(FirmType.LEGAL_SERVICES_PROVIDER);
  }

  @Test
  void save_and_findByFirmNumber_roundTripsSubtypeFields() {
    repository.save(
        LspProviderEntity.builder()
            .firmNumber("LSP-FIELDS-1")
            .name("Subtype LSP")
            .constitutionalStatus("Charity")
            .notForProfitOrganisationFlag(Boolean.TRUE)
            .indemnityReceivedDate(LocalDate.of(2026, 3, 31))
            .companiesHouseNumber("CH123456")
            .build());

    repository.save(
        AdvocateProviderEntity.builder()
            .firmNumber("ADV-FIELDS-1")
            .name("Subtype Practitioner")
            .advocateType("Barrister")
            .advocateLevel("Level 3")
            .solicitorRegulationAuthorityRollNumber("SRA12345")
            .barristerLevel("Level 4")
            .barCouncilRollNumber("BC67890")
            .build());

    ProviderEntity reloadedLsp = repository.findByFirmNumber("LSP-FIELDS-1").orElseThrow();
    ProviderEntity reloadedPractitioner = repository.findByFirmNumber("ADV-FIELDS-1").orElseThrow();

    assertThat(reloadedLsp).isInstanceOf(LspProviderEntity.class);
    assertThat(((LspProviderEntity) reloadedLsp).getConstitutionalStatus()).isEqualTo("Charity");
    assertThat(((LspProviderEntity) reloadedLsp).getNotForProfitOrganisationFlag()).isTrue();
    assertThat(((LspProviderEntity) reloadedLsp).getIndemnityReceivedDate())
        .isEqualTo(LocalDate.of(2026, 3, 31));
    assertThat(((LspProviderEntity) reloadedLsp).getCompaniesHouseNumber()).isEqualTo("CH123456");

    assertThat(reloadedPractitioner).isInstanceOf(AdvocateProviderEntity.class);
    assertThat(((AdvocateProviderEntity) reloadedPractitioner).getAdvocateType())
        .isEqualTo("Barrister");
    assertThat(((AdvocateProviderEntity) reloadedPractitioner).getAdvocateLevel())
        .isEqualTo("Level 3");
    assertThat(
            ((AdvocateProviderEntity) reloadedPractitioner)
                .getSolicitorRegulationAuthorityRollNumber())
        .isEqualTo("SRA12345");
    assertThat(((AdvocateProviderEntity) reloadedPractitioner).getBarristerLevel())
        .isEqualTo("Level 4");
    assertThat(((AdvocateProviderEntity) reloadedPractitioner).getBarCouncilRollNumber())
        .isEqualTo("BC67890");
  }
}
