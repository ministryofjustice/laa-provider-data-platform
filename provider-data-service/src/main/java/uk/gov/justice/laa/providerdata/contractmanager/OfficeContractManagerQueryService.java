package uk.gov.justice.laa.providerdata.contractmanager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.contractmanager.mapper.ContractManagerMapper;
import uk.gov.justice.laa.providerdata.contractmanager.repository.OfficeContractManagerLinkRepository;
import uk.gov.justice.laa.providerdata.model.OfficeContractManagerV2;
import uk.gov.justice.laa.providerdata.office.OfficeQueryService;
import uk.gov.justice.laa.providerdata.provider.ProviderQueryService;

/** Service for retrieving contract managers linked to provider offices. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficeContractManagerQueryService {

  private final OfficeContractManagerLinkRepository officeContractManagerLinkRepository;
  private final ContractManagerMapper mapper;
  private final ProviderQueryService providerService;
  private final OfficeQueryService officeService;

  /**
   * Retrieves all contract managers assigned to a given office and provider.
   *
   * <p>The provider identifier may be either a provider GUID or a firm number. The office
   * identifier may be either a {@code ProviderOfficeLinkEntity.guid} value or a provider office
   * account number ({@code ProviderOfficeLinkEntity.accountNumber}).
   *
   * @param providerGuidOrFirmNumber provider GUID or firm number
   * @param officeGuidOrCode provider office link GUID or office account number
   * @param pageable page request parameters
   * @return paged OfficeContractManagerV2 DTOs
   */
  public Page<OfficeContractManagerV2> getContractManagers(
      String providerGuidOrFirmNumber, String officeGuidOrCode, Pageable pageable) {
    var provider = providerService.getProvider(providerGuidOrFirmNumber);
    var providerOfficeLink = officeService.getProviderOfficeLink(provider, officeGuidOrCode);

    log.info(
        "Fetching contract managers for provider={} officeLink={}",
        provider.getGuid(),
        providerOfficeLink.getGuid());

    return officeContractManagerLinkRepository
        .findByOfficeLink_Guid(providerOfficeLink.getGuid(), pageable)
        .map(link -> mapper.toOfficeContractManagerV2(link.getContractManager()));
  }
}
