package uk.gov.justice.laa.providerdata.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.repository.ProviderRepository;

/** Service responsible for provider firm read operations. */
@Service
@Transactional(readOnly = true)
public class ProviderService {

  private final ProviderRepository providerRepository;

  /**
   * Inject dependencies.
   *
   * @param providerRepository to find provider firms
   */
  public ProviderService(ProviderRepository providerRepository) {
    this.providerRepository = providerRepository;
  }

  /**
   * Returns a single provider firm by GUID or firm number.
   *
   * @param providerFirmGUIDorFirmNumber UUID string (primary key) or firm number (unique key)
   * @return the matching {@link ProviderEntity}
   * @throws ItemNotFoundException if no provider matches the given identifier
   */
  public ProviderEntity getProvider(String providerFirmGUIDorFirmNumber) {
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
}
