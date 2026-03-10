package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.OfficeEntity;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;

/** Repository for OfficeLiaisonManagerLink entity. */
@Repository
public interface OfficeLiaisonManagerLinkRepository
    extends JpaRepository<OfficeLiaisonManagerLinkEntity, UUID> {

  /** Returns all active (no end date) liaison manager links for the given office. */
  List<OfficeLiaisonManagerLinkEntity> findByOfficeAndActiveDateToIsNull(OfficeEntity office);

  List<OfficeLiaisonManagerLinkEntity> findByOffice_Guid(UUID officeGuid);

  List<OfficeLiaisonManagerLinkEntity> findByOffice_GuidOrderByActiveDateFromDesc(UUID officeGuid);
}
