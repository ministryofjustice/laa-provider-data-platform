package uk.gov.justice.laa.providerdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.AdvocatePractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.BarristerPractitionerEntity;
import uk.gov.justice.laa.providerdata.entity.FirmType;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;

class ProviderFirmRepositoryTest extends PostgresqlSpringBootTest {

  @Autowired private ProviderRepository repository;
  @Autowired private EntityManager entityManager;

  @Test
  void save_and_findByFirmNumber_roundTrips() {
    ProviderEntity saved =
        repository.saveAndFlush(
            LspProviderEntity.builder().firmNumber("F12345").name("Test Firm").build());
    entityManager.clear();

    assertThat(saved.getGuid()).isNotNull();

    var reloaded = repository.findByFirmNumber("F12345");
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get()).isInstanceOf(LspProviderEntity.class);
    assertThat(reloaded.get().getName()).isEqualTo("Test Firm");
    assertThat(reloaded.get().getFirmType()).isEqualTo(FirmType.LEGAL_SERVICES_PROVIDER);
  }

  @Test
  void save_and_findByFirmNumber_roundTripsSubtypeFields() {
    repository.saveAndFlush(
        LspProviderEntity.builder()
            .firmNumber("LSP-FIELDS-1")
            .name("Subtype LSP")
            .constitutionalStatus("Charity")
            .notForProfitOrganisationFlag(Boolean.TRUE)
            .indemnityReceivedDate(LocalDate.of(2026, 3, 31))
            .companiesHouseNumber("CH123456")
            .build());

    repository.saveAndFlush(
        AdvocatePractitionerEntity.builder()
            .firmNumber("ADV-FIELDS-1")
            .name("Subtype Advocate Practitioner")
            .advocateLevel("Level 3")
            .solicitorRegulationAuthorityRollNumber("SRA12345")
            .build());

    repository.saveAndFlush(
        BarristerPractitionerEntity.builder()
            .firmNumber("BAR-FIELDS-1")
            .name("Subtype Barrister Practitioner")
            .barristerLevel("Level 4")
            .barCouncilRollNumber("BC67890")
            .build());
    entityManager.clear();

    ProviderEntity reloadedLsp = repository.findByFirmNumber("LSP-FIELDS-1").orElseThrow();
    ProviderEntity reloadedAdvocate = repository.findByFirmNumber("ADV-FIELDS-1").orElseThrow();
    ProviderEntity reloadedBarrister = repository.findByFirmNumber("BAR-FIELDS-1").orElseThrow();

    assertThat(reloadedLsp).isInstanceOf(LspProviderEntity.class);
    assertThat(((LspProviderEntity) reloadedLsp).getConstitutionalStatus()).isEqualTo("Charity");
    assertThat(((LspProviderEntity) reloadedLsp).getNotForProfitOrganisationFlag()).isTrue();
    assertThat(((LspProviderEntity) reloadedLsp).getIndemnityReceivedDate())
        .isEqualTo(LocalDate.of(2026, 3, 31));
    assertThat(((LspProviderEntity) reloadedLsp).getCompaniesHouseNumber()).isEqualTo("CH123456");

    assertThat(reloadedAdvocate).isInstanceOf(AdvocatePractitionerEntity.class);
    assertThat(reloadedAdvocate.getFirmType()).isEqualTo(FirmType.ADVOCATE);
    assertThat(((AdvocatePractitionerEntity) reloadedAdvocate).getAdvocateType())
        .isEqualTo("Advocate");
    assertThat(((AdvocatePractitionerEntity) reloadedAdvocate).getAdvocateLevel())
        .isEqualTo("Level 3");
    assertThat(
            ((AdvocatePractitionerEntity) reloadedAdvocate)
                .getSolicitorRegulationAuthorityRollNumber())
        .isEqualTo("SRA12345");

    assertThat(reloadedBarrister).isInstanceOf(BarristerPractitionerEntity.class);
    assertThat(reloadedBarrister.getFirmType()).isEqualTo(FirmType.ADVOCATE);
    assertThat(((BarristerPractitionerEntity) reloadedBarrister).getAdvocateType())
        .isEqualTo("Barrister");
    assertThat(((BarristerPractitionerEntity) reloadedBarrister).getBarristerLevel())
        .isEqualTo("Level 4");
    assertThat(((BarristerPractitionerEntity) reloadedBarrister).getBarCouncilRollNumber())
        .isEqualTo("BC67890");
  }
}
