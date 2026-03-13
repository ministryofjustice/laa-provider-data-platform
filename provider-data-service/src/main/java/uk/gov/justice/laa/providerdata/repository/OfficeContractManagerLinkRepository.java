package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;

/** Repository for OfficeContractManagerLink entity. */
@Repository
public interface OfficeContractManagerLinkRepository
    extends JpaRepository<OfficeContractManagerLinkEntity, UUID> {

  List<OfficeContractManagerLinkEntity> findByOfficeGuid(@Param("officeGuid") UUID officeGuid);

  void deleteByOfficeGuid(@Param("officeGuid") UUID officeGuid);
}
