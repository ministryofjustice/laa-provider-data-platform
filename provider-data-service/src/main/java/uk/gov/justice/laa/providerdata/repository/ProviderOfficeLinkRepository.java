package uk.gov.justice.laa.providerdata.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.providerdata.entity.ProviderOfficeLinkEntity;

/** Repository for ProviderOfficeLink entity. */
@Repository
public interface ProviderOfficeLinkRepository
    extends JpaRepository<ProviderOfficeLinkEntity, UUID> {}
