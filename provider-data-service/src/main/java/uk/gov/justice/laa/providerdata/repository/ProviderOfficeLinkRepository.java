package uk.gov.justice.laa.providerdata.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.ProviderEntity;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;

/** Repository for ProviderOfficeLink entity. */
@Repository
public interface ProviderOfficeLinkRepository
    extends JpaRepository<ProviderOfficeLinkEntity, UUID> {

  Page<ProviderOfficeLinkEntity> findByProvider(ProviderEntity provider, Pageable pageable);

  Optional<ProviderOfficeLinkEntity> findByProviderAndGuid(ProviderEntity provider, UUID guid);

  Optional<ProviderOfficeLinkEntity> findByProviderAndOffice_Guid(
      ProviderEntity provider, UUID officeGUID);

  Optional<ProviderOfficeLinkEntity> findByProviderAndAccountNumber(
      ProviderEntity provider, String accountNumber);

  Optional<ProviderOfficeLinkEntity> findByProvider_GuidAndOffice_Guid(
      UUID providerGuid, UUID officeGuid);

  Optional<ProviderOfficeLinkEntity> findByProvider_GuidAndAccountNumber(
      UUID providerGuid, String accountNumber);

  Optional<ProviderOfficeLinkEntity> findByProviderAndHeadOfficeFlagTrue(ProviderEntity provider);

  Page<ProviderOfficeLinkEntity> findByGuidInOrAccountNumberIn(
      Collection<UUID> guids, Collection<String> accountNumbers, Pageable pageable);

  List<ProviderOfficeLinkEntity> findAllByGuidInOrAccountNumberIn(
      Collection<UUID> guids, Collection<String> accountNumbers);

  Page<ProviderOfficeLinkEntity> findByProviderIn(
      Collection<ProviderEntity> providers, Pageable pageable);
}
