package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.LspProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;

/**
 * Repository for {@link LspProviderOfficeLinkEntity}.
 *
 * <p>Because the entity type is an inheritance subtype, Hibernate automatically scopes all queries
 * to LSP firm-office links via the discriminator column.
 */
@Repository
public interface LspProviderOfficeLinkRepository
    extends JpaRepository<LspProviderOfficeLinkEntity, UUID> {

  Page<LspProviderOfficeLinkEntity> findByProvider(ProviderEntity provider, Pageable pageable);

  Optional<LspProviderOfficeLinkEntity> findByProviderAndGuid(ProviderEntity provider, UUID guid);

  Optional<LspProviderOfficeLinkEntity> findByProviderAndAccountNumber(
      ProviderEntity provider, String accountNumber);

  Optional<LspProviderOfficeLinkEntity> findByProviderAndHeadOfficeFlagTrue(
      ProviderEntity provider);

  List<LspProviderOfficeLinkEntity> findByProviderAndHeadOfficeFlagFalseAndActiveDateToIsNull(
      ProviderEntity provider);
}
