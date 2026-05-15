package uk.gov.justice.laa.providerdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.providerdata.PostgresqlSpringBootTest;
import uk.gov.justice.laa.providerdata.entity.EnversRevisionEntity;
import uk.gov.justice.laa.providerdata.entity.LspProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;

class ProviderFirmEnversRepositoryTest extends PostgresqlSpringBootTest {

  @Autowired private ProviderRepository providerRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void save_then_update_provider_creates_two_envers_revisions() {
    ProviderEntity savedProvider =
        providerRepository.saveAndFlush(
            LspProviderEntity.builder().firmNumber("ENVERS-0001").name("Before update").build());

    savedProvider.setName("After update");
    providerRepository.saveAndFlush(savedProvider);
    entityManager.clear();

    try (EntityManager readEntityManager = entityManagerFactory.createEntityManager()) {
      var auditReader = AuditReaderFactory.get(readEntityManager);
      List<Number> revisions =
          auditReader.getRevisions(LspProviderEntity.class, savedProvider.getGuid());

      assertThat(revisions).hasSize(2);

      Number firstRevision = revisions.get(0);
      Number secondRevision = revisions.get(1);

      ProviderEntity firstSnapshot =
          auditReader.find(LspProviderEntity.class, savedProvider.getGuid(), firstRevision);
      ProviderEntity secondSnapshot =
          auditReader.find(LspProviderEntity.class, savedProvider.getGuid(), secondRevision);

      assertThat(firstSnapshot.getName()).isEqualTo("Before update");
      assertThat(secondSnapshot.getName()).isEqualTo("After update");

      EnversRevisionEntity revisionMetadata =
          auditReader.findRevision(EnversRevisionEntity.class, secondRevision);
      assertThat(revisionMetadata.getRevisionUser()).isEqualTo("SYSTEM");
    }
  }
}
