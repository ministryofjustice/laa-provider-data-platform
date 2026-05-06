package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;

/**
 * Repository for {@link AdvocateProviderOfficeLinkEntity}.
 *
 * <p>Because the entity type is an inheritance subtype, Hibernate automatically scopes all queries
 * to Advocate firm-office links via the discriminator column.
 */
@Repository
public interface AdvocateProviderOfficeLinkRepository
    extends JpaRepository<AdvocateProviderOfficeLinkEntity, UUID> {

  Optional<AdvocateProviderOfficeLinkEntity> findByProviderAndHeadOfficeFlagTrue(
      ProviderEntity provider);

  List<AdvocateProviderOfficeLinkEntity> findByProvider(ProviderEntity provider);

  List<AdvocateProviderOfficeLinkEntity> findByProviderAndActiveDateToIsNull(
      ProviderEntity provider);

  @Query(
      """
      SELECT COUNT(apl) > 0
      FROM AdvocateProviderOfficeLinkEntity apl
      WHERE apl.activeDateTo IS NULL
      AND EXISTS (
          SELECT 1 FROM ProviderParentLinkEntity ppl
          WHERE ppl.provider = apl.provider
          AND ppl.parent = :chambers
      )
      """)
  boolean existsActivePractitionerForChambers(@Param("chambers") ProviderEntity chambers);
}
