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
 * <p><strong>Behavior:</strong>
 *
 * <ul>
 *   <li>Ensures there is at most one assignment per office (MVP rule). Any existing assignment for
 *       the office is removed before creating a new link.
 *   <li>Looks up the {@code ContractManagerEntity} by its GUID and throws an {@link
 *       IllegalArgumentException} if it does not exist.
 *   <li>Obtains a lightweight reference to the {@code OfficeEntity} via {@link
 *       EntityManager#getReference(Class, Object)}, assuming the office GUID is the primary key.
 * </ul>
 *
 * <p><strong>Transactions:</strong> The {@link #assign(UUID, UUID)} method is transactional. It
 * will atomically remove any previous assignment for the office and persist the new link or roll
 * back on failure.
 */
@Service
public class OfficeContractManagerAssignmentService {

  private final ContractManagerRepository contractManagerRepository;
  private final OfficeContractManagerLinkRepository linkRepository;
  private final EntityManager entityManager;

  /**
   * Constructs a new {@code OfficeContractManagerAssignmentService}.
   *
   * @param contractManagerRepository repository for querying {@link ContractManagerEntity} records
   * @param linkRepository repository for managing {@link OfficeContractManagerLinkEntity} links
   * @param entityManager JPA {@link EntityManager} used to obtain references to entities
   */
  public OfficeContractManagerAssignmentService(
      ContractManagerRepository contractManagerRepository,
      OfficeContractManagerLinkRepository linkRepository,
      EntityManager entityManager) {
    this.contractManagerRepository = contractManagerRepository;
    this.linkRepository = linkRepository;
    this.entityManager = entityManager;
  }

  /**
   * Assigns a contract manager to an office.
   *
   * <p><strong>MVP Rule:</strong> Only one assignment per office is permitted. This method deletes
   * any existing {@link OfficeContractManagerLinkEntity} for the provided {@code officeGuid} before
   * creating the new link.
   *
   * <p><strong>Lookup semantics:</strong> The contract manager must exist; otherwise an {@link
   * IllegalArgumentException} is thrown. The office is referenced via {@link
   * EntityManager#getReference(Class, Object)} assuming the GUID is the primary key.
   *
   * @param officeGuid the GUID of the {@link OfficeEntity} receiving the assignment; assumed to be
   *     its primary key
   * @param contractManagerGuid the GUID of the {@link ContractManagerEntity} being assigned
   * @return an {@link AssignmentResult} containing the office GUID and the assigned contract
   *     manager's external/business ID
   * @throws IllegalArgumentException if no contract manager exists with the given {@code
   *     contractManagerGuid}
   */
  @Transactional
  public AssignmentResult assign(UUID officeGuid, UUID contractManagerGuid) {
    ContractManagerEntity manager =
        contractManagerRepository
            .findById(contractManagerGuid)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Unknown contractManagerGUID: " + contractManagerGuid));

    // Lightweight reference; assumes OfficeEntity primary key is the GUID.
    OfficeEntity officeRef = entityManager.getReference(OfficeEntity.class, officeGuid);

    // MVP: ensure only one assignment exists
    linkRepository.deleteByOfficeGuid(officeGuid);

    OfficeContractManagerLinkEntity link =
        OfficeContractManagerLinkEntity.builder()
            .office(officeRef)
            .contractManager(manager)
            .build();

    linkRepository.save(link);

    return new AssignmentResult(officeGuid, manager.getContractManagerId());
  }

  /**
   * Simple result DTO representing the outcome of an assignment operation.
   *
   * @param officeGuid the GUID of the office that received the assignment
   * @param contractManagerId the business/external identifier of the assigned contract manager
   */
  public record AssignmentResult(UUID officeGuid, String contractManagerId) {}
}
