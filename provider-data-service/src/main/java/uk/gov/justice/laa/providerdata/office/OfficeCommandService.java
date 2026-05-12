package uk.gov.justice.laa.providerdata.office;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.office.repository.AdvocateProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.office.repository.ChamberProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.office.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.office.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.office.repository.ProviderOfficeLinkRepository;

/** Service responsible for office entity persistence operations. */
@Service
@Transactional
@RequiredArgsConstructor
public class OfficeCommandService {

  private final OfficeRepository officeRepository;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;
  private final ChamberProviderOfficeLinkRepository chamberProviderOfficeLinkRepository;
  private final ProviderOfficeLinkRepository providerOfficeLinkRepository;
  private final AdvocateProviderOfficeLinkRepository advocateProviderOfficeLinkRepository;

  /** Persists a new or updated office entity. */
  public OfficeEntity save(OfficeEntity office) {
    return officeRepository.save(office);
  }

  /** Persists a new or updated LSP provider-office link. */
  public LspProviderOfficeLinkEntity saveLspOfficeLink(LspProviderOfficeLinkEntity link) {
    return lspProviderOfficeLinkRepository.save(link);
  }

  /** Persists a new or updated provider-office link (generic, any firm type). */
  public ProviderOfficeLinkEntity saveProviderOfficeLink(ProviderOfficeLinkEntity link) {
    return providerOfficeLinkRepository.save(link);
  }

  /** Persists a new or updated Advocate provider-office link. */
  public AdvocateProviderOfficeLinkEntity saveAdvocateOfficeLink(
      AdvocateProviderOfficeLinkEntity link) {
    return advocateProviderOfficeLinkRepository.save(link);
  }

  /** Persists a new or updated Chambers provider-office link. */
  public ChamberProviderOfficeLinkEntity saveChamberOfficeLink(
      ChamberProviderOfficeLinkEntity link) {
    return chamberProviderOfficeLinkRepository.save(link);
  }

  /** Deletes an Advocate provider-office link. */
  public void deleteAdvocateOfficeLink(AdvocateProviderOfficeLinkEntity link) {
    advocateProviderOfficeLinkRepository.delete(link);
  }
}
