package uk.gov.justice.laa.providerdata.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.service.OfficeContractManagerAssignmentService;

@SpringBootTest
@ActiveProfiles("test")
class OfficeContractManagerLinkRepositoryTest {

  @Autowired private ContractManagerRepository contractManagerRepository;

  @Autowired private OfficeContractManagerLinkRepository linkRepository;

  @Autowired private EntityManager entityManager;

  @Test
  @Transactional
  void assign_thenRepositoryCanQueryByOfficeGuid() {
    // Arrange: contract manager
    ContractManagerEntity cm =
        ContractManagerEntity.builder()
            .contractManagerId("CM-001")
            .firstName("Pat")
            .lastName("Jones")
            .build();
    cm = contractManagerRepository.saveAndFlush(cm);

    // Arrange: office
    OfficeEntity office = new OfficeEntity();
    entityManager.persist(office);
    entityManager.flush();

    UUID officeGuid = office.getGuid();

    OfficeContractManagerAssignmentService service =
        new OfficeContractManagerAssignmentService(
            contractManagerRepository, linkRepository, entityManager);

    // Act
    service.assign(officeGuid, cm.getGuid());

    // Assert
    var links = linkRepository.findAllByOfficeGuid(officeGuid);
    assertThat(links).hasSize(1);
    assertThat(links.get(0).getContractManager().getContractManagerId()).isEqualTo("CM-001");
  }
}
