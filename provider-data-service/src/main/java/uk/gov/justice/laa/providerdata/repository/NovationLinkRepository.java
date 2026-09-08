package uk.gov.justice.laa.providerdata.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.NovationEntity;
import uk.gov.justice.laa.providerdata.entity.NovationLinkEntity;

/** Repository for NovationLink entity. */
@Repository
public interface NovationLinkRepository extends JpaRepository<NovationLinkEntity, UUID> {

  /** Finds all relationships belonging to the given Novation, in creation order. */
  List<NovationLinkEntity> findByNovationOrderByCreatedTimestampAsc(NovationEntity novation);
}
