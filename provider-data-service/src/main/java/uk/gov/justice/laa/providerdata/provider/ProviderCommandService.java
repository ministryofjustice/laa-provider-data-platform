package uk.gov.justice.laa.providerdata.provider;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderParentLinkRepository;
import uk.gov.justice.laa.providerdata.provider.repository.ProviderRepository;

/** Service responsible for provider entity persistence operations. */
@Service("providerModuleCommandService")
@Transactional
@RequiredArgsConstructor
public class ProviderCommandService {

  private final ProviderRepository providerRepository;
  private final ProviderParentLinkRepository providerParentLinkRepository;

  /** Persists a new or updated provider entity. */
  public ProviderEntity save(ProviderEntity provider) {
    return providerRepository.save(provider);
  }

  /**
   * Replaces all parent links for the given provider atomically.
   *
   * @param provider the provider whose parent links are being replaced
   * @param newLinks the replacement links (may be empty)
   */
  public void replaceParentLinks(ProviderEntity provider, List<ProviderParentLinkEntity> newLinks) {
    List<ProviderParentLinkEntity> existing = providerParentLinkRepository.findByProvider(provider);
    providerParentLinkRepository.deleteAll(existing);
    newLinks.forEach(providerParentLinkRepository::save);
  }

  /** Persists a single provider parent link entity. */
  public ProviderParentLinkEntity saveParentLink(ProviderParentLinkEntity link) {
    return providerParentLinkRepository.save(link);
  }
}
