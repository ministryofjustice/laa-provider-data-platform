package uk.gov.justice.laa.providerdata.liaisonmanager;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.liaisonmanager.repository.OfficeLiaisonManagerLinkRepository;
import uk.gov.justice.laa.providerdata.office.OfficeQueryService;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;

/** Service responsible for liaison manager read operations. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OfficeLiaisonManagerQueryService {

  private final OfficeQueryService officeQueryService;
  private final OfficeLiaisonManagerLinkRepository officeLiaisonManagerLinkRepository;

  /**
   * Returns a paginated page of liaison managers linked to a provider office.
   *
   * @param providerFirmGuidOrNumber GUID or firm number identifying the parent provider.
   * @param officeGuidOrCode GUID or account number identifying the office.
   * @param pageable the page being requested.
   * @return page of {@link OfficeLiaisonManagerLinkEntity} for the office, ordered by active date
   *     descending.
   */
  public Page<OfficeLiaisonManagerLinkEntity> getOfficeLiaisonManagers(
      String providerFirmGuidOrNumber, String officeGuidOrCode, Pageable pageable) {
    ProviderOfficeLinkEntity providerOfficeLink =
        officeQueryService.getProviderOfficeLink(providerFirmGuidOrNumber, officeGuidOrCode);
    return officeLiaisonManagerLinkRepository.findByOfficeLink_GuidOrderByActiveDateFromDesc(
        providerOfficeLink.getGuid(), pageable);
  }
}
