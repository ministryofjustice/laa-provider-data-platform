package uk.gov.justice.laa.providerdata.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.providerdata.entity.ContractManagerEntity;
import uk.gov.justice.laa.providerdata.repository.ContractManagerRepository;
import uk.gov.justice.laa.providerdata.repository.ContractManagerSpecifications;

/**
 * Service for retrieving contract manager records for the {@code GET /provider-contract-managers}
 * endpoint.
 *
 * <p>This service queries the local PDP database using Spring Data JPA Specifications and applies
 * optional filters:
 *
 * <ul>
 *   <li>{@code contractManagerIds}: exact-match filtering on contract manager IDs (multi)
 *   <li>{@code name}: case-insensitive fuzzy search across first name and last name (including
 *       "first last" and "last first")
 * </ul>
 *
 * <p>Pagination is applied using a {@link Pageable} (resolved by the controller via {@code
 * PageParamValidator.resolve(page, pageSize)}).
 */
@Service
@RequiredArgsConstructor
public class ProviderContractManagersService {

  private final ContractManagerRepository contractManagerRepository;

  /**
   * Retrieves contract managers matching the optional filters, returning a paged result.
   *
   * <p>If {@code contractManagerIds} is {@code null} or empty, no ID filtering is applied.
   *
   * <p>If {@code name} is {@code null} or blank, no name filtering is applied.
   *
   * @param contractManagerIds optional list of contract manager IDs to filter by (multi)
   * @param name optional fuzzy search term applied to both first and last names
   * @param pageable paging parameters (page number + page size)
   * @return a {@link Page} of {@link ContractManagerEntity} matching the filters
   */
  public Page<ContractManagerEntity> getContractManagers(
      List<String> contractManagerIds, String name, Pageable pageable) {

    // Normalize to avoid weird DB function calls on parameters (and to keep filtering consistent).
    String normalizedName = (name == null || name.isBlank()) ? null : name;

    var spec = ContractManagerSpecifications.filterBy(contractManagerIds, normalizedName);

    return contractManagerRepository.findAll(spec, pageable);
  }
}
