package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.AdvocateProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
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

  Optional<AdvocateProviderOfficeLinkEntity> findByProviderAndOffice_Guid(
      ProviderEntity provider, UUID officeGuid);

  Optional<AdvocateProviderOfficeLinkEntity> findByProviderAndAccountNumber(
      ProviderEntity provider, String accountNumber);

  List<AdvocateProviderOfficeLinkEntity> findByOffice(OfficeEntity office);

  Page<AdvocateProviderOfficeLinkEntity> findByOffice(OfficeEntity office, Pageable pageable);
}
