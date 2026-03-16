package uk.gov.justice.laa.providerdata.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.providerdata.mapper.ContractManagerMapper;
import uk.gov.justice.laa.providerdata.model.OfficeContractManagerV2;
import uk.gov.justice.laa.providerdata.repository.OfficeContractManagerLinkRepository;

/** Service for retrieving contract managers linked to provider offices. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractManagerService {

  private final OfficeContractManagerLinkRepository officeContractManagerLinkRepository;
  private final ContractManagerMapper mapper;

  /**
   * Retrieves all contract managers assigned to a given office and provider.
   *
   * @param officeGuidStr office GUID as string
   * @param providerGuidStr provider GUID as string (optional: can pass null to ignore provider)
   * @return list of OfficeContractManagerV2 DTOs
   */
  public List<OfficeContractManagerV2> getContractManagers(
      String officeGuidStr, String providerGuidStr) {
    // Both are mandatory
    UUID officeGuid = UUID.fromString(officeGuidStr);
    UUID providerGuid = UUID.fromString(providerGuidStr);

    log.info("Fetching contract managers for office={} provider={}", officeGuid, providerGuid);

    return officeContractManagerLinkRepository
        .findByOfficeGuidAndProviderGuid(officeGuid, providerGuid)
        .stream()
        .map(link -> mapper.toOfficeContractManagerV2(link.getContractManager()))
        .toList();
  }
}
