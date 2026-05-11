package uk.gov.justice.laa.providerdata.liaisonmanager.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.liaisonmanager.OfficeLiaisonManagerLinkEntity;
import uk.gov.justice.laa.providerdata.office.ProviderOfficeLinkEntity;

/** Repository for OfficeLiaisonManagerLink entity. */
@Repository
public interface OfficeLiaisonManagerLinkRepository
    extends JpaRepository<OfficeLiaisonManagerLinkEntity, UUID> {

  /** Returns all active (no end date) liaison manager links for the given office. */
  List<OfficeLiaisonManagerLinkEntity> findByOfficeLinkAndActiveDateToIsNull(
      ProviderOfficeLinkEntity office);

  List<OfficeLiaisonManagerLinkEntity> findByOfficeLink_Guid(UUID officeLinkGuid);

  Page<OfficeLiaisonManagerLinkEntity> findByOfficeLink_GuidOrderByActiveDateFromDesc(
      UUID officeLinkGuid, Pageable pageable);
}
