package uk.gov.justice.laa.providerdata.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.repository.ContractManagerRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeContractManagerLinkRepository;

/**
 * Service responsible for assigning a {@link ContractManagerEntity} to an {@link OfficeEntity}.
 *
 * <ul>
 *   <li>Ensures there is at most one assignment per office (for now). Any existing assignment for
 *       the office is removed before creating a new link.
 *   <li>Looks up the {@code ContractManagerEntity} by its GUID and throws an {@link
 *       IllegalArgumentException} if it does not exist.
 *   <li>Obtains a lightweight reference to the {@code OfficeEntity} via {@link
 *       EntityManager#getReference(Class, Object)}, assuming the office GUID is the primary key.
 * </ul>
 *
 * <p>The {@link #assign(String, String, UUID)} method is transactional. It will atomically remove
 * any previous assignment for the office and persist the new link or roll back on failure.
 */
@Service
public class OfficeContractManagerAssignmentService {

  private final ContractManagerRepository contractManagerRepository;
  private final OfficeContractManagerLinkRepository linkRepository;
  private final ProviderService providerService;
  private final OfficeService officeService;

  /**
   * Constructs a new {@code OfficeContractManagerAssignmentService}.
   *
   * @param contractManagerRepository repository for querying {@link ContractManagerEntity} records
   * @param linkRepository repository for managing {@link OfficeContractManagerLinkEntity} links
   * @param providerService service for resolving provider identifiers
   * @param officeService service for resolving provider office identifiers
   */
  public OfficeContractManagerAssignmentService(
      ContractManagerRepository contractManagerRepository,
      OfficeContractManagerLinkRepository linkRepository,
      ProviderService providerService,
      OfficeService officeService) {
    this.contractManagerRepository = contractManagerRepository;
    this.linkRepository = linkRepository;
    this.providerService = providerService;
    this.officeService = officeService;
  }

  /**
   * Assigns a contract manager to an office.
   *
   * <p>Currently only one assignment per office is permitted. This method resolves the provider
   * using a provider GUID or firm number, then resolves the office using either the provider office
   * link GUID or the office account number. It deletes any existing {@link
   * OfficeContractManagerLinkEntity} for the resolved office before creating the new link.
   *
   * <p>The contract manager must exist; otherwise an {@link IllegalArgumentException} is thrown.
   * Provider and office resolution follow the same semantics as the other provider-office endpoints
   * in the application.
   *
   * @param providerGuidOrFirmNumber the provider GUID or firm number
   * @param officeGuidOrCode the provider office link GUID or office account number
   * @param contractManagerGuid the GUID of the {@link ContractManagerEntity} being assigned
   * @return an {@link AssignmentResult} containing the provider office link GUID and the assigned
   *     contract manager's external/business ID
   * @throws IllegalArgumentException if no contract manager exists with the given {@code
   *     contractManagerGuid}
   */
  @Transactional
  public AssignmentResult assign(
      String providerGuidOrFirmNumber, String officeGuidOrCode, UUID contractManagerGuid) {
    ContractManagerEntity manager =
        contractManagerRepository
            .findById(contractManagerGuid)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Unknown contractManagerGUID: " + contractManagerGuid));

    var provider = providerService.getProvider(providerGuidOrFirmNumber);
    var providerOfficeLink = officeService.getProviderOfficeLink(provider, officeGuidOrCode);
    UUID providerOfficeLinkGuid = providerOfficeLink.getGuid();

    // MVP: ensure only one assignment exists
    linkRepository.deleteByOfficeLink_Guid(providerOfficeLinkGuid);

    OfficeContractManagerLinkEntity link =
        OfficeContractManagerLinkEntity.builder()
            .officeLink(providerOfficeLink)
            .contractManager(manager)
            .build();

    linkRepository.save(link);

    return new AssignmentResult(providerOfficeLinkGuid, manager.getContractManagerId());
  }

  /**
   * Simple result DTO representing the outcome of an assignment operation.
   *
   * @param officeGuid the GUID of the provider office link that received the assignment
   * @param contractManagerId the business/external identifier of the assigned contract manager
   */
  public record AssignmentResult(UUID officeGuid, String contractManagerId) {}
}
