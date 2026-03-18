package uk.gov.justice.laa.providerdata.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.providerdata.mapper.ContractManagerMapper;
import uk.gov.justice.laa.providerdata.model.ContractManagerV2;
import uk.gov.justice.laa.providerdata.repository.ContractManagerRepository;

/**
 * Service for retrieving contract manager records for the {@code GET /provider-contract-managers}
 * endpoint.
 *
 * <p>This service queries the local PDP database using the Spring Data JPA layer, applying optional
 * filters:
 *
 * <ul>
 *   <li>{@code contractManagerIds}: exact-match filtering on contract manager IDs (multi)
 *   <li>{@code name}: case-insensitive fuzzy search across first name and last name
 * </ul>
 *
 * <p>Pagination is intentionally not implemented yet (MVP). The repository returns all matching
 * rows ordered by last name, first name, then contract manager ID.
 */
@Service
@RequiredArgsConstructor
public class ProviderContractManagersService {

  private final ContractManagerRepository contractManagerRepository;
  private final ContractManagerMapper contractManagerMapper;

  /**
   * Retrieves contract managers matching the optional filters.
   *
   * <p>If {@code contractManagerIds} is {@code null} or empty, no ID filtering is applied.
   *
   * <p>If {@code name} is {@code null} or blank, no name filtering is applied.
   *
   * @param contractManagerIds optional list of contract manager IDs to filter by (multi)
   * @param name optional fuzzy search term applied to both first and last names
   * @return list of {@link ContractManagerV2} DTOs matching the filters
   */
  public List<ContractManagerV2> getContractManagers(List<String> contractManagerIds, String name) {
    boolean idsEmpty = (contractManagerIds == null || contractManagerIds.isEmpty());

    List<String> ids = idsEmpty ? List.of() : contractManagerIds;

    return contractManagerRepository.search(ids, idsEmpty, name).stream()
        .map(contractManagerMapper::toContractManagerV2)
        .toList();
  }
}
