package uk.gov.justice.laa.providerdata.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.PdsProviderOfficeLinkEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;

/** Repository for Public Defender Service provider office links. */
@Repository
public interface PdsProviderOfficeLinkRepository
    extends JpaRepository<PdsProviderOfficeLinkEntity, UUID> {

  Optional<PdsProviderOfficeLinkEntity> findByProviderAndHeadOfficeFlagTrue(
      ProviderEntity provider);
}
