package uk.gov.justice.laa.providerdata.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.ProviderParentLinkEntity;

/** Repository for ProviderParentLink entity. */
@Repository
public interface ProviderParentLinkRepository
    extends JpaRepository<ProviderParentLinkEntity, UUID> {}
