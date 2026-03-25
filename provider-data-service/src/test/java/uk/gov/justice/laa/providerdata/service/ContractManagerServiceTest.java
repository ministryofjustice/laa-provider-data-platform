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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;
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

  @Mock private ProviderService providerService;

  @Mock private OfficeService officeService;

  @InjectMocks private ContractManagerService service;

  private UUID officeGuid;
  private UUID providerGuid;
  private ContractManagerEntity entity;
  private OfficeContractManagerLinkEntity link;
  private OfficeContractManagerV2 dto;
  private ProviderEntity provider;
  private ProviderOfficeLinkEntity providerOfficeLink;

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

    provider = ProviderEntity.builder().guid(providerGuid).firmNumber("FRM001").build();
    OfficeEntity office = OfficeEntity.builder().guid(officeGuid).build();
    providerOfficeLink =
        ProviderOfficeLinkEntity.builder()
            .guid(UUID.randomUUID())
            .provider(provider)
            .office(office)
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
    String providerGuidValue = providerGuid.toString();
    String providerOfficeLinkGuidValue = providerOfficeLink.getGuid().toString();

    when(providerService.getProvider(providerGuidValue)).thenReturn(provider);
    when(officeService.getOfficeLink(provider, providerOfficeLinkGuidValue))
        .thenReturn(providerOfficeLink);
    var pageable = PageRequest.of(0, 100);

    when(linkRepository.findByOffice_Guid(officeGuid, pageable))
        .thenReturn(new PageImpl<>(List.of(link), pageable, 1));

    when(mapper.toOfficeContractManagerV2(entity)).thenReturn(dto);

    var result =
        service.getContractManagers(providerGuidValue, providerOfficeLinkGuidValue, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getContractManagerId()).isEqualTo("CM123");
    assertThat(result.getContent().getFirst().getFirstName()).isEqualTo("John");
    assertThat(result.getContent().getFirst().getLastName()).isEqualTo("Smith");
  }

  /**
   * Tests that an empty list is returned when no contract managers are linked to the given office.
   */
  @Test
  void shouldReturnEmptyListWhenNoContractManagersExist() {
    when(providerService.getProvider("FRM001")).thenReturn(provider);
    when(officeService.getOfficeLink(provider, "ACC001")).thenReturn(providerOfficeLink);
    var pageable = PageRequest.of(0, 100);

    when(linkRepository.findByOffice_Guid(officeGuid, pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    var result = service.getContractManagers("FRM001", "ACC001", pageable);

    assertThat(result.getContent()).isEmpty();
  }
}
