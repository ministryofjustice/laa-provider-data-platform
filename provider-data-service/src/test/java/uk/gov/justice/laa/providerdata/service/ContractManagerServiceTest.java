package uk.gov.justice.laa.providerdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;
import uk.gov.justice.laa.providerdata.mapper.ContractManagerMapper;
import uk.gov.justice.laa.providerdata.model.OfficeContractManagerV2;
import uk.gov.justice.laa.providerdata.repository.OfficeContractManagerLinkRepository;

/**
 * Unit tests for {@link ContractManagerService}.
 *
 * <p>Verifies that contract managers linked to an office are retrieved from the repository and
 * correctly mapped to API DTO objects.
 */
@ExtendWith(MockitoExtension.class)
class ContractManagerServiceTest {

  @Mock private OfficeContractManagerLinkRepository linkRepository;

  @Mock private ContractManagerMapper mapper;

  @InjectMocks private ContractManagerService service;

  private UUID officeGuid;
  private UUID providerGuid;
  private ContractManagerEntity entity;
  private OfficeContractManagerLinkEntity link;
  private OfficeContractManagerV2 dto;

  @BeforeEach
  void setUp() {

    officeGuid = UUID.randomUUID();
    providerGuid = UUID.randomUUID();

    entity =
        ContractManagerEntity.builder()
            .contractManagerId("CM123")
            .firstName("John")
            .lastName("Smith")
            .build();

    link = OfficeContractManagerLinkEntity.builder().contractManager(entity).build();

    dto =
        OfficeContractManagerV2.builder()
            .contractManagerId("CM123")
            .firstName("John")
            .lastName("Smith")
            .build();
  }

  /**
   * Tests that contract managers linked to an office are successfully retrieved and mapped to DTO
   * objects.
   */
  @Test
  void shouldReturnContractManagersForOffice() {

    when(linkRepository.findByOffice_Guid(officeGuid)).thenReturn(List.of(link));

    when(mapper.toOfficeContractManagerV2(entity)).thenReturn(dto);

    List<OfficeContractManagerV2> result =
        service.getContractManagers(officeGuid.toString(), providerGuid.toString());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContractManagerId()).isEqualTo("CM123");
    assertThat(result.get(0).getFirstName()).isEqualTo("John");
    assertThat(result.get(0).getLastName()).isEqualTo("Smith");
  }

  /**
   * Tests that an empty list is returned when no contract managers are linked to the given office.
   */
  @Test
  void shouldReturnEmptyListWhenNoContractManagersExist() {

    when(linkRepository.findByOffice_Guid(officeGuid)).thenReturn(List.of());

    List<OfficeContractManagerV2> result =
        service.getContractManagers(officeGuid.toString(), providerGuid.toString());

    assertThat(result).isEmpty();
  }
}
