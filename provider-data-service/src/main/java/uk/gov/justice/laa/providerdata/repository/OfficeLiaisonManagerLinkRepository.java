package uk.gov.justice.laa.providerdata.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.OfficeLiaisonManagerLinkEntity;

/** Repository for OfficeLiaisonManagerLink entity. */
@Repository
public interface OfficeLiaisonManagerLinkRepository
    extends JpaRepository<OfficeLiaisonManagerLinkEntity, UUID> {}
