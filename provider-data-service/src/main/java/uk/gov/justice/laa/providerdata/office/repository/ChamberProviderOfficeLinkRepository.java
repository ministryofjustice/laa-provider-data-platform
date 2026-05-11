package uk.gov.justice.laa.providerdata.office.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.office.ChamberProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.provider.ProviderEntity;

/**
 * Repository for {@link ChamberProviderOfficeLinkEntity}.
 *
 * <p>Because the entity type is an inheritance subtype, Hibernate automatically scopes all queries
 * to Chambers firm-office links via the discriminator column.
 */
@Repository
public interface ChamberProviderOfficeLinkRepository
    extends JpaRepository<ChamberProviderOfficeLinkEntity, UUID> {

  Optional<ChamberProviderOfficeLinkEntity> findByProviderAndHeadOfficeFlagTrue(
      ProviderEntity provider);
}
