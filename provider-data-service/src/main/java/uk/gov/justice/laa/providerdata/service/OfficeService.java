package uk.gov.justice.laa.providerdata.service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.repository.LspProviderOfficeLinkRepository;
import uk.gov.justice.laa.providerdata.repository.OfficeRepository;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/** Service responsible for provider firm office operations. */
@Service
@Transactional
public class OfficeService {

  private final ProviderRepository providerRepository;
  private final OfficeRepository officeRepository;
  private final LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository;

  /**
   * Inject dependencies.
   *
   * @param providerRepository to find firms.
   * @param officeRepository to save offices.
   * @param lspProviderOfficeLinkRepository to save and query LSP office links.
   */
  public OfficeService(
      ProviderRepository providerRepository,
      OfficeRepository officeRepository,
      LspProviderOfficeLinkRepository lspProviderOfficeLinkRepository) {
    this.providerRepository = providerRepository;
    this.officeRepository = officeRepository;
    this.lspProviderOfficeLinkRepository = lspProviderOfficeLinkRepository;
  }

  /**
   * Creates a new office for an LSP provider firm and links it to the provider.
   *
   * <p>The caller is responsible for mapping the API request into {@code officeTemplate} and {@code
   * linkTemplate} before calling this method. The service sets {@code provider}, {@code office},
   * and {@code accountNumber} on the link before persisting.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param officeTemplate unpersisted office entity with address and contact fields populated
   * @param linkTemplate unpersisted link entity with payment and VAT fields populated
   * @return identifiers for the created provider, office, and link
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public OfficeCreationResult createLspOffice(
      String providerFirmGUIDorFirmNumber,
      OfficeEntity officeTemplate,
      LspProviderOfficeLinkEntity linkTemplate) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);

    OfficeEntity savedOffice = officeRepository.save(officeTemplate);

    String accountNumber = generateAccountNumber();
    linkTemplate.setProvider(provider);
    linkTemplate.setOffice(savedOffice);
    linkTemplate.setAccountNumber(accountNumber);
    lspProviderOfficeLinkRepository.save(linkTemplate);

    return new OfficeCreationResult(
        provider.getGuid(), provider.getFirmNumber(), savedOffice.getGuid(), accountNumber);
  }

  /**
   * Returns a paginated page of LSP offices for the given provider.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param page zero-based page index
   * @param pageSize number of items per page
   * @return page of {@link LspProviderOfficeLinkEntity} for the provider
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  @Transactional(readOnly = true)
  public Page<LspProviderOfficeLinkEntity> getLspOffices(
      String providerFirmGUIDorFirmNumber, int page, int pageSize) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    return lspProviderOfficeLinkRepository.findByProvider(provider, PageRequest.of(page, pageSize));
  }

  /**
   * Returns a single LSP office by GUID or account number.
   *
   * <p>The {@code officeGUIDorCode} parameter is first tried as a UUID; if that fails it is treated
   * as an account number.
   *
   * @param providerFirmGUIDorFirmNumber GUID or firm number identifying the parent provider
   * @param officeGUIDorCode office GUID or account number
   * @return the matching {@link LspProviderOfficeLinkEntity}
   * @throws ItemNotFoundException if no matching office is found
   */
  @Transactional(readOnly = true)
  public LspProviderOfficeLinkEntity getLspOffice(
      String providerFirmGUIDorFirmNumber, String officeGUIDorCode) {

    ProviderEntity provider = findProvider(providerFirmGUIDorFirmNumber);
    return findLink(provider, officeGUIDorCode)
        .orElseThrow(() -> new ItemNotFoundException("Office not found: " + officeGUIDorCode));
  }

  private Optional<LspProviderOfficeLinkEntity> findLink(
      ProviderEntity provider, String officeGUIDorCode) {
    try {
      UUID guid = UUID.fromString(officeGUIDorCode);
      return lspProviderOfficeLinkRepository.findByProviderAndOffice_Guid(provider, guid);
    } catch (IllegalArgumentException e) {
      return lspProviderOfficeLinkRepository.findByProviderAndAccountNumber(
          provider, officeGUIDorCode);
    }
  }

  private ProviderEntity findProvider(String providerFirmGUIDorFirmNumber) {
    try {
      UUID guid = UUID.fromString(providerFirmGUIDorFirmNumber);
      return providerRepository
          .findById(guid)
          .orElseThrow(
              () ->
                  new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
    } catch (IllegalArgumentException e) {
      return providerRepository
          .findByFirmNumber(providerFirmGUIDorFirmNumber)
          .orElseThrow(
              () ->
                  new ItemNotFoundException("Provider not found: " + providerFirmGUIDorFirmNumber));
    }
  }

  private static String generateAccountNumber() {
    return UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.UK);
  }
}
