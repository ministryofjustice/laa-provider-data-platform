package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;

/** Repository for OfficeContractManagerLink entity. */
@Repository
public interface OfficeContractManagerLinkRepository
    extends JpaRepository<OfficeContractManagerLinkEntity, UUID> {

  List<OfficeContractManagerLinkEntity> findByOfficeGuid(UUID officeGuid);

  void deleteByOfficeGuid(UUID officeGuid);

  /**
   * Retrieves all contract manager links for a given office GUID.
   *
   * @param officeGuid the GUID of the office
   * @param providerGuid the GUID of the provider
   * @return list of OfficeContractManagerLinkEntity records
   */
  @Query(
      """
                  SELECT ocm
                  FROM OfficeContractManagerLinkEntity ocm
                  JOIN ocm.office po
                  JOIN ProviderOfficeLinkEntity pol ON pol.office = po
                  WHERE po.guid = :officeGuid
                  AND pol.provider.guid = :providerGuid
                  """)
  List<OfficeContractManagerLinkEntity> findByOfficeGuidAndProviderGuid(
      @Param("officeGuid") UUID officeGuid, @Param("providerGuid") UUID providerGuid);
}
