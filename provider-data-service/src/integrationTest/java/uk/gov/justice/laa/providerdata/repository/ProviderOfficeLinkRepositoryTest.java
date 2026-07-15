package uk.gov.justice.laa.providerdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;

@Transactional
class ProviderOfficeLinkRepositoryTest extends PostgresqlSpringBootTest {

  @Autowired private ProviderRepository providerRepository;
  @Autowired private OfficeRepository officeRepository;
  @Autowired private ProviderOfficeLinkRepository repository;
  @Autowired private EntityManager entityManager;

  private ProviderEntity provider;
  private OfficeEntity office;
  private LspProviderOfficeLinkEntity link;

  @BeforeEach
  void setUp() {
    provider =
        providerRepository.save(
            LspProviderEntity.builder().firmNumber("FRM-LINK-TEST").name("Link Test Firm").build());

    office =
        officeRepository.save(
            OfficeEntity.builder()
                .addressLine1("1 Link Street")
                .addressTownOrCity("London")
                .addressPostCode("EC1A 1BB")
                .build());

    link = new LspProviderOfficeLinkEntity();
    link.setProvider(provider);
    link.setOffice(office);
    link.setAccountNumber("LNK001");
    link.setHeadOfficeFlag(true);
    link = (LspProviderOfficeLinkEntity) repository.save(link);
  }

  @Test
  void findByProvider_returnsSavedLink() {
    Page<ProviderOfficeLinkEntity> page =
        repository.findByProvider(provider, PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent().getFirst().getAccountNumber()).isEqualTo("LNK001");
  }

  @Test
  void findByProviderAndGuid_returnsLink() {
    Optional<ProviderOfficeLinkEntity> result =
        repository.findByProviderAndGuid(provider, link.getGuid());

    assertThat(result).isPresent();
    assertThat(result.get().getAccountNumber()).isEqualTo("LNK001");
  }

  @Test
  void findByProviderAndAccountNumber_returnsLink() {
    Optional<ProviderOfficeLinkEntity> result =
        repository.findByProviderAndAccountNumber(provider, "LNK001");

    assertThat(result).isPresent();
    assertThat(result.get().getOffice().getGuid()).isEqualTo(office.getGuid());
  }

  @Test
  void findByProvider_returnsEmpty_forDifferentProvider() {
    ProviderEntity other =
        providerRepository.save(
            LspProviderEntity.builder().firmNumber("FRM-OTHER").name("Other Firm").build());

    Page<ProviderOfficeLinkEntity> page = repository.findByProvider(other, PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isZero();
  }

  /**
   * Regression test for a shadowing bug: {@code AdvocateProviderOfficeLinkEntity} used to
   * re-declare {@code intervenedFlag}, {@code intervenedChangeDate}, {@code paymentHeldFlag},
   * {@code paymentHeldReason}, and {@code debtRecoveryFlag}, which are already declared on the
   * parent {@link ProviderOfficeLinkEntity}. That shadowing caused Hibernate to persist the
   * parent's copy of each column while the generated getters always read the child's null copy.
   * This test flushes and clears the persistence context to force a genuine database round-trip,
   * which the shadowing bug would have caused to fail.
   */
  @Test
  void advocateOfficeLink_roundTripsShadowedFieldsThroughDatabase() {
    ProviderEntity advocateProvider =
        providerRepository.save(
            LspProviderEntity.builder().firmNumber("FRM-ADV-TEST").name("Advocate Test").build());

    OfficeEntity advocateOffice =
        officeRepository.save(
            OfficeEntity.builder()
                .addressLine1("1 Advocate Street")
                .addressTownOrCity("London")
                .addressPostCode("EC1A 1BB")
                .build());

    AdvocateProviderOfficeLinkEntity advocateLink = new AdvocateProviderOfficeLinkEntity();
    advocateLink.setProvider(advocateProvider);
    advocateLink.setOffice(advocateOffice);
    advocateLink.setAccountNumber("ADV-LNK-001");
    advocateLink.setHeadOfficeFlag(false);
    advocateLink.setIntervenedFlag(Boolean.TRUE);
    advocateLink.setIntervenedChangeDate(LocalDate.of(2025, 6, 1));
    advocateLink.setPaymentHeldFlag(Boolean.TRUE);
    advocateLink.setPaymentHeldReason("Under review");
    advocateLink.setDebtRecoveryFlag(Boolean.TRUE);
    UUID savedGuid = repository.save(advocateLink).getGuid();

    entityManager.flush();
    entityManager.clear();

    ProviderOfficeLinkEntity reloaded = repository.findById(savedGuid).orElseThrow();

    assertThat(reloaded.getIntervenedFlag()).isTrue();
    assertThat(reloaded.getIntervenedChangeDate()).isEqualTo(LocalDate.of(2025, 6, 1));
    assertThat(reloaded.getPaymentHeldFlag()).isTrue();
    assertThat(reloaded.getPaymentHeldReason()).isEqualTo("Under review");
    assertThat(reloaded.getDebtRecoveryFlag()).isTrue();
  }
}
