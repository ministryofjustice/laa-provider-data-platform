package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.OfficeContractManagerLinkEntity;

/** Repository for OfficeContractManagerLink entity. */
@Repository
public interface OfficeContractManagerLinkRepository
    extends JpaRepository<OfficeContractManagerLinkEntity, UUID> {

  @Query(
      """
            select l
            from OfficeContractManagerLinkEntity l
            where l.office.guid = :officeGuid
            """)
  List<OfficeContractManagerLinkEntity> findAllByOfficeGuid(@Param("officeGuid") UUID officeGuid);

  @Query(
      """
            select l
            from OfficeContractManagerLinkEntity l
            where l.office.guid = :officeGuid
            order by l.createdTimestamp desc
            """)
  Optional<OfficeContractManagerLinkEntity> findLatestByOfficeGuid(
      @Param("officeGuid") UUID officeGuid);

  @Modifying
  @Query(
      """
            delete from OfficeContractManagerLinkEntity l
            where l.office.guid = :officeGuid
            """)
  void deleteByOfficeGuid(@Param("officeGuid") UUID officeGuid);
}
