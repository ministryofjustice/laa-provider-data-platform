package uk.gov.justice.laa.providerdata.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.exception.ItemNotFoundException;
import uk.gov.justice.laa.providerdata.mapper.LiaisonManagerMapper;
import uk.gov.justice.laa.providerdata.model.GetLiaisonManager200Response;
import uk.gov.justice.laa.providerdata.model.ProviderLiaisonManagerPatchV2;
import uk.gov.justice.laa.providerdata.repository.LiaisonManagerRepository;

/** Service for managing Liaison Managers. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProviderLiaisonManagerService {

  private final LiaisonManagerRepository repository;
  private final LiaisonManagerMapper mapper;

  /**
   * Gets a liaison manager.
   *
   * @param guid ID
   * @return The data
   */
  public GetLiaisonManager200Response getLiaisonManager(UUID guid) {
    var entity =
        repository
            .findById(guid)
            .orElseThrow(() -> new ItemNotFoundException("Liaison Manager not found"));
    return new GetLiaisonManager200Response().data(mapper.toProviderLiaisonManagerV2(entity));
  }

  /**
   * Updates a liaison manager.
   *
   * @param guid ID
   * @param patch Patch data
   * @return The updated data
   */
  @Transactional
  public GetLiaisonManager200Response updateLiaisonManager(
      UUID guid, ProviderLiaisonManagerPatchV2 patch) {
    var entity =
        repository
            .findById(guid)
            .orElseThrow(() -> new ItemNotFoundException("Liaison Manager not found"));

    if (patch.getTelephoneNumber() != null) {
      entity.setTelephoneNumber(patch.getTelephoneNumber());
    }
    if (patch.getEmailAddress() != null) {
      entity.setEmailAddress(patch.getEmailAddress());
    }

    entity = repository.save(entity);
    return new GetLiaisonManager200Response().data(mapper.toProviderLiaisonManagerV2(entity));
  }
}
